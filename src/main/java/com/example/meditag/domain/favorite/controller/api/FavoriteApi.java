package com.example.meditag.domain.favorite.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.favorite.dto.FavoriteSetRequestDTO;
import com.example.meditag.domain.favorite.dto.FavoriteResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "즐겨찾기 관리", description = "즐겨찾기 관련 API")
@RequestMapping("/favorite")
public interface FavoriteApi {

    @PutMapping
    ResponseEntity<String> setFavorite(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody FavoriteSetRequestDTO request
    );

    @DeleteMapping
    ResponseEntity<String> removeFavorite(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @GetMapping
    ResponseEntity<FavoriteResponseDTO> getFavorite(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}