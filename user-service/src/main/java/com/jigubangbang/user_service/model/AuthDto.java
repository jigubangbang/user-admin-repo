package com.jigubangbang.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthDto implements UserDetails {

    private String id;          // user_id
    private String password;    // 암호화된 비밀번호
    private String role;        // ROLE_USER, ROLE_ADMIN
    private String status;      // ACTIVE, BANNED, WITHDRAWN

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null || role.isBlank()) {
            return Collections.singleton(() -> "ROLE_USER");
        }
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
        return Collections.singleton(() -> authority);
    }

    @Override
    public String getUsername() {
        return id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"BANNED".equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }
}
