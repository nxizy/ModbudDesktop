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
}
