package org.example.modbuddesktopproject.Modbus;

import java.util.ArrayList;
import java.util.List;

public class ModbusResponse {
    public static String printFrame(byte[] frame, int length) {
        StringBuilder framePrint = new StringBuilder();

        for (int i = 0; i < length; i++) {
            framePrint.append(String.format("%02X ", frame[i] & 0xFF));
            System.out.printf("%02X ", frame[i] & 0xFF);
        }
        System.out.println();
        return framePrint.toString();
    }

    public static int extractRegisterValue(byte[] res){
        return ((res[3] & 0xFF) << 8) | (res[4] & 0xFF);
    }

    // Read Coils
    public static String extractCoils(
            byte[] coilBytes,
            int startAddress,
            int quantity
    ) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < quantity; i++) {

            boolean coil = ((coilBytes[i / 8] >> (i % 8)) & 1) == 1;

            sb.append("Coil ")
                    .append(startAddress + i)
                    .append(": ")
                    .append(coil ? "ON" : "OFF")
                    .append("\n");
        }

        return sb.toString();
    }
}
