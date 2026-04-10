package com.connto.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friendships")
public class Friendship {

    @EmbeddedId
    private FriendshipId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public FriendshipId getId() {
        return id;
    }

    public void setId(FriendshipId id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** The other participant when {@code me} is one side of this friendship row. */
    public UUID peerUserId(UUID me) {
        UUID one = id.getUserOne();
        UUID two = id.getUserTwo();
        if (one.equals(me)) {
            return two;
        }
        if (two.equals(me)) {
            return one;
        }
        throw new IllegalStateException("Friendship does not involve the given user");
    }
}
