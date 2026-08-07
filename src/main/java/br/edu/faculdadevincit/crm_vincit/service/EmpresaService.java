package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Empresa;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmpresaCreateDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmpresaResponseDTO;
import br.edu.faculdadevincit.crm_vincit.repository.EmpresaRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.DataIntegrityViolationException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    public Page<EmpresaResponseDTO> findAll(Pageable pageable) {
        return empresaRepository.findByInternaFalse(pageable).map(EmpresaResponseDTO::new);
    }

    public EmpresaResponseDTO findById(Long id) {
        return new EmpresaResponseDTO(buscarEmpresaGerenciavel(id));
    }

    public EmpresaResponseDTO create(EmpresaCreateDTO dto) {
        validarCodigoDisponivel(dto.getCodigo(), null);

        Empresa empresa = new Empresa();
        aplicarDados(empresa, dto);
        empresa.setInterna(false);
        empresa.setCriadoEm(LocalDateTime.now());
        empresa.setAtualizadoEm(LocalDateTime.now());

        return new EmpresaResponseDTO(empresaRepository.save(empresa));
    }

    public EmpresaResponseDTO update(Long id, EmpresaCreateDTO dto) {
        Empresa empresa = buscarEmpresaGerenciavel(id);
        validarCodigoDisponivel(dto.getCodigo(), id);

        aplicarDados(empresa, dto);
        empresa.setAtualizadoEm(LocalDateTime.now());

        return new EmpresaResponseDTO(empresaRepository.save(empresa));
    }

    private void aplicarDados(Empresa empresa, EmpresaCreateDTO dto) {
        empresa.setCodigo(dto.getCodigo());
        empresa.setNome(dto.getNome());
        empresa.setLogoUrl(dto.getLogoUrl());
        empresa.setTimezone(dto.getTimezone());
        empresa.setProtocoloRiscoHoras(dto.getProtocoloRiscoHoras());
        empresa.setNotificacaoVisualHabilitada(dto.getNotificacaoVisualHabilitada());
        empresa.setNotificacaoSonoraHabilitada(dto.getNotificacaoSonoraHabilitada());
    }

    private void validarCodigoDisponivel(String codigo, Long idAtual) {
        empresaRepository.findByCodigo(codigo).ifPresent(existente -> {
            if (!existente.getId().equals(idAtual)) {
                throw new DataIntegrityViolationException("Já existe uma empresa com este código.");
            }
        });
    }

    /**
     * Empresas marcadas como `interna` (a "casa" do usuário master) nunca são retornadas nem
     * editáveis por este service — só existem para o master ter uma empresa própria pra logar.
     */
    private Empresa buscarEmpresaGerenciavel(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa com id " + id + " não encontrada"));
        if (Boolean.TRUE.equals(empresa.getInterna())) {
            throw new ResourceNotFoundException("Empresa com id " + id + " não encontrada");
        }
        return empresa;
    }
}
