package com.connto.backend.repository;

import com.connto.backend.domain.Profile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserId(UUID userId);

    @Query(
            """
            SELECT p FROM Profile p
            WHERE p.userId <> :excludeUserId
            AND (
              LOWER(p.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(p.tags, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            ORDER BY p.displayName ASC
            """)
    List<Profile> search(@Param("excludeUserId") UUID excludeUserId, @Param("q") String q);
}
