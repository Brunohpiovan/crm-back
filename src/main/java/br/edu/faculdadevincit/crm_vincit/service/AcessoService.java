package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Acesso;
import br.edu.faculdadevincit.crm_vincit.model.dtos.AcessoDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.AcessoResponseDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioLogDto;
import br.edu.faculdadevincit.crm_vincit.repository.AcessoRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
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

    public List<AcessoResponseDto> findAllByUser(Long id) {
        List<Acesso> logs = acessoRepository.findByUsuarioId(id);
        return mapeiaDto(logs);
    }


    public List<AcessoResponseDto> mapeiaDto(List<Acesso> logs) {
        return logs.stream()
                .map(log -> {
                    UsuarioLogDto usuarioDto = new UsuarioLogDto(
                            log.getUsuario().getId(),
                            log.getUsuario().getNome(),
                            log.getUsuario().getLogin()
                    );

                    return new AcessoResponseDto(
                            log.getId(),
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



    public Long save(Acesso log){
        Acesso savedLog = acessoRepository.save(log);
        return savedLog.getId();
    }

    public void updateExitTime(Long id) {
        Acesso log = acessoRepository.findById(id).orElseThrow(() -> new RuntimeException("Log não encontrado"));
        if(log.getData_saida()==null){
            log.setData_saida(LocalDateTime.now());
        }else {
            return;
        }
        acessoRepository.save(log);
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
