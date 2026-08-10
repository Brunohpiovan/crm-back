package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Oportunidade;
import com.juridiqsystem.crm.model.OportunidadeComentario;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.OportunidadeComentarioDTO;
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

import java.util.Set;
import java.util.UUID;

@Service
public class OportunidadeComentarioService {

    private static final long MAX_SIZE_BYTES = 100L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif");

    @Autowired
    private OportunidadeComentarioRepository oportunidadeComentarioRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private S3Service s3Service;

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

        String urlAnexo = null;
        if (file != null && !file.isEmpty()) {
            validarArquivo(file);
            urlAnexo = uploadArquivo(file);
        }

        OportunidadeComentario comentario = new OportunidadeComentario(oportunidade.getId(), autor, conteudo, urlAnexo);
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

    private void validarArquivo(MultipartFile file) {
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new RuntimeException("O arquivo selecionado excede o limite de 100 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Tipo de arquivo não permitido. Envie uma imagem (PNG, JPG, JPEG, WEBP ou GIF).");
        }
    }

    private String uploadArquivo(MultipartFile file) {
        String key = "comentarios/" + UUID.randomUUID() + extrairExtensao(file.getOriginalFilename());
        return s3Service.uploadFile(file, key);
    }

    private String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal == null) {
            return "";
        }
        int idx = nomeOriginal.lastIndexOf('.');
        return idx >= 0 ? nomeOriginal.substring(idx) : "";
    }
}
