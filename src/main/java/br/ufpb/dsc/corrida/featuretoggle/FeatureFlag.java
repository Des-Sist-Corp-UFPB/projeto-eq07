package br.ufpb.dsc.corrida.featuretoggle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entidade JPA que representa um Feature Flag persistido no PostgreSQL.
 *
 * <p>Os valores desta tabela são cacheados em memória via Spring Cache (Caffeine).
 * Alterações diretas no banco só são refletidas após expiração do cache ou
 * chamada explícita a {@link DatabaseFeatureToggleProvider#evictCache(String)}.
 */
@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Chave única do feature flag (ex: "PAYMENT_V2", "AUDIT_NEW_PIPELINE").
     * Deve ser em letras maiúsculas com underscores.
     */
    @Column(name = "key_name", length = 100, nullable = false, unique = true)
    private String keyName;

    /** Se {@code true}, a feature está habilitada e os métodos anotados serão executados normalmente. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** Descrição legível para humanos sobre o propósito desta flag. */
    @Column(name = "description", length = 255)
    private String description;

    /** Timestamp da última atualização do registro. */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
