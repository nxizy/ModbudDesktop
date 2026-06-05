package org.example.modbuddesktopproject.Services;

import com.fazecast.jSerialComm.SerialPort;
import org.example.modbuddesktopproject.Modbus.CRC16;
import org.example.modbuddesktopproject.Modbus.ModbusFrame;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRequestDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusResponseDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRequestDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCResponseDTO;

public class ModbusService {

    public static ModbusResponseDTO readHoldingRegisters(ModbusRequestDTO request, SerialPort port) throws InterruptedException {
        byte[] req = ModbusFrame.readHoldingRegisters(request.getSlaveId(), request.getAddress(), request.getQuantity());
        byte functionCode = req[1];
        port.setComPortParameters(
                9600,
                8,
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY
        );
        SerialService.openPort(port);
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING,
                3000,
                0
        );
        int sentBytes = port.writeBytes(req, req.length);

        Thread.sleep(500);
        byte[] res = new byte[8];
        int bytesRead = port.readBytes(
                res,
                res.length
        );
        boolean crcError;
        if(bytesRead < 8){
            crcError = true;
        } else {
            crcError = !CRC16.validateCRC(res, bytesRead);
        }
        boolean modBusError = bytesRead != 8
                || CRC16.isExceptionResponse(res);
        int resValue = ModbusResponse.extractRegisterValue(res);
        return ModbusResponseDTO.builder()
                .slaveId(request.getSlaveId())
                .functionCode(functionCode)
                .address(request.getAddress())
                .sentByteQuantity(sentBytes)
                .value(resValue)
                .sentBytes(req)
                .receivedBytes(res)
                .hasCrcError(crcError)
                .hasModbusError(modBusError)
                .build();
    }

    public static ModbusWMCResponseDTO writeMultipleCoils(ModbusWMCRequestDTO request, SerialPort port) throws InterruptedException{
        byte[] req = ModbusFrame.writeMultipleCoils(request.getSlaveId(), request.getAddress(), request.getQuantity(), request.getCoils());
        byte functionCode = req[1];
        port.setComPortParameters(
                9600,
                8,
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY
        );
        SerialService.openPort(port);
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING,
                3000,
                0
        );
        int sentBytes = port.writeBytes(req, req.length);
        Thread.sleep(500);
        byte[] res = new byte[8];
        int bytesRead = port.readBytes(
                res,
                res.length
        );
        boolean crcError;
        if(bytesRead < 8){
            crcError = true;
        } else {
            crcError = !CRC16.validateCRC(res, bytesRead);
        }
        boolean modBusError = bytesRead != 8
                        || CRC16.isExceptionResponse(res);
        boolean addressError;
        addressError = rebuildBytes(res[2], res[3]) != request.getAddress();
        boolean quantityError;
        quantityError = rebuildBytes(res[4], res[5]) != request.getQuantity();
        return ModbusWMCResponseDTO.builder()
                .slaveId(request.getSlaveId())
                .functionCode(functionCode)
                .address(request.getAddress())
                .sentBytes(req)
                .receivedBytes(res)
                .hasCrcError(crcError)
                .hasModbusError(modBusError)
                .hasAddressError(addressError)
                .hasQuantityError(quantityError)
                .build();
    }

    private static int rebuildBytes(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }
}
