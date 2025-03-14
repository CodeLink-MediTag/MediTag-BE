-- 전문 검색 인덱스 생성 (MySQL 기준)
CREATE FULLTEXT INDEX idx_question ON predefined_answer(question);