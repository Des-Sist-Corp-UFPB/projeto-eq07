package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.dto.user.AutenticacaoRespostaDTO;
import br.ufpb.dsc.corrida.dto.user.LoginDto;
import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.service.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    @PostMapping("/registrar")
    public ResponseEntity<AutenticacaoRespostaDTO> registrar(@RequestBody @Valid RegistrarUsuarioDTO dadosUsuario) {
        String token = service.registrar(dadosUsuario);
        return ResponseEntity.ok(new AutenticacaoRespostaDTO(token, "Usuário registrado com sucesso"));
    }

    @PostMapping("/login")
    public ResponseEntity<AutenticacaoRespostaDTO> login(@RequestBody @Valid LoginDto credenciais) {
        String token = service.login(credenciais);
        return ResponseEntity.ok(new AutenticacaoRespostaDTO(token, "Autenticado com sucesso"));
    }
}
