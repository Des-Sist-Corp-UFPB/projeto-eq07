package br.ufpb.dsc.corrida.domain;

import br.ufpb.dsc.corrida.dto.user.EditarUsuarioDTO;
import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.enums.Papel;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void deveCriarUsuarioComSenhaCriptografada() {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("Nome Teste", "username_teste", "login_teste", "senha123");
        String senhaCriptografada = "encoded_senha123";

        Usuario usuario = new Usuario(dto, senhaCriptografada);

        assertEquals("Nome Teste", usuario.getNome());
        assertEquals("username_teste", usuario.getUserUsername());
        assertEquals("login_teste", usuario.getLogin());
        assertEquals(senhaCriptografada, usuario.getSenha());
        assertEquals(Papel.USUARIO, usuario.getPapel());
        assertFalse(usuario.getDeletado());
    }

    @Test
    void deveEditarCamposComSucesso() {
        Usuario usuario = new Usuario(1L, "Nome Antigo", "user_antigo", "login_antigo", "senha_antiga", Papel.USUARIO, false);
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Nome Novo", "user_novo", "login_novo", "senha_nova");

        usuario.editar(dto);

        assertEquals("Nome Novo", usuario.getNome());
        assertEquals("user_novo", usuario.getUserUsername());
        assertEquals("login_novo", usuario.getLogin());
        assertEquals("senha_nova", usuario.getSenha());
    }

    @Test
    void deveEditarApenasCamposNaoNulos() {
        Usuario usuario = new Usuario(1L, "Nome Antigo", "user_antigo", "login_antigo", "senha_antiga", Papel.USUARIO, false);
        EditarUsuarioDTO dto = new EditarUsuarioDTO(null, "user_novo", null, null);

        usuario.editar(dto);

        assertEquals("Nome Antigo", usuario.getNome());
        assertEquals("user_novo", usuario.getUserUsername());
        assertEquals("login_antigo", usuario.getLogin());
        assertEquals("senha_antiga", usuario.getSenha());
    }

    @Test
    void deveAlternarStatusDeletado() {
        Usuario usuario = new Usuario(1L, "Nome", "user", "login", "senha", Papel.USUARIO, false);

        usuario.changeDeletado();
        assertTrue(usuario.getDeletado());

        usuario.changeDeletado();
        assertFalse(usuario.getDeletado());
    }

    @Test
    void deveRetornarAuthoritiesCorretas() {
        Usuario usuario = new Usuario(1L, "Nome", "user", "login", "senha", Papel.USUARIO, false);

        Collection<? extends GrantedAuthority> authorities = usuario.getAuthorities();

        assertEquals(1, authorities.size());
        assertEquals("ROLE_USUARIO", authorities.iterator().next().getAuthority());
    }

    @Test
    void deveRetornarLoginNoGetUsername() {
        Usuario usuario = new Usuario(1L, "Nome", "user", "login_teste", "senha", Papel.USUARIO, false);
        assertEquals("login_teste", usuario.getUsername());
    }

    @Test
    void deveRetornarSenhaNoGetPassword() {
        Usuario usuario = new Usuario(1L, "Nome", "user", "login", "senha_teste", Papel.USUARIO, false);
        assertEquals("senha_teste", usuario.getPassword());
    }
}
