package org.example.modbuddesktopproject.Scheduler.models;

import lombok.*;

@Getter
@Setter
@Builder
public class MonitorItem {
    private String name;
    private int slaveId;
    private int functionCode;
    private int address;
    private int quantity;

    @Override
    public String toString() {
        return "MonitorItem{" +
                "name='" + name + '\'' +
                ", slaveId=" + slaveId +
                ", functionCode=" + functionCode +
                ", address=" + address +
                ", quantity=" + quantity +
                '}';
    }
}
