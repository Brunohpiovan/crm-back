package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.ContatoDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeClienteRequest;
import com.juridiqsystem.crm.model.dtos.OportunidadeCreateRequest;
import com.juridiqsystem.crm.model.enums.Origem;
import com.juridiqsystem.crm.model.enums.SituacaoOportunidade;
import com.juridiqsystem.crm.repository.EtapaRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
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
                etapa.getPublicId(),
                usuario.getPublicId(),
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
