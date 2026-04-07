package com.connto.backend.repository;

import com.connto.backend.domain.Friendship;
import com.connto.backend.domain.FriendshipId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    @Query(
            """
            SELECT f FROM Friendship f
            WHERE f.id.userLow = :a AND f.id.userHigh = :b
            """)
    Optional<Friendship> findByOrderedPair(@Param("a") UUID userLow, @Param("b") UUID userHigh);

    @Query(
            """
            SELECT f FROM Friendship f
            WHERE f.id.userLow = :uid OR f.id.userHigh = :uid
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findAllForUser(@Param("uid") UUID userId);
}
