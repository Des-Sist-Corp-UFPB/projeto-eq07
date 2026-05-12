package br.ufpb.dsc.corrida.dto.user;

import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.enums.Papel;

public record UsuarioResposta(Long id, String nome, String username, String login, Papel papel) {
    public UsuarioResposta(Usuario usuario) {
        this(usuario.getId(), usuario.getNome(), usuario.getUserUsername(), usuario.getUsername(), usuario.getPapel());
    }
}
