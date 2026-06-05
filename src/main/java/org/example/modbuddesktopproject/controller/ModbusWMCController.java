package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.Services.ModbusService;
import org.example.modbuddesktopproject.Services.SerialService;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRequestDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCResponseDTO;

import java.util.function.UnaryOperator;

public class ModbusWMCController {
    private final SerialService serialService = new SerialService();

    @FXML private ComboBox<String> comboPorts;
    @FXML private Button btnRefreshPorts;
    @FXML private TextField inputSlaveId;
    @FXML private TextField inputAddress;
    @FXML private TextField inputBitQuantity;
    @FXML private TextField inputCoils;
    @FXML private TextArea txtASentData;
    @FXML private TextArea txtAReceivedData;
    @FXML private TextArea txtAStatus;
    private SerialPort selectedPort;
    private int slaveId;
    private int bitQuantity;
    private int address;

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

    public void sendModbusWMCRequest() throws InterruptedException{

    }

    private Task<ModbusWMCResponseDTO> getModbusWMCResponseDTOTask(ModbusWMCRequestDTO request, Alert loading) {
        Task<ModbusWMCResponseDTO> task =
                new Task<>() {
                    @Override
                    protected ModbusWMCResponseDTO call() throws Exception {
                        return ModbusService.writeMultipleCoils(request, selectedPort);
                    }
                };

        task.setOnSucceeded(event -> {
            loading.close();

            ModbusWMCResponseDTO res = task.getValue();

            txtASentData.setText(ModbusResponse.printFrame(res.getSentBytes(), res.getSentBytes().length));
            txtAReceivedData.setText(ModbusResponse.printFrame(res.getReceivedBytes(), res.getReceivedBytes().length));
            StringBuilder errorMessage = new StringBuilder();
            if(res.isHasCrcError()){
                errorMessage.append("Erro no CRC. ");
            }
            if(res.isHasModbusError()){
                errorMessage.append("Erro no Modbus. ");
            }
            if(res.isHasAddressError()){
                errorMessage.append("Erro no endereço. ");
            }
            if(res.isHasQuantityError()){
                errorMessage.append("Erro na quantidade de bytes. ");
            }
            if(!res.isHasCrcError() && !res.isHasModbusError() && !res.isHasAddressError() && !res.isHasQuantityError()) {
                errorMessage.append("Sem erros");
            }
            txtAStatus.setText(errorMessage.toString());
        });

        task.setOnFailed(event -> {

            loading.close();

            task.getException()
                    .printStackTrace();
        });
        return task;
    }

    public void setSlaveId(){
        String text = inputSlaveId.getText();
        try {
            slaveId = Integer.parseInt(text);
            if (slaveId < 1 || slaveId > 247) {
                showError("O Slave ID deve estar entre 1 e 247.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("O Slave ID deve ser um número inteiro.");
        }
    }


    public void setBitQuantity(){
        String text = inputBitQuantity.getText();
        bitQuantity = Integer.parseInt(text);
        if(bitQuantity > 15) {
            showError("A Quantidade de Bits deve ser menor que 16.");
            return;
        }
    }

    public void setAddress(){
        String text = inputAddress.getText();
        address = Integer.parseInt(text);
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
