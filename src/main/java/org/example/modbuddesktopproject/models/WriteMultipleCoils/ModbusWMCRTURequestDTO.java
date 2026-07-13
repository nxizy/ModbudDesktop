package org.example.modbuddesktopproject.models.WriteMultipleCoils;

import lombok.*;

@Getter
@Setter
@Builder
public class ModbusWMCRTURequestDTO {
    private int slaveId;
    private int address;
    private int quantity;
    private boolean[] coils;
}