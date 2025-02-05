package com.example.meditag.domain.member.repository;

import com.example.meditag.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    //email을 받아 회원 중복 확인
    Boolean existsByEmail(String email);

    //email을 받아 DB 테이블에서 회원을 조회하는 메소드 작성
    Member findByEmail(String email);
}
