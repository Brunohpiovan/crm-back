package com.juridiqsystem.crm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Linha de controle usada para impedir que duas instâncias da aplicação executem o mesmo
 * job de scheduler ao mesmo tempo (ver SchedulerLockRepository).
 */
@Getter
@Setter
@Entity
@Table(name = "scheduler_lock")
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerLock {

    @Id
    @Column(name = "nome", length = 100)
    private String nome;

    @Column(name = "running", nullable = false)
    private boolean running;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;
}
