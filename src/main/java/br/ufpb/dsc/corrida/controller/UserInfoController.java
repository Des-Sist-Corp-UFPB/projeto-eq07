package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.dto.userinfo.AtualizarUserInfoDTO;
import br.ufpb.dsc.corrida.dto.userinfo.CriarUserInfoDTO;
import br.ufpb.dsc.corrida.dto.userinfo.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.service.userinfo.UserInfoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para o módulo de informações de corredor.
 *
 * <p>Expõe os endpoints:
 * <ul>
 *   <li>{@code GET /user-info/{usuarioId}} — busca o perfil de um corredor</li>
 *   <li>{@code POST /user-info} — cria um novo perfil</li>
 *   <li>{@code PUT /user-info/{usuarioId}} — atualiza o perfil existente</li>
 * </ul>
 *
 * <p>Todas as rotas requerem autenticação (coberto por {@code anyRequest().authenticated()}
 * na configuração de segurança existente). Exceções são tratadas centralmente
 * pelo {@link br.ufpb.dsc.corrida.exception.GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/user-info")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * Busca as informações de um corredor pelo ID do usuário.
     *
     * @param usuarioId ID do usuário
     * @return 200 OK com o DTO de resposta
     */
    @GetMapping("/{usuarioId}")
    public ResponseEntity<UserInfoRespostaDTO> buscarPorUsuarioId(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(userInfoService.buscarPorUsuarioId(usuarioId));
    }

    /**
     * Cria um novo perfil de informações para um corredor.
     *
     * @param dto dados do perfil a ser criado
     * @return 201 Created com o DTO de resposta
     */
    @PostMapping
    public ResponseEntity<UserInfoRespostaDTO> criar(@RequestBody @Valid CriarUserInfoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userInfoService.criar(dto));
    }

    /**
     * Atualiza parcialmente o perfil de um corredor.
     *
     * @param usuarioId ID do usuário cujo perfil será atualizado
     * @param dto       dados a atualizar
     * @return 200 OK com o DTO de resposta atualizado
     */
    @PutMapping("/{usuarioId}")
    public ResponseEntity<UserInfoRespostaDTO> atualizar(
            @PathVariable Long usuarioId,
            @RequestBody AtualizarUserInfoDTO dto) {
        return ResponseEntity.ok(userInfoService.atualizar(usuarioId, dto));
    }
}
