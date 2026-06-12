package br.ufpb.dsc.corrida.service.user;

import br.ufpb.dsc.corrida.user.*;
import br.ufpb.dsc.corrida.user.dto.SolicitacaoConexaoDTO;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserConnectionService — Unit Tests")
class UserConnectionServiceTest {

    @Mock
    private UserConnectionRepository userConnectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @InjectMocks
    private UserConnectionService userConnectionService;

    private User requester;
    private User receiver;

    @BeforeEach
    void setUp() {
        requester = new User();
        // Use reflection or constructor if id is private and has no setter, but since we mocked or we can instantiate, let's inject IDs
        org.springframework.test.util.ReflectionTestUtils.setField(requester, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(requester, "nome", "User One");
        org.springframework.test.util.ReflectionTestUtils.setField(requester, "username", "userone");

        receiver = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(receiver, "id", 2L);
        org.springframework.test.util.ReflectionTestUtils.setField(receiver, "nome", "User Two");
        org.springframework.test.util.ReflectionTestUtils.setField(receiver, "username", "usertwo");
    }

    @Test
    @DisplayName("sendConnectionRequest() — should throw exception when requester tries to connect to themselves")
    void sendConnectionRequest_shouldThrowException_whenConnectingToSelf() {
        assertThatThrownBy(() -> userConnectionService.sendConnectionRequest(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é possível conectar-se a si mesmo");
    }

    @Test
    @DisplayName("sendConnectionRequest() — should throw exception when requester user does not exist")
    void sendConnectionRequest_shouldThrowException_whenRequesterNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userConnectionService.sendConnectionRequest(1L, 2L))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("sendConnectionRequest() — should throw exception when receiver user does not exist")
    void sendConnectionRequest_shouldThrowException_whenReceiverNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userConnectionService.sendConnectionRequest(1L, 2L))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("sendConnectionRequest() — should throw exception when connection request already pending")
    void sendConnectionRequest_shouldThrowException_whenRequestAlreadyPending() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));

        UserConnection existingConnection = new UserConnection();
        existingConnection.setRequester(requester);
        existingConnection.setReceiver(receiver);
        existingConnection.setStatus(null); // Pending

        when(userConnectionRepository.findConnectionBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(existingConnection));

        assertThatThrownBy(() -> userConnectionService.sendConnectionRequest(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Já existe uma solicitação pendente ou conexão ativa");
    }

    @Test
    @DisplayName("sendConnectionRequest() — should throw exception when already connected")
    void sendConnectionRequest_shouldThrowException_whenAlreadyConnected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));

        UserConnection existingConnection = new UserConnection();
        existingConnection.setRequester(requester);
        existingConnection.setReceiver(receiver);
        existingConnection.setStatus(true); // Connected

        when(userConnectionRepository.findConnectionBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(existingConnection));

        assertThatThrownBy(() -> userConnectionService.sendConnectionRequest(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Já existe uma solicitação pendente ou conexão ativa");
    }

    @Test
    @DisplayName("sendConnectionRequest() — should create and save new pending request when no connection exists")
    void sendConnectionRequest_shouldSaveAndReturn_whenNoConnectionExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(userConnectionRepository.findConnectionBetweenUsers(1L, 2L)).thenReturn(Optional.empty());

        UserConnection newConnection = new UserConnection();
        newConnection.setRequester(requester);
        newConnection.setReceiver(receiver);
        newConnection.setStatus(null);
        newConnection.setCreatedAt(LocalDateTime.now());

        when(userConnectionRepository.save(any(UserConnection.class))).thenReturn(newConnection);

        UserConnection result = userConnectionService.sendConnectionRequest(1L, 2L);

        assertThat(result).isNotNull();
        assertThat(result.getRequester().getId()).isEqualTo(1L);
        assertThat(result.getReceiver().getId()).isEqualTo(2L);
        assertThat(result.getStatus()).isNull();
        assertThat(result.getCreatedAt()).isNotNull();

        verify(userConnectionRepository).save(any(UserConnection.class));
    }

    @Test
    @DisplayName("sendConnectionRequest() — should update existing declined request to pending when status was false")
    void sendConnectionRequest_shouldUpdateExistingDeclinedRequest_whenStatusIsFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));

        UserConnection existingConnection = new UserConnection();
        existingConnection.setRequester(receiver); // Receiver was requester previously
        existingConnection.setReceiver(requester);
        existingConnection.setStatus(false); // Declined

        when(userConnectionRepository.findConnectionBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(existingConnection));
        when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(i -> i.getArguments()[0]);

        UserConnection result = userConnectionService.sendConnectionRequest(1L, 2L);

        assertThat(result).isNotNull();
        assertThat(result.getRequester().getId()).isEqualTo(1L); // Now requester is 1L
        assertThat(result.getReceiver().getId()).isEqualTo(2L);
        assertThat(result.getStatus()).isNull(); // Switched back to pending

        verify(userConnectionRepository).save(existingConnection);
    }

    @Test
    @DisplayName("acceptConnectionRequest() — should throw exception when connection request not found")
    void acceptConnectionRequest_shouldThrowException_whenRequestNotFound() {
        when(userConnectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userConnectionService.acceptConnectionRequest(99L, 2L))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("acceptConnectionRequest() — should throw exception when user accepting is not the receiver")
    void acceptConnectionRequest_shouldThrowException_whenUserIsNotReceiver() {
        UserConnection connection = new UserConnection();
        connection.setRequester(requester);
        connection.setReceiver(receiver);
        connection.setStatus(null);

        when(userConnectionRepository.findById(10L)).thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> userConnectionService.acceptConnectionRequest(10L, 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Apenas o destinatário pode aceitar");
    }

    @Test
    @DisplayName("acceptConnectionRequest() — should accept connection successfully and save")
    void acceptConnectionRequest_shouldAcceptSuccessfully_whenValid() {
        UserConnection connection = new UserConnection();
        connection.setRequester(requester);
        connection.setReceiver(receiver);
        connection.setStatus(null);

        when(userConnectionRepository.findById(10L)).thenReturn(Optional.of(connection));
        when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(i -> i.getArguments()[0]);

        UserConnection result = userConnectionService.acceptConnectionRequest(10L, 2L);

        assertThat(result.getStatus()).isTrue();
        verify(userConnectionRepository).save(connection);
    }

    @Test
    @DisplayName("declineConnectionRequest() — should decline connection successfully and save")
    void declineConnectionRequest_shouldDeclineSuccessfully_whenValid() {
        UserConnection connection = new UserConnection();
        connection.setRequester(requester);
        connection.setReceiver(receiver);
        connection.setStatus(null);

        when(userConnectionRepository.findById(10L)).thenReturn(Optional.of(connection));
        when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(i -> i.getArguments()[0]);

        UserConnection result = userConnectionService.declineConnectionRequest(10L, 2L);

        assertThat(result.getStatus()).isFalse();
        verify(userConnectionRepository).save(connection);
    }

    @Test
    @DisplayName("removeConnection() — should delete connection record between users")
    void removeConnection_shouldDeleteRecord_whenConnectionExists() {
        UserConnection connection = new UserConnection();
        connection.setRequester(requester);
        connection.setReceiver(receiver);

        when(userConnectionRepository.findConnectionBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(connection));

        userConnectionService.removeConnection(1L, 2L);

        verify(userConnectionRepository).delete(connection);
    }

    @Test
    @DisplayName("removeConnection() — should do nothing if no connection exists")
    void removeConnection_shouldDoNothing_whenNoConnection() {
        when(userConnectionRepository.findConnectionBetweenUsers(1L, 2L))
                .thenReturn(Optional.empty());

        userConnectionService.removeConnection(1L, 2L);

        verify(userConnectionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getPendingRequestsCount() — should return unresolved count")
    void getPendingRequestsCount_shouldReturnCount() {
        when(userConnectionRepository.countByReceiverIdAndStatus(2L, null)).thenReturn(5L);

        Long count = userConnectionService.getPendingRequestsCount(2L);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("getPendingRequestsList() — should return sorted DTO list")
    void getPendingRequestsList_shouldReturnSortedList() {
        UserConnection conn1 = new UserConnection();
        org.springframework.test.util.ReflectionTestUtils.setField(conn1, "id", 100L);
        conn1.setRequester(requester);
        conn1.setReceiver(receiver);
        conn1.setStatus(null);
        conn1.setCreatedAt(LocalDateTime.now().minusDays(1));

        UserConnection conn3 = new UserConnection(); // Another requester
        User requester2 = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(requester2, "id", 3L);
        org.springframework.test.util.ReflectionTestUtils.setField(requester2, "nome", "User Three");
        org.springframework.test.util.ReflectionTestUtils.setField(requester2, "username", "userthree");
        org.springframework.test.util.ReflectionTestUtils.setField(conn3, "id", 101L);
        conn3.setRequester(requester2);
        conn3.setReceiver(receiver);
        conn3.setStatus(null);
        conn3.setCreatedAt(LocalDateTime.now());

        when(userConnectionRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(2L, null))
                .thenReturn(Arrays.asList(conn3, conn1));

        // Mock info repository
        UserInfo info1 = new UserInfo();
        info1.setFotoPerfil("photo1.jpg");
        UserInfo info2 = new UserInfo();
        info2.setFotoPerfil("photo2.jpg");

        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(info1));
        when(userInfoRepository.findByUsuarioId(3L)).thenReturn(Optional.of(info2));

        List<SolicitacaoConexaoDTO> list = userConnectionService.getPendingRequestsList(2L);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(101L);
        assertThat(list.get(0).requesterId()).isEqualTo(3L);
        assertThat(list.get(0).foto()).isEqualTo("photo2.jpg");

        assertThat(list.get(1).id()).isEqualTo(100L);
        assertThat(list.get(1).requesterId()).isEqualTo(1L);
        assertThat(list.get(1).foto()).isEqualTo("photo1.jpg");
    }
}
