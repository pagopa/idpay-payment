package it.gov.pagopa.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScheduleConfigTest {

    @Test
    void testConfigureTasks_UnitTest() {
        // Given
        ScheduleConfig scheduleConfig = new ScheduleConfig();
        int expectedPoolSize = 5;
        ReflectionTestUtils.setField(scheduleConfig, "maxScheduleThreadNumber", expectedPoolSize);

        ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();

        // When
        scheduleConfig.configureTasks(taskRegistrar);

        // Then
        assertNotNull(taskRegistrar.getScheduler());
        ThreadPoolTaskScheduler taskScheduler = (ThreadPoolTaskScheduler) taskRegistrar.getScheduler();
        assertEquals(expectedPoolSize, taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
    }

    @Test
    void testScheduleConfig_SpringContextLoad() {
        // Given & When & Then
        new ApplicationContextRunner()
                .withUserConfiguration(ScheduleConfig.class)
                .withPropertyValues("app.threads.schedule-max-number=10")
                .run(context -> {
                    ScheduleConfig config = context.getBean(ScheduleConfig.class);
                    assertNotNull(config);

                    ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();
                    config.configureTasks(taskRegistrar);

                    ThreadPoolTaskScheduler taskScheduler = (ThreadPoolTaskScheduler) taskRegistrar.getScheduler();
                    assertNotNull(taskScheduler);
                    assertEquals(10, taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
                });
    }
}