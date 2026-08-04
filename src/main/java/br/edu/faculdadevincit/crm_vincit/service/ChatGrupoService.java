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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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

    public Optional<Long> getGrupoByUsuario(Long id_usuario1, Long id_usuario2) {
        Optional<Usuario> usuario1 = usuarioRepository.findById(id_usuario1);
        Optional<Usuario> usuario2 = usuarioRepository.findById(id_usuario2);

        if (usuario1.isEmpty() || usuario2.isEmpty()) {
            return Optional.empty();
        }

        return chatGrupoRepository.findGrupoPrivadoByUsuarios(usuario1.get().getId(), usuario2.get().getId())
                .stream()
                .min(Comparator.comparing(ChatGrupo::getCriadoEm))
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
            String imagemFundoUrl = grupo.getImagemFundoUrl();

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

        boolean anexosEnviadosAoS3 = foto != null || imagemFundo != null;
        ChatGrupoResponseDTO grupoDTO = new ChatGrupoResponseDTO(salvarGrupo(grupo, anexosEnviadosAoS3));
        ChatGrupoResponseById idUsers = new ChatGrupoResponseById(grupo);
        for (Long userId : idUsers.getUsuarios()) {
            messagingTemplate.convertAndSend("/topic/newPublicGroup/" + userId, grupoDTO);
        }
    }


    public void update(Long id,GrupoCreateDTO dto, MultipartFile foto,MultipartFile imagemFundo) {
        ChatGrupo newgrupo = dtoToModel(dto);
        ChatGrupo grupo = chatGrupoRepository.findById(id).orElseThrow(()->new RuntimeException("Grupo nao encontrado"));
        if(foto == null && newgrupo.getAvatarUrl().contains("assets/img/avatar/grupo3.jpg") && grupo.getAvatarUrl().contains(bucketName)){
            String keyAntiga = grupo.getAvatarUrl().replace(s3Service.getBaseUrl() + "/", "");
            s3Service.deleteFile(keyAntiga);
            grupo.setAvatarUrl("assets/img/avatar/grupo3.jpg");
        }
        if(foto!=null && grupo.getAvatarUrl().contains(s3Service.getBaseUrl())){
            String keyAntiga = grupo.getAvatarUrl().replace(s3Service.getBaseUrl() + "/", "");
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
            String keyAntiga = grupo.getImagemFundoUrl().replace(s3Service.getBaseUrl() + "/", "");
            s3Service.deleteFile(keyAntiga);
            grupo.setImagemFundoUrl(null);
        }
        if (imagemFundo != null && grupo.getImagemFundoUrl() != null && grupo.getImagemFundoUrl().contains(bucketName)) {
            String keyAntiga = grupo.getImagemFundoUrl().replace(s3Service.getBaseUrl() + "/", "");
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
        boolean anexosAlteradosNoS3 = foto != null || imagemFundo != null;
        ChatGrupoResponseDTO grupoDTO = new ChatGrupoResponseDTO(salvarGrupo(grupo, anexosAlteradosNoS3));
        for (Long userId : idUsers.getUsuarios()) {
            messagingTemplate.convertAndSend("/topic/attPublicGroup/" + userId, grupoDTO);
        }
        for (Long userId : usuariosRemovidos) {
            messagingTemplate.convertAndSend("/topic/delPublicGroup/" + userId, grupoDTO.getId());
        }

    }

    /**
     * Os uploads/deletes no S3 acima já aconteceram quando este método é chamado. Se o save no
     * banco falhar depois, o S3 já está no estado novo mas o grupo não foi persistido — logamos
     * para deixar rastro dessa inconsistência (regra: não esconder erro, mas também não perder o
     * registro de que o anexo já mudou no S3) e propagamos a exceção normalmente.
     */
    private ChatGrupo salvarGrupo(ChatGrupo grupo, boolean anexosAlteradosNoS3) {
        try {
            return chatGrupoRepository.save(grupo);
        } catch (RuntimeException e) {
            if (anexosAlteradosNoS3) {
                log.error("Grupo '{}' teve avatar/imagem de fundo alterados no S3, mas a persistência no banco falhou. avatarUrl={}, imagemFundoUrl={}",
                        grupo.getNome(), grupo.getAvatarUrl(), grupo.getImagemFundoUrl(), e);
            }
            throw e;
        }
    }

    private ChatGrupo dtoToModel(GrupoCreateDTO dto){
        ChatGrupo grupo = new ChatGrupo();

        grupo.setId(dto.getId());
        grupo.setNome(dto.getNome());
        grupo.setAvatarUrl(dto.getUrlPicture());
        grupo.setUsuarios(buscarUsuarios(dto.getUsuarios()));
        grupo.setImagemFundoUrl(dto.getBackgroundImageUrl());
        return grupo;
    }

    private List<Usuario> buscarUsuarios(List<Long> usuarioIds) {
        List<Usuario> usuarios = usuarioRepository.findAllById(usuarioIds);
        if (usuarios.size() != new HashSet<>(usuarioIds).size()) {
            Set<Long> encontrados = usuarios.stream().map(Usuario::getId).collect(Collectors.toSet());
            List<Long> faltando = usuarioIds.stream().filter(id -> !encontrados.contains(id)).toList();
            throw new EntityNotFoundException("Usuário(s) com ID " + faltando + " não encontrado(s)");
        }
        return usuarios;
    }

    public ChatGrupoResponseById findById(Long id) {
        ChatGrupo grupo = chatGrupoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("grupo nao encontrado"));

        String avatarUrl = grupo.getAvatarUrl();
        String imagemFundoUrl = grupo.getImagemFundoUrl();

        return new ChatGrupoResponseById(
                grupo.getId(),
                grupo.getNome(),
                avatarUrl,
                imagemFundoUrl,
                grupo.getUsuarios().stream().map(Usuario::getId).toList()
        );
    }

}
