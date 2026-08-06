package br.ufpb.dsc.corrida.home;

import br.ufpb.dsc.corrida.user.Papel;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o método {@link UserRepository#findTop6ByOrderByIdDesc}
 * usado pela home page ("Atletas recém-cadastrados").
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("HomeUserRepository — Usuários Recentes")
class HomeUserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private User buildUser(String suffix) {
        long ts = System.nanoTime();
        User user = new User();
        user.setNome("Atleta " + suffix);
        user.setPapel(Papel.USUARIO);
        user.setSenha("senha_segura");
        user.setLogin("atleta_home_" + suffix + "_" + ts + "@test.com");
        user.setUsername("atleta_home_" + suffix + "_" + ts);
        user.setDeletado(false);
        user.setBloqueado(false);
        return userRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Retorna usuários em ordem decrescente de ID (mais recente primeiro)")
    void deveRetornarUsuariosOrdenadosPorIdDesc() {
        User u1 = buildUser("primeiro");
        User u2 = buildUser("segundo");
        User u3 = buildUser("terceiro");

        List<User> resultado = userRepository.findTop6ByPapelOpcionalId(Papel.ADMINISTRADOR, 1L);

        // Os IDs devem estar em ordem decrescente
        List<Long> ids = resultado.stream().map(User::getId).toList();
        for (int i = 0; i < ids.size() - 1; i++) {
            assertThat(ids.get(i)).isGreaterThanOrEqualTo(ids.get(i + 1));
        }

        // Os três usuários criados devem estar nos resultados (dentro dos top 6)
        List<Long> idsCriados = List.of(u1.getId(), u2.getId(), u3.getId());
        assertThat(resultado.stream().map(User::getId).toList())
                .containsAll(idsCriados);
    }

    @Test
    @DisplayName("Limita em 6 resultados mesmo havendo mais de 6 usuários")
    void deveRetornarNoMaximo6Usuarios() {
        // Cria 8 usuários
        for (int i = 1; i <= 8; i++) {
            buildUser("limite_" + i);
        }

        List<User> resultado = userRepository.findTop6ByPapelOpcionalId(Papel.ADMINISTRADOR, 1L);

        assertThat(resultado).hasSizeLessThanOrEqualTo(6);
    }

    @Test
    @DisplayName("Retorna lista (vazia ou menor que 6) quando não há usuários suficientes")
    void deveRetornarListaVaziaOuMenorQueSeis() {
        // Não persiste nenhum usuário extra; verifica que o método não lança exceção
        List<User> resultado = userRepository.findTop6ByPapelOpcionalId(Papel.ADMINISTRADOR, 1L);

        // O resultado deve ter no máximo 6 itens
        assertThat(resultado).hasSizeLessThanOrEqualTo(6);
        // Todos os IDs devem ser positivos (sanidade)
        assertThat(resultado).allMatch(u -> u.getId() > 0);
    }

    @Test
    @DisplayName("Usuário cadastrado mais recentemente aparece na primeira posição")
    void usuarioMaisRecenteDeveSerOPrimeiro() {
        buildUser("anterior_" + System.nanoTime());
        User maisRecente = buildUser("recente_" + System.nanoTime());

        List<User> resultado = userRepository.findTop6ByPapelOpcionalId(Papel.ADMINISTRADOR, 1L);

        // O primeiro elemento deve ter ID >= que o usuário mais recente criado
        assertThat(resultado.get(0).getId()).isGreaterThanOrEqualTo(maisRecente.getId());
    }
}
