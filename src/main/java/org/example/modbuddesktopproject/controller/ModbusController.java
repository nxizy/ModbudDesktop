package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.modbuddesktopproject.Services.SerialService;

import java.util.function.UnaryOperator;


public class ModbusController {

    private final SerialService serialService = new SerialService();

    @FXML private ComboBox<String> comboPorts;
    @FXML private Button btnRefreshPorts;
    @FXML private TextField inputSlaveId;
    @FXML private TextField inputAddress;
    @FXML private TextField inputBitQuantity;
    private SerialPort selectedPort;
    private int slaveId;

    public void initialize(){
        btnRefreshPorts.setOnAction(e -> loadPorts());
        comboPorts.setOnAction(e -> setSelectedPort());
        loadPorts();

        // Filtro para slaveId
        UnaryOperator<TextFormatter.Change> slaveFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change; // Accept change
            }
            return null; // Reject change
        };
        //Filtro para endereco
        UnaryOperator<TextFormatter.Change> enderecoFilter = change -> {
            String newText = change.getControlNewText();

            if (newText.matches("\\d{0,4}")) {
                return change;
            }

            return null;
        };

        inputSlaveId.setTextFormatter(new TextFormatter<>(slaveFilter));
        inputAddress.setTextFormatter(new TextFormatter<>(enderecoFilter));

    }

    public void setSelectedPort(){
        selectedPort = serialService.getPort(comboPorts.getValue());
        System.out.println(selectedPort);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Porta selecionada");
        alert.setHeaderText(null);
        alert.setContentText(comboPorts.getValue());
        alert.showAndWait();
    }

    public void setSlaveId(){
        String text = inputSlaveId.getText();
        try {
            int slaveId = Integer.parseInt(text);
            if (slaveId < 1 || slaveId > 247) {
                showError("O Slave ID deve estar entre 1 e 247.");
                return;
            }

            // Continua o envio da requisição
//            sendModbusRequest(slaveId);

        } catch (NumberFormatException e) {
            showError("O Slave ID deve ser um número inteiro.");
        }
    }


    public void setBitQuantity(){
        String text = inputBitQuantity.getText();
        int bitQuantity = Integer.parseInt(text);
        if(bitQuantity > 15) {
            showError("A Quantidade de Bits deve ser menor que 16.");
            return;
        }
    }

    private void loadPorts(){
        comboPorts.getItems().clear();

        var ports = serialService.getAvailablePorts();
        comboPorts.getItems().addAll(ports);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
