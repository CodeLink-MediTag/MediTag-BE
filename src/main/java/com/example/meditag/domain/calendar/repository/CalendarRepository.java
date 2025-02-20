package com.example.meditag.domain.calendar.repository;

import com.example.meditag.domain.calendar.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {
}
