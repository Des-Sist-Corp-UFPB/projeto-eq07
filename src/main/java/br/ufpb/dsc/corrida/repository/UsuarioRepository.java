package br.ufpb.dsc.corrida.repository;

import br.ufpb.dsc.corrida.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    UserDetails findByLogin(String login);
    Usuario findByUsername(String username);
    Boolean existsByLogin(String login);
    Boolean existsByUsername(String username);
    Boolean existsByLoginAndIdNot(String login, Long id);
    Boolean existsByUsernameAndIdNot(String username, Long id);
}
