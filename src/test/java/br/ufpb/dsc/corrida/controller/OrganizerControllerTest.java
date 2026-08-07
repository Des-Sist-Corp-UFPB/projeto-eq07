package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.Organizer;
import br.ufpb.dsc.corrida.organizer.OrganizerService;
import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorCompletoDTO;
import br.ufpb.dsc.corrida.user.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Organizer Controller â€” Integration & Web Tests")
class OrganizerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizerService organizerService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("GET /registrar/organizador â€” should return registrar-organizador view")
    void getRegistrarOrganizador_shouldReturnView() throws Exception {
        mockMvc.perform(get("/registrar/organizador"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registrar-organizador"))
                .andExpect(model().attributeExists("organizadorStep1"))
                .andExpect(model().attributeExists("organizadorStep2"));
    }

    @Test
    @DisplayName("POST /registrar/organizador â€” should submit and redirect on success")
    void postRegistrarOrganizador_shouldSubmitAndRedirect_whenValid() throws Exception {
        User mockUser = new User();

        when(organizerService.registrarOrganizador(any(RegistrarOrganizadorCompletoDTO.class))).thenReturn(mockUser);

        mockMvc.perform(post("/registrar/organizador")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("step1.nome", "JoÃ£o da Silva")
                        .param("step1.username", "joao_org")
                        .param("step1.login", "joao@org.com")
                        .param("step1.senha", "senha1234")
                        .param("step1.cref", "123456-G/SP")
                        .param("step1.cpf", "123.456.789-00")
                        .param("step1.email", "joao@org.com")
                        .param("step1.whatsapp", "11999999999")
                        .param("step1.ufConselho", "SP")
                        .param("step2.name", "Super Corridas LTDA")
                        .param("step2.foundedAt", "2020-01-01")
                        .param("step2.description", "OrganizaÃ§Ã£o"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success=true"));

        verify(organizerService).registrarOrganizador(any(RegistrarOrganizadorCompletoDTO.class));
    }

    @Test
    @DisplayName("GET /organizacao/{id} â€” should display organization details screen")
    void getOrganizacaoDetails_shouldReturnView() throws Exception {
        // 1. Criar a estrutura aninhada para evitar o NullPointerException no Thymeleaf
        User mockUsuario = new User();
        mockUsuario.setNome("Organizador Teste");

        Organizer mockOrganizer = new Organizer();
        mockOrganizer.setUsuario(mockUsuario);

        Organization mockOrg = new Organization();
        mockOrg.setId(100L);
        mockOrg.setName("Super Corridas");
        mockOrg.setOrganizer(mockOrganizer); // Vincula o organizer preenchido

        // 2. Configurar o Mockito para retornar a estrutura completa
        when(organizerService.buscarOrganizacaoPorId(100L)).thenReturn(mockOrg);

        // 3. Executar a requisiÃ§Ã£o
        mockMvc.perform(get("/organizacao/100"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/organizacao-detalhes"))
                .andExpect(model().attribute("organizacao", mockOrg));
    }
}

