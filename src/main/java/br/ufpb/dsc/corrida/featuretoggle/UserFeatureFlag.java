package br.ufpb.dsc.corrida.featuretoggle;

import br.ufpb.dsc.corrida.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_feature_flag")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "feature_name", nullable = false)
    private String featureName;
}
