package br.ufpb.dsc.corrida.user;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositório JPA para a entidade {@link UserInfo}.
 */
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    /**
     * Busca o UserInfo associado a um usuário pelo ID do usuário.
     *
     * @param usuarioId ID do usuário
     * @return Optional contendo o UserInfo, se existir
     */
    Optional<UserInfo> findByUsuarioId(Long usuarioId);

    /**
     * Verifica se já existe um UserInfo para o usuário informado.
     *
     * @param usuarioId ID do usuário
     * @return {@code true} se já existe um registro
     */
    boolean existsByUsuarioId(Long usuarioId);

    @Modifying
    @Query("""
        UPDATE UserInfo ui SET ui.totalKmRun = ui.totalKmRun + :kms 
        WHERE ui.usuario.id IN (
            SELECT i.usuario.id FROM Inscricao i 
            WHERE i.corrida.id = :raceId AND i.status = 'ATIVA' AND i.compareceu = true
        )
    """)
    void addKilometersToPresentParticipants(@Param("raceId") Long raceId, @Param("kms") Float kms);
}
