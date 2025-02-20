package com.example.meditag.domain.medicine_calendar.service;

import com.example.meditag.domain.medicine_calendar.repository.MedicineCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicineCalendarService {

    private final MedicineCalendarRepository medicineCalendarRepository;
}
