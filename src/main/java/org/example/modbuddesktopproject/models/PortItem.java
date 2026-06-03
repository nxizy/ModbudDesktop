package org.example.modbuddesktopproject.models;

import com.fazecast.jSerialComm.SerialPort;

public class PortItem {
    private final SerialPort port;

    public PortItem(SerialPort port) {
        this.port = port;
    }

    public SerialPort getPort() {
        return port;
    }

    @Override
    public String toString() {
        return port.getSystemPortName() +
                " - " +
                port.getDescriptivePortName();
    }
}
