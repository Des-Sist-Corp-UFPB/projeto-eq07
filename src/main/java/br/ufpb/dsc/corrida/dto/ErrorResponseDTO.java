package br.ufpb.dsc.corrida.dto;

import java.time.LocalDateTime;

/**
 * DTO padronizado para respostas de erro da API.
 * Garante que todas as respostas de erro sigam o mesmo formato.
 */
public record ErrorResponseDTO(
        int status,
        String erro,
        String mensagem,
        LocalDateTime timestamp
) {
    public ErrorResponseDTO(int status, String erro, String mensagem) {
        this(status, erro, mensagem, LocalDateTime.now());
    }
}
