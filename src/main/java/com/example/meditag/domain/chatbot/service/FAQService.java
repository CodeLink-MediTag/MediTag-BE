package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.entity.FAQ;
import com.example.meditag.domain.chatbot.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FAQService {

    private final FAQRepository faqRepository;

    /**
     * 사용자 입력과 가장 유사한 질문 검색
     */
    public FAQ findClosestQuestion(String userInput) {
        List<FAQ> faqs = faqRepository.findAll();

        // 유사도 계산 (Levenshtein Distance)
        return faqs.stream()
                .min(Comparator.comparing(faq -> calculateSimilarity(userInput, faq.getQuestion())))
                .orElse(null);
    }

    /**
     * 문자열 유사도 계산 (Levenshtein Distance)
     */
    private int calculateSimilarity(String input, String question) {
        return org.apache.commons.text.similarity.LevenshteinDistance.getDefaultInstance().apply(input, question);
    }
}

