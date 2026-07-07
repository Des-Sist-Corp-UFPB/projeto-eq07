package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Entidade de Inscrição em Corridas (Relação N:M entre Usuários e Corridas).
 */
@Entity
@Table(name = "inscricao", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "corrida_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrida_id", nullable = false)
    private Race corrida;

    @Column(name = "data_inscricao", nullable = false)
    private OffsetDateTime dataInscricao = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean compareceu = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "status_inscricao")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StatusInscricao status = StatusInscricao.ATIVA;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
