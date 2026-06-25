package br.ufpb.dsc.corrida.userConections;

import br.ufpb.dsc.corrida.audit.Auditable;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import br.ufpb.dsc.corrida.storage.StorageService;
import br.ufpb.dsc.corrida.user.UserInfoRepository;
import br.ufpb.dsc.corrida.user.UserRepository;
import br.ufpb.dsc.corrida.userConections.dto.SolicitacaoConexaoDTO;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserConnectionService {

    @Autowired
    private UserConnectionRepository userConnectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private StorageService storageService;

    @Transactional
    @Auditable(action = "SEND_CONNECTION", resource = "CONECTION")
    public UserConnection sendConnectionRequest(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new IllegalArgumentException("Não é possível conectar-se a si mesmo.");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário solicitante não encontrado."));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário destinatário não encontrado."));

        Optional<UserConnection> existingOpt = userConnectionRepository.findConnectionBetweenUsers(requesterId, receiverId);

        if (existingOpt.isPresent()) {
            UserConnection existing = existingOpt.get();
            if (existing.getStatus() == null || existing.getStatus()) {
                throw new IllegalStateException("Já existe uma solicitação pendente ou conexão ativa.");
            }
            // If it was declined (status == false), we can reset it to pending and update requester/receiver
            existing.setRequester(requester);
            existing.setReceiver(receiver);
            existing.setStatus(null);
            existing.setCreatedAt(LocalDateTime.now());
            return userConnectionRepository.save(existing);
        }

        UserConnection connection = new UserConnection();
        connection.setRequester(requester);
        connection.setReceiver(receiver);
        connection.setStatus(null);
        connection.setCreatedAt(LocalDateTime.now());

        return userConnectionRepository.save(connection);
    }

    @Transactional
    @Auditable(action = "ACCEPT_CONNECTION", resource = "CONECTION")
    public UserConnection acceptConnectionRequest(Long requestId, Long receiverId) {
        UserConnection connection = userConnectionRepository.findById(requestId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Solicitação de conexão não encontrada."));

        if (!connection.getReceiver().getId().equals(receiverId)) {
            throw new IllegalArgumentException("Apenas o destinatário pode aceitar a solicitação.");
        }

        connection.setStatus(true);
        return userConnectionRepository.save(connection);
    }

    @Transactional
    @Auditable(action = "DECLINE_CONNECTION", resource = "CONECTION")
    public UserConnection declineConnectionRequest(Long requestId, Long receiverId) {
        UserConnection connection = userConnectionRepository.findById(requestId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Solicitação de conexão não encontrada."));

        if (!connection.getReceiver().getId().equals(receiverId)) {
            throw new IllegalArgumentException("Apenas o destinatário pode recusar a solicitação.");
        }

        connection.setStatus(false);
        return userConnectionRepository.save(connection);
    }

    @Transactional
    @Auditable(action = "REMOVE_CONNECTION", resource = "CONECTION")
    public void removeConnection(Long requesterId, Long receiverId) {
        Optional<UserConnection> connectionOpt = userConnectionRepository.findConnectionBetweenUsers(requesterId, receiverId);
        connectionOpt.ifPresent(userConnectionRepository::delete);
    }

    @Transactional(readOnly = true)
    public Long getPendingRequestsCount(Long userId) {
        return userConnectionRepository.countByReceiverIdAndStatus(userId, null);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoConexaoDTO> getPendingRequestsList(Long userId) {
        List<UserConnection> pending = userConnectionRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(userId, null);
        return pending.stream().map(conn -> {
            User req = conn.getRequester();
            String fotoKey = userInfoRepository.findByUsuarioId(req.getId())
                    .map(UserInfo::getFotoPerfil)
                    .orElse(null);
            String foto = (fotoKey != null && !fotoKey.isBlank())
                    ? storageService.getPresignedUrl(fotoKey)
                    : null;
            return new SolicitacaoConexaoDTO(
                    conn.getId(),
                    req.getId(),
                    req.getNome(),
                    req.getUserUsername(),
                    foto,
                    conn.getCreatedAt()
            );
        }).toList();
    }
}
