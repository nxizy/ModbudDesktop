package org.example.modbuddesktopproject.Modbus;

public class ModbusResponseTCP {
    public static String printFrame(byte[] frame) {

        StringBuilder framePrint = new StringBuilder();

        for (byte b : frame) {
            framePrint.append(String.format("%02X ", b & 0xFF));
            System.out.printf("%02X ", b & 0xFF);
        }

        System.out.println();

        return framePrint.toString();
    }

    public static int getTransactionId(byte[] response) {
        return ((response[0] & 0xFF) << 8)
                | (response[1] & 0xFF);
    }

    public static int getProtocolId(byte[] response) {
        return ((response[2] & 0xFF) << 8)
                | (response[3] & 0xFF);
    }

    public static int getLength(byte[] response) {
        return ((response[4] & 0xFF) << 8)
                | (response[5] & 0xFF);
    }

    public static int getUnitId(byte[] response) {
        return response[6] & 0xFF;
    }

    public static int getFunctionCode(byte[] response) {
        return response[7] & 0xFF;
    }

    public static int getByteCount(byte[] response) {
        return response[8] & 0xFF;
    }

    public static int[] extractRegisters(byte[] response) {

        int byteCount = response[8] & 0xFF;
        int quantity = byteCount / 2;
        int[] registers = new int[quantity];
        int index = 9;

        for (int i = 0; i < quantity; i++) {
            registers[i] = ((response[index] & 0xFF) << 8) | (response[index + 1] & 0xFF);
            index += 2;
        }

        return registers;
    }

    public static void printRegisters(int[] registers) {

        for (int i = 0; i < registers.length; i++) {
            System.out.println(
                    "Holding Register " + i + ": " + registers[i]
            );
        }

    }

    public static int getStartAddress(byte[] response) {
        return ((response[8] & 0xFF) << 8)
                | (response[9] & 0xFF);
    }

    public static int getQuantity(byte[] response) {
        return ((response[10] & 0xFF) << 8)
                | (response[11] & 0xFF);
    }

    public static boolean isException(byte[] response) {
        return (response[7] & 0x80) != 0;
    }

    public static int getExceptionCode(byte[] response) {
        return response[8] & 0xFF;
    }
}
