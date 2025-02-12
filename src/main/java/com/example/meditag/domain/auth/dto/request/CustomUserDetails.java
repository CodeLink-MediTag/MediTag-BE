package com.example.meditag.domain.auth.dto.request;

import com.example.meditag.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {  // 🔹 Spring Security의 UserDetails 인터페이스 구현

    private final Member member;  // 🔹 인증할 사용자의 정보를 담고 있는 Member 객체

    // 🔹 생성자로 Member 객체를 받아 저장
    public CustomUserDetails(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 🔹 사용자의 권한(ROLE)을 반환하는 메서드

        Collection<GrantedAuthority> collection = new ArrayList<>();  // 🔹 권한 정보를 저장할 리스트 생성

        collection.add(new GrantedAuthority() {  // 🔹 익명 클래스를 사용하여 GrantedAuthority 구현
            @Override
            public String getAuthority() {
                return member.getRole();  // 🔹 Member에서 사용자의 역할(ROLE) 반환 (예: ROLE_USER, ROLE_ADMIN)
            }
        });

        return collection;  // 🔹 사용자의 권한 리스트 반환
    }

    @Override
    public String getPassword() {
        // 🔹 사용자의 비밀번호 반환
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        // 🔹 사용자의 아이디(username) 반환
        return member.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        // 🔹 계정이 만료되지 않았는지 여부 (true: 만료되지 않음)
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 🔹 계정이 잠겨있지 않은지 여부 (true: 잠겨있지 않음)
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 🔹 비밀번호가 만료되지 않았는지 여부 (true: 만료되지 않음)
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 🔹 계정이 활성화되어 있는지 여부 (true: 활성화됨)
        return true;
    }
}
