package br.ufpb.dsc.corrida.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    UserDetails findByLogin(String login);
    User findByUsername(String username);
    Boolean existsByLogin(String login);
    Boolean existsByUsername(String username);
    Boolean existsByLoginAndIdNot(String login, Long id);
    Boolean existsByUsernameAndIdNot(String username, Long id);

    /**
     * Home page — top-6 usuários cadastrados mais recentemente.
     * Ordena por id decrescente (proxy para data de cadastro, sem coluna extra).
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.papel != :papel 
          AND (:userId IS NULL OR u.id != :userId)
        ORDER BY u.id DESC
        LIMIT 6
    """)
    List<User> findTop6ByPapelOpcionalId(
        @Param("papel") Papel papel, 
        @Param("userId") Long userId
    );
}
