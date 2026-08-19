package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Oportunidade;
import com.juridiqsystem.crm.model.OportunidadeProcesso;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.OportunidadeProcessoResponse;
import com.juridiqsystem.crm.repository.OportunidadeProcessoRepository;
import com.juridiqsystem.crm.repository.OportunidadeRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vincular/desvincular/listar Processos de uma Oportunidade. A resolução do Processo (busca na
 * Escavador + upsert local, via ProcessoService) roda fora de qualquer transação desta classe —
 * só a escrita do vínculo em si (criarVinculo) é transacional.
 */
@Service
public class OportunidadeProcessoService {

    private final OportunidadeRepository oportunidadeRepository;
    private final OportunidadeProcessoRepository oportunidadeProcessoRepository;
    private final ProcessoRepository processoRepository;
    private final ProcessoService processoService;
    private final UsuarioRepository usuarioRepository;
    private final OportunidadeProcessoService self;

    public OportunidadeProcessoService(OportunidadeRepository oportunidadeRepository,
                                        OportunidadeProcessoRepository oportunidadeProcessoRepository,
                                        ProcessoRepository processoRepository,
                                        ProcessoService processoService,
                                        UsuarioRepository usuarioRepository,
                                        @Lazy OportunidadeProcessoService self) {
        this.oportunidadeRepository = oportunidadeRepository;
        this.oportunidadeProcessoRepository = oportunidadeProcessoRepository;
        this.processoRepository = processoRepository;
        this.processoService = processoService;
        this.usuarioRepository = usuarioRepository;
        this.self = self;
    }

    public List<OportunidadeProcessoResponse> listar(String oportunidadePublicId) {
        Oportunidade oportunidade = buscarOportunidade(oportunidadePublicId);
        return oportunidadeProcessoRepository.findByOportunidadeId(oportunidade.getId()).stream()
                .map(OportunidadeProcessoResponse::new)
                .toList();
    }

    public OportunidadeProcessoResponse vincular(String oportunidadePublicId, String numeroCnj) {
        Oportunidade oportunidade = buscarOportunidade(oportunidadePublicId);
        Processo processo = processoRepository.findByEmpresaIdAndNumeroCnj(oportunidade.getEmpresaId(), numeroCnj)
                .orElseGet(() -> processoService.consultarEUpsertPorCnj(numeroCnj));
        return self.criarVinculo(oportunidade.getId(), processo.getId());
    }

    @Transactional
    public OportunidadeProcessoResponse criarVinculo(Long oportunidadeId, Long processoId) {
        oportunidadeProcessoRepository.findByOportunidadeIdAndProcessoId(oportunidadeId, processoId)
                .ifPresent(v -> {
                    throw new ConflictException("Este processo já está vinculado a esta oportunidade.");
                });

        OportunidadeProcesso vinculo = new OportunidadeProcesso();
        vinculo.setOportunidade(oportunidadeRepository.getReferenceById(oportunidadeId));
        vinculo.setProcesso(processoRepository.getReferenceById(processoId));
        vinculo.setVinculadoEm(LocalDateTime.now());
        vinculo.setVinculadoPor(currentUser());
        OportunidadeProcesso salvo = oportunidadeProcessoRepository.save(vinculo);
        return new OportunidadeProcessoResponse(salvo);
    }

    @Transactional
    public void desvincular(String oportunidadePublicId, String processoPublicId) {
        Oportunidade oportunidade = buscarOportunidade(oportunidadePublicId);
        OportunidadeProcesso vinculo = oportunidadeProcessoRepository
                .findByOportunidadeIdAndProcessoPublicId(oportunidade.getId(), processoPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo entre esta oportunidade e o processo " + processoPublicId + " não encontrado"));
        oportunidadeProcessoRepository.delete(vinculo);
    }

    private Oportunidade buscarOportunidade(String oportunidadePublicId) {
        return oportunidadeRepository.findByPublicId(oportunidadePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidade com id " + oportunidadePublicId + " nao encontrada"));
    }

    private Usuario currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuarioRepository.findById(usuario.getId()).orElse(usuario);
        }
        throw new ResourceNotFoundException("Usuário autenticado não encontrado");
    }
}
