package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.SchedulerLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SchedulerLockRepository extends JpaRepository<SchedulerLock, String> {

    /**
     * UPDATE condicional atômico: só marca a linha como "running" se ela estiver livre ou se o
     * lock anterior estiver parado há mais tempo que o limite de obsolescência (proteção contra
     * lock preso por uma instância que caiu no meio do processamento). Retorna 1 se este chamador
     * adquiriu o lock, 0 se outra instância já está processando.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE SchedulerLock l SET l.running = true, l.iniciadoEm = :agora
    WHERE l.nome = :nome AND (l.running = false OR l.iniciadoEm < :limiteObsolescencia)
    """)
    int tentarAdquirir(@Param("nome") String nome,
                        @Param("agora") LocalDateTime agora,
                        @Param("limiteObsolescencia") LocalDateTime limiteObsolescencia);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchedulerLock l SET l.running = false WHERE l.nome = :nome")
    void liberar(@Param("nome") String nome);
}
