package com.example.meditag.domain.calendar.service;

import com.example.meditag.domain.calendar.repository.CalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;

}
