package com.example.meditag.domain.alarm.repository;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    @Query("SELECT a FROM Alarm a " +
            "JOIN a.calendar c " +
            "JOIN c.medicine m " +
            "WHERE m = :medicine AND c.date = :date")
    List<Alarm> findByMedicineAndDate(@Param("medicine") Medicine medicine, @Param("date") LocalDate date);

}
