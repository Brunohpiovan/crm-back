package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ContatoDTO;
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
        Participante participante = criarParticipante(contatoDTO);
        Oportunidade oportunidade = criarOportunidade(contatoDTO, etapa, usuario, participante);

        oportunidadeService.create(oportunidade, null);
    }

    private Etapa findEtapaEntrada() {
        return etapaRepository.findById(Long.parseLong(etapaId))
                .orElseThrow(() -> new RuntimeException("A etapa com id " + etapaId + " não existe no funil"));
    }

    private Usuario findUsuarioDisponivel() {
        return usuarioRepository.findRandomUsuarioDisponivel()
                .orElseThrow(() -> new RuntimeException("Nenhum usuário disponível encontrado"));
    }

    private Participante criarParticipante(ContatoDTO dto) {
        Participante participante = new Participante();
        participante.setNome(dto.getNome());
        participante.setLogin(dto.getEmail());
        participante.setCelular(dto.getCelular());
        return participante;
    }

    private Oportunidade criarOportunidade(ContatoDTO dto, Etapa etapa, Usuario criador, Participante cliente) {
        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setTitulo(dto.getNome());
        oportunidade.setCriador(criador);
        oportunidade.setCliente(cliente);
        oportunidade.setValor(BigDecimal.ZERO);
        oportunidade.setOrigem(Origem.SITE);
        oportunidade.setInteresse(dto.getInteresse());
        oportunidade.setEtapa(etapa);
        oportunidade.setSituacao(SituacaoOportunidade.ABERTO);
        return oportunidade;
    }
}
