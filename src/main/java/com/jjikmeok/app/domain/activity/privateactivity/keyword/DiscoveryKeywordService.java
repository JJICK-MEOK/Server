package com.jjikmeok.app.domain.activity.privateactivity.keyword;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DiscoveryKeywordService {

    private static final Pattern SITE_QUERY_PATTERN = Pattern.compile("(?i)(?:^|\\s)site:([^\\s]+)");

    private static final List<String> ACTIVITY_TYPES = List.of(
            "프로그램",
            "원데이클래스",
            "동아리",
            "행사 강연"
    );

    private static final List<String> INTENT_TERMS = List.of(
            "모집",
            "신청"
    );

    private static final Map<ActivityCategory, String> CATEGORY_TERMS = buildCategoryTerms();
    private static final List<DiscoveryBrandSource> BRAND_SOURCES = buildBrandSources();
    private static final Map<ActivityCategory, List<String>> DEFAULT_KEYWORDS = buildDefaults();

    public List<ActivityCategory> categories() {
        List<ActivityCategory> categories = new ArrayList<>();
        Collections.addAll(categories, ActivityCategory.values());
        return categories;
    }

    public List<String> keywordsFor(ActivityCategory category, int limit) {
        List<String> keywords = DEFAULT_KEYWORDS.getOrDefault(category, List.of());
        if (limit <= 0 || limit >= keywords.size()) {
            return keywords;
        }
        return keywords.subList(0, limit);
    }

    public Map<ActivityCategory, List<String>> snapshot() {
        return Collections.unmodifiableMap(DEFAULT_KEYWORDS);
    }

    public List<DiscoveryBrandSource> brandSources() {
        return BRAND_SOURCES;
    }

    public boolean isAllowedSourceUrlForKeyword(String keyword, String url) {
        String expectedDomain = siteDomain(keyword);
        if (expectedDomain != null) {
            return matchesHost(host(url), expectedDomain);
        }
        return isEnabledBrandSourceUrl(url);
    }

    public boolean isEnabledBrandSourceUrl(String url) {
        String host = host(url);
        if (host.isBlank()) {
            return false;
        }
        return BRAND_SOURCES.stream()
                .filter(DiscoveryBrandSource::enabled)
                .anyMatch(source -> matchesHost(host, source.domain()));
    }

    private static Map<ActivityCategory, String> buildCategoryTerms() {
        Map<ActivityCategory, String> terms = new EnumMap<>(ActivityCategory.class);
        terms.put(ActivityCategory.SPORTS, "운동/액티비티");
        terms.put(ActivityCategory.CULTURE, "문화/예술");
        terms.put(ActivityCategory.CRAFT, "공예/만들기");
        terms.put(ActivityCategory.COOKING, "요리/베이킹");
        terms.put(ActivityCategory.PHOTO_VIDEO, "사진/영상");
        terms.put(ActivityCategory.HUMANITIES, "책/글");
        terms.put(ActivityCategory.TRAVEL, "여행/탐방");
        terms.put(ActivityCategory.LANGUAGE, "언어/해외");
        terms.put(ActivityCategory.VOLUNTEER, "봉사활동");
        terms.put(ActivityCategory.CAREER, "성장/커리어");
        return terms;
    }

    private static Map<ActivityCategory, List<String>> buildDefaults() {
        Map<ActivityCategory, List<String>> keywords = new EnumMap<>(ActivityCategory.class);
        for (ActivityCategory category : ActivityCategory.values()) {
            String categoryTerm = categoryTerm(category);
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (DiscoveryBrandSource source : BRAND_SOURCES) {
                if (!source.enabled() || source.category() != category) {
                    continue;
                }
                for (String type : ACTIVITY_TYPES) {
                    values.add(siteQuery(source.domain(), categoryTerm, type));
                }
            }
            keywords.put(category, List.copyOf(values));
        }
        return keywords;
    }

    private static String siteQuery(String domain, String categoryTerm, String activityType) {
        return "site:" + domain + " " + categoryTerm + " " + activityType + " " + String.join(" ", INTENT_TERMS);
    }

    private static List<DiscoveryBrandSource> buildBrandSources() {
        List<DiscoveryBrandSource> sources = new ArrayList<>();

        // Enabled sources are limited to domains that returned same-domain activity URLs
        // and crawlable HTML in the 2026-07-04 validation pass.
        source(sources, "블랙야크 알파인 클럽", "bac.blackyak.com", ActivityCategory.SPORTS, true, "검증 URL: /bac/clubday2025/");
        source(sources, "더클라임", "theclimb.co.kr", ActivityCategory.SPORTS, true, "검증 URL: /first-trial/");

        source(sources, "소잉팩토리", "sewingfactory.co.kr", ActivityCategory.CRAFT, true, "검증 URL: /class/");

        source(sources, "르 꼬르동 블루 코리아", "cordonbleu.edu", ActivityCategory.COOKING, true, "검증 URL: /programmes/ko");
        source(sources, "전통주갤러리", "thesool.com", ActivityCategory.COOKING, true, "검증 URL: /front/contents/M000000179/view.do");
        source(sources, "브레드가든", "breadgarden.co.kr", ActivityCategory.COOKING, true, "검증 URL: /about/about_050100.html");

        source(sources, "민음사 북클럽", "minumsa.com", ActivityCategory.HUMANITIES, true, "검증 URL: /bookclub/bookclub-register/");
        source(sources, "예스24 클래스", "yes24.com", ActivityCategory.HUMANITIES, true, "검증 URL: /product/class");
        source(sources, "교보문고 이벤트", "kyobobook.co.kr", ActivityCategory.HUMANITIES, true, "검증 URL: /make/240762");

        source(sources, "아트맵", "art-map.co.kr", ActivityCategory.CULTURE, true, "검증 URL: /notice/view.php");
        source(sources, "예스24 티켓", "ticket.yes24.com", ActivityCategory.CULTURE, true, "검증 URL: /perf/52132");
        source(sources, "그라운드시소", "groundseesaw.co.kr", ActivityCategory.CULTURE, true, "검증 URL: /product/");
        source(sources, "피크닉", "piknic.kr", ActivityCategory.CULTURE, true, "검증 URL: /home/include/board_view.php");
        source(sources, "플랫폼엘", "platform-l.org", ActivityCategory.CULTURE, true, "검증 URL: /learning/detail");
        source(sources, "사비나미술관", "savinamuseum.com", ActivityCategory.CULTURE, true, "검증 URL: /kor/edulist.action");

        source(sources, "후지필름 코리아", "fujifilm-korea.co.kr", ActivityCategory.PHOTO_VIDEO, true, "검증 URL: /support/experience");
        source(sources, "필름로그", "filmlog.co.kr", ActivityCategory.PHOTO_VIDEO, true, "검증 URL: /workshop/workshop.html");
        source(sources, "패스트캠퍼스", "fastcampus.co.kr", ActivityCategory.PHOTO_VIDEO, true, "검증 URL: /openseminar_new");

        source(sources, "원티드 이벤트", "event.wanted.co.kr", ActivityCategory.CAREER, true, "검증 URL: /swmaestro17_busan");
        source(sources, "인프런", "inflearn.com", ActivityCategory.CAREER, true, "검증 URL: /challenge/");
        source(sources, "패스트캠퍼스 커리어", "fastcampus.co.kr", ActivityCategory.CAREER, true, "검증 URL: /openseminar_new");
        source(sources, "스파르타코딩클럽", "spartacodingclub.kr", ActivityCategory.CAREER, true, "검증 URL: /catalog/featured/summary");
        source(sources, "프로그래머스", "programmers.co.kr", ActivityCategory.CAREER, true, "검증 URL: /pages/");
        source(sources, "러닝스푼즈", "learningspoons.com", ActivityCategory.CAREER, true, "검증 URL: /creator/");
        source(sources, "디캠프", "dcamp.kr", ActivityCategory.CAREER, true, "검증 URL: /program/batch");
        source(sources, "GDG", "gdg.community.dev", ActivityCategory.CAREER, true, "검증 URL: /events/details/");
        source(sources, "마이크로소프트 리액터", "developer.microsoft.com", ActivityCategory.CAREER, true, "검증 URL: /reactor/events/");

        source(sources, "마실와이드", "masilwide.com", ActivityCategory.TRAVEL, true, "검증 URL: /20250710-3/");
        source(sources, "어반플레이", "urbanplay.co.kr", ActivityCategory.TRAVEL, true, "검증 URL: /contents_artisan_school");
        source(sources, "로컬스티치", "localstitch.kr", ActivityCategory.TRAVEL, true, "검증 URL: /tour");

        source(sources, "한국해비타트", "habitat.or.kr", ActivityCategory.VOLUNTEER, true, "검증 URL: /welfare/schedule");
        source(sources, "카라", "ekara.org", ActivityCategory.VOLUNTEER, true, "검증 URL: /parttake/serve");
        source(sources, "동물자유연대", "animals.or.kr", ActivityCategory.VOLUNTEER, true, "검증 URL: /sponsor/volunteer");
        source(sources, "루트임팩트", "rootimpact.org", ActivityCategory.VOLUNTEER, true, "검증 URL: /news/notice/90/");

        return List.copyOf(sources);
    }

    private static void source(
            List<DiscoveryBrandSource> sources,
            String name,
            String domain,
            ActivityCategory category,
            boolean enabled,
            String note
    ) {
        sources.add(new DiscoveryBrandSource(name, normalizeDomain(domain), category, enabled, note));
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) {
            return "";
        }
        String normalized = domain.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
            try {
                normalized = URI.create(normalized).getHost();
            } catch (Exception ignored) {
            }
        }
        if (normalized != null && normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }
        return normalized == null ? "" : normalized;
    }

    private static String categoryTerm(ActivityCategory category) {
        return CATEGORY_TERMS.getOrDefault(category, category == null ? "활동" : category.getLabel());
    }

    private static String siteDomain(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        Matcher matcher = SITE_QUERY_PATTERN.matcher(keyword);
        if (!matcher.find()) {
            return null;
        }
        return normalizeDomain(matcher.group(1));
    }

    private static String host(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            String host = URI.create(url).getHost();
            return normalizeDomain(host);
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean matchesHost(String host, String domain) {
        if (host == null || host.isBlank() || domain == null || domain.isBlank()) {
            return false;
        }
        String normalizedHost = normalizeDomain(host);
        String normalizedDomain = normalizeDomain(domain);
        return normalizedHost.equals(normalizedDomain) || normalizedHost.endsWith("." + normalizedDomain);
    }
}
