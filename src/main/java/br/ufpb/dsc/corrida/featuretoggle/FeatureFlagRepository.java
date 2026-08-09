package br.ufpb.dsc.corrida.featuretoggle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório JPA para a entidade {@link FeatureFlag}.
 *
 * <p>Acesso direto ao banco deve ser feito preferencialmente via
 * {@link FeatureToggleService}, que aplica a camada de cache sobre as consultas.
 */
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    /**
     * Busca um Feature Flag pela sua chave única.
     *
     * @param keyName chave do feature flag (ex: "PAYMENT_V2")
     * @return {@link Optional} contendo o flag se encontrado, ou vazio caso contrário
     */
    Optional<FeatureFlag> findByKeyName(String keyName);
}
