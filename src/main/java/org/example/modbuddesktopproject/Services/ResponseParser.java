package org.example.modbuddesktopproject.Services;

import org.example.modbuddesktopproject.Modbus.CRC16;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsRTUResponseDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsTCPRequestDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsTCPResponseDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRRTUResponseDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRTCPResponseDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRTUResponseDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCTCPResponseDTO;

public class ResponseParser {

    public static ModbusRHRRTUResponseDTO readHoldingRegistersRTUParser(byte[] res) {
        int slaveId = res[0] & 0xFF;
        int address = CRC16.uniteByte(res[2], res[3]);
        int resValue = ModbusResponse.extractRegisterValue(res);
        boolean error = hasException(res);
        return ModbusRHRRTUResponseDTO.builder()
                .slaveId(slaveId)
                .address(address)
                .value(resValue)
                .receivedBytes(res)
                .hasError(error)
                .build();
    }

    public static ModbusWMCRTUResponseDTO writeMultipleCoilsRTUParser(byte[] res) {
        int slaveId = res[0] & 0xFF;
        int address = CRC16.uniteByte(res[2], res[3]);
        boolean error = hasException(res);
        return ModbusWMCRTUResponseDTO.builder()
                .slaveId(slaveId)
                .address(address)
                .receivedBytes(res)
                .hasError(error)
                .build();
    }

    public static ReadCoilsRTUResponseDTO readCoilsRTUParser(byte[] res) {
        int slaveId = res[0] & 0xFF;
        int address = CRC16.uniteByte(res[2], res[3]);
        int quantity = CRC16.uniteByte(res[4], res[5]);
        boolean error = hasException(res);
        byte[] coilBytes = new byte[0];
        if (!error) {
            int byteCount = res[2] & 0xFF;

            coilBytes = new byte[byteCount];

            System.arraycopy(
                    res,
                    3,
                    coilBytes,
                    0,
                    byteCount
            );
        }

        return ReadCoilsRTUResponseDTO.builder()
                .slaveId(slaveId)
                .address(address)
                .quantity(quantity)
                .coilBytes(coilBytes)
                .receivedBytes(res)
                .hasError(error)
                .build();
    }

    public static ReadCoilsTCPResponseDTO readCoilsTCPParser(byte[] res){
        int transactionId = CRC16.uniteByte(res[0], res[1]);
        int unitId = res[6] & 0xFF;
        byte[] coilBytes = new byte[0];
        int byteCount = res[8] & 0xFF;
        coilBytes = new byte[byteCount];
        System.arraycopy(res, 9, coilBytes, 0, byteCount);
        return ReadCoilsTCPResponseDTO
                .builder()
                .transactionId(transactionId)
                .unitId(unitId)
                .coilBytes(coilBytes)
                .receivedBytes(res)
                .build();
    }

    public static ModbusRHRTCPResponseDTO readHoldingRegistersTCPParser(byte[] res) {
        int transactionId = CRC16.uniteByte(res[0], res[1]);
        int unitId = res[6] & 0xFF;
        int value = CRC16.uniteByte(res[9], res[10]);
        return ModbusRHRTCPResponseDTO
                .builder()
                .transactionId(transactionId)
                .unitId(unitId)
                .value(value)
                .receivedBytes(res)
                .build();
    }

    public static ModbusWMCTCPResponseDTO writeMultipleCoilsTCPParser(byte[] res) {
        int transactionId = CRC16.uniteByte(res[0], res[1]);
        int unitId = res[6] & 0xFF;
        return ModbusWMCTCPResponseDTO
                .builder()
                .transactionId(transactionId)
                .unitId(unitId)
                .receivedBytes(res)
                .build();
    }

    private static boolean hasException(byte[] res) {
        boolean exceptionResponse = CRC16.isExceptionResponse(res);
        boolean crcError;
        if (exceptionResponse) {
            crcError = !CRC16.validateCRC(res, 5);
        } else {
            crcError = !CRC16.validateCRC(res, res.length);
        }
        boolean modBusError = CRC16.isExceptionResponse(res);

        return crcError || modBusError;
    }
}
