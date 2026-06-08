package org.example.modbuddesktopproject.models.ReadCoils;

import lombok.*;

@Getter
@Setter
@Builder

public class ReadCoilsRequestDTO {
    private int slaveId;
    private int address;
    private int quantity;
}
