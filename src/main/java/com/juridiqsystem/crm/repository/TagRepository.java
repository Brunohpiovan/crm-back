package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Tag;
import com.juridiqsystem.crm.model.enums.Situacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByPublicId(String publicId);

    List<Tag> findAllByPublicIdIn(List<String> publicIds);

    boolean existsByNome(String nome);

    @Query("SELECT t FROM Tag t ORDER BY " +
            "CASE t.situacao WHEN 'ATIVA' THEN 0 ELSE 1 END, " +
            "LOWER(t.nome) ASC")
    List<Tag> findAllOrdered();

    List<Tag> findBySituacaoOrderByNomeAsc(Situacao situacao);



}
