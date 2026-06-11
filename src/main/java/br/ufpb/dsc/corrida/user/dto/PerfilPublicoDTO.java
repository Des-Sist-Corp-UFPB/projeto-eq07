package br.ufpb.dsc.corrida.user.dto;

public record PerfilPublicoDTO(
    String nome,
    String username,
    String fotoPerfil,
    Float totalKmRun
) {}
