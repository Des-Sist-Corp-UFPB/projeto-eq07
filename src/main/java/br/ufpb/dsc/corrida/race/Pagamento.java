package br.ufpb.dsc.corrida.race;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidade que representa um pagamento Pix associado a uma inscrição.
 * Relação 1:1 com {@link Inscricao}.
 */
@Entity
@Table(name = "pagamento")
@Getter
@Setter
@NoArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK para a inscrição — relação 1:1. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscricao_id", nullable = false, unique = true)
    private Inscricao inscricao;

    /** ID oficial do pagamento no Mercado Pago. Preenchido após a criação. */
    @Column(name = "mp_payment_id", unique = true)
    private Long mpPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private MetodoPagamento paymentMethod = MetodoPagamento.PIX;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    /** Valor cobrado (= valorInscricao da corrida no momento da criação). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Chave Pix copia-e-cola (string longa do payload Pix). */
    @Column(name = "qr_code_pix", columnDefinition = "TEXT")
    private String qrCodePix;

    /** QR Code em Base64 para exibição como imagem no browser. */
    @Column(name = "qr_code_base64_pix", columnDefinition = "TEXT")
    private String qrCodeBase64Pix;

    /** UUID gerado por nós para X-Idempotency-Key — garante idempotência de rede. */
    @Column(name = "idempotency_key", length = 36, unique = true)
    private String idempotencyKey;

    /** Momento em que a cobrança Pix expira (30 min após criação). */
    @Column(name = "expiration_date")
    private OffsetDateTime expirationDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
