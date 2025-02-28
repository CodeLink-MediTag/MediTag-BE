package com.example.meditag.domain.alarm.repository;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    @Query("SELECT a FROM Alarm a JOIN a.medicine m JOIN Calendar c ON m = c.medicine WHERE m = :medicine AND c = :calendar")
    List<Alarm> findByMedicineAndCalendar(@Param("medicine") Medicine medicine, @Param("calendar") Calendar calendar);
}
