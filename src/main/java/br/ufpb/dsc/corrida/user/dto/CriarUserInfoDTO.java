package br.ufpb.dsc.corrida.user.dto;

import br.ufpb.dsc.corrida.user.Genero;
import br.ufpb.dsc.corrida.user.NivelCondicionamento;
import jakarta.validation.constraints.Positive;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO para criação de um registro UserInfo.
 *
 * @param usuarioId          ID do usuário (obrigatório)
 * @param peso               Peso em kg (obrigatório, deve ser positivo)
 * @param altura             Altura em cm (obrigatório, deve ser positivo)
 * @param genero             Gênero (obrigatório)
 * @param dataNasc           Data de nascimento (obrigatório)
 * @param fotoPerfil         URL da foto de perfil (opcional)
 * @param nivelCondicionamento Nível de condicionamento (opcional)
 * @param notasMedicas       Observações médicas (opcional)
 */
public record CriarUserInfoDTO(
        @NotNull
        Long usuarioId,

        @NotNull(message = "O peso é obrigatório")
        @Positive(message = "O peso deve ser maior que 0")
        Float peso,

        @NotNull(message = "A altura é obrigatória")
        @Positive(message = "A altura deve ser maior que 0")
        Float altura,

        @NotNull
        Genero genero,

        @NotNull
        LocalDate dataNasc,

        String fotoPerfil,

        NivelCondicionamento nivelCondicionamento,

        String notasMedicas,

        String cpf,

        Boolean consentimentoSaude
) {}
