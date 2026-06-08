package org.example.modbuddesktopproject.Modbus;

public class ModbusFrame {
    private static final byte READ_HOLDING_REGISTERS = 0x03;
    private static final byte WRITE_MULTIPLE_COILS = 0x0F;

    public static byte[] readHoldingRegisters(
            int slaveId,
            int startAddress,
            int quantity
    ) {
        //Para caso o slaveId saia do range do Modbus
        if (slaveId < 1 || slaveId > 247) {
            throw new IllegalArgumentException(
                    "Slave ID inválido"
            );
        }
        byte[] rawData = {
                (byte) (slaveId & 0xFF),
                READ_HOLDING_REGISTERS,
                CRC16.getHighByte(startAddress),
                CRC16.getLowByte(startAddress),
                CRC16.getHighByte(quantity),
                CRC16.getLowByte(quantity)
        };
        int crc = CRC16.calculate(rawData);
        byte lowByteCRC = CRC16.getLowByte(crc);
        byte highByteCRC = CRC16.getHighByte(crc);
        byte[] frame = new byte[8];
        System.arraycopy(rawData, 0, frame,0, 6);
        frame[6] = lowByteCRC;
        frame[7] = highByteCRC;
        return frame;
    }

    public static byte[] writeMultipleCoils(
            int slaveId,
            int startAddress,
            int quantity,
            boolean[] coils
    ){
        if (quantity != coils.length) {
            quantity = coils.length;
        }
        int byteCount = (coils.length + 7) / 8;
        byte[] dataBytes = packCoils(coils);
        byte[] rawData = new byte[7 + dataBytes.length];
        rawData[0] = (byte) slaveId;
        rawData[1] = WRITE_MULTIPLE_COILS;
        rawData[2] = CRC16.getHighByte(startAddress);
        rawData[3] = CRC16.getLowByte(startAddress);
        rawData[4] = CRC16.getHighByte(quantity);
        rawData[5] = CRC16.getLowByte(quantity);
        rawData[6] = (byte) byteCount;
        System.arraycopy(dataBytes, 0, rawData, 7, dataBytes.length);
        int crc = CRC16.calculate(rawData);
        byte lowByteCRC = CRC16.getLowByte(crc);
        byte highByteCRC = CRC16.getHighByte(crc);
        byte[] frame = new byte[rawData.length + 2];
        System.arraycopy(rawData, 0, frame, 0, rawData.length);
        frame[frame.length - 2] = lowByteCRC;
        frame[frame.length - 1] = highByteCRC;
        return frame;
    }

    private static byte[] packCoils(boolean[] coils){
        int byteCount = (coils.length + 7) / 8;
        byte[] dataBytes = new byte[byteCount];
        for (int i = 0; i < coils.length; i++) {
            int currentByte = i / 8;
            int currentBit = i % 8;
            if (coils[i]) {
                dataBytes[currentByte] |= (byte) (1 << currentBit);
            }
        }
        return dataBytes;
    }

}
