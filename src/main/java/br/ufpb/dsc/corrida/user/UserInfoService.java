package br.ufpb.dsc.corrida.user;

import br.ufpb.dsc.corrida.audit.Auditable;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoJaExistenteException;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.user.dto.AtualizarUserInfoDTO;
import br.ufpb.dsc.corrida.user.dto.CriarUserInfoDTO;
import br.ufpb.dsc.corrida.user.dto.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.storage.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;


import java.util.UUID;


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
    private UserRepository usuarioRepository;

    @Autowired
    private MinioService minioService;

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
    @Auditable(action = "CREATE_USER_INFO", resource = "USER_INFO")
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
        userInfo.setFotoPerfilObjectKey(dto.fotoPerfil());
        userInfo.setNivelCondicionamento(dto.nivelCondicionamento());
        userInfo.setNotasMedicas(dto.notasMedicas());
        userInfo.setConsentimentoSaude(Boolean.TRUE.equals(dto.consentimentoSaude()));
        if (dto.cpf() != null && !dto.cpf().isBlank()) {
            validarCpf(dto.cpf());
            userInfo.setCpf(dto.cpf().replaceAll("\\D", ""));
        }
        userInfo.setTotalKmRun(0.0f);

        var salvo = userInfoRepository.save(userInfo);
        log.info("UserInfo criado com sucesso para usuarioId={}", dto.usuarioId());
        return toDTO(salvo);
    }

    /**
     * Busca as informações de um corredor pelo ID do usuário.
     *
     * @param usuarioId ID do usuário
     * @return DTO com os dados do corredor
     * @throws UserInfoNaoEncontradoException se não há registro para o userId informado
     */
    @Transactional(readOnly = true)
    @Auditable(action = "GET_USER_INFO", resource = "USER_INFO")
    public UserInfoRespostaDTO buscarPorUsuarioId(Long usuarioId) {
        log.info("Buscando UserInfo para usuarioId={}", usuarioId);

        var userInfo = userInfoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UserInfoNaoEncontradoException(
                        "Informações de corredor não encontradas para o usuário com ID: " + usuarioId));

        return toDTO(userInfo);
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
    @Auditable(action = "UPDATE_USER_INFO", resource = "USER_INFO", entityClass = UserInfo.class, idParam = "usuarioId")
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
            userInfo.setFotoPerfilObjectKey(dto.fotoPerfil());
        }
        if (dto.nivelCondicionamento() != null) {
            userInfo.setNivelCondicionamento(dto.nivelCondicionamento());
        }
        if (dto.notasMedicas() != null) {
            userInfo.setNotasMedicas(dto.notasMedicas());
        }
        if (dto.consentimentoSaude() != null) {
            userInfo.setConsentimentoSaude(dto.consentimentoSaude());
        }
        if (dto.cpf() != null) {
            if (dto.cpf().isBlank()) {
                userInfo.setCpf(null);
            } else {
                validarCpf(dto.cpf());
                userInfo.setCpf(dto.cpf().replaceAll("\\D", ""));
            }
        }

        var atualizado = userInfoRepository.save(userInfo);
        log.info("UserInfo atualizado com sucesso para usuarioId={}", usuarioId);
        return toDTO(atualizado);
    }

    /**
     * Salva a foto de perfil enviada em um diretório físico local e atualiza a URL na entidade.
     *
     * @param usuarioId ID do usuário
     * @param file Arquivo contendo a foto
     * @return DTO com os dados do corredor atualizados
     * @throws IllegalArgumentException se o arquivo for inválido
     * @throws RuntimeException se houver falha de I/O
     */
    @Transactional
    @Auditable(action = "UPDATE_PROFILE_PHOTO", resource = "USER_INFO", entityClass = UserInfo.class, idParam = "usuarioId")
    public UserInfoRespostaDTO uploadFotoPerfil(Long usuarioId, MultipartFile file) {
        log.info("Iniciando upload de foto de perfil para usuarioId={}", usuarioId);

        var userInfo = userInfoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UserInfoNaoEncontradoException(
                        "Informações de corredor não encontradas para o usuário com ID: " + usuarioId));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de imagem não pode estar vazio");
        }

        try {
            // Gerar nome de arquivo único
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
            String extension = "";
            int i = originalFileName.lastIndexOf('.');
            if (i > 0) {
                extension = originalFileName.substring(i);
            }

            String objectName = "fotos-perfil/" + usuarioId + "/" + UUID.randomUUID().toString() + extension;

            // Salvar no MinIO
            String objectKey = minioService.upload(file, objectName);
            
            // Guardar a chave antiga para apagar depois
            String oldObjectKey = userInfo.getFotoPerfilObjectKey();

            // Atualizar entidade com a chave do objeto
            userInfo.setFotoPerfilObjectKey(objectKey);
            var atualizado = userInfoRepository.save(userInfo);

            // Excluir a antiga (best-effort)
            if (oldObjectKey != null && !oldObjectKey.isEmpty()) {
                try {
                    minioService.delete(oldObjectKey);
                } catch (Exception e) {
                    log.warn("Falha ao remover foto antiga do MinIO: {}", e.getMessage());
                }
            }

            log.info("Foto de perfil salva com sucesso para usuarioId={}. ObjectKey: {}", usuarioId, objectKey);
            return toDTO(atualizado);

        } catch (Exception e) {
            log.error("Erro ao salvar o arquivo de foto de perfil", e);
            throw new RuntimeException("Não foi possível salvar a imagem. Tente novamente mais tarde.", e);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────

    private UserInfoRespostaDTO toDTO(UserInfo userInfo) {
        String presignedUrl = minioService.getPresignedUrl(userInfo.getFotoPerfilObjectKey());
        return new UserInfoRespostaDTO(userInfo, presignedUrl);
    }

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

    /**
     * Valida os dígitos verificadores do CPF utilizando o algoritmo mod 11.
     *
     * @param cpf string de CPF (com ou sem máscara)
     * @throws IllegalArgumentException se o CPF for inválido
     */
    public static void validarCpf(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("CPF não pode ser nulo");
        }
        String cleanCpf = cpf.replaceAll("\\D", "");
        if (cleanCpf.length() != 11) {
            throw new IllegalArgumentException("CPF deve conter exatamente 11 dígitos");
        }
        // CPFs com todos os dígitos iguais são inválidos
        if (cleanCpf.matches("(\\d)\\1{10}")) {
            throw new IllegalArgumentException("CPF inválido");
        }

        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += (cleanCpf.charAt(i) - '0') * (10 - i);
            }
            int resto = 11 - (soma % 11);
            int digito1 = (resto == 10 || resto == 11) ? 0 : resto;

            if (digito1 != (cleanCpf.charAt(9) - '0')) {
                throw new IllegalArgumentException("CPF inválido (dígito verificador incorreto)");
            }

            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += (cleanCpf.charAt(i) - '0') * (11 - i);
            }
            resto = 11 - (soma % 11);
            int digito2 = (resto == 10 || resto == 11) ? 0 : resto;

            if (digito2 != (cleanCpf.charAt(10) - '0')) {
                throw new IllegalArgumentException("CPF inválido (dígito verificador incorreto)");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("CPF inválido: " + e.getMessage());
        }
    }
}
