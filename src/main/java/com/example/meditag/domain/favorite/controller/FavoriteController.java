package com.example.meditag.domain.favorite.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.favorite.controller.api.FavoriteApi;
import com.example.meditag.domain.favorite.dto.FavoriteSetRequestDTO;
import com.example.meditag.domain.favorite.dto.FavoriteResponseDTO;
import com.example.meditag.domain.favorite.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FavoriteController implements FavoriteApi {

    private final FavoriteService favoriteService;

    @Override
    public ResponseEntity<String> setFavorite(CustomUserDetails customUserDetails, FavoriteSetRequestDTO request) {
        favoriteService.setFavorite(customUserDetails.getUsername(), request.getMedicineId());
        return ResponseEntity.ok("약 즐겨찾기 등록이 완료 되었습니다.");
    }

    @Override
    public ResponseEntity<String> removeFavorite(CustomUserDetails customUserDetails) {
        favoriteService.removeFavorite(customUserDetails.getUsername());
        return ResponseEntity.ok("즐겨찾기가 해제되었습니다.");
    }

    @Override
    public ResponseEntity<FavoriteResponseDTO> getFavorite(CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(favoriteService.getFavorite(customUserDetails.getUsername()));
    }
}