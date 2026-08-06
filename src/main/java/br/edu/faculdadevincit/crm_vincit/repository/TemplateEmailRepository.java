package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.TemplateEmail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateEmailRepository extends JpaRepository<TemplateEmail, Long> {
    @EntityGraph(attributePaths = "urlAnexo")
    Optional<TemplateEmail> findByPublicId(String publicId);

    @Query("SELECT t FROM TemplateEmail t ORDER BY " +
            "CASE t.situacao WHEN 'ATIVA' THEN 0 ELSE 1 END, " +
            "LOWER(t.nome) ASC")
    List<TemplateEmail> findAllOrdered();

    // urlAnexo é @ElementCollection (lazy por padrão) - sem o fetch aqui, com
    // spring.jpa.open-in-view=false, a sessão do Hibernate já está fechada quando o service tenta
    // ler a lista (TemplateEmailIdDTO, e o próprio update()), disparando LazyInitializationException.
    @EntityGraph(attributePaths = "urlAnexo")
    @Override
    Optional<TemplateEmail> findById(Long id);

}
