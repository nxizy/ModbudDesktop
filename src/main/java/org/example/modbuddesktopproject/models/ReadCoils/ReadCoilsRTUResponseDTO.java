package org.example.modbuddesktopproject.models.ReadCoils;

import lombok.*;

@Getter
@Setter
@Builder
public class ReadCoilsRTUResponseDTO {
    private int slaveId;
    private int address;
    private int quantity;
    private byte[] coilBytes;
    private byte[] receivedBytes;
    private boolean hasError;
}
