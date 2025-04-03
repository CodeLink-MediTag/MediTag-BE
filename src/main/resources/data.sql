-- 오늘 약 복약 정보 요청
INSERT INTO faq (question, answer, action_type, action_target, created_at, updated_at)
VALUES
    ('약 뭐 먹어야 돼?', '복약 정보를 불러오고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),
    ('약 뭐야?', '복약 정보를 불러오고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),
    ('복용할 약 알려줘', '복약 정보를 불러오고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),
    ('복약 정보 알려줘', '복약 정보를 불러오고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),
    ('약 언제 먹지?', '복약 정보를 확인 중입니다.', 'FETCH_DATA', null, NOW(), NOW()),
    ('복용 시간 알려줘', '복약 정보를 확인 중입니다.', 'FETCH_DATA', null, NOW(), NOW()),

-- 복용 여부 확인
    ('나 약 먹었어?', '복약 기록을 확인하고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),
    ('복용 완료했는지 알려줘', '복용 상태를 확인하고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),

-- 날짜 지정 질문
    ('3월 31일에 먹은 약 뭐야?', '해당 날짜의 복약 정보를 불러옵니다.', 'FETCH_DATA', null, NOW(), NOW()),
    ('어제 먹은 약 뭐였어?', '어제 복약 기록을 확인하고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),

-- 녹음 정보 확인
    ('주의사항 듣고 싶어', '녹음된 내용을 불러오고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),
    ('녹음 뭐 있었지?', '녹음 기록을 확인하고 있어요.', 'FETCH_DATA', null, NOW(), NOW()),

-- 화면 이동 (예: 메인으로)
    ('메인으로 돌아가줘', '메인 화면으로 이동합니다.', 'NAVIGATE', '/main', NOW(), NOW()),
    ('홈으로 가자', '홈 화면으로 이동합니다.', 'NAVIGATE', '/home', NOW(), NOW());
