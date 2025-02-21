package com.example.meditag.domain.auth.dto.request;

import com.example.meditag.domain.member.entity.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class CustomUserDetails implements UserDetails {  // Spring Security의 UserDetails 인터페이스 구현

    private final Member member;  // 인증할 사용자의 정보를 담고 있는 Member 객체

    // 생성자로 Member 객체를 받아 저장
    public CustomUserDetails(Member member) {
        this.member = member;
        log.info("[CustomUserDetails] CustomUserDetails 생성자 주입: 사용자 ID={}", member.getUsername());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        log.info("[CustomUserDetails/getAuthorities] 1. 사용자 권한(ROLE) 반환 시작");

        Collection<GrantedAuthority> collection = new ArrayList<>();  // 권한 정보를 저장할 리스트 생성

        collection.add(new GrantedAuthority() {  // 익명 클래스를 사용하여 GrantedAuthority 구현
            @Override
            public String getAuthority() {
                log.info("[CustomUserDetails/getAuthority] 3. 사용자의 역할(ROLE) 반환: {}", member.getRole());
                return member.getRole();  // Member에서 사용자의 역할(ROLE) 반환 (예: ROLE_USER, ROLE_ADMIN)
            }
        });

        log.info("[CustomUserDetails/getAuthorities] 2. 권한 반환 완료");
        return collection;  // 사용자의 권한 리스트 반환
    }

    @Override
    public String getPassword() {
        // 사용자의 비밀번호 반환
        log.info("[CustomUserDetails/getPassword] 사용자 비밀번호 반환");
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        // 사용자의 아이디(username) 반환
        String username = member.getUsername();
        log.info("[CustomUserDetails/getUsername] 반환할 사용자 아이디: {}", username);
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정이 만료되지 않았는지 여부 (true: 만료되지 않음)
        log.info("[CustomUserDetails/isAccountNonExpired] 계정 만료 여부 확인 (true로 설정)");
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정이 잠겨있지 않은지 여부 (true: 잠겨있지 않음)
        log.info("[CustomUserDetails/isAccountNonLocked] 계정 잠김 여부 확인 (true로 설정)");
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호가 만료되지 않았는지 여부 (true: 만료되지 않음)
        log.info("[CustomUserDetails/isCredentialsNonExpired] 비밀번호 만료 여부 확인 (true로 설정)");
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 계정이 활성화되어 있는지 여부 (true: 활성화됨)
        log.info("[CustomUserDetails/isEnabled] 계정 활성화 여부 확인 (true로 설정)");
        return true;
    }
}
