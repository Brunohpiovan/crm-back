package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByNome(String nome);

    @Query("SELECT t FROM Tag t ORDER BY " +
            "CASE t.situacao WHEN 'ATIVA' THEN 0 ELSE 1 END, " +
            "LOWER(t.nome) ASC")
    List<Tag> findAllOrdered();

    List<Tag> findBySituacaoOrderByNomeAsc(Situacao situacao);



}
