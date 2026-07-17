package com.jjikmeok.app.domain.activity.privateactivity.sheets;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.GoogleCredentials;
import com.jjikmeok.app.domain.activity.privateactivity.dto.DiscoveryCandidateDto;
import com.jjikmeok.app.domain.ai.dto.DiscoveryAnalysisDto;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySheetStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.publicactivity.dto.NormalizedActivity;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivityRegionResolver;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleSheetsService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/spreadsheets");
    private static final String HEADER_RANGE = "A1:AC1";
    private static final String DATA_RANGE = "A2:AC";
    private static final int FIRST_DATA_SHEET_ROW = 2;

    private final ActivityRegionResolver activityRegionResolver;
    private final RestClient restClient = RestClient.create();
    private final List<DiscoverySheetRowDto> memoryRows = new CopyOnWriteArrayList<>();
    private final AtomicInteger memorySequence = new AtomicInteger();

    @Value("${app.discovery.sheets.enabled:false}")
    private boolean enabled;

    @Value("${app.discovery.sheets.spreadsheet-id:}")
    private String spreadsheetId;

    @Value("${app.discovery.sheets.sheet-name:찍먹활동DB}")
    private String sheetName;

    @Value("${app.discovery.sheets.credentials-path:}")
    private String credentialsPath;

    @Value("${app.discovery.sheets.credentials-json:}")
    private String credentialsJson;

    @Value("${app.activity-sync.default-region-id:1}")
    private Long defaultRegionId;

    private volatile GoogleCredentials googleCredentials;

    @PostConstruct
    void init() {
        if (enabled && hasApiConfiguration()) {
            googleCredentials = loadCredentials();
            if (googleCredentials != null) {
                ensureHeaders();
            }
            if (googleCredentials == null) {
                log.warn("[시트] Google Sheets 자격 증명 로드에 실패했습니다. 메모리 모드로 동작합니다.");
            }
        }
    }

    public DiscoverySheetRowDto append(DiscoveryAnalysisDto analysis) {
        if (useSheet()) {
            return appendToSheet(analysis);
        }
        return appendToMemory(analysis);
    }

    public DiscoverySheetRowDto append(DiscoveryCandidateDto candidate) {
        if (useSheet()) {
            return appendCandidateToSheet(candidate);
        }
        return appendCandidateToMemory(candidate);
    }

    public DiscoverySheetRowDto upsertPublicActivity(NormalizedActivity activity) {
        return upsertPublicActivity(null, activity);
    }

    public DiscoverySheetRowDto upsertPublicActivity(SourceType sourceType, NormalizedActivity activity) {
        DiscoverySheetRowDto existing = findPublicRow(activity);
        if (existing != null) {
            DiscoverySheetRowDto updated = copyPublicRow(existing, activity, sourceType);
            updateRow(updated);
            return updated;
        }

        if (useSheet()) {
            return appendPublicActivityToSheet(sourceType, activity);
        }
        return appendPublicActivityToMemory(sourceType, activity);
    }

    public boolean hasPublicActivity(NormalizedActivity activity) {
        return findPublicRow(activity) != null;
    }

    public List<DiscoverySheetRowDto> snapshot() {
        if (useSheet()) {
            return readRowsFromSheet(DATA_RANGE);
        }
        return List.copyOf(memoryRows);
    }

    public List<DiscoverySheetRowDto> findReadyRows() {
        if (useSheet()) {
            return readReadyRowsFromSheet();
        }
        return memoryRows.stream()
                .filter(row -> row.status() == DiscoverySheetStatus.READY)
                .toList();
    }

    public void updateRow(DiscoverySheetRowDto row) {
        updateRows(row == null ? List.of() : List.of(row));
    }

    public void updateRows(List<DiscoverySheetRowDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        Map<Integer, DiscoverySheetRowDto> uniqueRows = new LinkedHashMap<>();
        for (DiscoverySheetRowDto row : rows) {
            if (row != null) {
                uniqueRows.put(row.rowNumber(), row);
            }
        }
        if (uniqueRows.isEmpty()) {
            return;
        }

        List<DiscoverySheetRowDto> normalizedRows = List.copyOf(uniqueRows.values());
        if (useSheet()) {
            writeRowsToSheet(normalizedRows);
            return;
        }
        normalizedRows.forEach(this::replaceMemoryRow);
    }

    public void clear() {
        if (useSheet()) {
            clearSheetRows();
            return;
        }
        memoryRows.clear();
        memorySequence.set(0);
    }

    private DiscoverySheetRowDto appendToMemory(DiscoveryAnalysisDto analysis) {
        int rowNumber = memorySequence.incrementAndGet();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.from(analysis, rowNumber, LocalDate.now(SEOUL), resolveRegionName(analysis.title(), analysis.address()));
        memoryRows.add(row);
        log.info("[시트] 메모리에 활동을 추가했습니다. row={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private DiscoverySheetRowDto appendCandidateToMemory(DiscoveryCandidateDto candidate) {
        int rowNumber = memorySequence.incrementAndGet();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromCandidate(candidate, rowNumber, LocalDate.now(SEOUL), resolveRegionName(candidate.title(), candidate.address()));
        memoryRows.add(row);
        log.info("[시트] 메모리에 후보를 추가했습니다. row={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private synchronized DiscoverySheetRowDto appendToSheet(DiscoveryAnalysisDto analysis) {
        SheetAppendPosition position = nextSheetAppendPosition();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.from(
                analysis,
                position.itemNumber(),
                LocalDate.now(SEOUL),
                resolveRegionName(analysis.title(), analysis.address())
        );
        putRowValues(rowRange(position.sheetRowNumber()), row.toSheetRow());
        replaceMemoryRow(row);
        log.info("[시트] 활동을 시트에 저장했습니다. row={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private synchronized DiscoverySheetRowDto appendCandidateToSheet(DiscoveryCandidateDto candidate) {
        SheetAppendPosition position = nextSheetAppendPosition();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromCandidate(
                candidate,
                position.itemNumber(),
                LocalDate.now(SEOUL),
                resolveRegionName(candidate.title(), candidate.address())
        );
        putRowValues(rowRange(position.sheetRowNumber()), row.toSheetRow());
        replaceMemoryRow(row);
        log.info("[시트] 후보를 시트에 저장했습니다. row={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private DiscoverySheetRowDto appendPublicActivityToMemory(NormalizedActivity activity) {
        int rowNumber = memorySequence.incrementAndGet();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromPublicActivity(activity, rowNumber, LocalDate.now(SEOUL), resolveRegionName(activity.title(), activity.address()));
        memoryRows.add(row);
        log.info("[공개] 활동을 메모리에 캐시했습니다. row={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private synchronized DiscoverySheetRowDto appendPublicActivityToSheet(NormalizedActivity activity) {
        SheetAppendPosition position = nextSheetAppendPosition();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromPublicActivity(
                activity,
                position.itemNumber(),
                LocalDate.now(SEOUL),
                resolveRegionName(activity.title(), activity.address())
        );
        putRowValues(rowRange(position.sheetRowNumber()), row.toSheetRow());
        replaceMemoryRow(row);
        log.info("[공개] 활동을 시트에 추가했습니다. row={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private List<DiscoverySheetRowDto> readRowsFromSheet(String range) {
        JsonNode root = getSheetValues(range);
        JsonNode values = root == null ? null : root.path("values");
        if (values == null || !values.isArray()) {
            return List.of();
        }

        List<DiscoverySheetRowDto> rows = new ArrayList<>();
        int sheetRowNumber = FIRST_DATA_SHEET_ROW;
        for (JsonNode valueRow : values) {
            if (!isEmptyRow(valueRow)) {
                rows.add(DiscoverySheetRowDto.fromSheetRow(sheetRowNumber, asValueList(valueRow)));
            }
            sheetRowNumber++;
        }
        return rows;
    }

    private List<DiscoverySheetRowDto> readReadyRowsFromSheet() {
        return readRowsFromSheet(DATA_RANGE).stream()
                .filter(row -> row != null && row.status() == DiscoverySheetStatus.READY)
                .toList();
    }

    private void writeRowsToSheet(List<DiscoverySheetRowDto> rows) {
        Map<Integer, Integer> sheetRowNumbers = findSheetRowNumbers(rows);
        List<Map<String, Object>> data = new ArrayList<>(rows.size());
        for (DiscoverySheetRowDto row : rows) {
            Map<String, Object> valueRange = new LinkedHashMap<>();
            valueRange.put("range", sheetRange(rowRange(sheetRowNumbers.get(row.rowNumber()))));
            valueRange.put("majorDimension", "ROWS");
            valueRange.put("values", List.of(row.toSheetRow()));
            data.add(valueRange);
        }
        putBatchRowValues(data);
    }

    private void clearSheetRows() {
        try {
            restClient.post()
                    .uri(builder -> builder
                            .scheme("https")
                            .host("sheets.googleapis.com")
                            .path("/v4/spreadsheets/{spreadsheetId}/values/{range}:clear")
                            .build(spreadsheetId, sheetRange(DATA_RANGE)))
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new IllegalStateException("Google Sheets clear failed: " + e.getMessage(), e);
        }
    }

    private JsonNode getSheetValues(String range) {
        try {
            return restClient.get()
                    .uri(builder -> builder
                            .scheme("https")
                            .host("sheets.googleapis.com")
                            .path("/v4/spreadsheets/{spreadsheetId}/values/{range}")
                            .build(spreadsheetId, sheetRange(range)))
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("Google Sheets read failed. range=" + range + ": " + e.getMessage(), e);
        }
    }

    private void ensureHeaders() {
        try {
            JsonNode root = getSheetValues(HEADER_RANGE);
            JsonNode values = root == null ? null : root.path("values");
            if (values != null && values.isArray() && !values.isEmpty()) {
                List<Object> current = asValueList(values.get(0));
                if (current.size() >= DiscoverySheetRowDto.sheetHeaders().length) {
                    return;
                }
            }
            putRowValues(HEADER_RANGE, new ArrayList<>(Arrays.asList(DiscoverySheetRowDto.sheetHeaders())));
        } catch (Exception e) {
            log.warn("[시트] 헤더 검증에 실패했습니다. reason={}", e.getMessage());
        }
    }

    private void putRowValues(String range, List<Object> values) {
        try {
            restClient.put()
                    .uri(builder -> builder
                            .scheme("https")
                            .host("sheets.googleapis.com")
                            .path("/v4/spreadsheets/{spreadsheetId}/values/{range}")
                            .queryParam("valueInputOption", "RAW")
                            .build(spreadsheetId, sheetRange(range)))
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .body(java.util.Map.of("values", java.util.List.of(values)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new IllegalStateException("Google Sheets update failed: " + e.getMessage(), e);
        }
    }

    private SheetAppendPosition nextSheetAppendPosition() {
        JsonNode root = getSheetValues(DATA_RANGE);
        JsonNode values = root == null ? null : root.path("values");
        if (values == null || !values.isArray() || values.isEmpty()) {
            return new SheetAppendPosition(FIRST_DATA_SHEET_ROW, 1);
        }

        List<List<Object>> rows = new ArrayList<>();
        values.forEach(valueRow -> rows.add(asValueList(valueRow)));
        return calculateNextAppendPosition(rows);
    }

    static SheetAppendPosition calculateNextAppendPosition(List<List<Object>> rows) {
        int lastDataSheetRow = FIRST_DATA_SHEET_ROW - 1;
        int lastItemNumber = 0;
        int nonEmptyRowCount = 0;

        if (rows != null) {
            for (int index = 0; index < rows.size(); index++) {
                List<Object> row = rows.get(index);
                if (isEmptyValues(row)) {
                    continue;
                }

                nonEmptyRowCount++;
                lastDataSheetRow = FIRST_DATA_SHEET_ROW + index;
                Integer itemNumber = parsePositiveInteger(value(row, 0));
                if (itemNumber != null) {
                    lastItemNumber = itemNumber;
                }
            }
        }

        int nextItemNumber = lastItemNumber > 0 ? lastItemNumber + 1 : nonEmptyRowCount + 1;
        return new SheetAppendPosition(lastDataSheetRow + 1, nextItemNumber);
    }

    private Map<Integer, Integer> findSheetRowNumbers(List<DiscoverySheetRowDto> rows) {
        Set<Integer> targetItemNumbers = new LinkedHashSet<>();
        for (DiscoverySheetRowDto row : rows) {
            targetItemNumbers.add(row.rowNumber());
        }

        Map<Integer, Integer> sheetRowNumbers = new LinkedHashMap<>();
        JsonNode root = getSheetValues("A2:A");
        JsonNode values = root == null ? null : root.path("values");
        if (values != null && values.isArray()) {
            for (int index = 0; index < values.size(); index++) {
                Integer currentItemNumber = parsePositiveInteger(text(values.get(index), 0));
                if (currentItemNumber != null && targetItemNumbers.contains(currentItemNumber)) {
                    sheetRowNumbers.put(currentItemNumber, FIRST_DATA_SHEET_ROW + index);
                }
            }
        }

        if (sheetRowNumbers.size() != targetItemNumbers.size()) {
            Set<Integer> missingItemNumbers = new LinkedHashSet<>(targetItemNumbers);
            missingItemNumbers.removeAll(sheetRowNumbers.keySet());
            throw new IllegalStateException("Google Sheets rows not found. numbers=" + missingItemNumbers);
        }
        return sheetRowNumbers;
    }

    private void replaceMemoryRow(DiscoverySheetRowDto row) {
        for (int i = 0; i < memoryRows.size(); i++) {
            if (memoryRows.get(i).rowNumber() == row.rowNumber()) {
                memoryRows.set(i, row);
                return;
            }
        }
        memoryRows.add(row);
    }

    private DiscoverySheetRowDto findPublicRow(NormalizedActivity activity) {
        String sourceUrl = blankToNull(activity.sourceUrl());
        String organizer = blankToNull(activity.organizer());

        for (DiscoverySheetRowDto row : snapshot()) {
            if (row == null) {
                continue;
            }
            if (sourceUrl != null && sourceUrl.equals(blankToNull(row.sourceUrl()))) {
                return row;
            }
            if (sourceUrl == null
                    && equalsNullable(organizer, blankToNull(row.sourceName()))
                    && equalsNullable(blankToNull(activity.title()), blankToNull(row.title()))
                    && equalsNullable(activity.startAt() == null ? null : activity.startAt().toLocalDate(), row.startAt())) {
                return row;
            }
        }
        return null;
    }

    private DiscoverySheetRowDto appendPublicActivityToMemory(SourceType sourceType, NormalizedActivity activity) {
        int rowNumber = memorySequence.incrementAndGet();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromPublicActivity(
                activity,
                rowNumber,
                LocalDate.now(SEOUL),
                resolveRegionName(activity.title(), activity.address()),
                sourceType
        );
        memoryRows.add(row);
        log.info("[공공] 공공 활동을 메모리에 적재했습니다. row={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private synchronized DiscoverySheetRowDto appendPublicActivityToSheet(SourceType sourceType, NormalizedActivity activity) {
        SheetAppendPosition position = nextSheetAppendPosition();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromPublicActivity(
                activity,
                position.itemNumber(),
                LocalDate.now(SEOUL),
                resolveRegionName(activity.title(), activity.address()),
                sourceType
        );
        putRowValues(rowRange(position.sheetRowNumber()), row.toSheetRow());
        replaceMemoryRow(row);
        log.info("[공공] 공공 활동을 시트에 저장했습니다. row={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private DiscoverySheetRowDto copyPublicRow(DiscoverySheetRowDto existing, NormalizedActivity activity, SourceType sourceType) {
        String keyword = sourceType == null ? existing.keyword() : sourceType.name();

        return new DiscoverySheetRowDto(
                existing.rowNumber(),
                existing.reviewer(),
                existing.status(),
                existing.createdAt(),
                existing.publishedAt(),
                keyword,
                visibleOrganizer(activity.organizer(), existing.sourceName(), activity.title()),
                activity.title(),
                activity.sourceUrl(),
                activity.thumbnailUrl(),
                activity.activityType(),
                activity.category(),
                activity.startAt() == null ? null : activity.startAt().toLocalDate(),
                activity.endAt() == null ? null : activity.endAt().toLocalDate(),
                activity.recruitStartAt() == null ? null : activity.recruitStartAt().toLocalDate(),
                activity.recruitEndAt() == null ? null : activity.recruitEndAt().toLocalDate(),
                activity.target(),
                activity.price(),
                activity.description(),
                activity.contactInfo(),
                activity.organizer(),
                firstText(existing.regionName(), resolveRegionName(activity.title(), activity.address())),
                activity.address(),
                existing.moodTag1(),
                existing.moodTag2(),
                existing.intensity(),
                existing.purpose(),
                existing.duration(),
                existing.groupSize(),
                existing.confidenceScore(),
                existing.searchSnippet()
        );
    }

    private DiscoverySheetRowDto copyPublicRow(DiscoverySheetRowDto existing, NormalizedActivity activity) {
        return new DiscoverySheetRowDto(
                existing.rowNumber(),
                existing.reviewer(),
                existing.status(),
                existing.createdAt(),
                existing.publishedAt(),
                null,
                visibleOrganizer(activity.organizer(), existing.sourceName(), activity.title()),
                activity.title(),
                activity.sourceUrl(),
                activity.thumbnailUrl(),
                activity.activityType(),
                activity.category(),
                activity.startAt() == null ? null : activity.startAt().toLocalDate(),
                activity.endAt() == null ? null : activity.endAt().toLocalDate(),
                activity.recruitStartAt() == null ? null : activity.recruitStartAt().toLocalDate(),
                activity.recruitEndAt() == null ? null : activity.recruitEndAt().toLocalDate(),
                activity.target(),
                activity.price(),
                activity.description(),
                activity.contactInfo(),
                activity.organizer(),
                firstText(existing.regionName(), resolveRegionName(activity.title(), activity.address())),
                activity.address(),
                existing.moodTag1(),
                existing.moodTag2(),
                existing.intensity(),
                existing.purpose(),
                existing.duration(),
                existing.groupSize(),
                existing.confidenceScore(),
                existing.searchSnippet()
        );
    }

    private boolean hasApiConfiguration() {
        return spreadsheetId != null && !spreadsheetId.isBlank();
    }

    private boolean isApiReady() {
        return hasApiConfiguration() && googleCredentials != null;
    }

    private boolean useSheet() {
        if (!enabled) {
            return false;
        }
        if (!isApiReady()) {
            throw new IllegalStateException("Google Sheets is enabled but spreadsheet-id or credentials are not configured correctly.");
        }
        return true;
    }

    private GoogleCredentials loadCredentials() {
        try {
            if (credentialsJson != null && !credentialsJson.isBlank()) {
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(credentialsJson.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    return GoogleCredentials.fromStream(inputStream).createScoped(SCOPES);
                }
            }

            if (credentialsPath != null && !credentialsPath.isBlank()) {
                Path path = Path.of(credentialsPath);
                if (Files.exists(path)) {
                    try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
                        return GoogleCredentials.fromStream(inputStream).createScoped(SCOPES);
                    }
                }
            }

            return GoogleCredentials.getApplicationDefault().createScoped(SCOPES);
        } catch (Exception e) {
            log.warn("[시트] Google Sheets 자격 증명 로드에 실패했습니다. reason={}", e.getMessage());
            return null;
        }
    }

    private String accessToken() {
        if (googleCredentials == null) {
            throw new IllegalStateException("Google Sheets 자격 증명이 없습니다.");
        }
        try {
            googleCredentials.refreshIfExpired();
            if (googleCredentials.getAccessToken() == null) {
                throw new IllegalStateException("Google Sheets 액세스 토큰을 가져오지 못했습니다.");
            }
            return googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            throw new IllegalStateException("Google Sheets 토큰 갱신에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private String rowRange(int rowNumber) {
        return "A" + rowNumber + ":AC" + rowNumber;
    }

    private String sheetRange(String range) {
        return "'" + sheetName + "'!" + range;
    }

    private List<Object> asValueList(JsonNode rowNode) {
        List<Object> values = new ArrayList<>();
        if (rowNode == null || !rowNode.isArray()) {
            return values;
        }

        rowNode.forEach(cell -> values.add(cell == null || cell.isNull() ? null : cell.asText()));
        return values;
    }

    private String text(JsonNode rowNode, int index) {
        if (rowNode == null || !rowNode.isArray() || index < 0 || index >= rowNode.size()) {
            return null;
        }
        JsonNode cell = rowNode.get(index);
        return cell == null || cell.isNull() ? null : cell.asText();
    }

    private boolean isEmptyRow(JsonNode rowNode) {
        if (rowNode == null || !rowNode.isArray() || rowNode.isEmpty()) {
            return true;
        }

        for (JsonNode cell : rowNode) {
            if (cell != null && !cell.isNull()) {
                String value = cell.asText();
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isEmptyValues(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        return values.stream().allMatch(value -> value == null || value.toString().isBlank());
    }

    private static Object value(List<Object> values, int index) {
        return values == null || index < 0 || index >= values.size() ? null : values.get(index);
    }

    private static Integer parsePositiveInteger(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.toString().trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void putBatchRowValues(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            restClient.post()
                    .uri(builder -> builder
                            .scheme("https")
                            .host("sheets.googleapis.com")
                            .path("/v4/spreadsheets/{spreadsheetId}/values:batchUpdate")
                            .build(spreadsheetId))
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .body(Map.of(
                            "valueInputOption", "RAW",
                            "data", data
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[시트] 행 상태를 일괄 업데이트했습니다. count={}", data.size());
        } catch (Exception e) {
            throw new IllegalStateException("Google Sheets batch update failed: " + e.getMessage(), e);
        }
    }

    record SheetAppendPosition(int sheetRowNumber, int itemNumber) {
    }

    private boolean equalsNullable(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean isPublicSourceName(String sourceName) {
        return "KOPIS".equals(sourceName)
                || "EXHIBITION".equals(sourceName)
                || "SEOUL_CULTURE".equals(sourceName)
                || "SEOUL_RESERVATION".equals(sourceName)
                || "WEBSITE".equals(sourceName)
                || "BAND".equals(sourceName)
                || "NAVER_CAFE".equals(sourceName)
                || "NAVER_BLOG".equals(sourceName)
                || "BRUNCH".equals(sourceName)
                || "TISTORY".equals(sourceName)
                || "NOTION".equals(sourceName);
    }

    private String visibleOrganizer(String organizer, String existingSourceName, String title) {
        String normalizedOrganizer = blankToNull(organizer);
        if (isPublicSourceName(normalizedOrganizer)) {
            normalizedOrganizer = null;
        }
        if (normalizedOrganizer != null) {
            return normalizedOrganizer;
        }

        String normalizedExisting = blankToNull(existingSourceName);
        if (isPublicSourceName(normalizedExisting)) {
            normalizedExisting = null;
        }
        if (normalizedExisting != null) {
            return normalizedExisting;
        }

        return blankToNull(title);
    }

    private String resolveRegionName(String title, String address) {
        try {
            return activityRegionResolver.resolve(title, address, defaultRegionId).getName();
        } catch (Exception e) {
            return null;
        }
    }

    private String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
