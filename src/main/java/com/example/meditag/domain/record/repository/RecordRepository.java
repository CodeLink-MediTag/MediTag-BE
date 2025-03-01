package com.example.meditag.domain.record.repository;

import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.record.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Recording, Long> {
    List<Recording> findByMemberOrderByRecordingTimeDesc(Member member);

}
