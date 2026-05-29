package com.jjikmeok.app.domain.ai.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.ai.dto.ExtractedActivityDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiActivityParser {

    private static final int MAX_CONTEXT_CHARS = 18_000;

    private final ChatClient chatClient;
    private final BeanOutputConverter<ExtractedActivityDto> outputConverter;

    public AiActivityParser(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(ExtractedActivityDto.class);
    }

    public ExtractedActivityDto parseFallback(String coreContext, SourceType sourceType) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt(sourceType))
                    .user(u -> u.text("""
                            [분석 대상 원문]
                            {text}

                            [출력 스키마]
                            {format}
                            """)
                            .param("text", compact(coreContext))
                            .param("format", outputConverter.getFormat()))
                    .call()
                    .entity(outputConverter);
        } catch (Exception e) {
            log.warn("⚠️ AI 보완 파싱 실패. sourceType={}, reason={}", sourceType, e.getMessage());
            return new ExtractedActivityDto(null, null, null, null, null, null, null, null, null);
        }
    }

    private String systemPrompt(SourceType sourceType) {
        return """
                당신은 외부 오픈 API 원문과 source_url 상세 페이지 본문에서 Activity 엔티티 보완 필드만 추출하는 데이터 정규화 엔진입니다.

                반드시 JSON 객체 하나만 반환하세요.
                설명, 마크다운, 코드블록, 부가 문장은 절대 반환하지 마세요.

                [공통 절대 규칙]
                - 원문에 명확히 존재하는 값만 추출합니다.
                - 추측하지 않습니다.
                - 모르는 값은 null입니다.
                - 2099년, 2999년, 9999년 날짜는 null입니다.
                - fallback 문구는 절대 반환하지 않습니다.
                - contactInfo는 전화번호 또는 이메일만 반환합니다.
                - "고객센터", "문의처", "담당자" 같은 일반 단어만 있으면 contactInfo는 null입니다.
                - target은 관람연령, 신청대상, 이용대상, 모집대상, 봉사대상, 참석자 조건만 반환합니다.
                - organizer는 주최, 주관, 운영기관, 모집기관명만 반환합니다.
                - description은 사용자에게 보여줄 자연스러운 1~3문장 요약입니다.
                - description에 URL, 신청 바로가기, HTML 태그, 메뉴명, 공유문구, 원본 XML/JSON을 넣지 않습니다.
                - price는 숫자만 반환합니다.
                - 무료, 전석무료, 참가비 없음이면 price=0입니다.
                - 유료인데 금액이 불명확하면 price=null입니다.
                - 비용 정보가 없으면 price=null입니다.
                - 날짜만 있으면 00:00:00으로 반환합니다.
                - 월/일만 있으면 2026년 기준입니다.
                - 모집기간과 실제 활동기간을 혼동하지 마세요.

                [날짜 필드]
                - recruitStartAt: 신청/모집/접수 시작일
                - recruitEndAt: 신청/모집/접수 마감일
                - startAt: 실제 행사/공연/전시/교육/봉사 시작일
                - endAt: 실제 행사/공연/전시/교육/봉사 종료일

                """ + sourcePrompt(sourceType);
    }

    private String sourcePrompt(SourceType sourceType) {
        if (sourceType == null) return "";

        return switch (sourceType) {
            case VOLUNTEER_1365 -> """
                    
                    [1365 봉사 API 전용 규칙]
                    - 봉사기간은 startAt/endAt입니다.
                    - 모집기간은 recruitStartAt/recruitEndAt입니다.
                    - 봉사대상은 target입니다. 예: 아동·청소년, 노인, 장애인, 기타, 환경, 동물.
                    - 모집기관은 organizer입니다.
                    - 봉사장소 또는 지도 주소는 address용 정보이지만 이 DTO에는 address 필드가 없으므로 description에 섞지 않습니다.
                    - contactInfo는 담당자 전화번호 또는 이메일만 반환합니다. 1522-3658은 제외합니다.
                    - description은 상세정보의 봉사내용을 중심으로 1~2문장으로 요약합니다.
                    - "활동구분", "첨부파일", "SNS 공유", "목록", "신청하기"는 무시합니다.
                    """;

            case KOPIS -> """
                    
                    [KOPIS 전용 규칙]
                    - 공연기간은 startAt/endAt입니다.
                    - 관람연령은 target입니다.
                    - 티켓가격은 price입니다.
                    - 주최·주관은 organizer입니다.
                    - 문의 전화번호 또는 이메일은 contactInfo입니다.
                    - 공연 소개, 출연진, 제작진 정보를 description으로 요약합니다.
                    - 공연장 주소는 description에 섞지 않습니다.
                    - recruitStartAt/recruitEndAt은 대부분 null입니다.
                    """;

            case YOUTH_CONTENT -> """
                    
                    [YOUTH_CONTENT 전용 규칙]
                    - 모집기간은 recruitStartAt/recruitEndAt입니다.
                    - 일경험 기간, 활동기간, 교육기간, 프로젝트 기간은 startAt/endAt입니다.
                    - 주관기관 또는 운영기관은 organizer입니다.
                    - 지역이 있으면 실제 활동 지역 판단에 활용합니다.
                    - target은 청년, 미취업 청년, 만 19~39세, 구직자 등 명확한 대상만 반환합니다.
                    - contactInfo는 문의처의 전화번호/이메일만 반환합니다.
                    - price는 무료가 명확하면 0, 비용 정보가 없으면 null입니다.
                    - 설문 문항, "내 답변", "구직상태", "알게 된 경로"는 무시합니다.
                    - docs.google 폼 본문은 일시/장소/참석자/신청기간/문의 필드를 각각 분리해서 판단합니다.
                    - miniintern 본문은 접수기간, 활동기간, 진행장소, 참가비용, 문의를 분리해서 판단합니다.
                    """;

            case EXHIBITION -> """
                    
                    [EXHIBITION 전용 규칙]
                    - PERIOD 또는 EVENT_PERIOD는 startAt/endAt입니다.
                    - CONTACT_POINT는 contactInfo입니다.
                    - CONTRIBUTOR 또는 CNTC_INSTT_NM은 organizer입니다.
                    - AUDIENCE가 A이면 "전체 관람가" 또는 "전체관람"으로 판단합니다.
                    - CHARGE가 무료 또는 0이면 price=0입니다.
                    - CHARGE가 01, 02 같은 코드값이면 price=null입니다.
                    - DESCRIPTION은 전시 주제와 주요 관람 내용을 중심으로 요약합니다.
                    - URL 상세 페이지에 요금정보, 입장연령, 도로명주소, 연락처가 있으면 그 값을 우선합니다.
                    """;

            case SEOUL_CULTURE -> """
                    
                    [SEOUL_CULTURE 전용 규칙]
                    - DATE, STRTDATE, END_DATE는 startAt/endAt입니다.
                    - PRO_TIME은 시간 정보입니다.
                    - ORG_NAME은 organizer입니다.
                    - USE_TRGT는 target입니다.
                    - USE_FEE는 price입니다.
                    - INQUIRY는 contactInfo입니다.
                    - ETC_DESC, PROGRAM, 상세 URL 본문 소개글은 description입니다.
                    """;

            case SEOUL_RESERVATION -> """
                    
                    [SEOUL_RESERVATION 전용 규칙]
                    - RCPTBGNDT/RCPTENDDT는 recruitStartAt/recruitEndAt입니다.
                    - SVCOPNBGNDT/SVCOPNENDDT는 startAt/endAt입니다.
                    - USETGTINFO, USE_TRGT는 target입니다.
                    - TELNO는 contactInfo입니다.
                    - SVCCHARGENM, PAYATNM은 price입니다.
                    - DTLCONT, 상세 URL 본문은 description입니다.
                    """;

            case TOUR_API -> """
                    
                    [TOUR_API 전용 규칙]
                    - eventstartdate/eventenddate는 startAt/endAt입니다.
                    - overview, intro, content는 description입니다.
                    - tel은 contactInfo입니다.
                    - recruitStartAt/recruitEndAt은 대부분 null입니다.
                    - 비용 정보가 없으면 price=null입니다.
                    """;

            default -> "";
        };
    }

    private String compact(String value) {
        if (value == null || value.length() <= MAX_CONTEXT_CHARS) return value;
        int head = MAX_CONTEXT_CHARS / 2;
        int tail = MAX_CONTEXT_CHARS - head;
        return value.substring(0, head) + "\n...[중간 생략]...\n" + value.substring(value.length() - tail);
    }
}