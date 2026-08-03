package br.ufpb.dsc.corrida.race;

/**
 * Status de uma inscrição de corrida.
 *
 * <ul>
 *   <li>AGUARDANDO_PAGAMENTO — inscrição criada, Pix gerado, aguardando confirmação de pagamento.</li>
 *   <li>CONFIRMADA — inscrição válida: corrida gratuita confirmada ou Pix aprovado via webhook.</li>
 *   <li>ATIVA — status legado para inscrições anteriores à integração de pagamento.</li>
 *   <li>CANCELADA — inscrição cancelada pelo atleta, expirada ou rejeitada pelo MP.</li>
 * </ul>
 */
public enum StatusInscricao {
    AGUARDANDO_PAGAMENTO,
    CONFIRMADA,
    ATIVA,
    CANCELADA
}
