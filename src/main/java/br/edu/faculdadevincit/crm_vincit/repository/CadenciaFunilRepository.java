package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CadenciaFunilRepository extends JpaRepository<CadenciaFunil, Long> {

    List<CadenciaFunil> findAllBySituacao(Situacao situacao);

}
