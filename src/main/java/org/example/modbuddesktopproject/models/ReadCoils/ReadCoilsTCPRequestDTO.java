package org.example.modbuddesktopproject.models.ReadCoils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadCoilsTCPRequestDTO {
    private int transactionId;
    private int unitId;
    private int startAddress;
    private int quantity;
}
