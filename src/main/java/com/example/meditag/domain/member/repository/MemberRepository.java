package com.example.meditag.domain.member.repository;

import com.example.meditag.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    //email을 받아 DB 테이블에서 회원을 조회하는 메소드 작성
    Optional<Member> findByUsername(String username);
//    Member Map<String, Object>


}
