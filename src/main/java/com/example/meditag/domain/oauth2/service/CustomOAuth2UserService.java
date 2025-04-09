package com.example.meditag.domain.oauth2.service;

import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.domain.oauth2.dto.CustomOAuth2User;
import com.example.meditag.domain.oauth2.dto.KakaoResponse;
import com.example.meditag.domain.oauth2.dto.NaverResponse;
import com.example.meditag.domain.oauth2.dto.OAuth2Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    public CustomOAuth2UserService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        log.info("[CustomOAuth2UserService] CustomOAuth2UserService 생성자 주입");
    }

    //사용자 정보 데이터를 인자로 받아오는 매소드
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
        OAuth2User oAuth2User = super.loadUser(userRequest);//유저정보가져오기
        log.info("[CustomOAuth2UserService/loadUser] 1. OAuth2 유저 정보 가져오기: {}", oAuth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();//구글, 카카오, 네이버 구분
        log.info("[CustomOAuth2UserService/loadUser] 2. 로그인한 소셜 서비스: {}", registrationId);

        OAuth2Response oAuth2Response = null;
        if (registrationId.equals("naver")) {
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        } else if (registrationId.equals("kakao")) {
            oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());
        } else {
            log.warn("[CustomOAuth2UserService/loadUser] 지원되지 않는 소셜 로그인 요청: {}", registrationId);
            return null;
        }
        log.info("[CustomOAuth2UserService/loadUser] 3. 소셜 유저 정보 변환 완료: {}", oAuth2Response);

        //customOAuth2User에서 만들어놓은 매소드를 활용 하여 username 데이터 넣기
        String username = oAuth2Response.getProvider() +" "+ oAuth2Response.getProviderId();
        log.info("[CustomOAuth2UserService/loadUser] 4. 소셜 정보 기반 username 생성: {}", username);

        Optional<Member> existData = memberRepository.findByUsername(username);
        log.info("[CustomOAuth2UserService/loadUser] 5. DB에서 username 조회 결과: {}", existData.orElse(null));

        String role = null;

        if (existData.isEmpty()) {  // Optional이므로 null 체크 대신 isEmpty() 사용
            log.info("[CustomOAuth2UserService/loadUser] 6. 신규 회원 등록 진행");

            Member member = Member.builder()
                    .username(username)  // email을 username으로 사용
                    .name(oAuth2Response.getName())       // 이름 설정
                    .role("ROLE_USER")                   // 기본 역할 설정
                    .build();

            memberRepository.save(member);
            log.info("[CustomOAuth2UserService/loadUser] 7. 신규 회원 저장 완료: {}", member);

        } else {
            Member member = existData.get();  // Optional에서 Member 객체 추출
            role = member.getRole();
            log.info("[CustomOAuth2UserService/loadUser] 6. 기존 회원 로그인 (role: {})", role);
            
            // 새로운 Member 객체를 생성하여 업데이트
            Member updatedMember = Member.builder()
                    .id(member.getId())           // 기존 ID 유지
                    .username(username)  // 이메일 업데이트
                    .name(member.getName())       // 기존 이름 유지
                    .role(member.getRole())       // 기존 역할 유지
                    .phone(member.getPhone())     // 기존 전화번호 유지
                    .password(member.getPassword()) // 기존 비밀번호 유지
                    .build();
            
            memberRepository.save(updatedMember);
            log.info("[CustomOAuth2UserService/loadUser] 7. 기존 회원 정보 업데이트 완료: {}", updatedMember);

        }
        return new CustomOAuth2User(oAuth2Response,role);
    }
}
