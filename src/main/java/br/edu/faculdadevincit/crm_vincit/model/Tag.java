package br.edu.faculdadevincit.crm_vincit.model;

import br.edu.faculdadevincit.crm_vincit.model.enums.Cor;
import br.edu.faculdadevincit.crm_vincit.model.enums.Pertence;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tag")
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    @Column(unique = true)
    private String nome;

    private Cor cor;

    private Pertence pertence;

    private Situacao situacao;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
