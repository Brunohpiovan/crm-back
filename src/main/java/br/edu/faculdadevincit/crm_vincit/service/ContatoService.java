package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ContatoDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeClienteRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import br.edu.faculdadevincit.crm_vincit.repository.EtapaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ContatoService {

    @Value("${etapa.entrada.id}")
    private String etapaId;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private OportunidadeService oportunidadeService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public void create(ContatoDTO contatoDTO) {
        Etapa etapa = findEtapaEntrada();
        Usuario usuario = findUsuarioDisponivel();

        OportunidadeClienteRequest cliente = new OportunidadeClienteRequest(
                null,
                contatoDTO.getNome(),
                contatoDTO.getEmail(),
                contatoDTO.getCelular()
        );

        OportunidadeCreateRequest request = new OportunidadeCreateRequest(
                contatoDTO.getNome(),
                etapa.getId(),
                usuario.getId(),
                cliente,
                BigDecimal.ZERO,
                null,
                Origem.SITE,
                contatoDTO.getInteresse(),
                null,
                null,
                SituacaoOportunidade.ABERTO,
                null
        );

        oportunidadeService.create(request, null);
    }

    private Etapa findEtapaEntrada() {
        return etapaRepository.findById(Long.parseLong(etapaId))
                .orElseThrow(() -> new RuntimeException("A etapa com id " + etapaId + " não existe no funil"));
    }

    private Usuario findUsuarioDisponivel() {
        return usuarioRepository.findRandomUsuarioDisponivel()
                .orElseThrow(() -> new RuntimeException("Nenhum usuário disponível encontrado"));
    }

}
