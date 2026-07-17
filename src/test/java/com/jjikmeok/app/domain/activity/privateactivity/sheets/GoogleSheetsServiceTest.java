package com.jjikmeok.app.domain.activity.privateactivity.sheets;

import com.jjikmeok.app.domain.activity.publicactivity.service.ActivityRegionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GoogleSheetsServiceTest {

    @Test
    void snapshot_whenSheetsEnabledButApiNotReady_throwsInsteadOfUsingMemory() {
        GoogleSheetsService service = new GoogleSheetsService(mock(ActivityRegionResolver.class));
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "spreadsheetId", "");

        assertThatThrownBy(service::snapshot)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google Sheets is enabled");
    }

    @Test
    void calculateNextAppendPosition_appendsBelowLastRowAndIncrementsLastNumber() {
        GoogleSheetsService.SheetAppendPosition position =
                GoogleSheetsService.calculateNextAppendPosition(List.of(
                        List.of("214", "existing"),
                        List.of(),
                        List.of("216", "last")
                ));

        assertThat(position.sheetRowNumber()).isEqualTo(5);
        assertThat(position.itemNumber()).isEqualTo(217);
    }

    @Test
    void calculateNextAppendPosition_startsAtFirstDataRowWhenSheetIsEmpty() {
        GoogleSheetsService.SheetAppendPosition position =
                GoogleSheetsService.calculateNextAppendPosition(List.of());

        assertThat(position.sheetRowNumber()).isEqualTo(2);
        assertThat(position.itemNumber()).isEqualTo(1);
    }
}
