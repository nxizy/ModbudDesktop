package org.example.modbuddesktopproject.models.ReadHoldingRegisters;

import lombok.*;

@Getter
@Setter
@Builder
public class ModbusRHRRTUResponseDTO {
    private int slaveId;
    private int address;
    private int value;
    private byte[] receivedBytes;
    private boolean hasError;
}
