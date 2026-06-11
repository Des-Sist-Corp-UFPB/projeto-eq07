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
}
