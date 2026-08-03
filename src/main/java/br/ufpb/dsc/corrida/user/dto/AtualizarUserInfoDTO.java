package br.ufpb.dsc.corrida.user.dto;

import br.ufpb.dsc.corrida.user.Genero;
import br.ufpb.dsc.corrida.user.NivelCondicionamento;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * DTO para atualização parcial de um registro UserInfo.
 * Todos os campos são opcionais — apenas os não-nulos serão aplicados.
 *
 * @param peso               Novo peso em kg (opcional)
 * @param altura             Nova altura em cm (opcional)
 * @param genero             Novo gênero (opcional)
 * @param dataNasc           Nova data de nascimento (opcional)
 * @param fotoPerfil         Nova URL da foto de perfil (opcional)
 * @param nivelCondicionamento Novo nível de condicionamento (opcional)
 * @param notasMedicas       Novas observações médicas (opcional)
 * @param consentimentoSaude Consentimento para análise de risco (opcional)
 * @param cpf                CPF somente dígitos, 11 chars (opcional)
 */
public record AtualizarUserInfoDTO(
        @Positive(message = "O peso deve ser maior que 0")
        Float peso,

        @Positive(message = "A altura deve ser maior que 0")
        Float altura,
        Genero genero,
        LocalDate dataNasc,
        String fotoPerfil,
        NivelCondicionamento nivelCondicionamento,
        String notasMedicas,
        Boolean consentimentoSaude,

        @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter exatamente 11 dígitos")
        String cpf
) {}
