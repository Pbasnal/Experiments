package com.amarkatha.scheduling;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulingConfig {

    @Bean
    Clock scheduleClock() {
        return Clock.systemUTC();
    }

    @Bean
    ScheduleCalendar scheduleCalendar(Clock scheduleClock) {
        return new ScheduleCalendar(scheduleClock);
    }

    @Bean
    ScheduleActions scheduleActions(ScheduleCalendar scheduleCalendar) {
        return new ScheduleActions(scheduleCalendar);
    }
}
