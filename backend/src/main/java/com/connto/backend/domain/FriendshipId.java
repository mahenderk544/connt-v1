package com.connto.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FriendshipId implements Serializable {

    @Column(name = "user_low", nullable = false)
    private UUID userLow;

    @Column(name = "user_high", nullable = false)
    private UUID userHigh;

    public FriendshipId() {}

    public FriendshipId(UUID userLow, UUID userHigh) {
        this.userLow = userLow;
        this.userHigh = userHigh;
    }

    public UUID getUserLow() {
        return userLow;
    }

    public void setUserLow(UUID userLow) {
        this.userLow = userLow;
    }

    public UUID getUserHigh() {
        return userHigh;
    }

    public void setUserHigh(UUID userHigh) {
        this.userHigh = userHigh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FriendshipId that = (FriendshipId) o;
        return Objects.equals(userLow, that.userLow) && Objects.equals(userHigh, that.userHigh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userLow, userHigh);
    }
}
