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
        Boolean consentimentoSaude,
        String cpf,
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
                userInfo.getConsentimentoSaude(),
                userInfo.getCpf(),
                userInfo.getCriadoEm(),
                userInfo.getAtualizadoEm()
        );
    }

    /**
     * Retorna o CPF formatado para exibição na UI (000.000.000-00).
     * Retorna null se o CPF não estiver cadastrado.
     */
    public String getCpfFormatado() {
        if (cpf == null || cpf.length() != 11) return null;
        return cpf.substring(0, 3) + "." +
               cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" +
               cpf.substring(9, 11);
    }
}
