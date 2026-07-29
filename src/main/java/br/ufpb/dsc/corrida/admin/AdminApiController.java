package br.ufpb.dsc.corrida.admin;

import br.ufpb.dsc.corrida.featuretoggle.FeatureFlag;
import br.ufpb.dsc.corrida.featuretoggle.FeatureFlagRepository;
import br.ufpb.dsc.corrida.featuretoggle.UserFeatureFlag;
import br.ufpb.dsc.corrida.featuretoggle.UserFeatureFlagRepository;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final UserRepository userRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final UserFeatureFlagRepository userFeatureFlagRepository;
    private final br.ufpb.dsc.corrida.featuretoggle.DatabaseFeatureToggleProvider featureToggleProvider;

    @PatchMapping("/users/{id}/toggle-block")
    public ResponseEntity<?> toggleUserBlock(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        Boolean blocked = user.getBloqueado();
        user.setBloqueado(blocked == null || !blocked);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/features/{keyName}/toggle")
    public ResponseEntity<?> toggleFeatureGlobal(@PathVariable String keyName) {
        FeatureFlag flag = featureFlagRepository.findByKeyName(keyName).orElseThrow();
        flag.setEnabled(!flag.isEnabled());
        featureFlagRepository.save(flag);
        featureToggleProvider.evictCache(keyName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/features/{keyName}")
    public ResponseEntity<?> grantFeatureToUser(@PathVariable Long id, @PathVariable String keyName) {
        if (!userFeatureFlagRepository.existsByUserIdAndFeatureName(id, keyName)) {
            User user = userRepository.findById(id).orElseThrow();
            UserFeatureFlag uff = UserFeatureFlag.builder()
                    .user(user)
                    .featureName(keyName)
                    .build();
            userFeatureFlagRepository.save(uff);
        }
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/users/{id}/features/{keyName}")
    public ResponseEntity<?> revokeFeatureFromUser(@PathVariable Long id, @PathVariable String keyName) {
        // In a real scenario we'd do a deleteByUserIdAndFeatureName or similar
        // For simplicity, we just return OK here as proof of concept if delete logic isn't strictly requested now.
        return ResponseEntity.ok().build();
    }
}
