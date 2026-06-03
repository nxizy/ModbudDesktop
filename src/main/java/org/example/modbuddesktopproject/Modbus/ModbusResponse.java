package org.example.modbuddesktopproject.Modbus;

public class ModbusResponse {
    public static void printFrame(byte[] frame, int length) {
        for (int i = 0; i < length; i++) {
            System.out.printf("%02X ", frame[i] & 0xFF);
        }
        System.out.println();
    }

    public static int extractRegisterValue(byte[] res){
        return ((res[3] & 0xFF) << 8) | (res[4] & 0xFF);
    }
}
