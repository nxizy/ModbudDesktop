package org.example.modbuddesktopproject.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.modbuddesktopproject.HelloApplication;
import org.example.modbuddesktopproject.Modbus.ModbusFrame;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.Modbus.ModbusResponseTCP;
import org.example.modbuddesktopproject.Services.TCPService;

public class TCPController {

    @FXML
    public void voltarParaMenu(ActionEvent event) {
        try{
            Stage janela = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            janela.close();

            Parent root = FXMLLoader.load(HelloApplication.class.getResource("Main.fxml"));

            Stage novaJanela = new Stage();
            novaJanela.setScene(new Scene(root));
            novaJanela.setTitle("Modbud - A Modbus Communication Project!");
            novaJanela.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void readHoldingRegisterTCP(ActionEvent event) {

        try {

            byte[] request = ModbusFrame.readHoldingRegistersTCP(
                    1,
                    1,
                    0,
                    2
            );

            byte[] response = TCPService.sendRequest(
                    "localhost",
                    502,
                    request
            );

            // Frame completo
            System.out.println("Frame recebido:");
            ModbusResponseTCP.printFrame(response);

            // Cabeçalho MBAP
            System.out.println("Transaction ID: " +
                    ModbusResponseTCP.getTransactionId(response));

            System.out.println("Protocol ID: " +
                    ModbusResponseTCP.getProtocolId(response));

            System.out.println("Length: " +
                    ModbusResponseTCP.getLength(response));

            System.out.println("Unit ID: " +
                    ModbusResponseTCP.getUnitId(response));

            System.out.println("Function Code: " +
                    ModbusResponseTCP.getFunctionCode(response));

            // Verifica exceção
            if (ModbusResponseTCP.isException(response)) {

                System.out.println(
                        "Exception Code: " +
                                ModbusResponseTCP.getExceptionCode(response)
                );

                return;
            }

            // Registradores
            int[] registers =
                    ModbusResponseTCP.extractRegisters(response);

            for (int i = 0; i < registers.length; i++) {
                System.out.println(
                        "Holding Register " + i + ": " +
                                registers[i]
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void readCoilsTCP(ActionEvent event){

        try {

            int startAddress = 0;
            int quantity = 2;

            byte[] request = ModbusFrame.readCoilsTCP(
                    1,
                    1,
                    startAddress,
                    quantity
            );

            byte[] response = TCPService.sendRequest(
                    "localhost",
                    502,
                    request
            );

            // Frame completo
            System.out.println("Frame recebido:");
            ModbusResponseTCP.printFrame(response);

            // MBAP
            System.out.println("Transaction ID: " +
                    ModbusResponseTCP.getTransactionId(response));

            System.out.println("Protocol ID: " +
                    ModbusResponseTCP.getProtocolId(response));

            System.out.println("Length: " +
                    ModbusResponseTCP.getLength(response));

            System.out.println("Unit ID: " +
                    ModbusResponseTCP.getUnitId(response));

            System.out.println("Function Code: " +
                    ModbusResponseTCP.getFunctionCode(response));

            // Exception Response
            if (ModbusResponseTCP.isException(response)) {

                System.out.println("Exception Code: " + ModbusResponseTCP.getExceptionCode(response));

                return;
            }

            // Byte Count
            int byteCount = ModbusResponseTCP.getByteCount(response);

            // Dados das coils (após o Byte Count)
            byte[] coilBytes = new byte[byteCount];
            System.arraycopy(response, 9, coilBytes, 0, byteCount);

            // Reutiliza o método já existente
            String coils = ModbusResponse.extractCoils(coilBytes, startAddress, quantity);

            System.out.println(coils);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void writeMultipleCoilsTCP(ActionEvent event){

        try {

            boolean[] coils = {
                    true,
                    false,
                    true,
                    true,
                    false,
                    false,
                    true,
                    false,
                    true,
                    false
            };

            byte[] request = ModbusFrame.writeMultipleCoilsTCP(
                    1,
                    1,
                    0,
                    coils.length,
                    coils
            );

            byte[] response = TCPService.sendRequest(
                    "localhost",
                    502,
                    request
            );

            // Frame completo
            System.out.println("Frame recebido:");
            ModbusResponseTCP.printFrame(response);

            // MBAP
            System.out.println("Transaction ID: " +
                    ModbusResponseTCP.getTransactionId(response));

            System.out.println("Protocol ID: " +
                    ModbusResponseTCP.getProtocolId(response));

            System.out.println("Length: " +
                    ModbusResponseTCP.getLength(response));

            System.out.println("Unit ID: " +
                    ModbusResponseTCP.getUnitId(response));

            System.out.println("Function Code: " +
                    ModbusResponseTCP.getFunctionCode(response));

            System.out.println("Start Address: " +
                    ModbusResponseTCP.getStartAddress(response));

            System.out.println("Quantidade escrita: " +
                    ModbusResponseTCP.getQuantity(response));

            // Exception Response
            if (ModbusResponseTCP.isException(response)) {

                System.out.println("Exception Code: " + ModbusResponseTCP.getExceptionCode(response));

                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
