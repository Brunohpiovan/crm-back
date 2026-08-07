package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.infra.security.TenantContext;
import br.edu.faculdadevincit.crm_vincit.infra.security.TokenService;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.*;
import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.DataIntegrityViolationException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UserNotFoundException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.AccessDeniedException;

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

    public UsuarioResponseNoAuthDto findByIdParaEdicao(String publicId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        Usuario usuario = usuarioRepository.findByPublicId(publicId).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));

        if (!isAdmin && !usuario.getLogin().equals(authenticatedUsername)) {
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
            throw new AccessDeniedException("Você não tem permissão para acessar este usuário.");
        }

        return new UsuarioResponseDto(usuario);
    }

    public UsuarioAllDTO save(UsuarioCreateDTO dto, MultipartFile foto) {
        validarDuplicidadeCriacao(dto.getLogin(), dto.getCpf());

        Usuario usuario = new Usuario(dto);
        String key = "user-avatar/" + dto.getNome().replaceAll("\\s+", "") + "pic";
        if (foto != null) {
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
    public void alterarSenhaPropria(AlterarSenhaDTO dto) {
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
        usuario.setAtualizadoEm(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    public LoginResponseDTO update(String publicId, UsuarioSelfUpdateDTO dto, MultipartFile foto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();

        Usuario usuarioDoBanco = buscarUsuarioPorPublicId(publicId);

        if (!usuarioDoBanco.getLogin().equals(authenticatedUsername)) {
            throw new AccessDeniedException("Você não tem permissão para alterar este usuário.");
        }

        aplicarAtualizacao(usuarioDoBanco, dto, foto);
        usuarioRepository.save(usuarioDoBanco);

        var token = tokenService.generateToken(usuarioDoBanco);
        return new LoginResponseDTO(token, null);
    }

    public UsuarioAllDTO updateAll(String publicId, UsuarioAdminUpdateDTO dto, MultipartFile foto) {
        Usuario usuarioDoBanco = buscarUsuarioPorPublicId(publicId);

        aplicarAtualizacao(usuarioDoBanco, dto, foto);
        usuarioDoBanco.setCargo(dto.getCargo());
        usuarioDoBanco.setBloqueado(dto.getBloqueado());
        usuarioRepository.save(usuarioDoBanco);

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
