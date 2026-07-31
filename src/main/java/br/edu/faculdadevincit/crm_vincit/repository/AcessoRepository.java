package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Acesso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcessoRepository extends JpaRepository<Acesso, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    List<Acesso> findByUsuarioId(Long usuarioId);

    @Override
    @EntityGraph(attributePaths = {"usuario"})
    List<Acesso> findAll();

}
