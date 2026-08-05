package br.ufpb.dsc.corrida.user;

import org.springframework.data.jpa.repository.JpaRepository;
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
    List<User> findTop6ByPapelNotOrderByIdDesc(Papel papel);
}
