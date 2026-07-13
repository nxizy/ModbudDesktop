package org.example.modbuddesktopproject.models.ReadHoldingRegisters;

import lombok.*;

@Getter
@Setter
@Builder
public class ModbusRHRRTURequestDTO {
    private int slaveId;
    private int address;
    private int quantity;
}
