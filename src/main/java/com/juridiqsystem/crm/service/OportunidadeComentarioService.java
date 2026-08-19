package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Oportunidade;
import com.juridiqsystem.crm.model.OportunidadeComentario;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.OportunidadeComentarioDTO;
import com.juridiqsystem.crm.model.enums.TipoAnexoComentario;
import com.juridiqsystem.crm.repository.OportunidadeComentarioRepository;
import com.juridiqsystem.crm.repository.OportunidadeRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class OportunidadeComentarioService {

    /** Nome exibido ao usuário; o que passa disso é truncado em vez de rejeitar o envio. */
    private static final int MAX_NOME_ANEXO = 200;

    @Autowired
    private OportunidadeComentarioRepository oportunidadeComentarioRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private AnexoComentarioValidator anexoValidator;

    public Page<OportunidadeComentarioDTO> listar(String oportunidadeId, Pageable pageable) {
        Oportunidade oportunidade = oportunidadeRepository.findByPublicId(oportunidadeId)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + oportunidadeId + " nao encontrada"));
        Usuario autenticado = currentUser();
        return oportunidadeComentarioRepository.findByOportunidadeIdOrderByCriadoEmDesc(oportunidade.getId(), pageable)
                .map(comentario -> new OportunidadeComentarioDTO(comentario, podeExcluir(comentario, autenticado)));
    }

    /**
     * O upload ao S3 roda antes de abrir a transação, mesmo racional de {@link OportunidadeService#create}:
     * a conexão/transação JPA nunca fica presa esperando uma chamada de rede.
     */
    @Transactional
    public OportunidadeComentarioDTO criar(String oportunidadeId, String conteudo, MultipartFile file) {
        Oportunidade oportunidade = oportunidadeRepository.findByPublicId(oportunidadeId)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + oportunidadeId + " nao encontrada"));
        Usuario autor = currentUser();

        OportunidadeComentario comentario = new OportunidadeComentario(oportunidade.getId(), autor, conteudo);
        if (file != null && !file.isEmpty()) {
            TipoAnexoComentario tipo = anexoValidator.validar(file);
            String nomeExibicao = nomeParaExibicao(file.getOriginalFilename(), tipo);
            comentario.anexar(uploadArquivo(file, tipo, nomeExibicao), nomeExibicao, tipo.getContentType(), file.getSize());
        }

        OportunidadeComentario salvo = oportunidadeComentarioRepository.save(comentario);
        return new OportunidadeComentarioDTO(salvo, true);
    }

    @Transactional
    public void excluir(String comentarioId) {
        OportunidadeComentario comentario = oportunidadeComentarioRepository.findByPublicId(comentarioId)
                .orElseThrow(() -> new RuntimeException("Comentário com id " + comentarioId + " nao encontrado"));

        if (!podeExcluir(comentario, currentUser())) {
            throw new RuntimeException("Você não tem permissão para excluir este comentário");
        }

        if (comentario.getUrlAnexo() != null && !comentario.getUrlAnexo().isEmpty()) {
            s3Service.deleteFile(getFileKeyFromUrl(comentario.getUrlAnexo()));
        }
        oportunidadeComentarioRepository.delete(comentario);
    }

    private boolean podeExcluir(OportunidadeComentario comentario, Usuario autenticado) {
        if (autenticado == null) {
            return false;
        }
        if (comentario.getAutor().getId().equals(autenticado.getId())) {
            return true;
        }
        return autenticado.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Usuario currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuarioRepository.findById(usuario.getId()).orElse(usuario);
        }
        throw new RuntimeException("Usuário autenticado não encontrado");
    }

    private String getFileKeyFromUrl(String url) {
        int idx = url.indexOf("comentarios/");
        if (idx < 0) {
            throw new RuntimeException("URL de anexo inválida: " + url);
        }
        return url.substring(idx);
    }

    /**
     * A key é sempre UUID + extensão do tipo confirmado pelo servidor — o nome que o cliente
     * mandou nunca entra nela. Assim nome de arquivo malicioso não vira caminho no bucket nem
     * troca a extensão do objeto gravado.
     */
    private String uploadArquivo(MultipartFile file, TipoAnexoComentario tipo, String nomeExibicao) {
        String key = "comentarios/" + UUID.randomUUID() + tipo.getExtensao();
        return s3Service.uploadFile(file, key, tipo.getContentType(), contentDisposition(tipo, nomeExibicao));
    }

    /**
     * Documento desce como download em vez de renderizar no navegador; imagem e PDF abrem inline
     * porque é isso que o usuário espera ao clicar. O filename aqui vai só em ASCII — o nome
     * completo, com acento, fica no banco e é o que a tela exibe.
     */
    private String contentDisposition(TipoAnexoComentario tipo, String nomeExibicao) {
        if (tipo.isInline()) {
            return null;
        }
        String nomeAscii = nomeExibicao.replaceAll("[^A-Za-z0-9._-]", "_");
        return "attachment; filename=\"" + nomeAscii + "\"";
    }

    /**
     * O nome original é dado do usuário e vai ser renderizado de volta na tela: tira separador de
     * caminho (um "..\..\x.pdf" não pode virar caminho em lugar nenhum), tira caracteres de
     * controle e limita o tamanho. Garante também que a extensão exibida seja a do tipo real, para
     * o usuário não confiar num ".pdf" que na verdade é outra coisa.
     */
    private String nomeParaExibicao(String nomeOriginal, TipoAnexoComentario tipo) {
        String base = nomeOriginal == null ? "" : nomeOriginal;
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.substring(base.lastIndexOf('\\') + 1);
        base = base.replaceAll("\\p{Cntrl}", "").trim();

        int ponto = base.lastIndexOf('.');
        if (ponto > 0) {
            base = base.substring(0, ponto);
        }
        if (base.isBlank()) {
            base = "anexo";
        }
        if (base.length() > MAX_NOME_ANEXO) {
            base = base.substring(0, MAX_NOME_ANEXO);
        }
        return base + tipo.getExtensao();
    }
}
