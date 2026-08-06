package br.ufpb.dsc.corrida.inscricao;

import java.time.OffsetDateTime;

/**
 * Resposta da API de inscritos de uma corrida.
 *
 * <p>Expõe apenas os dados necessários para o organizador:
 * identidade do atleta, data de inscrição e status.
 */
public record InscritoDTO(
        Long inscricaoId,
        Long usuarioId,
        String nome,
        String userUsername,
        OffsetDateTime dataInscricao,
        String status
) {
    /** Factory method a partir da entidade {@link Inscricao}. */
    public static InscritoDTO from(Inscricao i) {
        return new InscritoDTO(
                i.getId(),
                i.getUsuario().getId(),
                i.getUsuario().getNome(),
                i.getUsuario().getUsername(),
                i.getDataInscricao(),
                i.getStatus().name()
        );
    }
}
