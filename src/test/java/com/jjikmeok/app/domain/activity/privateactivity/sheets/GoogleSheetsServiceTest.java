package com.jjikmeok.app.domain.activity.privateactivity.sheets;

import com.jjikmeok.app.domain.activity.publicactivity.service.ActivityRegionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
}
