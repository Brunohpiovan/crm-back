package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.TemplateEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateEmailRepository extends JpaRepository<TemplateEmail, Long> {
    @Query("SELECT t FROM TemplateEmail t ORDER BY " +
            "CASE t.situacao WHEN 'ATIVA' THEN 0 ELSE 1 END, " +
            "LOWER(t.nome) ASC")
    List<TemplateEmail> findAllOrdered();

}
