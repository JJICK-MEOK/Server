package com.jjikmeok.app.domain.activity.publicactivity.scheduler;

import com.jjikmeok.app.domain.activity.publicactivity.publish.PublicActivityPublishService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublicActivityPublishSchedulerTest {

    @Test
    void runPublicActivityPublish_callsOnlyPublicPublisher() {
        PublicActivityPublishService publishService = mock(PublicActivityPublishService.class);
        PublicActivityPublishScheduler scheduler = new PublicActivityPublishScheduler(publishService);

        scheduler.runPublicActivityPublish();

        verify(publishService).publishReadyRows();
    }
}
