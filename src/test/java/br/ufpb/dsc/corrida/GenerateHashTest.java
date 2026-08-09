package br.ufpb.dsc.corrida;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHashTest {
    @Test
    public void testGenerateHash() {
        System.out.println("HASH_GEN_START");
        System.out.println(new BCryptPasswordEncoder().encode("admin123"));
        System.out.println("HASH_GEN_END");
    }
}
