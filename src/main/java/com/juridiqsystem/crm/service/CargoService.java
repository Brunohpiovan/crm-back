package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.Cargo;
import com.juridiqsystem.crm.model.dtos.CargoCreateRequest;
import com.juridiqsystem.crm.model.dtos.CargoResponse;
import com.juridiqsystem.crm.model.dtos.CargoUpdateRequest;
import com.juridiqsystem.crm.model.dtos.PermissaoResponse;
import com.juridiqsystem.crm.model.enums.Permissao;
import com.juridiqsystem.crm.repository.CargoRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Cargos customizados da empresa (tela "Cargos e Permissões", em Configurações). Todas as rotas
 * correspondentes são ROLE_ADMIN: configurar quem pode o que é inerentemente admin-only, e por
 * isso deliberadamente não existe uma Permissao delegável para esta área.
 *
 * <p>As regras de proteção do cargo administrador (não pode ser criado por API, editado,
 * renomeado nem excluído) vivem aqui, no backend — a UI só as espelha.</p>
 */
@Service
public class CargoService {

    private static final String NOME_CARGO_ADMINISTRADOR = "Administrador";
    private static final String NOME_CARGO_PADRAO = "Funcionário";

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SecurityLogger securityLogger;

    /** Catálogo estático de permissões delegáveis — fonte única de verdade para o frontend. */
    public List<PermissaoResponse> permissoesDisponiveis() {
        return Arrays.stream(Permissao.values()).map(PermissaoResponse::from).toList();
    }

    public List<CargoResponse> listar() {
        return buscarCargos(empresaAtual());
    }

    private List<CargoResponse> buscarCargos(Long empresaId) {
        return cargoRepository.findByEmpresaId(empresaId).stream()
                .map(CargoResponse::from)
                .toList();
    }

    /**
     * Variante usada pela área do usuário master (/master/empresas/{id}/usuarios): o master
     * pertence à empresa interna, então precisa "mirar" a empresa-cliente para enxergar os cargos
     * dela ao cadastrar um usuário lá — mesmo papel de UsuarioService.saveParaEmpresa, só que sem
     * runAs, já que Cargo é filtrado por empresaId explícito e não por @TenantId (ver Cargo).
     */
    public List<CargoResponse> listarParaEmpresa(Long empresaId) {
        return buscarCargos(empresaId);
    }

    public CargoResponse criar(CargoCreateRequest request) {
        Long empresaId = empresaAtual();
        String nome = request.nome().trim();

        if (cargoRepository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, nome)) {
            throw new ConflictException("Já existe um cargo com este nome nesta empresa.");
        }

        Cargo cargo = new Cargo();
        cargo.setEmpresaId(empresaId);
        cargo.setNome(nome);
        // Cargo criado pela API nasce sempre sem privilégio de administrador: o cargo
        // administrador é único por empresa e criado junto com ela (ver criarCargosPadrao).
        cargo.setAdministrador(false);
        cargo.setPermissoes(normalizarPermissoes(request.permissoes()));
        cargo.setCriadoEm(LocalDateTime.now());
        cargo.setAtualizadoEm(LocalDateTime.now());

