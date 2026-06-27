package com.jjikmeok.app.domain.activity.privateactivity.sheets;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.GoogleCredentials;
import com.jjikmeok.app.domain.activity.privateactivity.dto.DiscoveryCandidateDto;
import com.jjikmeok.app.domain.ai.dto.DiscoveryAnalysisDto;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySheetStatus;
import com.jjikmeok.app.domain.activity.publicactivity.dto.NormalizedActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class GoogleSheetsService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/spreadsheets");

    private final RestClient restClient = RestClient.create();
    private final List<DiscoverySheetRowDto> memoryRows = new CopyOnWriteArrayList<>();
    private final AtomicInteger memorySequence = new AtomicInteger(1);

    @Value("${app.discovery.sheets.enabled:false}")
    private boolean enabled;

    @Value("${app.discovery.sheets.spreadsheet-id:}")
    private String spreadsheetId;

    @Value("${app.discovery.sheets.sheet-name:Discovery}")
    private String sheetName;

    @Value("${app.discovery.sheets.credentials-path:}")
    private String credentialsPath;

    @Value("${app.discovery.sheets.credentials-json:}")
    private String credentialsJson;

    private volatile GoogleCredentials googleCredentials;

    @PostConstruct
    void init() {
        if (enabled && hasApiConfiguration()) {
            googleCredentials = loadCredentials();
            if (googleCredentials != null) {
                ensureHeaders();
            }
            if (googleCredentials == null) {
                log.warn("[발견] Google Sheets 자격 증명을 불러오지 못해 메모리 모드로 동작합니다.");
            }
        }
    }

    public DiscoverySheetRowDto append(DiscoveryAnalysisDto analysis) {
        if (enabled && isApiReady()) {
            return appendToSheet(analysis);
        }
        return appendToMemory(analysis);
    }

    public DiscoverySheetRowDto append(DiscoveryCandidateDto candidate) {
        if (enabled && isApiReady()) {
            return appendCandidateToSheet(candidate);
        }
        return appendCandidateToMemory(candidate);
    }

    public DiscoverySheetRowDto upsertPublicActivity(NormalizedActivity activity) {
        DiscoverySheetRowDto existing = findPublicRow(activity);
        if (existing != null) {
            DiscoverySheetRowDto updated = copyPublicRow(existing, activity);
            updateRow(updated);
            return updated;
        }

        if (enabled && isApiReady()) {
            return appendPublicActivityToSheet(activity);
        }
        return appendPublicActivityToMemory(activity);
    }

    public List<DiscoverySheetRowDto> snapshot() {
        if (enabled && isApiReady()) {
            List<DiscoverySheetRowDto> rows = readRowsFromSheet("A2:AC");
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return List.copyOf(memoryRows);
    }

    public List<DiscoverySheetRowDto> findReadyRows() {
        if (enabled && isApiReady()) {
            return readReadyRowsFromSheet();
        }
        return memoryRows.stream()
                .filter(row -> row.status() == DiscoverySheetStatus.READY)
                .toList();
    }

    public void updateRow(DiscoverySheetRowDto row) {
        if (row == null) {
            return;
        }

        if (enabled && isApiReady()) {
            writeRowToSheet(row);
        }
        replaceMemoryRow(row);
    }

    public void clear() {
        if (enabled && isApiReady()) {
            clearSheetRows();
        }
        memoryRows.clear();
        memorySequence.set(1);
    }

    private DiscoverySheetRowDto appendToMemory(DiscoveryAnalysisDto analysis) {
        int rowNumber = memorySequence.incrementAndGet();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.from(analysis, rowNumber, LocalDateTime.now(SEOUL));
        memoryRows.add(row);
        log.info("[발견] 임시 저장했습니다. 행 번호={}, 제목={}", row.rowNumber(), row.title());
        return row;
    }

    private DiscoverySheetRowDto appendCandidateToMemory(DiscoveryCandidateDto candidate) {
        int rowNumber = memorySequence.incrementAndGet();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromCandidate(candidate, rowNumber, LocalDateTime.now(SEOUL));
        memoryRows.add(row);
        log.info("[발견] 추출 결과를 먼저 저장했습니다. 행 번호={}, 제목={}", row.rowNumber(), row.title());
        return row;
    }

    private DiscoverySheetRowDto appendToSheet(DiscoveryAnalysisDto analysis) {
        int rowNumber = nextRowNumber();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.from(analysis, rowNumber, LocalDateTime.now(SEOUL));
        putRowValues(rowRange(rowNumber), row.toSheetRow());
        replaceMemoryRow(row);
        log.info("[발견] 시트에 저장했습니다. 행 번호={}, 제목={}", row.rowNumber(), row.title());
        return row;
    }

    private DiscoverySheetRowDto appendCandidateToSheet(DiscoveryCandidateDto candidate) {
        int rowNumber = nextRowNumber();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromCandidate(candidate, rowNumber, LocalDateTime.now(SEOUL));
        putRowValues(rowRange(rowNumber), row.toSheetRow());
        replaceMemoryRow(row);
        log.info("[발견] 추출 결과를 시트에 먼저 저장했습니다. 행 번호={}, 제목={}", row.rowNumber(), row.title());
        return row;
    }

    private DiscoverySheetRowDto appendPublicActivityToMemory(NormalizedActivity activity) {
        int rowNumber = memorySequence.incrementAndGet();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromPublicActivity(activity, rowNumber, LocalDateTime.now(SEOUL));
        memoryRows.add(row);
        log.info("[Sheets] Public activity cached in memory. rowNumber={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private DiscoverySheetRowDto appendPublicActivityToSheet(NormalizedActivity activity) {
        int rowNumber = nextRowNumber();
        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromPublicActivity(activity, rowNumber, LocalDateTime.now(SEOUL));
        putRowValues(rowRange(rowNumber), row.toSheetRow());
        replaceMemoryRow(row);
        log.info("[Sheets] Public activity appended to sheet. rowNumber={}, title={}", row.rowNumber(), row.title());
        return row;
    }

    private List<DiscoverySheetRowDto> readRowsFromSheet(String range) {
        JsonNode root = getSheetValues(range);
        JsonNode values = root == null ? null : root.path("values");
        if (values == null || !values.isArray()) {
            return List.of();
        }

        List<DiscoverySheetRowDto> rows = new ArrayList<>();
        int rowNumber = 2;
        for (JsonNode valueRow : values) {
            rows.add(DiscoverySheetRowDto.fromSheetRow(rowNumber++, asValueList(valueRow)));
        }
        return rows;
    }

    private List<DiscoverySheetRowDto> readReadyRowsFromSheet() {
        JsonNode root = getSheetValues("B2:B");
        JsonNode values = root == null ? null : root.path("values");
        if (values == null || !values.isArray()) {
            return List.of();
        }

        List<DiscoverySheetRowDto> rows = new ArrayList<>();
        int rowNumber = 2;
        for (JsonNode statusRow : values) {
            String status = text(statusRow, 0);
            if (DiscoverySheetStatus.READY.name().equalsIgnoreCase(status)) {
                DiscoverySheetRowDto row = readRowFromSheet(rowNumber);
                if (row != null) {
                    rows.add(row);
                }
            }
            rowNumber++;
        }
        return rows;
    }

    private DiscoverySheetRowDto readRowFromSheet(int rowNumber) {
        JsonNode root = getSheetValues(rowRange(rowNumber));
        JsonNode values = root == null ? null : root.path("values");
        if (values != null && values.isArray() && !values.isEmpty()) {
            return DiscoverySheetRowDto.fromSheetRow(rowNumber, asValueList(values.get(0)));
        }
        return null;
    }

    private void writeRowToSheet(DiscoverySheetRowDto row) {
        putRowValues(rowRange(row.rowNumber()), row.toSheetRow());
    }

    private void clearSheetRows() {
        try {
            restClient.post()
                    .uri(builder -> builder
                            .scheme("https")
                            .host("sheets.googleapis.com")
                    .path("/v4/spreadsheets/{spreadsheetId}/values/{range}:clear")
                            .build(spreadsheetId, sheetName + "!A2:AC"))
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("[발견] Google Sheets 초기화에 실패했습니다. reason={}", e.getMessage());
        }
    }

    private JsonNode getSheetValues(String range) {
        try {
            return restClient.get()
                    .uri(builder -> builder
                            .scheme("https")
                            .host("sheets.googleapis.com")
                            .path("/v4/spreadsheets/{spreadsheetId}/values/{range}")
                            .build(spreadsheetId, sheetName + "!" + range))
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("[발견] Google Sheets 조회에 실패했습니다. range={}, reason={}", range, e.getMessage());
            return null;
        }
    }

    private void ensureHeaders() {
        try {
            JsonNode root = getSheetValues("A1:AC1");
            JsonNode values = root == null ? null : root.path("values");
            if (values != null && values.isArray() && !values.isEmpty()) {
                List<Object> current = asValueList(values.get(0));
                if (current.size() >= DiscoverySheetRowDto.sheetHeaders().length) {
                    return;
                }
            }
            putRowValues("A1:AC1", new ArrayList<>(Arrays.asList(DiscoverySheetRowDto.sheetHeaders())));
        } catch (Exception e) {
            log.warn("[Sheets] Failed to ensure headers. reason={}", e.getMessage());
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
                            .build(spreadsheetId, sheetName + "!" + range))
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .body(java.util.Map.of("values", java.util.List.of(values)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new IllegalStateException("Google Sheets 업데이트에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private int nextRowNumber() {
        return snapshot().stream()
                .mapToInt(DiscoverySheetRowDto::rowNumber)
                .max()
                .orElse(1) + 1;
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
        String sourceName = activity.sourceType() == null ? null : activity.sourceType().name();

        for (DiscoverySheetRowDto row : snapshot()) {
            if (row == null || !isPublicSourceName(row.sourceName())) {
                continue;
            }
            if (sourceUrl != null && sourceUrl.equals(blankToNull(row.sourceUrl()))) {
                return row;
            }
            if (sourceUrl == null
                    && equalsNullable(sourceName, row.sourceName())
                    && equalsNullable(blankToNull(activity.title()), blankToNull(row.title()))
                    && equalsNullable(activity.startAt(), row.startAt())) {
                return row;
            }
        }
        return null;
    }

    private DiscoverySheetRowDto copyPublicRow(DiscoverySheetRowDto existing, NormalizedActivity activity) {
        return new DiscoverySheetRowDto(
                existing.rowNumber(),
                DiscoverySheetStatus.PUBLISHED,
                existing.createdAt(),
                existing.publishedAt(),
                null,
                activity.sourceType() == null ? existing.sourceName() : activity.sourceType().name(),
                activity.title(),
                activity.sourceUrl(),
                activity.thumbnailUrl(),
                activity.activityType(),
                activity.category(),
                activity.startAt(),
                activity.endAt(),
                activity.recruitStartAt(),
                activity.recruitEndAt(),
                activity.target(),
                activity.price(),
                activity.description(),
                activity.contactInfo(),
                activity.organizer(),
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
            log.warn("[발견] Google Sheets 자격 증명 로드에 실패했습니다. reason={}", e.getMessage());
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
                throw new IllegalStateException("Google Sheets 액세스 토큰이 없습니다.");
            }
            return googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            throw new IllegalStateException("Google Sheets 인증에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private String rowRange(int rowNumber) {
        return "A" + rowNumber + ":AC" + rowNumber;
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
                || "SEOUL_RESERVATION".equals(sourceName);
    }
}
