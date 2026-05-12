package br.ufpb.dsc.corrida.domain;

import br.ufpb.dsc.corrida.dto.user.EditarUsuarioDTO;
import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.enums.Papel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collection;
import java.util.List;

@Entity(name = "Usuario")
@Table(name = "usuario")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Usuario implements UserDetails {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String username;
    private String login;
    private String senha;
    @Enumerated(EnumType.STRING)
    private Papel papel;
    private Boolean deletado;

    public Usuario(RegistrarUsuarioDTO usuario) {
        var bcrypt = new BCryptPasswordEncoder();
        this.nome = usuario.nome();
        this.login = usuario.login();
        this.username = usuario.username();
        this.senha = bcrypt.encode(usuario.senha());
        this.papel = Papel.USUARIO;
        this.deletado = false;
    }

    public void editar(EditarUsuarioDTO dadosUsuario) {
        if (dadosUsuario.nome() != null) {
            this.nome = dadosUsuario.nome();
        }
        if (dadosUsuario.login() != null) {
            this.login = dadosUsuario.login();
        }
        if (dadosUsuario.username() != null) {
            this.username = dadosUsuario.username();
        }
        if (dadosUsuario.senha() != null) {
            this.senha = dadosUsuario.senha();
        }
    }

    public void changeDeletado() {
        this.deletado = !this.deletado;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+papel.name()));
    }

    public String getUserUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return login;
    }
}
