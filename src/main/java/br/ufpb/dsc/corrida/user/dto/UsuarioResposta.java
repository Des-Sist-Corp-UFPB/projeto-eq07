package br.ufpb.dsc.corrida.user.dto;

import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.Papel;

public record UsuarioResposta(Long id, String nome, String username, String login, Papel papel) {
    public UsuarioResposta(User usuario) {
        this(usuario.getId(), usuario.getNome(), usuario.getUserUsername(), usuario.getUsername(), usuario.getPapel());
    }
}
