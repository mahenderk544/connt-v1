package com.connto.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FriendshipId implements Serializable {

    @Column(name = "user_one", nullable = false)
    private UUID userOne;

    @Column(name = "user_two", nullable = false)
    private UUID userTwo;

    public FriendshipId() {}

    public FriendshipId(UUID userOne, UUID userTwo) {
        this.userOne = userOne;
        this.userTwo = userTwo;
    }

    public UUID getUserOne() {
        return userOne;
    }

    public void setUserOne(UUID userOne) {
        this.userOne = userOne;
    }

    public UUID getUserTwo() {
        return userTwo;
    }

    public void setUserTwo(UUID userTwo) {
        this.userTwo = userTwo;
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
        return Objects.equals(userOne, that.userOne) && Objects.equals(userTwo, that.userTwo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userOne, userTwo);
    }
}
