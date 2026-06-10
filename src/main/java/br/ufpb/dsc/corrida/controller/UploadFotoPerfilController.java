package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.dto.userinfo.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.service.userinfo.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user-info")
@RequiredArgsConstructor
public class UploadFotoPerfilController {

    private final UserInfoService userInfoService;

    /**
     * Endpoint para fazer o upload da foto de perfil do corredor.
     * Recebe multipart/form-data.
     *
     * @param usuarioId ID do usuário.
     * @param file Arquivo da foto.
     * @return DTO atualizado com a nova URL da foto.
     */
    @PostMapping("/{usuarioId}/foto-perfil")
    public ResponseEntity<UserInfoRespostaDTO> uploadFoto(
            @PathVariable Long usuarioId,
            @RequestParam("file") MultipartFile file) {
        
        UserInfoRespostaDTO resposta = userInfoService.uploadFotoPerfil(usuarioId, file);
        return ResponseEntity.ok(resposta);
    }
}
