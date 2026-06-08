package org.example.modbuddesktopproject.models;

import lombok.*;

@Getter
@Setter
@Builder
public class ReadCoilsResponseDTO {

    private int slaveId;
    private int address;
    private int quantity;

    private byte[] coilBytes;

    private byte[] sentBytes;
    private byte[] receivedBytes;

    private boolean hasCrcError;
    private boolean hasModbusError;
}
