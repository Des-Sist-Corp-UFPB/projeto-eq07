package br.ufpb.dsc.corrida.featuretoggle;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeatureFlagRepository extends JpaRepository<UserFeatureFlag, Long> {
    boolean existsByUserIdAndFeatureName(Long userId, String featureName);
}
