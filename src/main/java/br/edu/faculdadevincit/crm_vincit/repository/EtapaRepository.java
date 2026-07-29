package br.edu.faculdadevincit.crm_vincit.repository;
import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    List<Etapa> findByFunilId(Long funilId);

    boolean existsByNomeAndFunilId(String nome, Long funilId);
}
