package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ChatGrupoResponseById;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ChatGrupoResponseDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.GrupoCreateDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import br.edu.faculdadevincit.crm_vincit.repository.ChatGrupoRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatGrupoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ChatGrupoRepository chatGrupoRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;


    @Autowired
    private CloudFrontService cloudFrontService;

    public Optional<Long> getGrupoByUsuario(Long id_usuario1, Long id_usuario2) {
        Optional<Usuario> usuario1 = usuarioRepository.findById(id_usuario1);
        Optional<Usuario> usuario2 = usuarioRepository.findById(id_usuario2);

        if (usuario1.isEmpty() || usuario2.isEmpty()) {
            return Optional.empty();
        }

        return chatGrupoRepository.findGrupoPrivadoByUsuarios(usuario1.get().getId(), usuario2.get().getId())
                .map(ChatGrupo::getId);
    }


    public Optional<List<ChatGrupoResponseDTO>> getGrupoByUsuarioAndPublic(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        List<ChatGrupo> chatGrupos = chatGrupoRepository.findGruposPublicosByUsuario(usuario.getId());

        if (chatGrupos == null || chatGrupos.isEmpty()) {
            return Optional.empty();
        }

        List<ChatGrupoResponseDTO> dtoList = chatGrupos.stream().map(grupo -> {
            String avatarUrl = grupo.getAvatarUrl();
            if (avatarUrl != null && avatarUrl.contains(cloudFrontService.getBaseUrl())) {
                avatarUrl = cloudFrontService.generateSignedUrl(avatarUrl, Duration.ofMinutes(30));
            }

            String imagemFundoUrl = grupo.getImagemFundoUrl();
            if (imagemFundoUrl != null && imagemFundoUrl.contains(cloudFrontService.getBaseUrl())) {
                imagemFundoUrl = cloudFrontService.generateSignedUrl(imagemFundoUrl, Duration.ofMinutes(30));
            }

            return new ChatGrupoResponseDTO(
                    grupo.getId(),
                    grupo.getNome(),
                    avatarUrl,
                    imagemFundoUrl,
                    grupo.getPrivado()
            );
        }).collect(Collectors.toList());

        return Optional.of(dtoList);
    }



    public void create(GrupoCreateDTO dto, MultipartFile foto, MultipartFile imagemFundo) {
        ChatGrupo grupo = dtoToModel(dto);

        if (foto != null) {
            String key = "img-grupo/" + dto.getNome().replaceAll("\\s+", "") + "pic";
            String url = s3Service.uploadFile(foto, key);
            grupo.setAvatarUrl(url);
        }

        if (grupo.getAvatarUrl() == null || grupo.getAvatarUrl().isEmpty()) {
            grupo.setAvatarUrl("assets/img/avatar/grupo3.jpg");
        }

        if (imagemFundo != null) {
            String backgroundKey = "img-grupo/" + dto.getNome().replaceAll("\\s+", "") + "background";
            String backgroundUrl = s3Service.uploadFile(imagemFundo, backgroundKey);
            grupo.setImagemFundoUrl(backgroundUrl); 
        }

        grupo.setPrivado(false);
        grupo.setCriadoEm(LocalDateTime.now());

        ChatGrupoResponseDTO grupoDTO = new ChatGrupoResponseDTO(chatGrupoRepository.save(grupo));
        ChatGrupoResponseById idUsers = new ChatGrupoResponseById(grupo);
        for (Long userId : idUsers.getUsuarios()) {
            messagingTemplate.convertAndSend("/topic/newPublicGroup/" + userId, grupoDTO);
        }
    }


    public void update(Long id,GrupoCreateDTO dto, MultipartFile foto,MultipartFile imagemFundo) {
        ChatGrupo newgrupo = dtoToModel(dto);
        ChatGrupo grupo = chatGrupoRepository.findById(id).orElseThrow(()->new RuntimeException("Grupo nao encontrado"));
        if(foto == null && newgrupo.getAvatarUrl().contains("assets/img/avatar/grupo3.jpg") && grupo.getAvatarUrl().contains(bucketName)){
            String keyAntiga = grupo.getAvatarUrl().replace(cloudFrontService.getBaseUrl() + "/", "");
            s3Service.deleteFile(keyAntiga);
            grupo.setAvatarUrl("assets/img/avatar/grupo3.jpg");
        }
        if(foto!=null && grupo.getAvatarUrl().contains(cloudFrontService.getBaseUrl())){
            String keyAntiga = grupo.getAvatarUrl().replace(cloudFrontService.getBaseUrl() + "/", "");
            s3Service.deleteFile(keyAntiga);
        }

        if (foto != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(formatter);
            String keyNova = "img-grupo/" + dto.getNome().replaceAll("\\s+", "") + "pic_"+timestamp;
            String urlNova = s3Service.uploadFile(foto, keyNova);
            grupo.setAvatarUrl(urlNova);
        }

        if (imagemFundo == null && newgrupo.getImagemFundoUrl() == null && grupo.getImagemFundoUrl() != null && grupo.getImagemFundoUrl().contains(bucketName)) {
            String keyAntiga = grupo.getImagemFundoUrl().replace(cloudFrontService.getBaseUrl() + "/", "");
            s3Service.deleteFile(keyAntiga);
            grupo.setImagemFundoUrl(null);
        }
        if (imagemFundo != null && grupo.getImagemFundoUrl() != null && grupo.getImagemFundoUrl().contains(bucketName)) {
            String keyAntiga = grupo.getImagemFundoUrl().replace(cloudFrontService.getBaseUrl() + "/", "");
            s3Service.deleteFile(keyAntiga);
        }
        if (imagemFundo != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(formatter);
            String keyNova = "img-grupo-background/" + dto.getNome().replaceAll("\\s+", "") + "_back_" + timestamp;
            String urlNova = s3Service.uploadFile(imagemFundo, keyNova);
            grupo.setImagemFundoUrl(urlNova);
        }


        List<Usuario> usuariosAntigos = grupo.getUsuarios();
        List<Long> novosUsuarios = dto.getUsuarios();
        List<Long> usuariosRemovidos = new ArrayList<>();
        for (Usuario usuario : usuariosAntigos) {
            Long usuarioId = usuario.getId();
            if (!novosUsuarios.contains(usuarioId)) {
                usuariosRemovidos.add(usuarioId);
            }
        }
        grupo.setNome(newgrupo.getNome());
        grupo.setPrivado(false);
        grupo.setUsuarios(newgrupo.getUsuarios());
        grupo.setAtualizadoEm(LocalDateTime.now());
        ChatGrupoResponseById idUsers = new ChatGrupoResponseById(grupo);
        ChatGrupoResponseDTO grupoDTO = new ChatGrupoResponseDTO(chatGrupoRepository.save(grupo));
        for (Long userId : idUsers.getUsuarios()) {
            messagingTemplate.convertAndSend("/topic/attPublicGroup/" + userId, grupoDTO);
        }
        for (Long userId : usuariosRemovidos) {
            messagingTemplate.convertAndSend("/topic/delPublicGroup/" + userId, grupoDTO.getId());
        }

    }

    private ChatGrupo dtoToModel(GrupoCreateDTO dto){
        ChatGrupo grupo = new ChatGrupo();

        grupo.setId(dto.getId());
        grupo.setNome(dto.getNome());
        grupo.setAvatarUrl(dto.getUrlPicture());
        List<Usuario> listaUsuarios = new ArrayList<>();
        for (Long usuarioId : dto.getUsuarios()) {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário com ID " + usuarioId + " não encontrado"));
            listaUsuarios.add(usuario);
        }
        grupo.setUsuarios(listaUsuarios);
        grupo.setImagemFundoUrl(dto.getBackgroundImageUrl());
        return grupo;
    }

    public ChatGrupoResponseById findById(Long id) {
        ChatGrupo grupo = chatGrupoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("grupo nao encontrado"));

        String avatarUrl = grupo.getAvatarUrl();
        if (avatarUrl != null && avatarUrl.contains(cloudFrontService.getBaseUrl())) {
            avatarUrl = cloudFrontService.generateSignedUrl(avatarUrl, Duration.ofMinutes(30));
        }

        String imagemFundoUrl = grupo.getImagemFundoUrl();
        if (imagemFundoUrl != null && imagemFundoUrl.contains(cloudFrontService.getBaseUrl())) {
            imagemFundoUrl = cloudFrontService.generateSignedUrl(imagemFundoUrl, Duration.ofMinutes(30));
        }

        return new ChatGrupoResponseById(
                grupo.getId(),
                grupo.getNome(),
                avatarUrl,
                imagemFundoUrl,
                grupo.getUsuarios().stream().map(Usuario::getId).toList()
        );
    }

}
