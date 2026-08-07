package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Empresa;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.EmpresaCreateDTO;
import com.juridiqsystem.crm.model.dtos.EmpresaResponseDTO;
import com.juridiqsystem.crm.model.dtos.EmpresaSelfUpdateDTO;
import com.juridiqsystem.crm.model.dtos.EmpresaUsuarioCountProjection;
import com.juridiqsystem.crm.repository.EmpresaRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.DataIntegrityViolationException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private S3Service s3Service;

    public Page<EmpresaResponseDTO> findAll(String search, Pageable pageable) {
        String termoBusca = (search == null || search.isBlank())
                ? null
                : "%" + search.trim().toLowerCase() + "%";

        Page<Empresa> empresas = empresaRepository.findByInternaFalseAndSearch(termoBusca, pageable);
        Map<Long, EmpresaUsuarioCountProjection> contagens = contarUsuariosPorEmpresa(empresas.getContent());

        return empresas.map(empresa -> {
            EmpresaUsuarioCountProjection contagem = contagens.get(empresa.getId());
            long quantidadeUsuarios = contagem != null ? contagem.getTotalUsuarios() : 0L;
            long quantidadeAdmins = contagem != null ? contagem.getTotalAdmins() : 0L;
            return new EmpresaResponseDTO(empresa, quantidadeUsuarios, quantidadeAdmins);
        });
    }

    private Map<Long, EmpresaUsuarioCountProjection> contarUsuariosPorEmpresa(List<Empresa> empresas) {
        if (empresas.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> empresaIds = empresas.stream().map(Empresa::getId).toList();
        return usuarioRepository.countUsuariosPorEmpresa(empresaIds).stream()
                .collect(Collectors.toMap(EmpresaUsuarioCountProjection::getEmpresaId, Function.identity()));
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

    /**
     * Autoatendimento: dados da empresa do usuário autenticado (aba "Configurações" do CRM),
     * sem receber id na URL — diferente de findById/update acima, que são o CRUD do master sobre
     * qualquer empresa-cliente por id (/master/empresas/**). Aqui a empresa é resolvida a partir
     * do próprio usuário logado, incluindo a empresa `interna` (o master não usa este endpoint,
     * mas nada nele depende da exclusão aplicada em buscarEmpresaGerenciavel).
     */
    public EmpresaResponseDTO getEmpresaAutenticada() {
        return new EmpresaResponseDTO(buscarEmpresaDoUsuarioAutenticado());
    }

    public EmpresaResponseDTO atualizarEmpresaAutenticada(EmpresaSelfUpdateDTO dto, MultipartFile logo) {
        Empresa empresa = buscarEmpresaDoUsuarioAutenticado();

        empresa.setNome(dto.getNome());
        empresa.setLogoUrl(resolveLogoUrl(empresa, dto, logo));
        empresa.setTimezone(dto.getTimezone());
        empresa.setProtocoloRiscoHoras(dto.getProtocoloRiscoHoras());
        empresa.setNotificacaoVisualHabilitada(dto.getNotificacaoVisualHabilitada());
        empresa.setNotificacaoSonoraHabilitada(dto.getNotificacaoSonoraHabilitada());
        empresa.setAtualizadoEm(LocalDateTime.now());

        return new EmpresaResponseDTO(empresaRepository.save(empresa));
    }

    private Empresa buscarEmpresaDoUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();

        return empresaRepository.findById(usuarioAutenticado.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa do usuário autenticado não encontrada"));
    }

    private String resolveLogoUrl(Empresa empresaDoBanco, EmpresaSelfUpdateDTO dto, MultipartFile logo) {
        String logoUrlAtual = empresaDoBanco.getLogoUrl();
        boolean logoAtualEhS3 = logoUrlAtual != null && logoUrlAtual.contains(s3Service.getBaseUrl());

        if (logoAtualEhS3 && (logo != null || dto.getLogoUrl() == null || dto.getLogoUrl().isBlank())) {
            s3Service.deleteFile(logoUrlAtual.substring(logoUrlAtual.indexOf(".com/") + 5));
        }
        if (logo != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String key = "logo/" + empresaDoBanco.getCodigo() + "_" + LocalDateTime.now().format(formatter);
            return s3Service.uploadFile(logo, key);
        }
        return dto.getLogoUrl();
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
