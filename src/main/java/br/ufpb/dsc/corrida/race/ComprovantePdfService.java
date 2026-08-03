package br.ufpb.dsc.corrida.race;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Serviço responsável pela geração do comprovante de inscrição em PDF.
 * Processa em memória (byte[]) sem I/O em disco.
 */
@Slf4j
@Service
public class ComprovantePdfService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Gera o PDF do comprovante de inscrição em memória.
     *
     * @param inscricao inscrição confirmada com pagamento e corrida carregados
     * @return bytes do PDF gerado
     */
    public byte[] gerarComprovante(Inscricao inscricao) {
        log.info("[PDF] Gerando comprovante para inscricaoId={}", inscricao.getId());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 70, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.ORANGE);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.DARK_GRAY);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

            // Título
            Paragraph title = new Paragraph("Comprovante de Inscrição", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph("Corrida DSC — UFPB Campus IV", bodyFont));
            doc.add(Chunk.NEWLINE);

            // Dados da corrida
            doc.add(new Paragraph("Dados da Corrida", headerFont));
            Race corrida = inscricao.getCorrida();
            doc.add(linha("Nome", corrida.getNome(), labelFont, bodyFont));
            doc.add(linha("Data", corrida.getDataInicio() != null
                    ? corrida.getDataInicio().format(FMT) : "-", labelFont, bodyFont));
            doc.add(linha("Local", corrida.getLargadaEndereco(), labelFont, bodyFont));
            doc.add(Chunk.NEWLINE);

            // Dados do atleta
            doc.add(new Paragraph("Dados do Atleta", headerFont));
            doc.add(linha("Nome", inscricao.getUsuario().getNome(), labelFont, bodyFont));
            doc.add(linha("E-mail", inscricao.getUsuario().getLogin(), labelFont, bodyFont));
            doc.add(Chunk.NEWLINE);

            // Dados do pagamento
            if (inscricao.getPagamento() != null) {
                Pagamento p = inscricao.getPagamento();
                doc.add(new Paragraph("Dados do Pagamento", headerFont));
                doc.add(linha("Método", "Pix", labelFont, bodyFont));
                doc.add(linha("Valor", "R$ " + p.getAmount().toPlainString(), labelFont, bodyFont));
                doc.add(linha("ID Transação MP", String.valueOf(p.getMpPaymentId()), labelFont, bodyFont));
                doc.add(linha("Status", "APROVADO", labelFont, bodyFont));
                doc.add(Chunk.NEWLINE);
            }

            // Número da inscrição e timestamp
            doc.add(new Paragraph("Número da Inscrição", headerFont));
            doc.add(linha("ID", String.valueOf(inscricao.getId()), labelFont, bodyFont));
            doc.add(linha("Confirmada em", inscricao.getUpdatedAt() != null
                    ? inscricao.getUpdatedAt().format(FMT) : "-", labelFont, bodyFont));

            doc.add(Chunk.NEWLINE);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
            doc.add(new Paragraph(
                    "Este comprovante é gerado automaticamente e não requer assinatura.", footerFont));

            doc.close();
            log.info("[PDF] Comprovante gerado com {} bytes para inscricaoId={}",
                    out.size(), inscricao.getId());
            return out.toByteArray();

        } catch (Exception e) {
            log.error("[PDF] Erro ao gerar comprovante para inscricaoId={}", inscricao.getId(), e);
            throw new RuntimeException("Falha ao gerar comprovante PDF.", e);
        }
    }

    private Phrase linha(String label, String valor, Font labelFont, Font bodyFont) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label + ": ", labelFont));
        phrase.add(new Chunk(valor != null ? valor : "-", bodyFont));
        phrase.add(Chunk.NEWLINE);
        return phrase;
    }
}
