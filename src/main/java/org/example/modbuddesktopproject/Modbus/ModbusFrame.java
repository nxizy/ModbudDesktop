package org.example.modbuddesktopproject.Modbus;

public class ModbusFrame {
    private static final byte READ_HOLDING_REGISTERS = 0x03;
    private static final byte READ_COILS = 0X01;
    private static final byte WRITE_MULTIPLE_COILS = 0x0F;

    // ==========
    //    RTU
    // ==========

    // Function 01 - Read Coils
    public static byte[] readCoils(
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
        System.arraycopy(rawData, 0, frame, 0, 6);
        frame[6] = lowByteCRC;
        frame[7] = highByteCRC;
        return frame;
    }


    // Function 03 - Read Holding Registers
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
        System.arraycopy(rawData, 0, frame, 0, 6);
        frame[6] = lowByteCRC;
        frame[7] = highByteCRC;
        return frame;
    }


    // Function 15 - Write Multiple Coils
    private static byte[] packCoils(boolean[] coils) {
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

    public static byte[] writeMultipleCoils(
            int slaveId,
            int startAddress,
            int quantity,
            boolean[] coils
    ) {
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


    // ==========
    //    TCP
    // ==========

    // Function 01 - Read Coils
    public static byte[] readCoilsTCP(
            int transactionId,
            int unitId,
            int startAddress,
            int quantity
    ) {
        //Quantidade de coils a serem lidos
        if (quantity < 1 || quantity > 2000) {
            throw new IllegalArgumentException(
                    "Quantidade de coils inválida"
            );
        }

        if (startAddress < 0 || startAddress > 65535) {
            throw new IllegalArgumentException("Endereço inicial inválido");
        }

        byte[] frame = {
                // Transaction Identifier
                CRC16.getHighByte(transactionId),
                CRC16.getLowByte(transactionId),

                // Protocol Identifier (sempre 0)
                0x00,
                0x00,

                // Length (Unit ID + PDU = 6 bytes)
                0x00,
                0x06,

                // Unit Identifier
                (byte) unitId,

                // PDU
                READ_COILS,
                CRC16.getHighByte(startAddress),
                CRC16.getLowByte(startAddress),
                CRC16.getHighByte(quantity),
                CRC16.getLowByte(quantity)
        };

        return frame;
    }

    // Function 03 - Read Holding Registers
    public static byte[] readHoldingRegistersTCP(
            int transactionId,
            int unitId,
            int startAddress,
            int quantity
    ) {

        // Validação do Unit Identifier
        if (unitId < 0 || unitId > 255) {
            throw new IllegalArgumentException("Unit ID inválido");
        }

        // Validação da quantidade
        if (quantity < 1 || quantity > 125) {
            throw new IllegalArgumentException("Quantidade de registradores inválida");
        }

        byte[] frame = {
                // Transaction Identifier
                CRC16.getHighByte(transactionId),
                CRC16.getLowByte(transactionId),

                // Protocol Identifier (sempre 0)
                0x00,
                0x00,

                // Length (Unit ID + PDU = 6 bytes)
                0x00,
                0x06,

                // Unit Identifier
                (byte) unitId,

                // PDU
                READ_HOLDING_REGISTERS,
                CRC16.getHighByte(startAddress),
                CRC16.getLowByte(startAddress),
                CRC16.getHighByte(quantity),
                CRC16.getLowByte(quantity)
        };

        return frame;
    }

    // Function 15 - Write Multiple Coils
    public static byte[] writeMultipleCoilsTCP(
            int transactionId,
            int unitId,
            int startAddress,
            int quantity,
            boolean[] coils
    ) {

        // Validação do Unit ID
        if (unitId < 0 || unitId > 255) {
            throw new IllegalArgumentException("Unit ID inválido");
        }

        // Ajusta a quantidade conforme o vetor recebido
        if (quantity != coils.length) {
            quantity = coils.length;
        }

        // Limite da FC15
        if (quantity < 1 || quantity > 1968) {
            throw new IllegalArgumentException(
                    "Quantidade de coils inválida."
            );
        }

        // Quantidade de bytes necessários
        int byteCount = (quantity + 7) / 8;

        // Empacota os bits
        byte[] dataBytes = packCoils(coils);

        // Length = Unit ID + Function + Address + Quantity + ByteCount + Data
        int length = 7 + byteCount;

        byte[] frame = new byte[13 + byteCount];

        // =====================
        // MBAP Header
        // =====================

        frame[0] = CRC16.getHighByte(transactionId);
        frame[1] = CRC16.getLowByte(transactionId);

        frame[2] = 0x00;
        frame[3] = 0x00;

        frame[4] = CRC16.getHighByte(length);
        frame[5] = CRC16.getLowByte(length);

        frame[6] = (byte) unitId;

        // =====================
        // PDU
        // =====================

        frame[7] = WRITE_MULTIPLE_COILS;

        frame[8] = CRC16.getHighByte(startAddress);
        frame[9] = CRC16.getLowByte(startAddress);

        frame[10] = CRC16.getHighByte(quantity);
        frame[11] = CRC16.getLowByte(quantity);

        frame[12] = (byte) byteCount;

        System.arraycopy(
                dataBytes,
                0,
                frame,
                13,
                byteCount
        );

        return frame;
    }
}
