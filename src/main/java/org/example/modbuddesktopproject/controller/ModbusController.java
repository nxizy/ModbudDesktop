package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.Services.ModbusService;
import org.example.modbuddesktopproject.Services.SerialService;
import org.example.modbuddesktopproject.models.ModbusRequestDTO;
import org.example.modbuddesktopproject.models.ModbusResponseDTO;

import java.util.function.UnaryOperator;


public class ModbusController {

    private final SerialService serialService = new SerialService();

    @FXML private ComboBox<String> comboPorts;
    @FXML private Button btnRefreshPorts;
    @FXML private TextField inputSlaveId;
    @FXML private TextField inputAddress;
    @FXML private TextField inputBitQuantity;
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

    public void sendModbusRequest() throws InterruptedException {
        ModbusRequestDTO request = ModbusRequestDTO.builder()
                .slaveId(slaveId)
                .address(address)
                .quantity(bitQuantity)
                .build();
        Alert loading = new Alert(Alert.AlertType.INFORMATION);
        loading.setTitle("Carregando");
        loading.setHeaderText(null);
        loading.setContentText("Lendo registradores...");
        loading.show();
        Task<ModbusResponseDTO> task = getModbusResponseDTOTask(request, loading);
        new Thread(task).start();

    }

    private Task<ModbusResponseDTO> getModbusResponseDTOTask(ModbusRequestDTO request, Alert loading) {
        Task<ModbusResponseDTO> task =
                new Task<>() {
                    @Override
                    protected ModbusResponseDTO call()
                            throws Exception {

                        return ModbusService
                                .readHoldingRegisters(
                                        request,
                                        selectedPort
                                );
                    }
                };

        task.setOnSucceeded(event -> {

            loading.close();

            ModbusResponseDTO res =
                    task.getValue();

            txtASentData.setText(ModbusResponse.printFrame(res.getSentBytes(), res.getSentBytes().length));
            txtAReceivedData.setText(ModbusResponse.printFrame(res.getReceivedBytes(), res.getReceivedBytes().length));
            if(res.isHasCrcError()){
                txtAStatus.setText("Erro no CRC.");
            }
            if(res.isHasModbusError()){
                txtAStatus.setText("Erro no Modbus");
            }
            if(res.isHasModbusError() && res.isHasCrcError()){
                txtAStatus.setText("Erro no CRC e no Modbus");
            }
            txtAStatus.setText(String.valueOf(ModbusResponse.extractRegisterValue(res.getReceivedBytes())));
        });

        task.setOnFailed(event -> {

            loading.close();

            task.getException()
                    .printStackTrace();
        });
        return task;
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
