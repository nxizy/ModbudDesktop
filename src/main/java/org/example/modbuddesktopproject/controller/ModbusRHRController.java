package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.modbuddesktopproject.HelloApplication;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.Services.ENUMs.Protocol;
import org.example.modbuddesktopproject.Services.MasterService;
import org.example.modbuddesktopproject.Services.MasterTransport;
import org.example.modbuddesktopproject.Services.ModbusService;
import org.example.modbuddesktopproject.Services.SerialService;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRRTURequestDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRRTUResponseDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRTCPRequestDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRTCPResponseDTO;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;


public class ModbusRHRController {

    private final SerialService serialService = new SerialService();

    @FXML private TextField inputSlaveId;
    @FXML private TextField inputAddress;
    @FXML private TextField inputBitQuantity;
    @FXML private TextArea txtAReceivedData;
    @FXML private TextArea txtAStatus;
    @FXML private TextField modeSelected;
    private int slaveId;
    private int bitQuantity;
    private int address;

    @FXML
    public void irParaMain(ActionEvent event) {
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

    public void initialize() {
        modeSelected.setText("Modo " + AppContext.getInstance().getTransport().whichProtocol() + " selecionado!");
        boolean tcp = AppContext.getInstance().getTransport().whichProtocol() == Protocol.TCP;
        boolean rtu = AppContext.getInstance().getTransport().whichProtocol() == Protocol.RTU;

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
        ModbusRHRRTURequestDTO request = ModbusRHRRTURequestDTO.builder()
                .slaveId(slaveId)
                .address(address)
                .quantity(bitQuantity)
                .build();
        int transactionId = ThreadLocalRandom.current().nextInt(0, 65536);
        ModbusRHRTCPRequestDTO requestTCP = ModbusRHRTCPRequestDTO.builder()
                .transactionId(transactionId)
                .unitId(slaveId)
                .startAddress(address)
                .quantity(bitQuantity)
                .build();
        Alert loading = new Alert(Alert.AlertType.INFORMATION);
        loading.setTitle("Carregando");
        loading.setHeaderText(null);
        loading.setContentText("Lendo registradores...");
        loading.show();
        switch(AppContext.getInstance().getTransport().whichProtocol()){
            case TCP:
                Task<ModbusRHRTCPResponseDTO> taskTCP = getModbusTCPResponseDTOTask(requestTCP, loading);
                new Thread(taskTCP).start();
                break;
            case RTU:
                Task<ModbusRHRRTUResponseDTO> taskRTU = getModbusResponseDTOTask(request, loading);
                new Thread(taskRTU).start();
                break;
        }
    }

    private Task<ModbusRHRRTUResponseDTO> getModbusResponseDTOTask(ModbusRHRRTURequestDTO request, Alert loading) {

        Task<ModbusRHRRTUResponseDTO> task =
                new Task<>() {
                    @Override
                    protected ModbusRHRRTUResponseDTO call() throws Exception {
                        SerialPort port = AppContext.getInstance().getSerialPort();
                        return ModbusService.readHoldingRegisters(request, port);
                    }
                };

        task.setOnSucceeded(event -> {

            loading.close();

            ModbusRHRRTUResponseDTO res = task.getValue();
            txtAReceivedData.setText(ModbusResponse.printFrame(res.getReceivedBytes(), res.getReceivedBytes().length));
            if(res.isHasError()){
                txtAStatus.setText("Erro, verifique o CRC, e o estado de comunicação do escravo.");
            }
            txtAStatus.setText(String.valueOf(res.getValue()));
        });

        task.setOnFailed(event -> {

            loading.close();

            task.getException()
                    .printStackTrace();
        });
        return task;
    }

    private Task<ModbusRHRTCPResponseDTO> getModbusTCPResponseDTOTask(ModbusRHRTCPRequestDTO request, Alert loading) {

        Task<ModbusRHRTCPResponseDTO> task =
                new Task<>() {
                    @Override
                    protected ModbusRHRTCPResponseDTO call() throws Exception {
                        MasterTransport transport = AppContext.getInstance().getTransport();
                        MasterService service = new MasterService(transport);
                        return service.readHoldingRegistersTCP(request);
                    }
                };

        task.setOnSucceeded(event -> {

            loading.close();

            ModbusRHRTCPResponseDTO res = task.getValue();
            txtAReceivedData.setText(ModbusResponse.printFrame(res.getReceivedBytes(), res.getReceivedBytes().length));
            txtAStatus.setText(String.valueOf(res.getValue()));
        });

        task.setOnFailed(event -> {

            loading.close();

            task.getException()
                    .printStackTrace();
        });
        return task;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
