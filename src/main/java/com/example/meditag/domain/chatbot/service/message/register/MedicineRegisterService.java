package com.example.meditag.domain.chatbot.service.message.register;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.calendar.repository.CalendarRepository;
import com.example.meditag.domain.chatbot.dto.MedicineRegisterSession;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineRegisterService {

    private final MedicineRepository medicineRepository;
    private final MemberRepository memberRepository;
    private final CalendarRepository calendarRepository;
    private final SessionStorage sessionStorage;

    public boolean isApplicable(String message) {
        return message.matches(".*(약|알림).*등록(할게|할래|해줘|해|하고싶어|하고 싶어).*");
    }

    public String execute(String username, String message) {
        if (message.contains("취소") || message.contains("그만")) {
            sessionStorage.clearSession(username);
            log.info("[{}] 사용자가 등록을 취소함", username);
            return "약 등록을 취소했어요. 다시 시작하려면 '약 등록할게'라고 말해주세요.";
        }

        MedicineRegisterSession session = sessionStorage.getOrCreateSession(username);

        if (session.getCurrentStep() == null) {
            session.setCurrentStep(MedicineRegisterSession.Step.NAME);
            log.info("[{}] 세션 시작됨. Step: NAME", username);
            return "약 등록을 시작할게요! 약 이름이 무엇인가요?";
        }

        try {
            log.info("[{}] 현재 Step: {}", username, session.getCurrentStep());
            log.info("[{}] 사용자 입력: {}", username, message);

            return switch (session.getCurrentStep()) {
                case NAME -> {
                    session.setName(message.trim());
                    session.setCurrentStep(MedicineRegisterSession.Step.CHARACTERISTIC);
                    yield "약의 특징은 무엇인가요?";
                }
                case CHARACTERISTIC -> {
                    session.setCharacteristic(message.trim());
                    session.setCurrentStep(MedicineRegisterSession.Step.START_DATE);
                    yield "복용 시작일은 언제인가요? 연도, 월, 일을 정확하게 말해주세요!";
                }
                case START_DATE -> {
                    session.setStartDate(parseNaturalDate(message));
                    session.setCurrentStep(MedicineRegisterSession.Step.DURATION);
                    yield "복용 기간은 며칠인가요?";
                }
                case DURATION -> {
                    int duration = extractDaysFromText(message);
                    session.setDuration(duration);
                    session.setCurrentStep(MedicineRegisterSession.Step.FREQUENCY);
                    yield "하루 복용 횟수는 몇 회인가요? (1~3회 권장)";
                }
                case FREQUENCY -> {
                    int frequency = extractFrequencyFromText(message);
                    session.setFrequency(frequency);
                    session.setCurrentStep(MedicineRegisterSession.Step.PRESCRIBED);
                    yield "이 약은 처방약인가요? (예/아니오)";
                }
                case PRESCRIBED -> {
                    boolean isPrescribed = !message.contains("아니");
                    session.setPrescribed(isPrescribed);

                    if (isPrescribed) {
                        Member member = memberRepository.findByUsername(username)
                                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
                        if (medicineRepository.existsByMemberAndPrescribed(member, true)) {
                            sessionStorage.clearSession(username);
                            yield "이미 등록된 처방약이 존재합니다!";
                        }
                    }

                    session.setCurrentStep(MedicineRegisterSession.Step.ALARM_DOSAGE_TIME);
                    if (isPrescribed) {
                        yield buildPrescriptionTimeQuestion(session.getFrequency());
                    } else {
                        yield "복용할 시간들을 입력해주세요! 예를 들어 오전 8시, 오후 6시 (시간 앞에 오전/오후를 붙여주세요)";
                    }
                }
                case ALARM_DOSAGE_TIME -> {
                    if (session.getFrequency() == null) {
                        sessionStorage.clearSession(username);
                        yield "복용 횟수 정보가 누락되었습니다. 약 등록을 다시 시작해주세요.";
                    }

                    final List<String> dosageLabels = new ArrayList<>();
                    final List<LocalTime> times = new ArrayList<>();

                    if (session.isPrescribed()) {
                        // ✅ 처방약: 아침/점심/저녁 중 선택값 → 고정 시각 자동 매핑
                        List<String> meals = parseMealSelections(message);
                        if (meals.size() != session.getFrequency()) {
                            yield "복용 횟수(" + session.getFrequency() + "회)와 동일한 개수로 "
                                    + "아침/점심/저녁 중에서 선택해 주세요.";
                        }

                        for (String meal : meals) {
                            LocalTime t = mapMealToTime(meal);
                            if (t == null) {
                                yield "아침/점심/저녁만 선택할 수 있어요.'";
                            }
                            dosageLabels.add(meal); // 라벨은 선택된 값 그대로
                            times.add(t);           // 시각은 고정값 (아침 08:00 / 점심 12:00 / 저녁 18:00)
                        }
                    } else {
                        // ✅ 일반약: 사용자가 입력한 시각 그대로 등록
                        List<String> parsed = extractTimesFromText(message);
                        if (parsed.size() != session.getFrequency()) {
                            yield "복용 횟수에 맞게 정확히 " + session.getFrequency() + "개의 시간을 입력해주세요!";
                        }
                        for (String timeStr : parsed) {
                            String[] parts = timeStr.split(":");
                            LocalTime localTime = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                            times.add(localTime);
                            dosageLabels.add(getDosageLabel(localTime)); // 라벨은 시간대로 자동 구분
                        }
                    }

                    session.setDosageTimes(times.stream()
                            .map(t -> String.format("%02d:%02d", t.getHour(), t.getMinute()))
                            .toArray(String[]::new));
                    session.setCurrentStep(MedicineRegisterSession.Step.COMPLETE);

                    Member member = memberRepository.findByUsername(username)
                            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

                    Medicine medicine = Medicine.builder()
                            .name(session.getName())
                            .characteristic(session.getCharacteristic())
                            .startDate(session.getStartDate())
                            .duration(session.getDuration())
                            .frequency(session.getFrequency())
                            .prescribed(session.isPrescribed())
                            .imageUrl(null)
                            .member(member)
                            .build();

                    medicineRepository.save(medicine);

                    // 기간 동안의 캘린더 + 알람 생성
                    for (int day = 0; day < session.getDuration(); day++) {
                        LocalDate currentDay = session.getStartDate().plusDays(day);
                        Calendar calendar = Calendar.builder()
                                .date(currentDay)
                                .medicine(medicine)
                                .alarms(new ArrayList<>())
                                .build();

                        for (int i = 0; i < times.size(); i++) {
                            LocalTime localTime = times.get(i);
                            LocalDateTime alarmDateTime = LocalDateTime.of(currentDay, localTime);

                            String label = session.isPrescribed()
                                    ? dosageLabels.get(i)         // 처방약: 선택 라벨 그대로
                                    : getDosageLabel(localTime);  // 일반약: 시간대 자동 라벨

                            Alarm alarm = Alarm.builder()
                                    .dosageTime(label)
                                    .alarmTime(alarmDateTime)
                                    .taking(false)
                                    .calendar(calendar)
                                    .build();

                            calendar.getAlarms().add(alarm);
                        }

                        calendarRepository.save(calendar);
                    }

                    sessionStorage.clearSession(username);
                    yield "약과 알림이 성공적으로 등록되었습니다!";
                }
                default -> {
                    sessionStorage.clearSession(username);
                    yield "약 등록을 다시 시작하려면 '약 등록할게'라고 말씀해주세요.";
                }
            };
        } catch (Exception e) {
            log.error("약 등록 처리 중 오류", e);
            sessionStorage.clearSession(username);
            return "입력 도중 오류가 발생했어요. 다시 시도해주세요.";
        }
    }

    // ===================== 날짜/횟수/시간 파싱 =====================

    private LocalDate parseNaturalDate(String message) {
        message = message.replaceAll("\\s+", ""); // 공백 제거
        LocalDate today = LocalDate.now();

        // ✅ 상대 날짜 표현
        if (message.contains("오늘")) return today;
        if (message.contains("내일")) return today.plusDays(1);
        if (message.contains("모레")) return today.plusDays(2);
        if (message.contains("어제")) return today.minusDays(1);
        if (message.contains("그제")) return today.minusDays(2);

        // ✅ 요일 인식 처리
        int baseWeekOffset = 0;
        if (message.contains("이번주")) baseWeekOffset = 0;
        else if (message.contains("다음주")) baseWeekOffset = 1;
        else if (message.contains("다다음주")) baseWeekOffset = 2;

        // 요일 한글 → DayOfWeek 매핑
        Map<String, DayOfWeek> dayOfWeekMap = Map.of(
                "월요일", DayOfWeek.MONDAY,
                "화요일", DayOfWeek.TUESDAY,
                "수요일", DayOfWeek.WEDNESDAY,
                "목요일", DayOfWeek.THURSDAY,
                "금요일", DayOfWeek.FRIDAY,
                "토요일", DayOfWeek.SATURDAY,
                "일요일", DayOfWeek.SUNDAY
        );

        for (String keyword : dayOfWeekMap.keySet()) {
            if (message.contains(keyword)) {
                DayOfWeek targetDay = dayOfWeekMap.get(keyword);
                LocalDate baseDate = today.plusWeeks(baseWeekOffset);

                // baseDate 기준 가장 가까운 해당 요일 찾기
                while (baseDate.getDayOfWeek() != targetDay) {
                    baseDate = baseDate.plusDays(1);
                }
                return baseDate;
            }
        }

        // ✅ 주말/평일 인식
        if (message.contains("주말")) {
            LocalDate sat = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            return sat;
        }
        if (message.contains("평일")) {
            // 오늘이 평일이면 오늘, 아니면 다음 월요일
            return (today.getDayOfWeek().getValue() <= 5)
                    ? today
                    : today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }

        // ✅ 날짜 형식 인식 (예: 25년 4월 11일, 4/11, 2025.04.11)
        Matcher matcher = Pattern.compile("(\\d{2,4})?[년\\-/\\.]*?(\\d{1,2})[월\\-/\\.]*?(\\d{1,2})[일]?$")
                .matcher(message);
        if (matcher.find()) {
            int year;
            if (matcher.group(1) != null) {
                int parsedYear = Integer.parseInt(matcher.group(1));
                year = parsedYear < 100 ? 2000 + parsedYear : parsedYear;
            } else {
                year = LocalDate.now().getYear();
            }

            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            return LocalDate.of(year, month, day);
        }

        throw new IllegalArgumentException("날짜 형식을 인식할 수 없습니다.");
    }

    private int extractDaysFromText(String message) {
        message = message.replaceAll("[\\s]", "");
        if (message.contains("일주일") || message.contains("한주") || message.contains("1주") || message.contains("1주일")) return 7;
        if (message.contains("이주일") || message.contains("2주") || message.contains("이주") || message.contains("2주일")) return 14;
        if (message.contains("삼일") || message.contains("3일")) return 3;
        if (message.contains("사일") || message.contains("4일")) return 4;
        if (message.contains("오일") || message.contains("5일")) return 5;
        if (message.contains("십오일") || message.contains("보름") || message.contains("15일")) return 15;
        if (message.contains("한달") || message.contains("한 달") || message.contains("30일") || message.contains("삼십일")) return 30;

        // ✅ 버그 수정: 문자클래스가 아닌 정상적인 그룹 매칭으로 교체
        Matcher m = Pattern.compile("(\\d+)\\s*(일|일간)").matcher(message);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 1;
    }

    private int extractFrequencyFromText(String message) {
        message = message.replaceAll("[\\s]", "");
        if (message.contains("한번") || message.contains("1회")) return 1;
        if (message.contains("두번") || message.contains("2회")) return 2;
        if (message.contains("세번") || message.contains("3회")) return 3;
        if (message.contains("네번") || message.contains("4회")) return 4;

        // ✅ 버그 수정: 문자클래스가 아닌 정상적인 그룹 매칭으로 교체
        Matcher m = Pattern.compile("(\\d+)\\s*(번|회)").matcher(message);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 1;
    }

    private List<String> extractTimesFromText(String message) {
        List<String> result = new ArrayList<>();
        message = message.replace("반", "30분")
                .replaceAll("(열두시|십이시)", "12시")
                .replaceAll("(열한시|십일시)", "11시")
                .replaceAll("(열시|십시)", "10시")
                .replaceAll("(아홉시|구시)", "9시")
                .replaceAll("(여덟시|팔시)", "8시")
                .replaceAll("(일곱시|칠시)", "7시")
                .replaceAll("(여섯시|육시)", "6시")
                .replaceAll("(다섯시|오시)", "5시")
                .replaceAll("(네시|사시)", "4시")
                .replaceAll("(세시|삼시)", "3시")
                .replaceAll("(두시|이시)", "2시")
                .replaceAll("(한시|일시)", "1시");

        Matcher matcher = Pattern.compile("(오전|오후)?\\s*(\\d{1,2})시\\s*(\\d{1,2})?분?").matcher(message);
        while (matcher.find()) {
            String meridiem = matcher.group(1);
            int hour = Integer.parseInt(matcher.group(2));
            int minute = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            if ("오후".equals(meridiem) && hour < 12) hour += 12;
            else if ("오전".equals(meridiem) && hour == 12) hour = 0;
            result.add(String.format("%02d:%02d", hour, minute));
        }

        if (result.isEmpty()) {
            Matcher fallback = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(message);
            while (fallback.find()) {
                result.add(String.format("%02d:%02d", Integer.parseInt(fallback.group(1)), Integer.parseInt(fallback.group(2))));
            }
        }

        return result;
    }

    // ===================== 처방약 전용: 식사 선택/고정 시각 매핑 =====================

    /** 처방약 질문 문구 */
    private String buildPrescriptionTimeQuestion(int timesPerDay) {
        String base = """
                처방약은 정해진 시간에 복용 알림을 드려요.
            아침(8시), 점심(12시), 저녁(6시) 중에서 복용 횟수에 맞게 선택해 주세요.
            """;
        return base + "현재 입력된 1일 복용 횟수: " + timesPerDay + "회";
    }

    /** '아침/점심/저녁' 선택 파싱 (중복 제거, 입력 순서 유지) */
    private List<String> parseMealSelections(String message) {
        // 구분자: 공백, 콤마, '와', '그리고', '+', '/'
        String normalized = message.replaceAll("[,\\+/]|그리고|및|와|과", " ");
        String[] tokens = normalized.trim().split("\\s+");

        Set<String> allow = new HashSet<>(Arrays.asList("아침", "점심", "저녁"));
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String t : tokens) {
            if (allow.contains(t)) ordered.add(t);
        }
        return new ArrayList<>(ordered);
    }

    /** 식사 라벨 → 고정 시각 매핑 */
    private LocalTime mapMealToTime(String meal) {
        switch (meal) {
            case "아침": return LocalTime.of(8, 0);
            case "점심": return LocalTime.NOON; // 12:00
            case "저녁": return LocalTime.of(18, 0);
            default: return null;
        }
    }

    // ===================== 공통 유틸 =====================

    private String getDosageLabel(LocalTime time) {
        if (time.isBefore(LocalTime.NOON)) return "아침";
        else if (time.isBefore(LocalTime.of(18, 0))) return "점심";
        else return "저녁";
    }

    public boolean isInProgress(String username) {
        MedicineRegisterSession session = sessionStorage.getOrCreateSession(username);
        return session.getCurrentStep() != null && session.getCurrentStep() != MedicineRegisterSession.Step.COMPLETE;
    }
}
