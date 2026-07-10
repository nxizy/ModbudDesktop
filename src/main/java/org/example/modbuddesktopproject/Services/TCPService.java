package org.example.modbuddesktopproject.Services;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class TCPService {

    public static byte[] sendRequest(
            String ip,
            int port,
            byte[] request
    ) {

        try (Socket socket = new Socket(ip, port)){

            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();

            // Envia o frame
            output.write(request);
            output.flush();

            // Buffer da resposta
            byte[] response = new byte[260];

            int bytesRead = input.read(response);

            return Arrays.copyOf(response,bytesRead);

        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

}
