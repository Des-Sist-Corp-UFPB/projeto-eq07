package br.ufpb.dsc.corrida.race;

/**
 * Ciclo de vida de uma corrida.
 *
 * <ul>
 *   <li>RASCUNHO  – criada mas não publicada; visível apenas ao organizador.</li>
 *   <li>PUBLICADA – visível publicamente.</li>
 *   <li>CANCELADA – cancelada pelo organizador; não visível publicamente.</li>
 *   <li>ENCERRADA – corrida já realizada; exibida apenas no histórico público.</li>
 * </ul>
 */
public enum StatusCorrida {
    RASCUNHO,
    PUBLICADA,
    CANCELADA,
    ENCERRADA
}
