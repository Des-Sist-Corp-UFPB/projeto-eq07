package br.ufpb.dsc.corrida.user.dto;

import br.ufpb.dsc.corrida.user.UserInfo;
import br.ufpb.dsc.corrida.user.Genero;
import br.ufpb.dsc.corrida.user.NivelCondicionamento;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de resposta para um registro UserInfo.
 */
public record UserInfoRespostaDTO(
        Long id,
        Long usuarioId,
        Float peso,
        Float altura,
        Genero genero,
        Float totalKmRun,
        LocalDate dataNasc,
        String fotoPerfil,
        NivelCondicionamento nivelCondicionamento,
        String notasMedicas,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    /**
     * Construtor de conveniência que mapeia uma entidade {@link UserInfo} para o DTO.
     *
     * @param userInfo entidade a ser mapeada
     */
    public UserInfoRespostaDTO(UserInfo userInfo) {
        this(userInfo, userInfo.getFotoPerfil());
    }

    /**
     * Construtor que permite injetar uma URL customizada para a foto de perfil.
     * Útil quando usamos presigned URLs do S3.
     *
     * @param userInfo entidade a ser mapeada
     * @param fotoPerfilUrl URL da foto (presigned) ou nome do arquivo
     */
    public UserInfoRespostaDTO(UserInfo userInfo, String fotoPerfilUrl) {
        this(
                userInfo.getId(),
                userInfo.getUsuario().getId(),
                userInfo.getPeso(),
                userInfo.getAltura(),
                userInfo.getGenero(),
                userInfo.getTotalKmRun(),
                userInfo.getDataNasc(),
                fotoPerfilUrl,
                userInfo.getNivelCondicionamento(),
                userInfo.getNotasMedicas(),
                userInfo.getCriadoEm(),
                userInfo.getAtualizadoEm()
        );
    }
}
