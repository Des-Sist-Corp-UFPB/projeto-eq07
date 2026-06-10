package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.dto.userinfo.AtualizarUserInfoDTO;
import br.ufpb.dsc.corrida.dto.userinfo.CriarUserInfoDTO;
import br.ufpb.dsc.corrida.dto.userinfo.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.enums.Genero;
import br.ufpb.dsc.corrida.enums.NivelCondicionamento;
import br.ufpb.dsc.corrida.repository.UserInfoRepository;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserInfo — Integration Tests")
class UserInfoIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private static Long usuarioId;
    private static String authToken;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeAll
    static void setUpAll(@Autowired UsuarioRepository usuarioRepository,
                         @Autowired TestRestTemplate restTemplate) {
        // Register a test user and obtain JWT token for subsequent requests
        var registrar = new RegistrarUsuarioDTO(
                "Atleta Teste", "atleta_test", "atleta_test@corrida.com", "senha123"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegistrarUsuarioDTO> request = new HttpEntity<>(registrar, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/user/registrar", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Usuario usuario = usuarioRepository.findByUsername("atleta_test");
        assertThat(usuario).isNotNull();
        usuarioId = usuario.getId();

        // Extract token from response body (JSON: {"token":"...","mensagem":"..."})
        String body = response.getBody();
        assertThat(body).isNotNull();
        try {
            var map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, java.util.Map.class);
            authToken = (String) map.get("token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token from response", e);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken);
        return headers;
    }

    private CriarUserInfoDTO validDto() {
        return new CriarUserInfoDTO(
                usuarioId, 70.5f, 175.0f, Genero.MALE,
                LocalDate.of(1995, 5, 20),
                null, NivelCondicionamento.INTERMEDIATE, null
        );
    }

    // ─────────────────────────────────────────────
    // POST /user-info
    // ─────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /user-info → 201 Created com payload válido")
    void post_deveRetornar201_comDadosValidos() throws Exception {
        HttpEntity<CriarUserInfoDTO> request = new HttpEntity<>(validDto(), authHeaders());

        ResponseEntity<UserInfoRespostaDTO> response =
                restTemplate.postForEntity("/user-info", request, UserInfoRespostaDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().peso()).isEqualTo(70.5f);
    }

    @Test
    @Order(2)
    @DisplayName("POST /user-info → 409 Conflict se userId já tem registro")
    void post_deveRetornar409_quandoJaExiste() {
        HttpEntity<CriarUserInfoDTO> request = new HttpEntity<>(validDto(), authHeaders());

        ResponseEntity<String> response =
                restTemplate.postForEntity("/user-info", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(3)
    @DisplayName("POST /user-info → 404 Not Found se userId não existe")
    void post_deveRetornar404_quandoUsuarioNaoExiste() {
        var dto = new CriarUserInfoDTO(
                99999L, 70.5f, 175.0f, Genero.MALE,
                LocalDate.of(1995, 5, 20), null, NivelCondicionamento.BEGINNER, null
        );

        HttpEntity<CriarUserInfoDTO> request = new HttpEntity<>(dto, authHeaders());

        ResponseEntity<String> response =
                restTemplate.postForEntity("/user-info", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(4)
    @DisplayName("POST /user-info → 400 Bad Request se peso <= 0")
    void post_deveRetornar400_quandoPesoInvalido() {
        var dto = new CriarUserInfoDTO(
                usuarioId, 0.0f, 175.0f, Genero.MALE,
                LocalDate.of(1995, 5, 20), null, null, null
        );

        // Need a fresh user for this test since the original user already has a record
        // We test validation error which is thrown before conflict check, so reuse different user
        // Actually we test with current user: service throws validation before checking conflict
        HttpEntity<CriarUserInfoDTO> request = new HttpEntity<>(dto, authHeaders());

        ResponseEntity<String> response =
                restTemplate.postForEntity("/user-info", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─────────────────────────────────────────────
    // GET /user-info/{usuarioId}
    // ─────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("GET /user-info/{usuarioId} → 200 OK com dados corretos")
    void get_deveRetornar200_comDadosCorretos() {
        HttpEntity<Void> request = new HttpEntity<>(authHeaders());

        ResponseEntity<UserInfoRespostaDTO> response =
                restTemplate.exchange("/user-info/" + usuarioId, HttpMethod.GET, request, UserInfoRespostaDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().peso()).isEqualTo(70.5f);
        assertThat(response.getBody().genero()).isEqualTo(Genero.MALE);
    }

    @Test
    @Order(6)
    @DisplayName("GET /user-info/{usuarioId} → 404 Not Found para userId desconhecido")
    void get_deveRetornar404_quandoNaoEncontrado() {
        HttpEntity<Void> request = new HttpEntity<>(authHeaders());

        ResponseEntity<String> response =
                restTemplate.exchange("/user-info/99999", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────
    // PUT /user-info/{usuarioId}
    // ─────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("PUT /user-info/{usuarioId} → 200 OK com campos atualizados")
    void put_deveRetornar200_comDadosAtualizados() {
        var dto = new AtualizarUserInfoDTO(
                85.0f, 180.0f, null, null, null, null, null
        );

        HttpEntity<AtualizarUserInfoDTO> request = new HttpEntity<>(dto, authHeaders());

        ResponseEntity<UserInfoRespostaDTO> response =
                restTemplate.exchange("/user-info/" + usuarioId, HttpMethod.PUT, request, UserInfoRespostaDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().peso()).isEqualTo(85.0f);
        assertThat(response.getBody().altura()).isEqualTo(180.0f);
    }

    @Test
    @Order(8)
    @DisplayName("PUT /user-info/{usuarioId} → 400 Bad Request se validação falha")
    void put_deveRetornar400_quandoValidacaoFalha() {
        var dto = new AtualizarUserInfoDTO(
                -10.0f, null, null, null, null, null, null
        );

        HttpEntity<AtualizarUserInfoDTO> request = new HttpEntity<>(dto, authHeaders());

        ResponseEntity<String> response =
                restTemplate.exchange("/user-info/" + usuarioId, HttpMethod.PUT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
