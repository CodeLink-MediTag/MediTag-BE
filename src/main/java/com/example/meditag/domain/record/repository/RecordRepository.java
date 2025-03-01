package com.example.meditag.domain.record.repository;

import com.example.meditag.domain.record.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordRepository extends JpaRepository<Recording, Long> {

}
