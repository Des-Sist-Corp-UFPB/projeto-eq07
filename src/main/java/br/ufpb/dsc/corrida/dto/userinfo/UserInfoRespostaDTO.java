package br.ufpb.dsc.corrida.dto.userinfo;

import br.ufpb.dsc.corrida.domain.UserInfo;
import br.ufpb.dsc.corrida.enums.Genero;
import br.ufpb.dsc.corrida.enums.NivelCondicionamento;

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
        this(
                userInfo.getId(),
                userInfo.getUsuario().getId(),
                userInfo.getPeso(),
                userInfo.getAltura(),
                userInfo.getGenero(),
                userInfo.getTotalKmRun(),
                userInfo.getDataNasc(),
                userInfo.getFotoPerfil(),
                userInfo.getNivelCondicionamento(),
                userInfo.getNotasMedicas(),
                userInfo.getCriadoEm(),
                userInfo.getAtualizadoEm()
        );
    }
}
