package org.example.modbuddesktopproject.models.ReadCoils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadCoilsTCPResponseDTO {
    private int transactionId;
    private int unitId;
    private byte[] coilBytes;
    private byte[] receivedBytes;
}