        Cargo salvo = cargoRepository.save(cargo);
        registrarAcao("Cargo criado: nome=" + salvo.getNome() + "; permissoes=" + salvo.getPermissoes(), "/cargos");
        return CargoResponse.from(salvo);
    }

    public CargoResponse atualizar(String publicId, CargoUpdateRequest request) {
        Long empresaId = empresaAtual();
        Cargo cargo = buscarCargo(empresaId, publicId);
        validarCargoEditavel(cargo, "/cargos/" + publicId);

        String nome = request.nome().trim();
        if (cargoRepository.existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(empresaId, nome, cargo.getId())) {
            throw new ConflictException("Já existe um cargo com este nome nesta empresa.");
        }

        cargo.setNome(nome);
        // Substitui o conjunto inteiro (a tela envia o estado final dos checkboxes) em vez de
        // fazer merge — merge deixaria uma permissão desmarcada sobreviver silenciosamente.
        cargo.setPermissoes(normalizarPermissoes(request.permissoes()));
        cargo.setAtualizadoEm(LocalDateTime.now());

        Cargo salvo = cargoRepository.save(cargo);
        registrarAcao("Cargo atualizado: nome=" + salvo.getNome() + "; permissoes=" + salvo.getPermissoes(),
                "/cargos/" + publicId);
        return CargoResponse.from(salvo);
    }

    public void excluir(String publicId) {
        Long empresaId = empresaAtual();
        Cargo cargo = buscarCargo(empresaId, publicId);
        validarCargoEditavel(cargo, "/cargos/" + publicId);

        if (usuarioRepository.existsByCargoId(cargo.getId())) {
            throw new ConflictException(
                    "Este cargo ainda está vinculado a usuários. Realoque esses usuários em outro cargo antes de excluí-lo.");
        }

        cargoRepository.delete(cargo);
        registrarAcao("Cargo excluído: nome=" + cargo.getNome(), "/cargos/" + publicId);
    }

    /**
     * Cargos iniciais de uma empresa recém-criada, espelhando o que o backfill fez para as
     * empresas existentes: um cargo administrador (acesso total, obrigatório — sem ele a empresa
     * não teria como ter um primeiro usuário administrador) e um cargo comum sem nenhuma
     * permissão delegada. Idempotente: não recria o que já existir.
     *
     * <p>Recebe a empresa por parâmetro (e não pelo TenantContext) porque quem cria a empresa é o
     * usuário master, que pertence à empresa interna — o tenant da requisição não é o da empresa
     * nova.</p>
     */
    public void criarCargosPadrao(Long empresaId) {
        if (cargoRepository.findByEmpresaIdAndAdministradorTrue(empresaId).isEmpty()) {
            cargoRepository.save(novoCargoPadrao(empresaId, NOME_CARGO_ADMINISTRADOR, true));
        }
        if (!cargoRepository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, NOME_CARGO_PADRAO)) {
            cargoRepository.save(novoCargoPadrao(empresaId, NOME_CARGO_PADRAO, false));
        }
    }

    private Cargo novoCargoPadrao(Long empresaId, String nome, boolean administrador) {
        Cargo cargo = new Cargo();
        cargo.setEmpresaId(empresaId);
        cargo.setNome(nome);
        cargo.setAdministrador(administrador);
        cargo.setPermissoes(new LinkedHashSet<>());
        cargo.setCriadoEm(LocalDateTime.now());
        cargo.setAtualizadoEm(LocalDateTime.now());
        return cargo;
    }

    private Cargo buscarCargo(Long empresaId, String publicId) {
        return cargoRepository.findByEmpresaIdAndPublicId(empresaId, publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo não encontrado."));
    }

    /**
     * O cargo administrador é um conceito fixo do sistema: sempre existe, sempre tem acesso
     * total. Editar sua lista de permissões não mudaria nada (getAuthorities() concede tudo por
     * causa do flag) e excluí-lo deixaria a empresa sem ninguém capaz de gerenciar usuários.
     */
    private void validarCargoEditavel(Cargo cargo, String path) {
        if (!cargo.isAdministrador()) {
            return;
        }
        securityLogger.log(SecurityEventType.RESOURCE_ACCESS_DENIED,
                "Tentativa de alterar/excluir o cargo administrador: cargo=" + cargo.getNome(),
                currentActorLogin(), null, path);
        throw new AccessDeniedException("O cargo de administrador é fixo do sistema e não pode ser alterado nem excluído.");
    }

    private Set<Permissao> normalizarPermissoes(List<Permissao> permissoes) {
        return permissoes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(permissoes);
    }

    private Long empresaAtual() {
        return TenantContext.get();
    }

    private void registrarAcao(String detalhe, String path) {
        securityLogger.log(SecurityEventType.ADMIN_ACTION, detalhe, currentActorLogin(), null, path);
    }

    private String currentActorLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}
