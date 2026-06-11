package br.ufpb.dsc.corrida.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserRepository extends JpaRepository<User, Long> {
    UserDetails findByLogin(String login);
    User findByUsername(String username);
    Boolean existsByLogin(String login);
    Boolean existsByUsername(String username);
    Boolean existsByLoginAndIdNot(String login, Long id);
    Boolean existsByUsernameAndIdNot(String username, Long id);
}
