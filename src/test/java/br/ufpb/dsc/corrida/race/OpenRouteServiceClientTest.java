package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenRouteServiceClient — Unit Tests")
public class OpenRouteServiceClientTest {

    private OpenRouteServiceClient client;

    @Mock
    private RestClient mockRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @BeforeEach
    void setUp() {
        client = new OpenRouteServiceClient("http://localhost:8080", "test-key", new ObjectMapper());
        ReflectionTestUtils.setField(client, "restClient", mockRestClient);
    }

    @Test
    @DisplayName("Should throw ExternalServiceException on RestClientResponseException (e.g., HTTP 500)")
    void shouldThrowExternalServiceExceptionOnHttpError() {
        when(mockRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        
        // ALTERAÇÃO AQUI: Forçando a assinatura para Object
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(mock(RestClientResponseException.class));

        assertThatThrownBy(() -> client.calcularRota(-34.863, -7.115, -34.863, -7.115))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Não foi possível calcular a rota neste momento");
    }

    @Test
    @DisplayName("Should throw ExternalServiceException on ResourceAccessException (e.g., connection timeout)")
    void shouldThrowExternalServiceExceptionOnTimeout() {
        when(mockRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        
        // ALTERAÇÃO AQUI: Forçando a assinatura para Object
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(new ResourceAccessException("Timeout"));

        assertThatThrownBy(() -> client.calcularRota(-34.863, -7.115, -34.863, -7.115))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Não foi possível calcular a rota neste momento");
    }

    @Test
    @DisplayName("Should throw ExternalServiceException on Geocoding RestClientResponseException")
    void shouldThrowExternalServiceExceptionOnGeocodingHttpError() {
        when(mockRestClient.get()).thenReturn(requestHeadersUriSpec);
        
        // CORREÇÃO: Adicionado o terceiro anyString() para bater com os 3 parâmetros passados na produção
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyString())).thenReturn(requestHeadersSpec);
        
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(mock(RestClientResponseException.class));

        assertThatThrownBy(() -> client.geocodificarEndereco("Rua Teste"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Não foi possível buscar o endereço neste momento");
    }
}
