package org.example.modbuddesktopproject.models.WriteMultipleCoils;

import lombok.*;

@Getter
@Setter
@Builder
public class ModbusWMCResponseDTO {
    private int slaveId;
    private int functionCode;
    private int address;
    private byte[] sentBytes;
    private byte[] receivedBytes;
    private boolean hasCrcError;
    private boolean hasModbusError;
    private boolean hasAddressError;
    private boolean hasQuantityError;
}
