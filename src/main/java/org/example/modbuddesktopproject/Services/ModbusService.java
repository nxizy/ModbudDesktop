package org.example.modbuddesktopproject.Services;

import com.fazecast.jSerialComm.SerialPort;
import javafx.concurrent.Task;
import org.example.modbuddesktopproject.Modbus.CRC16;
import org.example.modbuddesktopproject.Modbus.ModbusFrame;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.models.ModbusRequestDTO;
import org.example.modbuddesktopproject.models.ModbusResponseDTO;
import org.example.modbuddesktopproject.models.ReadCoilsRequestDTO;
import org.example.modbuddesktopproject.models.ReadCoilsResponseDTO;

public class ModbusService {

    public static ModbusResponseDTO readHoldingRegisters(ModbusRequestDTO request, SerialPort port) throws InterruptedException {
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

    public static ReadCoilsResponseDTO readCoils(ReadCoilsRequestDTO request, SerialPort port) throws InterruptedException {
        byte[] req = ModbusFrame.readCoils(request.getSlaveId(), request.getAddress(), request.getQuantity());

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

        // Calcula quantos bytes serão retornados para os coils
        int dataBytes = (request.getQuantity() + 7) / 8;

        byte[] res = new byte[5 + dataBytes];

        int bytesRead = port.readBytes(
                res,
                res.length
        );

        if (bytesRead < 5) {
            throw new RuntimeException(
                    "Resposta Modbus inválida"
            );
        }

        boolean crcError = !CRC16.validateCRC(
                res,
                bytesRead
        );

        boolean modBusError = CRC16.isExceptionResponse(
                res
        );

        byte[] coilBytes = new byte[0];

        if (!modBusError) {

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

        return ReadCoilsResponseDTO.builder()
                .slaveId(request.getSlaveId())
                .address(request.getAddress())
                .quantity(request.getQuantity())
                .coilBytes(coilBytes)
                .sentBytes(req)
                .receivedBytes(res)
                .hasCrcError(crcError)
                .hasModbusError(modBusError)
                .build();
    }
}
