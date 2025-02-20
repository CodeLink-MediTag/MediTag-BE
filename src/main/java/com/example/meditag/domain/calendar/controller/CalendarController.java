package com.example.meditag.domain.calendar.controller;

import com.example.meditag.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class CalendarController {

    private final CalendarService calendarService;
}
