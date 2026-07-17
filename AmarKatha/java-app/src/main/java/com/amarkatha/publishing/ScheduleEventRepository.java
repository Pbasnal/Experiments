package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.ScheduleEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, UUID> {
}
