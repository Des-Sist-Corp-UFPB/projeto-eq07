package br.ufpb.dsc.corrida.inscricao;

import br.ufpb.dsc.corrida.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscricaoViewController — Unit Tests")
class InscricaoViewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InscricaoRepository inscricaoRepository;

    @InjectMocks
    private InscricaoViewController viewController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(viewController).build();
    }

    @Test
    @DisplayName("minhasInscricoes() — retorna a view user/minhas-inscricoes com a lista de inscrições")
    void minhasInscricoes_sucesso() throws Exception {
        Inscricao inscricao = new Inscricao();
        inscricao.setId(10L);
        inscricao.setStatus(StatusInscricao.CONFIRMADA);

        when(inscricaoRepository.findByUsuarioAndStatusInOrderByIdDesc(any(), eq(List.of(
                StatusInscricao.AGUARDANDO_PAGAMENTO,
                StatusInscricao.CONFIRMADA,
                StatusInscricao.ATIVA
        )))).thenReturn(List.of(inscricao));

        mockMvc.perform(get("/minhas-inscricoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/minhas-inscricoes"))
                .andExpect(model().attributeExists("inscricoes"));
    }
}
