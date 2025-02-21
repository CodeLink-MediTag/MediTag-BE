package com.example.meditag.domain.oauth2.service;

import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.domain.oauth2.dto.CustomOAuth2User;
import com.example.meditag.domain.oauth2.dto.NaverResponse;
import com.example.meditag.domain.oauth2.dto.OAuth2Response;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    public CustomOAuth2UserService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    //사용자정보 데이터를 인자로 받아오는 매소드
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
        OAuth2User oAuth2User = super.loadUser(userRequest);//유저정보가져오기
        System.out.println(oAuth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();//구글, 카카오, 네이버 구분

        OAuth2Response oAuth2Response = null;
        if(registrationId.equals("naver")){

            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
            //바구니에서 데이터를 뽑아온것.

        } else{
            return null;
        }

        //customOAuth2User에서 만들어놓은 매소드를 활용 하여 username 데이터 넣기
        String username = oAuth2Response.getProvider()+" "+oAuth2Response.getProviderId();
        Optional<Member> existData = memberRepository.findByUsername(username);
        String role = null;

        if (existData.isEmpty()) {  // Optional이므로 null 체크 대신 isEmpty() 사용
            Member member = Member.builder()
                    .username(oAuth2Response.getEmail())  // email을 username으로 사용
                    .name(oAuth2Response.getName())       // 이름 설정
                    .role("ROLE_USER")                   // 기본 역할 설정
                    .build();
            
            memberRepository.save(member);
        } else {
            Member member = existData.get();  // Optional에서 Member 객체 추출
            role = member.getRole();
            
            // 새로운 Member 객체를 생성하여 업데이트
            Member updatedMember = Member.builder()
                    .id(member.getId())           // 기존 ID 유지
                    .username(oAuth2Response.getEmail())  // 이메일 업데이트
                    .name(member.getName())       // 기존 이름 유지
                    .role(member.getRole())       // 기존 역할 유지
                    .phone(member.getPhone())     // 기존 전화번호 유지
                    .password(member.getPassword()) // 기존 비밀번호 유지
                    .build();
            
            memberRepository.save(updatedMember);
        }

        return new CustomOAuth2User(oAuth2Response,role);
    }
}
