package org.example.modbuddesktopproject.models;

import lombok.*;

@Getter
@Setter
@Builder
public class ModbusRequestDTO {
    private int slaveId;
    private int address;
    private int quantity;
}
