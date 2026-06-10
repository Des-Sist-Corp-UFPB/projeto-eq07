package br.ufpb.dsc.corrida.service.userinfo;

import br.ufpb.dsc.corrida.domain.UserInfo;
import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.dto.userinfo.AtualizarUserInfoDTO;
import br.ufpb.dsc.corrida.dto.userinfo.CriarUserInfoDTO;
import br.ufpb.dsc.corrida.dto.userinfo.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.enums.Genero;
import br.ufpb.dsc.corrida.enums.NivelCondicionamento;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoJaExistenteException;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import br.ufpb.dsc.corrida.repository.UserInfoRepository;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserInfoService — Unit Tests")
class UserInfoServiceTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserInfoService service;

    private Usuario usuario;
    private CriarUserInfoDTO dtoValido;

    @BeforeEach
    void setUp() {
        usuario = mock(Usuario.class);
        lenient().when(usuario.getId()).thenReturn(1L);

        dtoValido = new CriarUserInfoDTO(
                1L,
                70.5f,
                175.0f,
                Genero.MALE,
                LocalDate.of(1995, 5, 20),
                null,
                NivelCondicionamento.INTERMEDIATE,
                null
        );
    }

    // ─────────────────────────────────────────────
    // criar()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("criar() — lança conflito se já existe UserInfo para o userId")
    void criar_deveRetornarConflito_quandoJaExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(userInfoRepository.existsByUsuarioId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(dtoValido))
                .isInstanceOf(UserInfoJaExistenteException.class);

        verify(userInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() — lança não-encontrado se usuário não existe")
    void criar_deveRetornarNaoEncontrado_quandoUsuarioNaoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(dtoValido))
                .isInstanceOf(UsuarioNaoEncontradoException.class);

        verify(userInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() — lança validação se peso <= 0")
    void criar_deveRetornarValidacao_quandoPesoInvalido() {
        var dtoInvalido = new CriarUserInfoDTO(
                1L, 0.0f, 175.0f, Genero.MALE,
                LocalDate.of(1995, 5, 20), null, null, null
        );

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(userInfoRepository.existsByUsuarioId(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.criar(dtoInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("peso");

        verify(userInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() — lança validação se altura <= 0")
    void criar_deveRetornarValidacao_quandoAlturaInvalida() {
        var dtoInvalido = new CriarUserInfoDTO(
                1L, 70.0f, -1.0f, Genero.MALE,
                LocalDate.of(1995, 5, 20), null, null, null
        );

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(userInfoRepository.existsByUsuarioId(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.criar(dtoInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("altura");

        verify(userInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() — persiste e retorna UserInfo com sucesso")
    void criar_devePersistirERetornar_comSucesso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(userInfoRepository.existsByUsuarioId(1L)).thenReturn(false);

        UserInfo saved = new UserInfo();
        saved.setUsuario(usuario);
        saved.setPeso(dtoValido.peso());
        saved.setAltura(dtoValido.altura());
        saved.setGenero(dtoValido.genero());
        saved.setDataNasc(dtoValido.dataNasc());
        saved.setNivelCondicionamento(dtoValido.nivelCondicionamento());
        saved.setTotalKmRun(0.0f);

        when(userInfoRepository.save(any(UserInfo.class))).thenReturn(saved);

        UserInfoRespostaDTO resultado = service.criar(dtoValido);

        assertThat(resultado).isNotNull();
        assertThat(resultado.peso()).isEqualTo(dtoValido.peso());
        assertThat(resultado.altura()).isEqualTo(dtoValido.altura());
        verify(userInfoRepository).save(any(UserInfo.class));
    }

    // ─────────────────────────────────────────────
    // buscarPorUsuarioId()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorUsuarioId() — lança não-encontrado se não há registro")
    void buscarPorUsuarioId_deveRetornarNaoEncontrado_quandoNaoExiste() {
        when(userInfoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorUsuarioId(99L))
                .isInstanceOf(UserInfoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("buscarPorUsuarioId() — retorna UserInfo com sucesso")
    void buscarPorUsuarioId_deveRetornar_comSucesso() {
        UserInfo info = new UserInfo();
        info.setUsuario(usuario);
        info.setPeso(70.5f);
        info.setAltura(175.0f);
        info.setGenero(Genero.MALE);
        info.setDataNasc(LocalDate.of(1995, 5, 20));
        info.setTotalKmRun(0.0f);

        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(info));

        UserInfoRespostaDTO resultado = service.buscarPorUsuarioId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.peso()).isEqualTo(70.5f);
    }

    // ─────────────────────────────────────────────
    // atualizar()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("atualizar() — lança não-encontrado se registro não existe")
    void atualizar_deveRetornarNaoEncontrado_quandoNaoExiste() {
        when(userInfoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

        var dto = new AtualizarUserInfoDTO(80.0f, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.atualizar(99L, dto))
                .isInstanceOf(UserInfoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("atualizar() — lança validação se peso <= 0")
    void atualizar_deveRetornarValidacao_quandoPesoInvalido() {
        UserInfo info = new UserInfo();
        info.setUsuario(usuario);
        info.setPeso(70.0f);
        info.setAltura(175.0f);
        info.setGenero(Genero.MALE);
        info.setDataNasc(LocalDate.of(1995, 5, 20));
        info.setTotalKmRun(0.0f);

        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(info));

        var dto = new AtualizarUserInfoDTO(0.0f, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.atualizar(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("peso");
    }

    @Test
    @DisplayName("atualizar() — lança validação se altura <= 0")
    void atualizar_deveRetornarValidacao_quandoAlturaInvalida() {
        UserInfo info = new UserInfo();
        info.setUsuario(usuario);
        info.setPeso(70.0f);
        info.setAltura(175.0f);
        info.setGenero(Genero.MALE);
        info.setDataNasc(LocalDate.of(1995, 5, 20));
        info.setTotalKmRun(0.0f);

        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(info));

        var dto = new AtualizarUserInfoDTO(null, -5.0f, null, null, null, null, null);

        assertThatThrownBy(() -> service.atualizar(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("altura");
    }

    @Test
    @DisplayName("atualizar() — persiste e retorna dados atualizados com sucesso")
    void atualizar_devePersistirERetornar_comSucesso() {
        UserInfo info = new UserInfo();
        info.setUsuario(usuario);
        info.setPeso(70.0f);
        info.setAltura(175.0f);
        info.setGenero(Genero.MALE);
        info.setDataNasc(LocalDate.of(1995, 5, 20));
        info.setTotalKmRun(0.0f);

        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(info));
        when(userInfoRepository.save(any(UserInfo.class))).thenReturn(info);

        var dto = new AtualizarUserInfoDTO(85.0f, null, null, null, null, null, null);

        UserInfoRespostaDTO resultado = service.atualizar(1L, dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.peso()).isEqualTo(85.0f);
        verify(userInfoRepository).save(info);
    }
}
