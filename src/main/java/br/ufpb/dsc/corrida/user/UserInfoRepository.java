package br.ufpb.dsc.corrida.user;
import org.springframework.data.jpa.repository.JpaRepository;

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

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE UserInfo ui SET ui.totalKmRun = ui.totalKmRun + :kms WHERE ui.usuario.id IN (SELECT i.usuario.id FROM br.ufpb.dsc.corrida.race.Inscricao i WHERE i.corrida.id = :raceId AND i.status = 'ATIVA' AND i.compareceu = true)")
    void addKilometersToPresentParticipants(@org.springframework.data.repository.query.Param("raceId") Long raceId, @org.springframework.data.repository.query.Param("kms") Float kms);
}
