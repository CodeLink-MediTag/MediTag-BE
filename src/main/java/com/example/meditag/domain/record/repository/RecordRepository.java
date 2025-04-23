package com.example.meditag.domain.record.repository;

import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.record.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Recording, Long> {

    // 🔧 날짜 범위에 해당하는 녹음 기록을 조회
    List<Recording> findByMemberAndRecordingTimeBetween(Member member, LocalDateTime start, LocalDateTime end);

    // ✅ (추가로 이미 있는 걸로 보이는 것)
    List<Recording> findByMemberOrderByRecordingTimeDesc(Member member);

    @Query("SELECT r FROM Recording r WHERE r.member = :member")
    List<Recording> findAllByMember(Member member);
}
