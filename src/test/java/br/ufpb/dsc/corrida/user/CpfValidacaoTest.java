package br.ufpb.dsc.corrida.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CpfValidacaoTest {

    @Test
    @DisplayName("Deve aceitar CPF válido com ou sem máscara")
    void deveAceitarCpfValido() {
        assertDoesNotThrow(() -> UserInfoService.validarCpf("52998224725"));
        assertDoesNotThrow(() -> UserInfoService.validarCpf("529.982.247-25"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11111111111",
            "00000000000",
            "99999999999",
            "12345678901",
            "52998224726", // Dígito verificador 2 errado
            "52998224715", // Dígito verificador 1 errado
            "123456789",   // Menos de 11 dígitos
            "1234567890123" // Mais de 11 dígitos
    })
    @DisplayName("Deve rejeitar CPFs inválidos ou malformados")
    void deveRejeitarCpfsInvalidos(String cpfInvalido) {
        assertThrows(IllegalArgumentException.class, () -> UserInfoService.validarCpf(cpfInvalido));
    }

    @Test
    @DisplayName("Deve rejeitar CPF nulo")
    void deveRejeitarCpfNulo() {
        assertThrows(IllegalArgumentException.class, () -> UserInfoService.validarCpf(null));
    }
}
