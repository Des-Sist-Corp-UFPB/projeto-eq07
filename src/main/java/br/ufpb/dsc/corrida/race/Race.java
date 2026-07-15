package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.organizer.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade principal do módulo Corrida.
 *
 * <p>Mapeada para a tabela {@code corrida} criada em V6.
 * Os benefícios são armazenados em {@code corrida_beneficio} via
 * {@link ElementCollection}.
 */
@Entity
@Table(name = "corrida")
@Getter
@Setter
@NoArgsConstructor
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "banner_url", length = 512)
    private String bannerUrl;

    @Column(name = "valor_inscricao", precision = 10, scale = 2)
    private BigDecimal valorInscricao;

    @Column(name = "max_inscricoes")
    private Integer maxInscricoes;

    @Column(nullable = false, name = "data_inicio")
    private OffsetDateTime dataInicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "status_corrida")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StatusCorrida status = StatusCorrida.RASCUNHO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "categoria_corrida")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CategoriaCorrida categoria;

    // === Ponto de Largada ===
    @Column(name = "largada_lat", nullable = false)
    private Double largadaLat;

    @Column(name = "largada_lng", nullable = false)
    private Double largadaLng;

    @Column(name = "largada_endereco", nullable = false, length = 512)
    private String largadaEndereco;

    // === Ponto de Chegada ===
    @Column(name = "chegada_lat", nullable = false)
    private Double chegadaLat;

    @Column(name = "chegada_lng", nullable = false)
    private Double chegadaLng;

    @Column(name = "chegada_endereco", nullable = false, length = 512)
    private String chegadaEndereco;

    // === Rota calculada pelo ORS ===
    @Column(name = "distancia_km", precision = 6, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "duracao_estimada_min")
    private Integer duracaoEstimadaMin;

    @Column(name = "data_fim")
    private OffsetDateTime dataFim;

    @Column(name = "rota_geojson", columnDefinition = "TEXT")
    private String rotaGeoJson;

    // === Benefícios ===
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "corrida_beneficio",
            joinColumns = @JoinColumn(name = "corrida_id")
    )
    @Column(name = "beneficio")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Set<BeneficioCorrida> beneficios = new HashSet<>();

    // === Organização ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void calculateDataFim() {
        if (this.dataInicio != null) {
            int duracao = this.duracaoEstimadaMin != null ? this.duracaoEstimadaMin : 240;
            this.dataFim = this.dataInicio.plusMinutes(duracao);
        }
    }
}
