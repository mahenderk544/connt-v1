package com.connto.backend.repository;

import com.connto.backend.domain.Friendship;
import com.connto.backend.domain.FriendshipId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    /**
     * Native SQL — true if a friendship row exists for this pair (either column order).
     */
    @Query(
            value =
                    """
                    SELECT COUNT(*) FROM friendships
                    WHERE (user_one = :a AND user_two = :b)
                       OR (user_one = :b AND user_two = :a)
                    """,
            nativeQuery = true)
    long countByUserPair(@Param("a") UUID a, @Param("b") UUID b);

    default boolean areFriends(UUID a, UUID b) {
        return countByUserPair(a, b) > 0;
    }

    /**
     * Two queries — avoids Hibernate/JPQL issues with OR across embedded-id paths.
     */
    @Query(
            """
            SELECT f FROM Friendship f
            WHERE f.id.userOne = :uid
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findWhereUserOne(@Param("uid") UUID userId);

    @Query(
            """
            SELECT f FROM Friendship f
            WHERE f.id.userTwo = :uid
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findWhereUserTwo(@Param("uid") UUID userId);
}
