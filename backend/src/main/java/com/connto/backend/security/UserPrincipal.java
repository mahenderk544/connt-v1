package com.connto.backend.security;

import com.connto.backend.domain.AppUser;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String phone;
    private final String passwordHash;
    private final boolean enabled;

    public UserPrincipal(UUID id, String phone, String passwordHash, boolean enabled) {
        this.id = id;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    public static UserPrincipal from(AppUser user) {
        return new UserPrincipal(
                user.getId(), user.getPhone(), user.getPasswordHash(), user.isEnabled());
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
