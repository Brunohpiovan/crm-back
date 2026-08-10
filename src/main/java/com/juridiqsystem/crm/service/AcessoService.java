package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Acesso;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.AcessoDTO;
import com.juridiqsystem.crm.model.dtos.AcessoResponseDto;
import com.juridiqsystem.crm.model.dtos.UsuarioLogDto;
import com.juridiqsystem.crm.repository.AcessoRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AcessoService {


    @Autowired
    private AcessoRepository acessoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<AcessoResponseDto> findAllLogAcessos() {
        List<Acesso> logs = acessoRepository.findAll();
        return mapeiaDto(logs);
    }

    public List<AcessoResponseDto> findAllByUser(String id) {
        Usuario usuario = usuarioRepository.findByPublicId(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        List<Acesso> logs = acessoRepository.findByUsuarioId(usuario.getId());
        return mapeiaDto(logs);
    }


    public List<AcessoResponseDto> mapeiaDto(List<Acesso> logs) {
        return logs.stream()
                .map(log -> {
                    UsuarioLogDto usuarioDto = new UsuarioLogDto(
                            log.getUsuario().getPublicId(),
                            log.getUsuario().getNome(),
                            log.getUsuario().getLogin()
                    );

                    return new AcessoResponseDto(
                            log.getPublicId(),
                            log.getEnderecoIp(),
                            log.getLocalizacao(),
                            log.getData_acesso(),
                            log.getData_saida(),
                            log.getProvedorInternet(),
                            log.getSistemaOperacional(),
                            log.getTipoDispositivo(),
                            log.getFusoHorario(),
                            log.getNavegador(),
                            log.getVersaoNavegador(),
                            log.getIdiomaNavegador()
                    );
                })
                .collect(Collectors.toList());
    }



    public String save(Acesso log){
        Acesso savedLog = acessoRepository.save(log);
        return savedLog.getPublicId();
    }

    /**
     * @Transactional pra manter a mesma sessão do Hibernate durante o método inteiro — sem isso
     * (open-in-view=false), o lazy-load de log.getUsuario() abaixo lançaria
     * LazyInitializationException, já que a query que buscou o Acesso já teria fechado a sessão.
     */
    @Transactional
    public void updateExitTime(String id) {
        Acesso log = acessoRepository.findByPublicId(id).orElseThrow(() -> new RuntimeException("Log não encontrado"));
        if (log.getData_saida() != null) {
            return;
        }
        log.setData_saida(LocalDateTime.now());
        acessoRepository.save(log);

        // Revoga qualquer JWT emitido antes deste logout (ver SecurityFilter/TokenService) —
        // sem isso, o token continuaria válido até a expiração natural (12h) mesmo depois de sair.
        Usuario usuario = log.getUsuario();
        usuario.setSessaoVersao(usuario.getSessaoVersao() + 1);
        usuarioRepository.save(usuario);
    }


    public Acesso preencheLog(AcessoDTO dto){
        Acesso acesso = new Acesso();
        acesso.setEnderecoIp(dto.getEnderecoIp());
        acesso.setLocalizacao(dto.getLocalizacao());
        acesso.setData_acesso(dto.getData_acesso());
        acesso.setData_saida(dto.getData_saida());
        acesso.setProvedorInternet(dto.getProvedorInternet());
        acesso.setSistemaOperacional(dto.getSistemaOperacional());
        acesso.setTipoDispositivo(dto.getTipoDispositivo());
        acesso.setFusoHorario(dto.getFusoHorario());
        acesso.setNavegador(dto.getNavegador());
        acesso.setVersaoNavegador(dto.getVersaoNavegador());
        acesso.setIdiomaNavegador(acesso.getIdiomaNavegador());
        return acesso;
    }
}
