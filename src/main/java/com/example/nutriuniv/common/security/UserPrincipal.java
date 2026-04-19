package com.example.nutriuniv.common.security;

import com.example.nutriuniv.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT 인증 필터 통과 후 SecurityContext에 저장되는 인증 객체.
 * principal.getId()로 현재 로그인한 유저 ID를 가져온다.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String role;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.role = user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return String.valueOf(id); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}