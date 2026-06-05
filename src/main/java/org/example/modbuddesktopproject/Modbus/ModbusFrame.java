package org.example.modbuddesktopproject.Modbus;

public class ModbusFrame {
    private static final byte READ_HOLDING_REGISTERS = 0x03;
    private static final byte READ_COILS = 0X01;

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


    // Function 01 - Read Coils
    public static byte[] readCoils(
            int slaveId,
            int startAddress,
            int quantity
    ){
        //Para caso o slaveId saia do range do Modbus
        if (slaveId < 1 || slaveId > 247) {
            throw new IllegalArgumentException(
                    "Slave ID inválido"
            );
        }
        //Quantidade de coils a serem lidos
        if (quantity < 1 || quantity > 2000) {
            throw new IllegalArgumentException(
                    "Quantidade de coils inválida"
            );
        }
        
        byte[] rawData = {
                (byte) (slaveId & 0xFF),
                READ_COILS,
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
}
