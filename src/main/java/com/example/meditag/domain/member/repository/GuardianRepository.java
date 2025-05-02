package com.example.meditag.domain.member.repository;

import com.example.meditag.domain.member.entity.Guardian;
import com.example.meditag.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    List<Guardian> findByMember(Member member);
}
