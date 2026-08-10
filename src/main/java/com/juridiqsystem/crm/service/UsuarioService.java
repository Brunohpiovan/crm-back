package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.security.TokenService;
import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.*;
import com.juridiqsystem.crm.model.enums.TipoParticipante;
import com.juridiqsystem.crm.model.enums.UserRole;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.DataIntegrityViolationException;
import com.juridiqsystem.crm.service.exceptions.UserNotFoundException;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ParticipanteService participanteService;

    @Autowired
    private SecurityLogger securityLogger;

    @Autowired
    private ImageContentValidator imageContentValidator;

    public UsuarioResponseNoAuthDto findByIdParaEdicao(String publicId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        Usuario usuario = usuarioRepository.findByPublicId(publicId).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));

        if (!isAdmin && !usuario.getLogin().equals(authenticatedUsername)) {
            securityLogger.log(SecurityEventType.RESOURCE_ACCESS_DENIED,
                    "Tentativa de acessar cadastro de outro usuário para edição: alvo=" + usuario.getLogin(),
                    authenticatedUsername, null, "/usuario/" + publicId + "/edicao");
            throw new AccessDeniedException("Você não tem permissão para acessar este usuário.");
        }

        return new UsuarioResponseNoAuthDto(usuario);
    }

    public List<UsuarioAllContactsDTO> findAllContacts(String userPublicId) {
        Usuario usuario = buscarUsuarioPorPublicId(userPublicId);
        return usuarioRepository.findAllContactsWithPrivateGroups(usuario.getId());
    }

    public List<UsuarioAllContactsDTO> findAllContactsDispo(String userPublicId) {
        Usuario usuario = buscarUsuarioPorPublicId(userPublicId);
        return usuarioRepository.findUsuariosSemGrupoPrivadoComum(usuario.getId());
    }

    public Page<UsuarioAllDTO> findAll(String search, Pageable pageable) {
        String termoBusca = (search == null || search.isBlank())
                ? null
                : "%" + search.trim().toLowerCase() + "%";
        return usuarioRepository.findAllPaginado(termoBusca, pageable);
    }

    public List<CriadorDto> findAllCriador() {
        return usuarioRepository.findAllCriadores();
    }

    public List<UsuarioAllDTO> findByAdmin() {
        return usuarioRepository.findResumoByCargo(UserRole.ADMINISTRADOR);
    }

    public UsuarioResponseDto findById(String publicId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        Usuario usuario = usuarioRepository.findByPublicId(publicId).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));

        if (!isAdmin && !usuario.getLogin().equals(authenticatedUsername)) {
            securityLogger.log(SecurityEventType.RESOURCE_ACCESS_DENIED,
                    "Tentativa de acessar cadastro de outro usuário: alvo=" + usuario.getLogin(),
                    authenticatedUsername, null, "/usuario/" + publicId);
            throw new AccessDeniedException("Você não tem permissão para acessar este usuário.");
        }

        return new UsuarioResponseDto(usuario);
    }

    public UsuarioAllDTO save(UsuarioCreateDTO dto, MultipartFile foto) {
        validarDuplicidadeCriacao(dto.getLogin(), dto.getCpf());

        Usuario usuario = new Usuario(dto);
        String key = "user-avatar/" + dto.getNome().replaceAll("\\s+", "") + "pic";
        if (foto != null) {
            imageContentValidator.validar(foto);
            String url = s3Service.uploadFile(foto, key);
            usuario.setUrlPicture(url);
        }
        if (usuario.getUrlPicture() == null || usuario.getUrlPicture().isEmpty()) {
            usuario.setUrlPicture("assets/img/avatar/padrao.jpeg");
        }
        usuario.setSenha(encoder.encode(usuario.getSenha()));

        Participante newParticipante = new Participante(usuario);
        newParticipante.setTipoParticipante(TipoParticipante.FUNCIONARIO);
        participanteService.create(newParticipante);

        usuario.setCriadoEm(LocalDateTime.now());
        usuario.setBloqueado(false);
        usuario = usuarioRepository.save(usuario);

        return new UsuarioAllDTO(usuario);
    }

    /**
     * Variantes usadas pelo MasterUsuarioController (/master/empresas/{empresaId}/usuarios):
     * o usuário master pertence à empresa interna, então essas operações "miram"
     * temporariamente a empresa-cliente informada via TenantContext.runAs antes de delegar
     * para os métodos normais acima — reaproveita 100% da lógica (incluindo a criação
     * automática do Participante em save()), sem duplicar nada.
     */
    public UsuarioAllDTO saveParaEmpresa(Long empresaId, UsuarioCreateDTO dto, MultipartFile foto) {
        return TenantContext.runAs(empresaId, () -> save(dto, foto));
    }

    public Page<UsuarioAllDTO> findAllParaEmpresa(Long empresaId, String search, Pageable pageable) {
        return TenantContext.runAs(empresaId, () -> findAll(search, pageable));
    }

    public UsuarioResponseNoAuthDto findByIdParaEdicaoMaster(Long empresaId, String publicId) {
        return TenantContext.runAs(empresaId, () -> new UsuarioResponseNoAuthDto(buscarUsuarioPorPublicId(publicId)));
    }

    public UsuarioAllDTO updateAllParaEmpresa(Long empresaId, String publicId, UsuarioAdminUpdateDTO dto, MultipartFile foto) {
        return TenantContext.runAs(empresaId, () -> updateAll(publicId, dto, foto));
    }

    public void deleteParaEmpresa(Long empresaId, String publicId) {
        TenantContext.runAs(empresaId, () -> {
            delete(publicId);
            return null;
        });
    }

    /**
     * Troca a senha do próprio usuário autenticado (usado pelo master, que não tem acesso à
     * tela normal de "Conta" nem ao PUT /usuario/{id} — esse último exige dados pessoais
     * completos e sincroniza um Participante que o master não possui). Não mexe em nenhum
     * outro campo do cadastro.
     */
    public LoginResponseDTO alterarSenhaPropria(AlterarSenhaDTO dto) {
        if (!dto.getNovaSenha().equals(dto.getNovaSenha2())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As senhas não coincidem");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();
        Usuario usuario = buscarUsuarioPorPublicId(usuarioAutenticado.getPublicId());

        if (!encoder.matches(dto.getSenhaAtual(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        }

        usuario.setSenha(encoder.encode(dto.getNovaSenha()));
        usuario.setSessaoVersao(usuario.getSessaoVersao() + 1);
        usuario.setAtualizadoEm(LocalDateTime.now());
        usuarioRepository.save(usuario);

        securityLogger.log(SecurityEventType.PASSWORD_CHANGED, null, usuario.getLogin(), null, "/usuario/senha");

        // sessaoVersao mudou (linha acima), então o token da requisição atual acabou de virar
        // inválido pro próprio SecurityFilter — sem devolver um novo aqui, o usuário seria
        // deslogado no ato de trocar a própria senha.
        String novoToken = tokenService.generateToken(usuario);
        return new LoginResponseDTO(novoToken, null);
    }

    public LoginResponseDTO update(String publicId, UsuarioSelfUpdateDTO dto, MultipartFile foto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();

        Usuario usuarioDoBanco = buscarUsuarioPorPublicId(publicId);

        if (!usuarioDoBanco.getLogin().equals(authenticatedUsername)) {
            securityLogger.log(SecurityEventType.RESOURCE_ACCESS_DENIED,
                    "Tentativa de editar cadastro de outro usuário: alvo=" + usuarioDoBanco.getLogin(),
                    authenticatedUsername, null, "/usuario/" + publicId);
            throw new AccessDeniedException("Você não tem permissão para alterar este usuário.");
        }

        boolean trocouSenha = dto.getSenha() != null && !dto.getSenha().isEmpty();

        aplicarAtualizacao(usuarioDoBanco, dto, foto);
        usuarioRepository.save(usuarioDoBanco);

        if (trocouSenha) {
            securityLogger.log(SecurityEventType.PASSWORD_CHANGED, null, usuarioDoBanco.getLogin(), null, "/usuario/" + publicId);
        }

        var token = tokenService.generateToken(usuarioDoBanco);
        return new LoginResponseDTO(token, null);
    }

    public UsuarioAllDTO updateAll(String publicId, UsuarioAdminUpdateDTO dto, MultipartFile foto) {
        Usuario usuarioDoBanco = buscarUsuarioPorPublicId(publicId);

        aplicarAtualizacao(usuarioDoBanco, dto, foto);
        usuarioDoBanco.setCargo(dto.getCargo());
        usuarioDoBanco.setBloqueado(dto.getBloqueado());
        usuarioRepository.save(usuarioDoBanco);

        securityLogger.log(SecurityEventType.ADMIN_ACTION,
                "Atualização administrativa de usuário-alvo=" + usuarioDoBanco.getLogin()
                        + "; cargo=" + dto.getCargo() + "; bloqueado=" + dto.getBloqueado(),
                currentActorLogin(), null, "/usuario/all/" + publicId);

        return new UsuarioAllDTO(usuarioDoBanco);
    }

    private Usuario buscarUsuarioPorPublicId(String publicId) {
        return usuarioRepository.findByPublicId(publicId)
                .orElseThrow(() -> new UserNotFoundException("Usuario nao encontrado"));
    }

    /**
     * Aplica os dados pessoais comuns (nome, login, senha, documentos, endereço) e sincroniza o
     * Participante espelhado. Cargo e bloqueado, quando aplicável, são responsabilidade do chamador
     * (só a atualização administrativa pode alterá-los).
     */
    private void aplicarAtualizacao(Usuario usuarioDoBanco, UsuarioDadosPessoaisDTO dto, MultipartFile foto) {
        validatePassword(dto);
        validarDuplicidadeAtualizacao(dto.getLogin(), dto.getCpf(), usuarioDoBanco.getId());

        String loginAntigo = usuarioDoBanco.getLogin();
        String urlPicture = resolveUrlPicture(usuarioDoBanco, dto, foto);
        preencherDadosPessoais(dto, usuarioDoBanco);
        usuarioDoBanco.setUrlPicture(urlPicture);

        sincronizarParticipante(usuarioDoBanco, loginAntigo);

        usuarioDoBanco.setAtualizadoEm(LocalDateTime.now());
    }

    private void validatePassword(UsuarioDadosPessoaisDTO dto) {
        if (dto.getSenha() != null && !dto.getSenha().isEmpty()) {
            if (!dto.getSenha().equals(dto.getSenha2())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As senhas não coincidem");
            }
        }
    }

    private String resolveUrlPicture(Usuario usuarioDoBanco, UsuarioDadosPessoaisDTO dto, MultipartFile foto) {
        String urlPictureSolicitada = dto.getUrlPicture();

        if (usuarioDoBanco.getUrlPicture().contains(s3Service.getBaseUrl()) && foto != null) {
            deleteOldPhoto(usuarioDoBanco);
            urlPictureSolicitada = "assets/img/avatar/padrao.jpeg";
        }
        if (foto == null && "assets/img/avatar/padrao.jpeg".equals(urlPictureSolicitada)) {
            deleteOldPhoto(usuarioDoBanco);
        }
        if (foto != null) {
            imageContentValidator.validar(foto);
            String key = generatePhotoKey(dto.getNome());
            return s3Service.uploadFile(foto, key);
        }
        return urlPictureSolicitada;
    }

    private void deleteOldPhoto(Usuario usuarioDoBanco) {
        String url = usuarioDoBanco.getUrlPicture();
        String key = url.substring(url.indexOf(".com/") + 5);
        this.s3Service.deleteFile(key);
    }

    private String generatePhotoKey(String nome) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return "user-avatar/" + nome.replaceAll("\\s+", "") + "pic_" + timestamp;
    }

    @Transactional
    public void delete(String publicId) {
        Usuario usuarioDoBanco = buscarUsuarioPorPublicId(publicId);
        // Hard delete violaria as FKs de protocolo, e-mail, oportunidade, mensagem interna,
        // log de acesso etc. (ON DELETE RESTRICT). Excluir passa a inativar o usuário,
        // preservando o histórico e impedindo login (ver AuthenticationService).
        usuarioDoBanco.setBloqueado(true);
        usuarioDoBanco.setAtualizadoEm(LocalDateTime.now());
        usuarioRepository.save(usuarioDoBanco);

        securityLogger.log(SecurityEventType.ADMIN_ACTION, "Usuário inativado (bloqueado)=" + usuarioDoBanco.getLogin(),
                currentActorLogin(), null, "/usuario/" + publicId);
    }

    /**
     * Login de quem está fazendo a chamada, pra registrar nos eventos ADMIN_ACTION quem executou
     * a ação administrativa (diferente do usuário-alvo, que pode ser outra pessoa).
     */
    private String currentActorLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    private void sincronizarParticipante(Usuario usuario, String loginAntigo) {
        Participante participante = participanteService.findByLoginSystem(loginAntigo);
        Participante novoParticipante = preencheParticipanteEntity(usuario, participante);
        participanteService.update(novoParticipante, participante.getId());
    }

    private Participante preencheParticipanteEntity(Usuario usuario, Participante participanteBanco) {
        participanteBanco.setNome(usuario.getNome());
        participanteBanco.setLogin(usuario.getLogin().toLowerCase());
        participanteBanco.setUrlPicture(usuario.getUrlPicture());
        participanteBanco.setRg(usuario.getRg());
        participanteBanco.setCpf(usuario.getCpf());
        participanteBanco.setDataNascimento(usuario.getDataNascimento());
        participanteBanco.setCelular(usuario.getCelular());
        participanteBanco.setEndereco(usuario.getEndereco());
        participanteBanco.setNumeroResidencial(usuario.getNumeroResidencial());
        participanteBanco.setComplemento(usuario.getComplemento());
        participanteBanco.setBairro(usuario.getBairro());
        participanteBanco.setUf(usuario.getUf());
        participanteBanco.setCidade(usuario.getCidade());
        participanteBanco.setObservacoes(usuario.getObservacoes());
        return participanteBanco;
    }

    private Usuario preencherDadosPessoais(UsuarioDadosPessoaisDTO dto, Usuario usuario) {
        usuario.setNome(dto.getNome());
        usuario.setLogin(dto.getLogin().toLowerCase());
        if (dto.getSenha() != null && !dto.getSenha().isEmpty()) {
            usuario.setSenha(encoder.encode(dto.getSenha()));
            // Revoga sessões antigas desse usuário (ver SecurityFilter). Em self-update (PUT
            // /usuario/{id}) o chamador é o próprio usuário e ganha um token novo logo depois,
            // no fim de update(); em admin-update (PUT /usuario/all/{id}) quem muda é um admin
            // trocando a senha de outra pessoa, então só a sessão do usuário-alvo é revogada.
            usuario.setSessaoVersao(usuario.getSessaoVersao() + 1);
        }
        usuario.setRg(dto.getRg());
        usuario.setCpf(dto.getCpf());
        usuario.setDataNascimento(dto.getDataNascimento());
        usuario.setCelular(dto.getCelular());
        usuario.setEndereco(dto.getEndereco());
        usuario.setNumeroResidencial(dto.getNumeroResidencial());
        usuario.setComplemento(dto.getComplemento());
        usuario.setBairro(dto.getBairro());
        usuario.setUf(dto.getUf());
        usuario.setCidade(dto.getCidade());
        usuario.setObservacoes(dto.getObservacoes());
        usuario.setCep(dto.getCep());
        return usuario;
    }

    private void validarDuplicidadeCriacao(String login, String cpf) {
        if (usuarioRepository.existsByLoginIgnoreCase(login.toLowerCase())) {
            throw new DataIntegrityViolationException("E-mail já cadastrado no sistema.");
        }
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new DataIntegrityViolationException("CPF já cadastrado no sistema.");
        }
    }

    private void validarDuplicidadeAtualizacao(String login, String cpf, Long idAtual) {
        if (usuarioRepository.existsByLoginIgnoreCaseAndIdNot(login.toLowerCase(), idAtual)) {
            throw new DataIntegrityViolationException("E-mail já cadastrado no sistema.");
        }
        if (usuarioRepository.existsByCpfAndIdNot(cpf, idAtual)) {
            throw new DataIntegrityViolationException("CPF já cadastrado no sistema.");
        }
    }
}
