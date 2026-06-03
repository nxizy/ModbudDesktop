package org.example.modbuddesktopproject.Services;

import com.fazecast.jSerialComm.SerialPort;
import org.example.modbuddesktopproject.Modbus.CRC16;
import org.example.modbuddesktopproject.Modbus.ModbusFrame;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.models.ModbusRequestDTO;
import org.example.modbuddesktopproject.models.ModbusResponseDTO;

public class ModbusService {

    public ModbusResponseDTO readHoldingRegisters(ModbusRequestDTO request, SerialPort port) throws InterruptedException {
        byte[] req = ModbusFrame.readHoldingRegisters(request.getSlaveId(), request.getAddress(), request.getQuantity());
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
        if(!CRC16.validateCRC(res, bytesRead)){
            crcError = true;
        }
        else{
            crcError = false;
        }
        boolean modBusError;
        if(CRC16.isExceptionResponse(res)){
            modBusError = true;
        }
        else {
            modBusError = false;
        }
        int resValue = ModbusResponse.extractRegisterValue(res);
        return ModbusResponseDTO.builder()
                .slaveId(request.getSlaveId())
                .address(request.getAddress())
                .sentByteQuantity(sentBytes)
                .value(resValue)
                .sentBytes(req)
                .receivedBytes(res)
                .hasCrcError(crcError)
                .hasModbusError(modBusError)
                .build();
    }
}
