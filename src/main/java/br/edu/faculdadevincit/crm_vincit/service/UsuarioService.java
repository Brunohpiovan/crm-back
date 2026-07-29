package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.infra.security.TokenService;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.*;
import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.AcessoRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.DataIntegrityViolationException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UserNotFoundException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.AccessDeniedException;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private AcessoRepository acessoRepository;


    @Autowired
    private ParticipanteService participanteService;

    @Autowired
    private CloudFrontService cloudFrontService;

    public UsuarioResponseNoAuthDto findByIdNoAuth(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));
        if (usuario.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {
            String signedUrl = cloudFrontService.generateSignedUrl(usuario.getUrlPicture(), Duration.ofMinutes(60));
            usuario.setUrlPicture(signedUrl);
        }
        return new UsuarioResponseNoAuthDto(usuario);
    }

    public List<UsuarioAllContactsDTO> findAllContacts(Long userId) {
        List<Usuario> usuarios = usuarioRepository.findAllContactsWithPrivateGroups(userId);

        return usuarios.stream()
                .map(usuario -> {
                    UsuarioAllContactsDTO dto = new UsuarioAllContactsDTO(usuario);

                    if (usuario.getUrlPicture() != null && usuario.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {
                        String signedUrl = cloudFrontService.generateSignedUrl(
                                usuario.getUrlPicture(),
                                Duration.ofMinutes(60)
                        );
                        dto.setUrlPicture(signedUrl);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }


    public List<UsuarioAllContactsDTO> findAllContactsDispo(Long userId) {
        List<Usuario> usuarios = usuarioRepository.findUsuariosSemGrupoPrivadoComum(userId);

        return usuarios.stream()
                .map(usuario -> {
                    UsuarioAllContactsDTO dto = new UsuarioAllContactsDTO(usuario);

                    if (usuario.getUrlPicture() != null && usuario.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {
                        String signedUrl = cloudFrontService.generateSignedUrl(
                                usuario.getUrlPicture(),
                                Duration.ofMinutes(60)
                        );
                        dto.setUrlPicture(signedUrl);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }



    public List<UsuarioAllDTO> findAll() {
        List<Usuario> usuarios = usuarioRepository.findAll();

        List<UsuarioAllDTO> usuarioResponseDtos = usuarios.stream()
                .map(UsuarioAllDTO::new)
                .collect(Collectors.toList());

        return usuarioResponseDtos;
    }

    public List<CriadorDto> findAllCriador() {
        List<Usuario> usuarios = usuarioRepository.findAll();

        return usuarios.stream()
                .map(usuario -> {
                    CriadorDto dto = new CriadorDto(usuario);

                    if (usuario.getUrlPicture() != null && usuario.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {
                        String signedUrl = cloudFrontService.generateSignedUrl(
                                usuario.getUrlPicture(),
                                Duration.ofMinutes(60)
                        );
                        dto.setUrlPicture(signedUrl);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }



    public List<UsuarioAllDTO> findByAdmin() {
        return usuarioRepository.findByCargo(UserRole.ADMINISTRADOR).stream()
                .map(UsuarioAllDTO::new)
                .collect(Collectors.toList());
    }

    public UsuarioResponseDto findById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();

        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));

        if (!usuario.getLogin().equals(authenticatedUsername)) {
            throw new AccessDeniedException("Você não tem permissão para acessar este usuário.");
        }

        UsuarioResponseDto resposta = new UsuarioResponseDto(usuario);

        if (usuario.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {
            String signedUrl = cloudFrontService.generateSignedUrl(usuario.getUrlPicture(), Duration.ofMinutes(60));
            resposta.setUrlPicture(signedUrl);
        }

        return resposta;
    }

    public UsuarioAllDTO save(UsuarioDTO dto,MultipartFile foto) {
        Usuario usuario = new Usuario(dto);
        String key = "user-avatar/" + dto.getNome().replaceAll("\\s+", "") + "pic";
        if(foto!=null){
            String url = s3Service.uploadFile(foto,key);
            usuario.setUrlPicture(url);
        }
        if(usuario.getUrlPicture()==null || usuario.getUrlPicture().isEmpty()){
            usuario.setUrlPicture("assets/img/avatar/padrao.jpeg");
        }
        usuario.setId(null);
        usuario.setSenha(encoder.encode(usuario.getSenha()));
        this.validaPorCpfeEmail(usuario, usuario.getId());
        Participante newParticipante = new Participante(usuario);
        newParticipante.setTipoParticipante(TipoParticipante.FUNCIONARIO);

        participanteService.create(newParticipante);
        usuario.setCriadoEm(LocalDateTime.now());
        usuario.setBloqueado(false);
        usuario = usuarioRepository.save(usuario);
        if (usuario.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {
            String signedUrl = cloudFrontService.generateSignedUrl(usuario.getUrlPicture(), Duration.ofMinutes(60));
            usuario.setUrlPicture(signedUrl);
        }
        return new UsuarioAllDTO(usuario);
    }

    public LoginResponseDTO update(Long id, UsuarioDTO dto, MultipartFile foto) {
        Object result = updateUser (id, dto, foto, true);
        return (LoginResponseDTO) result;
    }

    public UsuarioAllDTO updateAll(Long id, UsuarioDTO dto, MultipartFile foto) {
        Object result = updateUser (id, dto, foto, false);
        Usuario user = (Usuario) result;
        if (user.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {
            String signedUrl = cloudFrontService.generateSignedUrl(user.getUrlPicture(), Duration.ofMinutes(60));
            user.setUrlPicture(signedUrl);
        }
        return new UsuarioAllDTO(user);
    }

    private Object updateUser (Long id, UsuarioDTO dto, MultipartFile foto, boolean generateToken) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        if (usuarioOptional.isEmpty()) {
            throw new UserNotFoundException("Usuario nao encontrado");
        }

        validatePassword(dto);

        Usuario usuarioDoBanco = usuarioOptional.get();
        validateCpfAndEmail(dto, usuarioDoBanco, id);

        handlePhotoUpdate(usuarioDoBanco, dto, foto);

        Usuario newUser  = preencheUsuarioEntity(dto, usuarioDoBanco);
        Participante participante = participanteService.findByLoginSystem(usuarioDoBanco.getLogin());
        Participante newParticipante = preencheParticipanteEntity(newUser , participante);
        participanteService.update(newParticipante, participante.getId());

        newUser.setAtualizadoEm(LocalDateTime.now());
        usuarioRepository.save(newUser );

        if (generateToken) {
            var token = tokenService.generateToken(newUser );
            return new LoginResponseDTO(token, null);
        }

        return newUser ;
    }

    private void validatePassword(UsuarioDTO dto) {
        if (dto.getSenha() != null && !dto.getSenha().isEmpty()) {
            if (!dto.getSenha().equals(dto.getSenha2())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As senhas não coincidem");
            }
        }
    }

    private void validateCpfAndEmail(UsuarioDTO dto, Usuario usuarioDoBanco, Long id) {
        Usuario valida = new Usuario(dto);
        if (!Objects.equals(usuarioDoBanco.getCpf(), dto.getCpf())
                || !Objects.equals(usuarioDoBanco.getLogin(), dto.getLogin())) {
            this.validaPorCpfeEmail(valida, id);
        }
    }

    private void handlePhotoUpdate(Usuario usuarioDoBanco, UsuarioDTO dto, MultipartFile foto) {
        if (usuarioDoBanco.getUrlPicture().contains(cloudFrontService.getBaseUrl()) && foto != null) {
            deleteOldPhoto(usuarioDoBanco);
            dto.setUrlPicture("assets/img/avatar/padrao.jpeg");
        }
        if (foto == null && dto.getUrlPicture().equals("assets/img/avatar/padrao.jpeg")) {
            deleteOldPhoto(usuarioDoBanco);
        }
        if (foto != null) {
            String key = generatePhotoKey(dto.getNome());
            String url = s3Service.uploadFile(foto, key);
            dto.setUrlPicture(url);
        }
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
    public void delete(Long id) {
        Usuario usuarioDoBanco = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        usuarioRepository.delete(usuarioDoBanco);
    }

    private Participante preencheParticipanteEntity(Usuario usuario,Participante participanteBanco){
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

    private Usuario preencheUsuarioEntity(UsuarioDTO usuarioRequest, Usuario usuario) {
        usuario.setNome(usuarioRequest.getNome());
        usuario.setLogin(usuarioRequest.getLogin().toLowerCase());
        usuario.setUrlPicture(usuarioRequest.getUrlPicture());
        if(usuarioRequest.getSenha()!=null){
            usuario.setSenha(encoder.encode(usuarioRequest.getSenha()));
        }
        usuario.setRg(usuarioRequest.getRg());
        usuario.setCpf(usuarioRequest.getCpf());
        usuario.setDataNascimento(usuarioRequest.getDataNascimento());
        usuario.setCelular(usuarioRequest.getCelular());
        if(usuarioRequest.getCargo()!=null){
            usuario.setCargo(usuarioRequest.getCargo());
        }
        usuario.setEndereco(usuarioRequest.getEndereco());
        usuario.setNumeroResidencial(usuarioRequest.getNumeroResidencial());
        usuario.setComplemento(usuarioRequest.getComplemento());
        usuario.setBairro(usuarioRequest.getBairro());
        usuario.setUf(usuarioRequest.getUf());
        usuario.setCidade(usuarioRequest.getCidade());
        usuario.setObservacoes(usuarioRequest.getObservacoes());
        usuario.setCep(usuarioRequest.getCep());
        usuario.setBloqueado(usuarioRequest.getBloqueado());
        return usuario;
    }

    private void validaPorCpfeEmail(Usuario objDTO, Long idAtual) {
        Optional<UserDetails> usuarioComMesmoLogin = usuarioRepository.findByLogin(objDTO.getLogin().toLowerCase());
        if (usuarioComMesmoLogin.isPresent()) {
            Usuario usuario = (Usuario) usuarioComMesmoLogin.get();
            if (!usuario.getId().equals(idAtual)) {
                throw new DataIntegrityViolationException("E-mail já cadastrado no sistema.");
            }
        }

        Optional<UserDetails> usuarioComMesmoCpf = usuarioRepository.findByCpf(objDTO.getCpf());
        if (usuarioComMesmoCpf.isPresent()) {
            Usuario usuario = (Usuario) usuarioComMesmoCpf.get();
            if (!usuario.getId().equals(idAtual)) {
                throw new DataIntegrityViolationException("CPF já cadastrado no sistema.");
            }
        }
    }
}