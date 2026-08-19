package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Marca o token como usado atomicamente, só se ainda não usado e não expirado — o UPDATE em
     * si é a checagem (WHERE usado = false), não um SELECT seguido de UPDATE em statements
     * separados. Evita a corrida em que duas requisições simultâneas com o mesmo token válido
     * ambas leem usado=false antes de qualquer uma persistir a troca de senha, permitindo o
     * "resgate" do mesmo token duas vezes. Retorna quantas linhas foram afetadas: 1 = token
     * consumido com sucesso por ESTA chamada; 0 = já estava usado, expirado ou não existe.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usado = true WHERE t.token = :token AND t.usado = false AND t.expiraEm > :agora")
    int marcarComoUsadoSeValido(@Param("token") String token, @Param("agora") LocalDateTime agora);
}
