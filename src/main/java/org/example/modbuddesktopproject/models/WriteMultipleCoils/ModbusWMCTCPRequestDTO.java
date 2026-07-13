package org.example.modbuddesktopproject.models.WriteMultipleCoils;

import lombok.*;

@Getter
@Setter
@Builder
public class ModbusWMCTCPRequestDTO {
    private int transactionId;
    private int unitId;
    private int startAddress;
    private int quantity;
    private boolean[] coils;
}
