package br.ufpb.dsc.corrida.userConections;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserConnectionRepository extends JpaRepository<UserConnection, Long> {

    @Query("SELECT c FROM UserConnection c WHERE (c.requester.id = :u1 AND c.receiver.id = :u2) OR (c.requester.id = :u2 AND c.receiver.id = :u1)")
    Optional<UserConnection> findConnectionBetweenUsers(@Param("u1") Long u1, @Param("u2") Long u2);

    List<UserConnection> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, Boolean status);

    Long countByReceiverIdAndStatus(Long receiverId, Boolean status);

    Optional<UserConnection> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    @Query("SELECT COUNT(c) FROM UserConnection c WHERE (c.requester.id = :userId OR c.receiver.id = :userId) AND c.status = true")
    Long countConnectionsByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM UserConnection c WHERE (c.requester.id = :userId OR c.receiver.id = :userId) AND c.status = true")
    List<UserConnection> findAcceptedConnectionsByUserId(@Param("userId") Long userId);
}
