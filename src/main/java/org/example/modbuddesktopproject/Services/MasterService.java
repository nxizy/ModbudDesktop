package org.example.modbuddesktopproject.Services;

import org.example.modbuddesktopproject.Modbus.ModbusRequestFrameCreator;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsRTURequestDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsRTUResponseDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsTCPRequestDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsTCPResponseDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRRTURequestDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRRTUResponseDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRTCPRequestDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRTCPResponseDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRTURequestDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRTUResponseDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCTCPRequestDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCTCPResponseDTO;

import java.io.IOException;

public class MasterService {
    private final MasterTransport transport;

    public MasterService(MasterTransport transport){
        this.transport = transport;
    }

    public ReadCoilsRTUResponseDTO readCoilsRTU(ReadCoilsRTURequestDTO req) throws IOException, InterruptedException {
        byte[] reqFrame = ModbusRequestFrameCreator.readCoils(req.getSlaveId(), req.getAddress(), req.getQuantity());
        byte[] resFrame = transport.sendRequest(reqFrame);
        return ResponseParser.readCoilsRTUParser(resFrame);
    }

    public ModbusRHRRTUResponseDTO readHoldingRegistersRTU(ModbusRHRRTURequestDTO req) throws IOException, InterruptedException {
        byte[] reqFrame = ModbusRequestFrameCreator.readHoldingRegisters(req.getSlaveId(), req.getAddress(), req.getQuantity());
        byte[] resFrame = transport.sendRequest(reqFrame);
        return ResponseParser.readHoldingRegistersRTUParser(resFrame);
    }

    public ModbusWMCRTUResponseDTO writeMultipleCoilsRTU(ModbusWMCRTURequestDTO req) throws IOException, InterruptedException {
        byte[] reqFrame = ModbusRequestFrameCreator.writeMultipleCoils(req.getSlaveId(), req.getAddress(), req.getQuantity(), req.getCoils());
        byte[] resFrame = transport.sendRequest(reqFrame);
        return ResponseParser.writeMultipleCoilsRTUParser(resFrame);
    }

    public ReadCoilsTCPResponseDTO readCoilsTCP(ReadCoilsTCPRequestDTO req) throws IOException, InterruptedException {
        byte[] reqFrame = ModbusRequestFrameCreator.readCoilsTCP(req.getTransactionId(), req.getUnitId(), req.getStartAddress(), req.getQuantity());
        byte[] resFrame = transport.sendRequest(reqFrame);
        return ResponseParser.readCoilsTCPParser(resFrame);
    }

    public ModbusRHRTCPResponseDTO readHoldingRegistersTCP(ModbusRHRTCPRequestDTO req) throws IOException, InterruptedException {
        byte[] reqFrame = ModbusRequestFrameCreator.readHoldingRegistersTCP(req.getTransactionId(), req.getUnitId(), req.getStartAddress(), req.getQuantity());
        byte[] resFrame = transport.sendRequest(reqFrame);
        return ResponseParser.readHoldingRegistersTCPParser(resFrame);
    }

    public ModbusWMCTCPResponseDTO writeMultipleCoilsTCP(ModbusWMCTCPRequestDTO req) throws IOException, InterruptedException {
        byte[] reqFrame = ModbusRequestFrameCreator.writeMultipleCoilsTCP(req.getTransactionId(), req.getUnitId(), req.getStartAddress(), req.getQuantity(), req.getCoils());
        byte[] resFrame = transport.sendRequest(reqFrame);
        return ResponseParser.writeMultipleCoilsTCPParser(resFrame);
    }

}
