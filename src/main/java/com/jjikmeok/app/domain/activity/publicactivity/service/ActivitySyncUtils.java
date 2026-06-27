package com.jjikmeok.app.domain.activity.publicactivity.service;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ActivitySyncUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?<!\\d)(?:0\\d{1,2}[-) .]?\\d{3,4}[- .]?\\d{4}|1[568]\\d{2}[- .]?\\d{4})(?!\\d)");

    private static final Pattern DATE_RANGE_PATTERN =
            Pattern.compile("(20\\d{2})[.\\-/년\\s]+(\\d{1,2})[.\\-/월\\s]+(\\d{1,2})[^0-9]+~[^0-9]*(?:(20\\d{2})[.\\-/년\\s]+)?(\\d{1,2})[.\\-/월\\s]+(\\d{1,2})");

    private static final Pattern SINGLE_DATE_TIME_PATTERN =
            Pattern.compile("(20\\d{2})[.\\-/년\\s]+(\\d{1,2})[.\\-/월\\s]+(\\d{1,2}).{0,12}?(\\d{1,2})\\s*[:시]\\s*(\\d{1,2})?");

    public record DateRange(LocalDateTime start, LocalDateTime end) {}

    public boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @SafeVarargs
    public final <T> T first(T... values) {
        for (T value : values) {
            if (value == null) continue;
            if (value instanceof String s) {
                if (!s.isBlank()) return value;
            } else {
                return value;
            }
        }
        return null;
    }

    public String firstText(String... values) {
        return first(values);
    }

    public LocalDateTime firstDateTime(LocalDateTime... values) {
        return first(values);
    }

    public String cleanText(String value) {
        if (value == null) return null;

        String cleaned = repairMojibake(HtmlUtils.htmlUnescape(value))
                .replace('\u00A0', ' ')
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</li>|</tr>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    public String repairMojibake(String value) {
        if (value == null || !value.matches(".*[ÃÂìíëê][\\u0080-\\u00ff]?.*")) return value;

        CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder();
        if (!encoder.canEncode(value)) return value;

        String repaired = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return hangulCount(repaired) > hangulCount(value) ? repaired : value;
    }

    private int hangulCount(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\uAC00' && ch <= '\uD7A3') count++;
        }
        return count;
    }

    public Integer extractPriceFromText(String value, boolean requirePriceSignal) {
        if (isBlank(value)) return null;

        String cleaned = cleanText(value);
        if (cleaned == null) return null;

        if (cleaned.matches(".*(무료|전액\\s*무료|참가비\\s*없음|0원).*")) return 0;

        Pattern pattern = requirePriceSignal
                ? Pattern.compile("(\\d{1,3}(?:,\\d{3})+|\\d+)\\s*(?:원|KRW|₩)")
                : Pattern.compile("\\b(\\d{1,3}(?:,\\d{3})+|\\d+)\\b");

        Matcher matcher = pattern.matcher(cleaned);
        while (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    public String normalizeUrl(String value) {
        if (isBlank(value)) return null;
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:image/")) return value;
        if (value.startsWith("//")) return "https:" + value;
        if (value.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$")) return "https://" + value;
        return value;
    }

    public String contactOnly(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) return null;

        Set<String> contacts = new LinkedHashSet<>();

        Matcher email = EMAIL_PATTERN.matcher(cleaned);
        while (email.find() && contacts.size() < 4) {
            contacts.add(email.group());
        }

        Matcher phone = PHONE_PATTERN.matcher(cleaned);
        while (phone.find() && contacts.size() < 4) {
            String normalized = normalizePhone(phone.group());
            if (normalized != null) contacts.add(normalized);
        }

        if (!contacts.isEmpty()) return String.join(" / ", contacts);

        if (cleaned.matches(".*(개별\\s*연락|별도\\s*연락|추후\\s*연락).*")) {
            return "개별 연락";
        }

        return null;
    }

    private String normalizePhone(String value) {
        if (value == null) return null;

        String phone = value.replaceAll("[) .]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("-$", "");

        if (phone.startsWith("00-")) return null;
        if (phone.matches("^0{2,}.*")) return null;
        if (phone.equals("1522-3658")) return null;
        if (phone.equals("1811-8447")) return null;

        return phone;
    }

    public boolean isFallbackText(String value) {
        if (value == null) return true;

        String compact = value.replaceAll("\\s+", "");

        return compact.isBlank()
                || compact.contains("원문링크")
                || compact.contains("문의안내")
                || compact.contains("장소정보")
                || compact.contains("참여대상")
                || compact.equals("고객센터")
                || compact.equals("운영기관")
                || compact.contains("확인하세요");
    }

    public LocalDateTime validOrNull(LocalDateTime value) {
        if (value == null) return null;
        if (value.getYear() >= 2099) return null;
        return value;
    }

    public String cleanTitle(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) return null;

        String[] cutMarkers = {
                "전시 기간:", "전시기간", "전시 장소:", "전시장소", "전시 소개",
                "행사 기간:", "행사기간", "행사 장소:", "행사장소", "행사 소개",
                "교육 기간:", "교육기간", "교육 장소:", "교육장소",
                "운영 기간:", "운영기간", "운영 장소:", "운영장소",
                "신청 기간:", "신청기간", "모집 기간:", "모집기간",
                "봉사 기간:", "봉사기간", "활동 기간:", "활동기간",
                "공연기간", "공연장소", "기간:", "장소:", "소개:", "내용:"
        };

        for (String marker : cutMarkers) {
            int idx = cleaned.indexOf(marker);
            if (idx > 0) cleaned = cleaned.substring(0, idx).trim();
        }

        return cleaned.length() > 120 ? cleaned.substring(0, 120).trim() : cleaned;
    }

    public String cleanDescriptionBody(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null || isFallbackText(cleaned)) return null;

        cleaned = removeFieldNoise(cleaned);

        if (cleaned.length() > 900) {
            cleaned = cleaned.substring(0, 900).trim();
        }

        return cleaned.isBlank() ? null : cleaned;
    }

    public String cleanAddressStrict(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null || isFallbackText(cleaned)) return null;

        cleaned = removeAfterFieldMarkers(cleaned);

        String[] parts = cleaned.split("\\s*[●•|]\\s*|\\s{2,}");
        for (String part : parts) {
            String candidate = normalizeAddressCandidate(part);
            if (isValidAddressCandidate(candidate)) return trim(candidate, 140);
        }

        String candidate = normalizeAddressCandidate(cleaned);
        return isValidAddressCandidate(candidate) ? trim(candidate, 140) : null;
    }

    public String extractAddressByLabel(String value, String... labels) {
        String cleaned = cleanText(value);
        if (cleaned == null) return null;

        for (String label : labels) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(label) + "\\s*[:：]?\\s*([^●•\\n]+)"
            ).matcher(cleaned);

            if (matcher.find()) {
                String candidate = cleanAddressStrict(matcher.group(1));
                if (candidate != null) return candidate;
            }
        }

        return null;
    }

    public String extractTargetFromMixedText(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) return null;

        Matcher matcher = Pattern.compile("(참석자|신청대상|참여대상|모집대상|교육대상|이용대상|관람대상|봉사대상|관람연령)\\s*[:：]?\\s*([^●•\\n]+)").matcher(cleaned);

        if (matcher.find()) {
            String target = cleanText(matcher.group(2));
            if (target == null) return null;

            target = target.replaceAll("(활동구분|첨부파일|문의|연락처|담당자명|전화번호|FAX|주소).*", "").trim();

            if (target.matches(".*(문의|신청 기간|접수 기간|알게 된 경로|내 답변|고용센터|현수막|홈페이지|아파트게시판).*")) {
                return null;
            }

            return trim(target, 120);
        }

        return null;
    }

    public DateRange extractDateRangeFromMixedText(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) return new DateRange(null, null);

        Matcher matcher = DATE_RANGE_PATTERN.matcher(cleaned);
        if (!matcher.find()) return new DateRange(null, null);

        int startYear = Integer.parseInt(matcher.group(1));
        int startMonth = Integer.parseInt(matcher.group(2));
        int startDay = Integer.parseInt(matcher.group(3));

        int endYear = matcher.group(4) == null ? startYear : Integer.parseInt(matcher.group(4));
        int endMonth = Integer.parseInt(matcher.group(5));
        int endDay = Integer.parseInt(matcher.group(6));

        return new DateRange(
                LocalDateTime.of(startYear, startMonth, startDay, 0, 0),
                LocalDateTime.of(endYear, endMonth, endDay, 0, 0)
        );
    }

    public LocalDateTime extractSingleDateTime(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) return null;

        Matcher matcher = SINGLE_DATE_TIME_PATTERN.matcher(cleaned);
        if (!matcher.find()) return null;

        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        int hour = Integer.parseInt(matcher.group(4));
        int minute = matcher.group(5) == null ? 0 : Integer.parseInt(matcher.group(5));

        return LocalDateTime.of(year, month, day, hour, minute);
    }

    private String normalizeAddressCandidate(String value) {
        String candidate = cleanText(value);
        if (candidate == null) return null;

        return candidate
                .replaceAll("^(장소|주소|위치|교육장소|행사장소|활동장소|봉사장소|공연장소|전시장소|도로명주소)\\s*[:：-]\\s*", "")
                .trim();
    }

    private String removeAfterFieldMarkers(String value) {
        String result = value;

        String[] markers = {
                "봉사대상", "활동구분", "첨부파일", "교육대상", "신청대상", "참여대상",
                "모집대상", "관람대상", "관람연령", "신청 기간", "신청기간", "모집 기간", "모집기간",
                "접수 기간", "접수기간", "문의", "연락처", "이메일", "담당자명", "전화번호", "FAX",
                "내 답변", "구직상태", "알게 된 경로", "봉사내용", "상세정보"
        };

        for (String marker : markers) {
            int idx = result.indexOf(marker);
            if (idx > 0) result = result.substring(0, idx).trim();
        }

        return result;
    }

    private String removeFieldNoise(String value) {
        return value
                .replaceAll("첨부파일\\s*첨부파일이 없습니다\\.?"," ")
                .replaceAll("링크공유\\s*"," ")
                .replaceAll("활동구분\\s*(오프라인|온라인)"," ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isValidAddressCandidate(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.length() > 180) return false;
        if (containsSurveyNoise(value)) return false;
        if (isFallbackText(value)) return false;

        if (value.matches(".*(신청|모집|접수|문의|이메일|참석자|참여대상|신청대상|교육대상|모집대상|봉사대상|활동구분|첨부파일|고객센터|전화번호).*")) {
            return false;
        }

        return value.matches(".*(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주|전주|포항|수원|성남|고양|용인|순천|김해|마포구|강남구|동대문구|종로구|성북구|덕진구|북구|남구|중구|서구|동구).*")
                || value.matches(".*(센터|병원|도서관|학교|대학교|박물관|미술관|회관|공원|빌딩|홀|극장|체육관|보호소|복지관|아트센터|카페|소극장|전시장|캠퍼스).*");
    }

    private boolean containsSurveyNoise(String value) {
        return value.matches(".*(내 답변|구직상태|알게 된 경로|고용센터|현수막|홈페이지|아파트게시판|선택|문항|설문).*");
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max).trim() : value;
    }
}