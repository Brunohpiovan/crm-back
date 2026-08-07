package com.juridiqsystem.crm.model.dtos;

/**
 * Projeção da query nativa UsuarioRepository.countUsuariosPorEmpresa (uma linha de contagem por
 * empresa). Os nomes dos getters precisam bater com os aliases da query (empresaId,
 * totalUsuarios, totalAdmins) para o Spring Data conseguir mapear o resultado.
 */
public interface EmpresaUsuarioCountProjection {
    Long getEmpresaId();
    Long getTotalUsuarios();
    Long getTotalAdmins();
}
