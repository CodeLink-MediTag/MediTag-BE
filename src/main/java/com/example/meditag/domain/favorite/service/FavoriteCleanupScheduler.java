package com.example.meditag.domain.favorite.service;

import com.example.meditag.domain.favorite.entity.Favorite;
import com.example.meditag.domain.favorite.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FavoriteCleanupScheduler {

    private final FavoriteRepository favoriteRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupExpiredFavorites() {
        LocalDate today = LocalDate.now();
        favoriteRepository.findAll().forEach(f -> {
            LocalDate start = f.getMedicine().getStartDate();
            Integer duration = f.getMedicine().getDuration();
            if (start != null && duration != null) {
                LocalDate end = start.plusDays(duration - 1);
                if (end.isBefore(today)) {
                    favoriteRepository.delete(f);
                }
            }
        });
    }
}