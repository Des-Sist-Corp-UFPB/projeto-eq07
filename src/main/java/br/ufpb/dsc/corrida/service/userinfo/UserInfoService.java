package br.ufpb.dsc.corrida.service.userinfo;

import br.ufpb.dsc.corrida.domain.UserInfo;
import br.ufpb.dsc.corrida.dto.userinfo.AtualizarUserInfoDTO;
import br.ufpb.dsc.corrida.dto.userinfo.CriarUserInfoDTO;
import br.ufpb.dsc.corrida.dto.userinfo.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoJaExistenteException;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.repository.UserInfoRepository;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Serviço responsável pela lógica de negócio do módulo de informações de corredor.
 *
 * <p>Gerencia a criação, consulta e atualização dos dados físicos e médicos
 * associados a cada usuário (relação 1:1 com a tabela de usuários).</p>
 */
@Slf4j
@Service
public class UserInfoService {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Cria um novo registro de informações para um corredor.
     *
     * <p>Regras de negócio aplicadas:
     * <ul>
     *   <li>O userId deve corresponder a um usuário existente.</li>
     *   <li>Só é permitido um registro por usuário.</li>
     *   <li>Peso deve ser maior que 0.</li>
     *   <li>Altura deve ser maior que 0.</li>
     * </ul>
     *
     * @param dto dados para criação do registro
     * @return DTO com os dados do registro criado
     * @throws UsuarioNaoEncontradoException  se o userId não corresponde a um usuário existente
     * @throws UserInfoJaExistenteException   se já existe um registro para o userId
     * @throws IllegalArgumentException       se peso ou altura forem inválidos (≤ 0)
     */
    @Transactional
    public UserInfoRespostaDTO criar(CriarUserInfoDTO dto) {
        log.info("Iniciando criação de UserInfo para usuarioId={}", dto.usuarioId());

        var usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException(
                        "Usuário com ID: " + dto.usuarioId() + " não encontrado"));

        if (userInfoRepository.existsByUsuarioId(dto.usuarioId())) {
            throw new UserInfoJaExistenteException(
                    "Já existe um perfil de informações para o usuário com ID: " + dto.usuarioId());
        }

        validarPeso(dto.peso());
        validarAltura(dto.altura());

        var userInfo = new UserInfo();
        userInfo.setUsuario(usuario);
        userInfo.setPeso(dto.peso());
        userInfo.setAltura(dto.altura());
        userInfo.setGenero(dto.genero());
        userInfo.setDataNasc(dto.dataNasc());
        userInfo.setFotoPerfil(dto.fotoPerfil());
        userInfo.setNivelCondicionamento(dto.nivelCondicionamento());
        userInfo.setNotasMedicas(dto.notasMedicas());
        userInfo.setTotalKmRun(0.0f);

        var salvo = userInfoRepository.save(userInfo);
        log.info("UserInfo criado com sucesso para usuarioId={}", dto.usuarioId());
        return new UserInfoRespostaDTO(salvo);
    }

    /**
     * Busca as informações de um corredor pelo ID do usuário.
     *
     * @param usuarioId ID do usuário
     * @return DTO com os dados do corredor
     * @throws UserInfoNaoEncontradoException se não há registro para o userId informado
     */
    @Transactional(readOnly = true)
    public UserInfoRespostaDTO buscarPorUsuarioId(Long usuarioId) {
        log.info("Buscando UserInfo para usuarioId={}", usuarioId);

        var userInfo = userInfoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UserInfoNaoEncontradoException(
                        "Informações de corredor não encontradas para o usuário com ID: " + usuarioId));

        return new UserInfoRespostaDTO(userInfo);
    }

    /**
     * Atualiza parcialmente as informações de um corredor.
     *
     * <p>Apenas os campos não-nulos do DTO serão aplicados.
     * Validações de peso e altura são aplicadas quando os respectivos campos são fornecidos.</p>
     *
     * @param usuarioId ID do usuário cujo registro será atualizado
     * @param dto       dados a atualizar (campos nulos são ignorados)
     * @return DTO com os dados atualizados
     * @throws UserInfoNaoEncontradoException se não há registro para o userId
     * @throws IllegalArgumentException       se peso ou altura fornecidos forem ≤ 0
     */
    @Transactional
    public UserInfoRespostaDTO atualizar(Long usuarioId, AtualizarUserInfoDTO dto) {
        log.info("Atualizando UserInfo para usuarioId={}", usuarioId);

        var userInfo = userInfoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UserInfoNaoEncontradoException(
                        "Informações de corredor não encontradas para o usuário com ID: " + usuarioId));

        if (dto.peso() != null) {
            validarPeso(dto.peso());
            userInfo.setPeso(dto.peso());
        }
        if (dto.altura() != null) {
            validarAltura(dto.altura());
            userInfo.setAltura(dto.altura());
        }
        if (dto.genero() != null) {
            userInfo.setGenero(dto.genero());
        }
        if (dto.dataNasc() != null) {
            userInfo.setDataNasc(dto.dataNasc());
        }
        if (dto.fotoPerfil() != null) {
            userInfo.setFotoPerfil(dto.fotoPerfil());
        }
        if (dto.nivelCondicionamento() != null) {
            userInfo.setNivelCondicionamento(dto.nivelCondicionamento());
        }
        if (dto.notasMedicas() != null) {
            userInfo.setNotasMedicas(dto.notasMedicas());
        }

        var atualizado = userInfoRepository.save(userInfo);
        log.info("UserInfo atualizado com sucesso para usuarioId={}", usuarioId);
        return new UserInfoRespostaDTO(atualizado);
    }

    // ─── Helpers de validação ───────────────────────────────────────────────

    private void validarPeso(Float peso) {
        if (peso == null || peso <= 0) {
            throw new IllegalArgumentException("O peso deve ser maior que 0");
        }
    }

    private void validarAltura(Float altura) {
        if (altura == null || altura <= 0) {
            throw new IllegalArgumentException("A altura deve ser maior que 0");
        }
    }
}
