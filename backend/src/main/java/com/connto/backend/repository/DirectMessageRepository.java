package com.connto.backend.repository;

import com.connto.backend.domain.DirectMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    @Query(
            """
            SELECT m FROM DirectMessage m
            WHERE (m.fromUser.id = :u1 AND m.toUser.id = :u2)
               OR (m.fromUser.id = :u2 AND m.toUser.id = :u1)
            ORDER BY m.createdAt DESC
            """)
    List<DirectMessage> findThread(@Param("u1") UUID user1, @Param("u2") UUID user2);
}
