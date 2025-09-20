package com.example.meditag.domain.record.repository;

import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.record.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecordRepository extends JpaRepository<Recording, Long> {

    List<Recording> findByMemberAndRecordingTimeBetween(Member member, LocalDateTime start, LocalDateTime end);

    List<Recording> findByMemberOrderByRecordingTimeDesc(Member member);

    @Query("SELECT r FROM Recording r WHERE r.member = :member")
    List<Recording> findAllByMember(Member member);

    // ✅ 삭제 전 소유자 검증
    Optional<Recording> findByIdAndMember(Long id, Member member);
}

