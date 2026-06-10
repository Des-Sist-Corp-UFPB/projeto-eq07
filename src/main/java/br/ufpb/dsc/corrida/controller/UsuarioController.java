package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.dto.user.*;
import br.ufpb.dsc.corrida.service.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/user")
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

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity edit(@RequestBody @Valid EditarUsuarioDTO dadosUsuario, @PathVariable Long id) {
        Usuario usuario = service.editar(dadosUsuario, id);
        return ResponseEntity.ok(new UsuarioResposta(usuario));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}/profile")
    public ModelAndView getProfilePage(@PathVariable String username) {
        PerfilPublicoDTO perfil = service.buscarPerfilPublico(username);
        ModelAndView mv = new ModelAndView("perfil-publico");
        mv.addObject("perfil", perfil);
        return mv;
    }

    @GetMapping("/{username}")
    public ResponseEntity<PerfilPublicoDTO> getProfileApi(@PathVariable String username) {
        PerfilPublicoDTO perfil = service.buscarPerfilPublico(username);
        return ResponseEntity.ok(perfil);
    }

}
