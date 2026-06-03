package org.example.modbuddesktopproject.models;

import lombok.*;

@Getter
@Setter
@Builder
public class ModbusResponseDTO {
    private int slaveId;
    private int address;
    private int sentByteQuantity;
    private int value;
    private byte[] sentBytes;
    private byte[] receivedBytes;
    private boolean hasCrcError;
    private boolean hasModbusError;
}
