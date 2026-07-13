package org.example.modbuddesktopproject.models.WriteMultipleCoils;

import lombok.*;

@Getter
@Setter
@Builder
public class ModbusWMCRTUResponseDTO {
    private int slaveId;
    private int address;
    private byte[] receivedBytes;
    private boolean hasError;
}
