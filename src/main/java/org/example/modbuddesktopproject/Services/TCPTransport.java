package org.example.modbuddesktopproject.Services;

import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.Services.ENUMs.Protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class TCPTransport implements MasterTransport{

    private final String ip;
    private final int port;

    public TCPTransport(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public byte[] sendRequest(
            byte[] req
    ) {
        int functionCode = req[7] & 0xFF;
        return switch(functionCode) {
            case 1 -> readCoilsTCP(req);
            case 3 -> readHoldingRegistersTCP(req);
            case 15 -> writeMultipleCoilsTCP(req);
            default -> null;
        };
    }

    @Override
    public Protocol whichProtocol() {
        return Protocol.TCP;
    }

    public byte[] readCoilsTCP(byte[] req) {
        return requestDataToSlave(req);
    }

    public byte[] readHoldingRegistersTCP(byte[] req) {
        return requestDataToSlave(req);
    }

    public byte[] writeMultipleCoilsTCP(byte[] req) {
        return requestDataToSlave(req);
    }

    public byte[] requestDataToSlave(byte[] req) {
        try (Socket socket = new Socket(ip, port)){

            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();

            // Envia o frame
            output.write(req);
            output.flush();

            System.out.println("Requisicao");
            ModbusResponse.printFrame(req, req.length);
            // Buffer da resposta
            byte[] response = new byte[260];

            int bytesRead = input.read(response);
            System.out.println("Resposta");
            ModbusResponse.printFrame(response, bytesRead);
            return Arrays.copyOf(response,bytesRead);

        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
