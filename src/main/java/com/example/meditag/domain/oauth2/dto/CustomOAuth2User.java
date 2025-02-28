package com.example.meditag.domain.oauth2.dto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2Response oAuth2Response;
    private final String role;

    public CustomOAuth2User(OAuth2Response oAuth2Response, String role) {
        this.oAuth2Response = oAuth2Response;
        this.role = role;
        log.info("[CustomOAuth2User] CustomOAuth2User 객체 생성 - 이름: {}, 역할: {}", oAuth2Response.getName(), role);
    }

    @Override
    public Map<String, Object> getAttributes() {
        log.info("[CustomOAuth2User/getAttributes] getAttributes() 호출");
        return null; // 필요하면 실제 데이터를 반환하도록 수정 가능
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        log.info("[CustomOAuth2User/getAuthorities] getAuthorities() 호출 - 역할: {}", role);
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add(() -> role);
        return collection;
    }

    @Override
    public String getName() {
        log.info("[CustomOAuth2User/getName] getName() 호출 - 이름: {}", oAuth2Response.getName());
        return oAuth2Response.getName();
    }

    public String getUsername() {
        String providerId = oAuth2Response.getProviderId();
        String provider = oAuth2Response.getProvider();
        String username = provider + " " + providerId;
        log.info("[CustomOAuth2User/getUsername] getUsername() 호출 - username: {}", username);
        return username;
    }
}
