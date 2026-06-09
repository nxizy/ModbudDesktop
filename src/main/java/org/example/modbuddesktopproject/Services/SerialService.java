package org.example.modbuddesktopproject.Services;

import com.fazecast.jSerialComm.SerialPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SerialService {

    public SerialPort getPort(String port){
        return SerialPort.getCommPort(port);
    }

    public List<String> getAvailablePorts(){
        SerialPort[] ports = SerialPort.getCommPorts();
        List<String> portsString = new ArrayList<>();
        for(SerialPort port: ports){
            portsString.add(
                    port.getSystemPortName()
            );
        }
        return portsString;
    }

    public static boolean openPort(SerialPort port){
        port.openPort();
        return port.isOpen();
    }

    public static void connect(SerialPort port) {
        port.setComPortParameters(
                9600,
                8,
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY
        );
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING,
                400,
                0
        );
        port.openPort();
        boolean opened = port.isOpen();
        if (!opened) {
            throw new RuntimeException(
                    "Não foi possível abrir a porta " +
                            port.getSystemPortName()
            );
        }
    }

    public static void disconnect(SerialPort port) {
        port.closePort();
    }
}
