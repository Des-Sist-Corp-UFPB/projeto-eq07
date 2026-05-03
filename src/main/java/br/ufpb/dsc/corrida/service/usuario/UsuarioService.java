package br.ufpb.dsc.corrida.service.usuario;

import br.ufpb.dsc.corrida.config.security.TokenService;
import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.dto.user.LoginDto;
import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
}
