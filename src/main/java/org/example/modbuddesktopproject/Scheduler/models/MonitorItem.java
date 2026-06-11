package org.example.modbuddesktopproject.Scheduler.models;

import lombok.*;

@Getter
@Setter
@Builder
public class MonitorItem {
    private Long id;
    private String name;
    private int slaveId;
    private int functionCode;
    private int address;
    private int quantity;
    private int msDelay;

    @Override
    public String toString() {
        return "MonitorItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slaveId=" + slaveId +
                ", functionCode=" + functionCode +
                ", address=" + address +
                ", quantity=" + quantity +
                ", msDelay=" + msDelay +
                '}';
    }
}
