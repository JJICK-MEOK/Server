package com.jjikmeok.app.domain.activity.privateactivity.collector;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.ai.dto.DiscoveryAnalysisDto;
import com.jjikmeok.app.domain.ai.service.DiscoveryAiAnalysisService;
import com.jjikmeok.app.domain.activity.privateactivity.deduplication.DiscoveryDeduplicationService;
import com.jjikmeok.app.domain.activity.privateactivity.dto.DiscoveryCandidateDto;
import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.ExtractionMode;
import com.jjikmeok.app.domain.activity.privateactivity.enums.RobotsPolicy;
import com.jjikmeok.app.domain.activity.privateactivity.extractor.DiscoveryMetadataExtractorService;
import com.jjikmeok.app.domain.activity.privateactivity.keyword.DiscoveryKeywordService;
import com.jjikmeok.app.domain.activity.privateactivity.search.SearchService;
import com.jjikmeok.app.domain.activity.privateactivity.service.DiscoveryUrlQualityService;
import com.jjikmeok.app.domain.activity.privateactivity.service.RobotsPolicyService;
import com.jjikmeok.app.domain.activity.privateactivity.sheets.GoogleSheetsService;
import com.jjikmeok.app.domain.activity.privateactivity.support.DiscoveryUrlNormalizer;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncUtils;
import com.jjikmeok.app.domain.activity.publicactivity.service.RawActivityArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscoveryCollectorService {

    private static final double MIN_CONFIDENCE_SCORE = 10.0;

    private final DiscoveryKeywordService keywordService;
    private final SearchService searchService;
    private final DiscoveryUrlQualityService urlQualityService;
    private final RobotsPolicyService robotsPolicyService;
    private final DiscoveryMetadataExtractorService metadataExtractorService;
    private final DiscoveryAiAnalysisService aiAnalysisService;
    private final DiscoveryDeduplicationService deduplicationService;
    private final GoogleSheetsService googleSheetsService;
    private final DiscoveryUrlNormalizer urlNormalizer;
    private final ActivitySyncUtils utils;
    private final RawActivityArchiveService rawActivityArchiveService;

    @Value("${app.discovery.analysis.max-ai-analysis-per-run:50}")
    private int maxAiAnalysisPerRun;

    public List<DiscoverySheetRowDto> runAll(int keywordLimit, int resultLimit) {
        return run(keywordService.categories(), keywordLimit, resultLimit);
    }

    public List<DiscoverySheetRowDto> run(ActivityCategory category, int keywordLimit, int resultLimit) {
        if (category == null) {
            return runAll(keywordLimit, resultLimit);
        }
        return run(List.of(category), keywordLimit, resultLimit);
    }

    public List<DiscoverySheetRowDto> run(List<ActivityCategory> categories, int keywordLimit, int resultLimit) {
        List<DiscoverySheetRowDto> rows = new ArrayList<>();
        Set<String> batchKeys = new HashSet<>();
        Set<String> sourceUrlKeys = existingSourceUrlKeys();
        AtomicInteger aiAnalysisCount = new AtomicInteger();

        for (ActivityCategory category : categories) {
            for (String keyword : keywordsFor(category, keywordLimit)) {
                rows.addAll(processKeyword(keyword, resultLimit, batchKeys, sourceUrlKeys, aiAnalysisCount));
            }
        }

        return rows;
    }

    private List<DiscoverySheetRowDto> processKeyword(
            String keyword,
            int resultLimit,
            Set<String> batchKeys,
            Set<String> sourceUrlKeys,
            AtomicInteger aiAnalysisCount
    ) {
        List<SearchResultDto> searchResults = searchResults(keyword, resultLimit);
        log.info("[Discovery] 검색 결과를 불러왔습니다. keyword={}, count={}", keyword, searchResults.size());

        List<DiscoverySheetRowDto> rows = new ArrayList<>();
        for (SearchResultDto searchResult : searchResults) {
            DiscoverySheetRowDto sheetRow = safeProcessSearchResult(
                    searchResult,
                    batchKeys,
                    sourceUrlKeys,
                    keyword,
                    aiAnalysisCount
            );
            if (sheetRow != null) {
                rows.add(sheetRow);
            }
        }
        return rows;
    }

    private DiscoverySheetRowDto safeProcessSearchResult(
            SearchResultDto searchResult,
            Set<String> batchKeys,
            Set<String> sourceUrlKeys,
            String keyword,
            AtomicInteger aiAnalysisCount
    ) {
        try {
            return processSearchResult(searchResult, batchKeys, sourceUrlKeys, keyword, aiAnalysisCount);
        } catch (Exception e) {
            log.warn(
                    "[Discovery] 후보 처리에 실패했습니다. keyword={}, url={}, reason={}",
                    keyword,
                    searchResult == null ? null : searchResult.url(),
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    private DiscoverySheetRowDto processSearchResult(
            SearchResultDto searchResult,
            Set<String> batchKeys,
            Set<String> sourceUrlKeys,
            String keyword,
            AtomicInteger aiAnalysisCount
    ) {
        if (searchResult == null) {
            return null;
        }

        String searchKey = key("search", searchResult.url(), searchResult.title(), null);
        if (!batchKeys.add(searchKey)) {
            return null;
        }

        var assessment = urlQualityService.evaluate(searchResult);
        SearchResultDto classifiedSearchResult = searchResult.withSourceChannel(assessment.sourceChannel());
        if (assessment.excluded()) {
            log.info("[Discovery] 품질 필터로 URL을 제외했습니다. platform={}, url={}", assessment.platform(), searchResult.url());
            return null;
        }

        ExtractionMode extractionMode = extractionMode(classifiedSearchResult, assessment.extractionMode());
        if (extractionMode == null) {
            return null;
        }

        DiscoveryCandidateDto candidate = extractCandidate(classifiedSearchResult, extractionMode, assessment.confidenceScore());
        if (candidate == null || candidate.confidenceScore() < MIN_CONFIDENCE_SCORE) {
            log.info("[Discovery] 후보 신뢰도가 낮아 제외했습니다. url={}", searchResult.url());
            return null;
        }

        String sourceUrlKey = normalizedUrlKey(candidate.sourceUrl());
        if (sourceUrlKey == null) {
            log.info("[Discovery] sourceUrl 이 없습니다. keyword={}, title={}", keyword, candidate.title());
            return null;
        }
        if (!sourceUrlKeys.add(sourceUrlKey)) {
            log.info("[Discovery] 이미 처리한 sourceUrl 입니다. url={}", candidate.sourceUrl());
            return null;
        }

        String candidateKey = key("candidate", candidate.sourceUrl(), candidate.title(), candidate.organizer());
        if (!batchKeys.add(candidateKey)) {
            return null;
        }

        rawActivityArchiveService.archiveDiscoveryCandidate(classifiedSearchResult, candidate);

        if (deduplicationService.findDuplicateReason(candidate.sourceUrl(), candidate.title(), candidate.organizer()).isPresent()) {
            log.info("[Discovery] 저장된 활동과 중복되어 제외했습니다. url={}", candidate.sourceUrl());
            return null;
        }

        DiscoverySheetRowDto sheetRow = appendCandidate(candidate);
        if (sheetRow == null) {
            return null;
        }

        if (aiAnalysisCount.get() >= maxAiAnalysisPerRun) {
            log.info("[Discovery] AI 분석 한도에 도달했습니다. url={}", candidate.sourceUrl());
            return sheetRow;
        }

        return analyzeCandidate(sheetRow, candidate, aiAnalysisCount);
    }

    private ExtractionMode extractionMode(SearchResultDto searchResult, ExtractionMode defaultMode) {
        RobotsPolicy robotsPolicy = robotsPolicyService.evaluate(uri(searchResult.url()));
        if (robotsPolicy == RobotsPolicy.DISALLOWED) {
            log.info("[Discovery] robots.txt 에서 차단된 URL 입니다. url={}", searchResult.url());
            return null;
        }
        if (robotsPolicy == RobotsPolicy.UNKNOWN && defaultMode == ExtractionMode.FULL_CONTENT) {
            return ExtractionMode.METADATA_ONLY;
        }
        return defaultMode;
    }

    private List<String> keywordsFor(ActivityCategory category, int keywordLimit) {
        try {
            return keywordService.keywordsFor(category, keywordLimit);
        } catch (Exception e) {
            log.warn("[Discovery] 키워드를 불러오지 못했습니다. category={}, reason={}", category, e.getMessage(), e);
            return List.of();
        }
    }

    private List<SearchResultDto> searchResults(String keyword, int resultLimit) {
        try {
            return searchService.search(keyword, resultLimit);
        } catch (Exception e) {
            log.warn("[Discovery] 검색 요청에 실패했습니다. keyword={}, reason={}", keyword, e.getMessage(), e);
            return List.of();
        }
    }

    private DiscoveryCandidateDto extractCandidate(
            SearchResultDto searchResult,
            ExtractionMode extractionMode,
            double confidenceScore
    ) {
        try {
            return metadataExtractorService.extract(searchResult, extractionMode, confidenceScore);
        } catch (Exception e) {
            log.warn("[Discovery] 메타데이터 추출에 실패했습니다. url={}, reason={}", searchResult.url(), e.getMessage(), e);
            return null;
        }
    }

    private DiscoverySheetRowDto appendCandidate(DiscoveryCandidateDto candidate) {
        try {
            return googleSheetsService.append(candidate);
        } catch (Exception e) {
            log.warn("[Discovery] 후보를 시트에 추가하지 못했습니다. url={}, reason={}", candidate.sourceUrl(), e.getMessage(), e);
            return null;
        }
    }

    private DiscoverySheetRowDto analyzeCandidate(
            DiscoverySheetRowDto sheetRow,
            DiscoveryCandidateDto candidate,
            AtomicInteger aiAnalysisCount
    ) {
        try {
            aiAnalysisCount.incrementAndGet();
            DiscoveryAnalysisDto analysis = aiAnalysisService.analyze(candidate);
            if (analysis == null) {
                return sheetRow;
            }

            DiscoverySheetRowDto analyzedRow = sheetRow.withAnalysis(analysis);
            googleSheetsService.updateRow(analyzedRow);
            return analyzedRow;
        } catch (Exception e) {
            log.warn("[Discovery] AI 분석에 실패했습니다. url={}, reason={}", candidate.sourceUrl(), e.getMessage(), e);
            return sheetRow;
        }
    }

    private Set<String> existingSourceUrlKeys() {
        Set<String> keys = new HashSet<>();
        try {
            for (DiscoverySheetRowDto row : googleSheetsService.snapshot()) {
                String key = normalizedUrlKey(row == null ? null : row.sourceUrl());
                if (key != null) {
                    keys.add(key);
                }
            }
        } catch (Exception e) {
            log.warn("[Discovery] 기존 시트 URL을 불러오지 못했습니다. reason={}", e.getMessage(), e);
        }
        return keys;
    }

    private String normalizedUrlKey(String sourceUrl) {
        String normalizedUrl = urlNormalizer.normalize(sourceUrl);
        return utils.isBlank(normalizedUrl) ? null : normalizedUrl;
    }

    private String key(String prefix, String sourceUrl, String title, String organizer) {
        String normalizedUrl = urlNormalizer.normalize(sourceUrl);
        String normalizedTitle = utils.cleanText(title);
        String normalizedOrganizer = utils.cleanText(organizer);
        return prefix + ":"
                + (utils.isBlank(normalizedUrl) ? "-" : normalizedUrl) + "|"
                + (utils.isBlank(normalizedTitle) ? "-" : normalizedTitle) + "|"
                + (utils.isBlank(normalizedOrganizer) ? "-" : normalizedOrganizer);
    }

    private URI uri(String value) {
        try {
            return value == null || value.isBlank() ? null : URI.create(value);
        } catch (Exception e) {
            return null;
        }
    }
}
