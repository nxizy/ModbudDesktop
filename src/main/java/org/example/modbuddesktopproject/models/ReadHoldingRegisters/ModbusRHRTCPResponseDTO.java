package org.example.modbuddesktopproject.models.ReadHoldingRegisters;

import lombok.*;

@Data
@Builder
public class ModbusRHRTCPResponseDTO {
    private int transactionId;
    private int unitId;
    private int value;
    private byte[] receivedBytes;
}
