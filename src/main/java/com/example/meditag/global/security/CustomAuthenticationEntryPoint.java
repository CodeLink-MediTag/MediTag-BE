//package com.example.meditag.global.security;
//
//import com.example.meditag.global.error.ErrorResponse;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.AuthenticationEntryPoint;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Component
//public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
//
//    @Override
//    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//
//        // 예외 유형에 따라 다른 응답 처리
//        ErrorResponse errorResponse;
//        int status;
//
//        if (authException instanceof org.springframework.security.authentication.BadCredentialsException) {
//            status = HttpServletResponse.SC_UNAUTHORIZED;
//            errorResponse = new ErrorResponse(status, "아이디 또는 비밀번호가 잘못되었습니다.");
//        }
//        else if (authException instanceof org.springframework.security.authentication.DisabledException) {
//            status = HttpServletResponse.SC_FORBIDDEN;
//            errorResponse = new ErrorResponse(status, "비활성화된 계정입니다.");
//        }
//        else if (authException instanceof org.springframework.security.authentication.LockedException) {
//            status = HttpServletResponse.SC_FORBIDDEN;
//            errorResponse = new ErrorResponse(status, "잠긴 계정입니다.");
//        }
//        else {
//            status = HttpServletResponse.SC_UNAUTHORIZED;
//            errorResponse = new ErrorResponse(status, "인증에 실패했습니다.");
//        }
//
//        response.setStatus(status);
//        new ObjectMapper().writeValue(response.getWriter(), errorResponse);
//    }
//}
