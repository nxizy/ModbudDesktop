package org.example.modbuddesktopproject.Services;

import com.fazecast.jSerialComm.SerialPort;
import org.example.modbuddesktopproject.Services.ENUMs.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RTUTransport implements MasterTransport{
    private final SerialPort port;
    private static final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private static final Object lock = new Object();

    public RTUTransport(SerialPort port) {
        this.port = port;
    }

    @Override
    public byte[] sendRequest(byte[] req) throws IOException, InterruptedException {
        int functionCode = req[1] & 0xFF;
        return switch (functionCode) {
            case 1 -> readCoils(req, port);
            case 3 -> readHoldingRegisters(req, port);
            case 15 -> writeMultipleCoils(req, port);
            default -> null;
        };
    }

    @Override
    public Protocol whichProtocol() {
        return Protocol.RTU;
    }


    public byte[] readHoldingRegisters(byte[] req, SerialPort port){
        if (!port.isOpen()) {
            throw new IllegalStateException(
                    "Porta serial não está aberta"
            );
        }
        port.flushIOBuffers();
        port.writeBytes(req, req.length);
        byte[] res = new byte[8];
        port.readBytes(
                res,
                res.length
        );
        return res;
    }

    public byte[] writeMultipleCoils(byte[] req, SerialPort port){
        if (!port.isOpen()) {
            throw new IllegalStateException(
                    "Porta serial não está aberta"
            );
        }
        port.flushIOBuffers();
        port.writeBytes(req, req.length);
        byte[] res = new byte[8];
        port.readBytes(
                res,
                res.length
        );
        return res;
    }

    public static byte[] readCoils(byte[] req, SerialPort port) throws InterruptedException, IOException {
        if (!port.isOpen()) {
            throw new IllegalStateException("Porta serial não está aberta");
        }

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
            return frame;
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
