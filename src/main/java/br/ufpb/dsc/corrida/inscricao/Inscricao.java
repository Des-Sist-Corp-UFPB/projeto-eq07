package br.ufpb.dsc.corrida.inscricao;

import br.ufpb.dsc.corrida.pagamento.Pagamento;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @Column(nullable = false, length = 30)
    private StatusInscricao status = StatusInscricao.AGUARDANDO_PAGAMENTO;

    @Column(name = "alerta_risco_reconhecido", nullable = false)
    private boolean alertaRiscoReconhecido = false;

    /** Pagamento Pix associado (apenas para corridas pagas). */
    @OneToOne(mappedBy = "inscricao", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Pagamento pagamento;

    /** Indica se o e-mail com comprovante PDF foi enviado com sucesso. */
    @Column(name = "email_enviado", nullable = false)
    private boolean emailEnviado = false;

    /** Timestamp do envio do e-mail (null se não enviado ou se falhou). */
    @Column(name = "email_enviado_em")
    private OffsetDateTime emailEnviadoEm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
