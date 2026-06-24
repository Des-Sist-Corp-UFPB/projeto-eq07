package br.ufpb.dsc.corrida.user;

import br.ufpb.dsc.corrida.config.security.TokenService;
import br.ufpb.dsc.corrida.user.dto.EditarUsuarioDTO;
import br.ufpb.dsc.corrida.user.dto.LoginDto;
import br.ufpb.dsc.corrida.user.dto.PerfilPublicoDTO;
import br.ufpb.dsc.corrida.user.dto.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistenteException;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import br.ufpb.dsc.corrida.storage.StorageService;
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
    private UserRepository repository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;


    public String registrar(RegistrarUsuarioDTO usuarioDTO) {
        log.info("Iniciando serviço de registro de usuário");
        if (repository.existsByLogin(usuarioDTO.login())) {
            throw new UsuarioJaExistenteException("Login já utilizado, tente outro");
        }
        if (repository.existsByUsername(usuarioDTO.username())) {
            throw new UsuarioJaExistenteException("Username já utilizado, tente outro");
        }
        User usuario = new User(usuarioDTO);
        repository.save(usuario);
        log.info("Usuário criado com sucesso");
        return tokenService.criarToken(usuario);
    }

    public String login(LoginDto credenciais) {
        log.info("Iniciando serviço de login do usuário {}", credenciais.login());
        var token = new UsernamePasswordAuthenticationToken(credenciais.login(), credenciais.senha());
        var autenticacao = authenticationManager.authenticate(token);
        return tokenService.criarToken((User) autenticacao.getPrincipal());
    }

    public User editar(EditarUsuarioDTO dadosUsuario, Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User usuarioLogado = (User) auth.getPrincipal();
        User usuario = repository.findById(id).orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário com ID: "+ id +" não encontrado"));
        if (usuario.getId() != usuarioLogado.getId()) throw new AcessoNaoPermitidoException("Acesso negado para edição de usuário");
        if (dadosUsuario.login() != null && repository.existsByLoginAndIdNot(dadosUsuario.login(), id)) throw new UsuarioJaExistenteException("Usuário com login: " + dadosUsuario.login() + " já existente");
        if (dadosUsuario.username() != null && repository.existsByUsernameAndIdNot(dadosUsuario.username(), id)) throw new UsuarioJaExistenteException("Usuário com username: " + dadosUsuario.username() + " já existente");
        usuario.editar(dadosUsuario);
        return usuario;
    }

    public void deletar(Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User usuarioLogado = (User) auth.getPrincipal();
        User usuario = repository.findById(id).orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário com ID: "+ id +" não encontrado"));
        if (usuarioLogado.getPapel() == Papel.USUARIO && usuarioLogado.getId() != usuario.getId()) throw new AcessoNaoPermitidoException("Acesso negado para deleção de usuário");
        usuario.changeDeletado();
    }

    @Autowired
    private StorageService storageService;

    public PerfilPublicoDTO buscarPerfilPublico(String username) {
        User usuario = repository.findByUsername(username);
        if (usuario == null) {
            throw new UsuarioNaoEncontradoException("Usuário com username: " + username + " não encontrado");
        }

        var userInfoOpt = userInfoRepository.findByUsuarioId(usuario.getId());

        String fotoPerfil = null;
        Float totalKmRun = 0.0f;

        if (userInfoOpt.isPresent()) {
            String fotoKey = userInfoOpt.get().getFotoPerfil();
            if (fotoKey != null && !fotoKey.isBlank()) {
                fotoPerfil = storageService.getPresignedUrl(fotoKey);
            }
            totalKmRun = userInfoOpt.get().getTotalKmRun();
        }

        return new PerfilPublicoDTO(
                usuario.getNome(),
                usuario.getUserUsername(),
                fotoPerfil,
                totalKmRun
        );
    }

}
