package com.example.meditag.domain.alarm.repository;

import com.example.meditag.domain.alarm.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
}
