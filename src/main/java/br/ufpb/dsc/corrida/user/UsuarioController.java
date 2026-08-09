package br.ufpb.dsc.corrida.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistenteException;
import br.ufpb.dsc.corrida.user.dto.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.user.dto.AtualizarUserInfoDTO;
import br.ufpb.dsc.corrida.user.dto.AutenticacaoRespostaDTO;
import br.ufpb.dsc.corrida.user.dto.CriarUserInfoDTO;
import br.ufpb.dsc.corrida.user.dto.EditarUsuarioDTO;
import br.ufpb.dsc.corrida.user.dto.LoginDto;
import br.ufpb.dsc.corrida.user.dto.PerfilPublicoDTO;
import br.ufpb.dsc.corrida.user.dto.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.user.dto.UsuarioResposta;
import br.ufpb.dsc.corrida.userConections.UserConnectionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/user")
public class UsuarioController {
    
    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    @Autowired
    private UsuarioService service;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private UserConnectionService userConnectionService;


    @PostMapping("/registrar")
    public String registrarUsuario(@ModelAttribute("usuario") @Valid RegistrarUsuarioDTO usuarioDTO, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/registrar";
        }
        try {
            service.registrar(usuarioDTO);
            return "redirect:/login?success=true";
        } catch (UsuarioJaExistenteException e) {
            log.warn("Tentativa de registrar usuário já existente: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "auth/registrar";
        } catch (Exception e) {
            log.error("Erro inesperado ao registrar usuário", e);
            model.addAttribute("error", "Ocorreu um erro inesperado. Tente novamente.");
            return "auth/registrar";
        }
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<AutenticacaoRespostaDTO> login(@ModelAttribute @Valid LoginDto credenciais) {
        String token = service.login(credenciais);
        return ResponseEntity.ok(new AutenticacaoRespostaDTO(token, "Autenticado com sucesso"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity edit(@RequestBody @Valid EditarUsuarioDTO dadosUsuario, @PathVariable Long id) {
        User usuario = service.editar(dadosUsuario, id);
        return ResponseEntity.ok(new UsuarioResposta(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}")
    public ResponseEntity<PerfilPublicoDTO> getProfileApi(@PathVariable String username) {
        PerfilPublicoDTO perfil = service.buscarPerfilPublico(username);
        return ResponseEntity.ok(perfil);
    }

    @GetMapping("/userInfo/{usuarioId}")
    public ResponseEntity<UserInfoRespostaDTO> buscarPorUsuarioId(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(userInfoService.buscarPorUsuarioId(usuarioId));
    }

    @PostMapping("/userInfo")
    public ResponseEntity<UserInfoRespostaDTO> criar(@RequestBody @Valid CriarUserInfoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userInfoService.criar(dto));
    }

    @PutMapping("/userInfo/{usuarioId}")
    public ResponseEntity<UserInfoRespostaDTO> atualizar(
            @PathVariable Long usuarioId,
            @RequestBody AtualizarUserInfoDTO dto) {
        return ResponseEntity.ok(userInfoService.atualizar(usuarioId, dto));
    }

    @PostMapping("/userInfo/{usuarioId}/foto-perfil")
    public ResponseEntity<UserInfoRespostaDTO> uploadFoto(
            @PathVariable Long usuarioId,
            @RequestParam("file") MultipartFile file) {
        
        UserInfoRespostaDTO resposta = userInfoService.uploadFotoPerfil(usuarioId, file);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/conexao/enviar/{receiverId}")
    public ResponseEntity<?> enviarConexao(
            @PathVariable Long receiverId,
            @AuthenticationPrincipal User loggedInUser) {
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado");
        }
        userConnectionService.sendConnectionRequest(loggedInUser.getId(), receiverId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conexao/aceitar/{requestId}")
    public ResponseEntity<?> aceitarConexao(
            @PathVariable Long requestId,
            @AuthenticationPrincipal User loggedInUser) {
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado");
        }
        userConnectionService.acceptConnectionRequest(requestId, loggedInUser.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conexao/recusar/{requestId}")
    public ResponseEntity<?> recusarConexao(
            @PathVariable Long requestId,
            @AuthenticationPrincipal User loggedInUser) {
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado");
        }
        userConnectionService.declineConnectionRequest(requestId, loggedInUser.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/conexao/remover/{receiverId}")
    public ResponseEntity<?> removerConexao(
            @PathVariable Long receiverId,
            @AuthenticationPrincipal User loggedInUser) {
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado");
        }
        userConnectionService.removeConnection(loggedInUser.getId(), receiverId);
        return ResponseEntity.ok().build();
    }
}
