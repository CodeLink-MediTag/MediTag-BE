package com.example.meditag.domain.chatbot.repository;

import com.example.meditag.domain.chatbot.entity.PredefinedAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PredefinedAnswerRepository extends JpaRepository<PredefinedAnswer, Long> {

    // 유사한 질문 찾기 (MySQL 기준)
    @Query(value = "SELECT * FROM predefined_answer WHERE MATCH(question) AGAINST(:query IN NATURAL LANGUAGE MODE) LIMIT 1", nativeQuery = true)
    Optional<PredefinedAnswer> findSimilarQuestion(@Param("query") String query);

    // 정확한 질문 찾기
    Optional<PredefinedAnswer> findByQuestion(String question);
}
