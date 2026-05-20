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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Transactional
    public String registrar(RegistrarUsuarioDTO usuarioDTO) {
        log.info("Iniciando serviço de registro de usuário");
        if (repository.existsByLogin(usuarioDTO.login())) {
            throw new UsuarioJaExistente("Login já utilizado, tente outro");
        }
        if (repository.existsByUsername(usuarioDTO.username())) {
            throw new UsuarioJaExistente("Username já utilizado, tente outro");
        }
        Usuario usuario = new Usuario(usuarioDTO, passwordEncoder.encode(usuarioDTO.senha()));
        repository.save(usuario);
        log.info("Usuário criado com sucesso");
        return tokenService.criarToken(usuario);
    }

    public String login(LoginDto credenciais) {
        log.info("Iniciando serviço de login do usuário {}", credenciais.login());
        var token = new UsernamePasswordAuthenticationToken(credenciais.login(), credenciais.senha());
        var autenticacao = authenticationManager.authenticate(token);
        var principal = autenticacao.getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new AcessoNaoPermitido("Principal inválido após autenticação");
        }
        return tokenService.criarToken((Usuario) principal);
    }

    @Transactional
    public Usuario editar(EditarUsuarioDTO dadosUsuario, Long id) {
        Usuario usuarioLogado = getUsuarioLogado();
        Usuario usuario = repository.findById(id).orElseThrow(() -> new UsuarioNaoEncontrado("Usuário com ID: "+ id +" não encontrado"));
        if (!usuario.getId().equals(usuarioLogado.getId())) throw new AcessoNaoPermitido("Acesso negado para edição de usuário");
        if (dadosUsuario.login() != null) {
            if (repository.existsByLoginAndIdNot(dadosUsuario.login(), id)) throw new UsuarioJaExistente("Usuário com login: " + dadosUsuario.login() + " já existente");
        }
        if (dadosUsuario.username() != null) {
            if (repository.existsByUsernameAndIdNot(dadosUsuario.username(), id)) throw new UsuarioJaExistente("Usuário com username: " + dadosUsuario.username() + " já existente");
        }
        usuario.editar(dadosUsuario);
        return usuario;
    }

    @Transactional
    public void deletar(Long id) {
        Usuario usuarioLogado = getUsuarioLogado();
        Usuario usuario = repository.findById(id).orElseThrow(() -> new UsuarioNaoEncontrado("Usuário com ID: "+ id +" não encontrado"));
        if (usuarioLogado.getPapel() == Papel.USUARIO) {
            if (!usuarioLogado.getId().equals(usuario.getId())) throw new AcessoNaoPermitido("Acesso negado para deleção de usuário");
        }
        usuario.changeDeletado();
    }

    private Usuario getUsuarioLogado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Usuario)) {
            throw new AcessoNaoPermitido("Acesso negado: Usuário não autenticado");
        }
        return (Usuario) auth.getPrincipal();
    }

}
