package br.ufpb.dsc.corrida.service.usuario;

import br.ufpb.dsc.corrida.config.security.TokenService;
import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.dto.user.EditarUsuarioDTO;
import br.ufpb.dsc.corrida.dto.user.LoginDto;
import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.enums.Papel;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitido;
import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistente;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontrado;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;


    public String registrar(RegistrarUsuarioDTO usuarioDTO) {
        log.info("Iniciando serviço de registro de usuário");
        if (repository.existsByLogin(usuarioDTO.login())) {
            throw new UsuarioJaExistente("Login já utilizado, tente outro");
        }
        if (repository.existsByUsername(usuarioDTO.username())) {
            throw new UsuarioJaExistente("Username já utilizado, tente outro");
        }
        Usuario usuario = new Usuario(usuarioDTO);
        repository.save(usuario);
        log.info("Usuário criado com sucesso");
        return tokenService.criarToken(usuario);
    }

    public String login(LoginDto credenciais) {
        log.info("Iniciando serviço de login do usuário {}", credenciais.login());
        var token = new UsernamePasswordAuthenticationToken(credenciais.login(), credenciais.senha());
        var autenticacao = authenticationManager.authenticate(token);
        return tokenService.criarToken((Usuario) autenticacao.getPrincipal());
    }

    public Usuario editar(EditarUsuarioDTO dadosUsuario, Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioLogado = (Usuario) auth.getPrincipal();
        Usuario usuario = repository.findById(id).orElseThrow(() -> new UsuarioNaoEncontrado("Usuário com ID: "+ id +" não encontrado"));
        if (usuario.getId() != usuarioLogado.getId()) throw new AcessoNaoPermitido("Acesso negado para edição de usuário");
        if (dadosUsuario.login() != null) {
            if (repository.existsByLoginAndIdNot(dadosUsuario.login(), id)) throw new UsuarioJaExistente("Usuário com login: " + dadosUsuario.login() + " já existente");
        }
        if (dadosUsuario.username() != null) {
            if (repository.existsByUsernameAndIdNot(dadosUsuario.username(), id)) throw new UsuarioJaExistente("Usuário com username: " + dadosUsuario.username() + " já existente");
        }
        usuario.editar(dadosUsuario);
        return usuario;
    }

    public void deletar(Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioLogado = (Usuario) auth.getPrincipal();
        Usuario usuario = repository.findById(id).orElseThrow(() -> new UsuarioNaoEncontrado("Usuário com ID: "+ id +" não encontrado"));
        if (usuarioLogado.getPapel() == Papel.USUARIO) {
            if (usuarioLogado.getId() != usuario.getId()) throw new AcessoNaoPermitido("Acesso negado para deleção de usuário");
        }
        usuario.changeDeletado();
    }

}
