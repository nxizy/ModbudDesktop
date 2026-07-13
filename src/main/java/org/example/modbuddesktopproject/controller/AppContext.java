package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import org.example.modbuddesktopproject.Services.MasterService;
import org.example.modbuddesktopproject.Services.MasterTransport;
import org.example.modbuddesktopproject.Services.RTUTransport;
import org.example.modbuddesktopproject.Services.TCPTransport;

public class AppContext {
    private static final AppContext INSTANCE = new AppContext();

    private MasterTransport transport;

    private SerialPort serialPort;

    private String ip;

    private int tcpPort;

    public void connectRTU(SerialPort port) {

        disconnect();

        if (!port.openPort()) {
            throw new RuntimeException("Erro ao abrir porta");
        }

        getInstance().serialPort = port;
        getInstance().setTransport(new RTUTransport(port));
    }

    public void connectTCP(String ip, int port) {

        disconnect();

        getInstance().ip = ip;
        getInstance().tcpPort = port;
        getInstance().setTransport(new TCPTransport(ip, port));
    }

    //Esse disconnect foi implementado, mas no momento atual da aplicação as funções não usam ativamente ele, pois a alteração de protocolo não pode ser feita em tempo de execução
    public void disconnect() {

        if (getInstance().serialPort != null && getInstance().serialPort.isOpen()) {
            getInstance().serialPort.closePort();
        }

        getInstance().serialPort = null;
        getInstance().transport = null;
        getInstance().ip = null;
        getInstance().tcpPort = 0;
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public void setTransport(MasterTransport transport) {
        this.transport = transport;
    }

    public MasterTransport getTransport() {
        return transport;
    }
}
