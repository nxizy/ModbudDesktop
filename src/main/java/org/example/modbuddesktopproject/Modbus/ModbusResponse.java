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
}
