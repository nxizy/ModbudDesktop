package org.example.modbuddesktopproject.models.WriteMultipleCoils;

import lombok.*;

@Data
@Builder
public class ModbusWMCTCPResponseDTO {
    private int transactionId;
    private int unitId;
    private byte[] receivedBytes;
}
