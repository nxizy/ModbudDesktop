package org.example.modbuddesktopproject.Services;

import com.fazecast.jSerialComm.SerialPort;
import org.example.modbuddesktopproject.Modbus.CRC16;
import org.example.modbuddesktopproject.Modbus.ModbusRequestFrameCreator;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.*;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsRTURequestDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsRTUResponseDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRTURequestDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRTUResponseDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModbusService {
    private static final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private static final Object lock = new Object();

    public static ModbusRHRRTUResponseDTO readHoldingRegisters(ModbusRHRRTURequestDTO request, SerialPort port) throws InterruptedException {
        if (!port.isOpen()) {
            throw new IllegalStateException(
                    "Porta serial não está aberta"
            );
        }
        byte[] req = ModbusRequestFrameCreator.readHoldingRegisters(request.getSlaveId(), request.getAddress(), request.getQuantity());
        byte functionCode = req[1];
        port.flushIOBuffers();
        int sentBytes = port.writeBytes(req, req.length);
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
        return ModbusRHRRTUResponseDTO.builder()
                .slaveId(request.getSlaveId())
                .address(request.getAddress())
                .value(resValue)
                .receivedBytes(res)
                .hasError(modBusError || crcError)
                .build();
    }

    public static ModbusWMCRTUResponseDTO writeMultipleCoils(ModbusWMCRTURequestDTO request, SerialPort port) throws InterruptedException{
        if (!port.isOpen()) {
            throw new IllegalStateException(
                    "Porta serial não está aberta"
            );
        }
        byte[] req = ModbusRequestFrameCreator.writeMultipleCoils(request.getSlaveId(), request.getAddress(), request.getQuantity(), request.getCoils());
        byte functionCode = req[1];
        port.flushIOBuffers();
        int sentBytes = port.writeBytes(req, req.length);
        byte[] res = new byte[8];
        int bytesRead = port.readBytes(
                res,
                res.length
        );
        boolean exceptionResponse = CRC16.isExceptionResponse(res);

        boolean crcError;

        if (exceptionResponse) {
            crcError = !CRC16.validateCRC(res, 5);
        } else {
            crcError = !CRC16.validateCRC(res, res.length);
        }
        boolean modBusError = CRC16.isExceptionResponse(res);

        boolean addressError = false;
        boolean quantityError = false;
        if (!modBusError && bytesRead >= 8) {
            addressError =
                    rebuildBytes(res[2], res[3]) != request.getAddress();

            quantityError =
                    rebuildBytes(res[4], res[5]) != request.getQuantity();
        }
        return ModbusWMCRTUResponseDTO.builder()
                .slaveId(request.getSlaveId())
                .address(request.getAddress())
                .receivedBytes(res)
                .hasError(addressError || quantityError || modBusError || crcError)
                .build();
    }

    public static ReadCoilsRTUResponseDTO readCoils(
            ReadCoilsRTURequestDTO request,
            SerialPort port
    ) throws InterruptedException, IOException {

        if (!port.isOpen()) {
            throw new IllegalStateException("Porta serial não está aberta");
        }

        // 1. monta requisição
        byte[] req = ModbusRequestFrameCreator.readCoils(
                request.getSlaveId(),
                request.getAddress(),
                request.getQuantity()
        );

        synchronized (lock) {
            buffer.reset();

            port.flushIOBuffers();
            port.writeBytes(req, req.length);

            long start = System.currentTimeMillis();

            byte[] frame = null;

            // 2. loop esperando frame completo
            while (System.currentTimeMillis() - start < 2000) { // timeout 2s

                Thread.sleep(5);

                int available = port.bytesAvailable();
                if (available > 0) {

                    byte[] temp = new byte[available];
                    port.readBytes(temp, temp.length);
                    buffer.write(temp);
                }

                byte[] current = buffer.toByteArray();

                frame = tryBuildFrameFC01(current);

                if (frame != null) break;
            }

            if (frame == null) {
                throw new RuntimeException("Timeout esperando resposta Modbus");
            }

            // 3. valida CRC
            boolean crcError = !CRC16.validateCRC(frame, frame.length);
            boolean modbusError = CRC16.isExceptionResponse(frame);

            byte[] coilBytes = new byte[0];

            if (!modbusError) {
                int byteCount = frame[2] & 0xFF;

                coilBytes = new byte[byteCount];

                System.arraycopy(
                        frame,
                        3,
                        coilBytes,
                        0,
                        byteCount
                );
            }

            System.out.println("REQ: " + Arrays.toString(req));
            System.out.println("RESP: " + Arrays.toString(frame));

            return ReadCoilsRTUResponseDTO.builder()
                    .slaveId(request.getSlaveId())
                    .address(request.getAddress())
                    .quantity(request.getQuantity())
                    .coilBytes(coilBytes)
                    .receivedBytes(frame)
                    .hasError(modbusError || crcError)
                    .build();
        }
    }

    private static byte[] tryBuildFrameFC01(byte[] data) {

        if (data.length < 5) return null;

        int byteCount = data[2] & 0xFF;

        int expectedSize = 3 + byteCount + 2;

        if (data.length < expectedSize) {
            return null; // ainda não chegou tudo
        }

        return Arrays.copyOfRange(data, 0, expectedSize);
    }

    public static List<Boolean> extractCoils(byte[] coilBytes, int quantity) {
        List<Boolean> coils = new ArrayList<>();

        if (coilBytes == null || coilBytes.length == 0) {
            return coils;
        }


        for (int i = 0; i < quantity; i++) {

            int byteIndex = i / 8;
            int bitIndex = i % 8;

            boolean value =
                    ((coilBytes[byteIndex] & 0xFF) & (1 << bitIndex))
                            != 0;

            coils.add(value);
        }

        return coils;
    }

    private static int rebuildBytes(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }
}
