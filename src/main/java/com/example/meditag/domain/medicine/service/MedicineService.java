package com.example.meditag.domain.medicine.service;

import com.example.meditag.domain.medicine.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
}
