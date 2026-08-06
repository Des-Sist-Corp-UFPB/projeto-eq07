package br.ufpb.dsc.corrida.pagamento;

import br.ufpb.dsc.corrida.inscricao.Inscricao;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComprovantePdfService — Unit Tests")
class ComprovantePdfServiceTest {

    @InjectMocks
    private ComprovantePdfService pdfService;

    @Test
    @DisplayName("gerarComprovante() — gera PDF com sucesso para inscrição gratuita (sem pagamento)")
    void gerarComprovante_semPagamento() {
        User user = new User();
        user.setNome("Atleta Teste");
        user.setLogin("atleta@teste.com");

        Race race = new Race();
        race.setNome("Corrida de Teste PDF");
        race.setDataInicio(OffsetDateTime.now().plusDays(10));
        race.setLargadaEndereco("Rua das Flores, 123");

        Inscricao inscricao = new Inscricao();
        inscricao.setId(101L);
        inscricao.setUsuario(user);
        inscricao.setCorrida(race);
        inscricao.setUpdatedAt(OffsetDateTime.now());

        byte[] pdfBytes = pdfService.gerarComprovante(inscricao);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("gerarComprovante() — gera PDF com sucesso para inscrição com pagamento Pix")
    void gerarComprovante_comPagamento() {
        User user = new User();
        user.setNome("Atleta Pago");
        user.setLogin("atleta.pago@teste.com");

        Race race = new Race();
        race.setNome("Meia Maratona Pago");
        race.setDataInicio(OffsetDateTime.now().plusDays(5));
        race.setLargadaEndereco("Av. Beira Mar, 500");

        Inscricao inscricao = new Inscricao();
        inscricao.setId(202L);
        inscricao.setUsuario(user);
        inscricao.setCorrida(race);
        inscricao.setUpdatedAt(OffsetDateTime.now());

        Pagamento pagamento = new Pagamento();
        pagamento.setAmount(new BigDecimal("50.00"));
        pagamento.setMpPaymentId(123456789L);
        inscricao.setPagamento(pagamento);

        byte[] pdfBytes = pdfService.gerarComprovante(inscricao);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }
}
