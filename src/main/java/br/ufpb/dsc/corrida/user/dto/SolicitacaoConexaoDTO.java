package br.ufpb.dsc.corrida.user.dto;

import java.time.LocalDateTime;

public record SolicitacaoConexaoDTO(
        Long id,
        Long requesterId,
        String nome,
        String username,
        String foto,
        LocalDateTime createdAt
) {}
