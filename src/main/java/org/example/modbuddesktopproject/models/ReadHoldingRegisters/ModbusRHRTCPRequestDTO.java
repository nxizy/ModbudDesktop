package org.example.modbuddesktopproject.models.ReadHoldingRegisters;

import lombok.*;

@Data
@Builder
public class ModbusRHRTCPRequestDTO {
    private int transactionId;
    private int unitId;
    private int startAddress;
    private int quantity;
}
