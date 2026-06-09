package org.example.modbuddesktopproject.Scheduler.models;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class MonitorResponse<T> {
    private MonitorItem monitorItem;
    private List<T> values;
    private LocalDateTime timestamp;


}
