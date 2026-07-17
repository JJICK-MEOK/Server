package com.jjikmeok.app.domain.activity.privateactivity.scheduler;

import com.jjikmeok.app.domain.activity.privateactivity.publish.DiscoveryPublishService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DiscoveryPublishSchedulerTest {

    @Test
    void runDiscoveryPublish_callsOnlyDiscoveryPublisher() {
        DiscoveryPublishService publishService = mock(DiscoveryPublishService.class);
        DiscoveryPublishScheduler scheduler = new DiscoveryPublishScheduler(publishService);

        scheduler.runDiscoveryPublish();

        verify(publishService).publishReadyRows();
    }
}
