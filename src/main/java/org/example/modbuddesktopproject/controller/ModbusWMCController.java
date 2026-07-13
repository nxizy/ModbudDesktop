package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.modbuddesktopproject.HelloApplication;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.Services.MasterService;
import org.example.modbuddesktopproject.Services.MasterTransport;
import org.example.modbuddesktopproject.Services.ModbusService;
import org.example.modbuddesktopproject.Services.SerialService;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRRTUResponseDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRHRTCPResponseDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRTURequestDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRTUResponseDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCTCPRequestDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCTCPResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;

public class ModbusWMCController {
    private final SerialService serialService = new SerialService();

    @FXML private TextField inputSlaveId;
    @FXML private TextField inputAddress;
    @FXML private TextField inputBitQuantity;
    @FXML private TextArea txtAReceivedData;
    @FXML private TextArea txtAStatus;
    @FXML private GridPane gridCoils;
    @FXML private TextField modeSelected;
    private SerialPort selectedPort;
    private int slaveId;
    private int bitQuantity;
    private int address;
    private final List<CheckBox> coilCheckBoxes = new ArrayList<>();

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

    public void initialize(){
        modeSelected.setText("Modo " + AppContext.getInstance().getTransport().whichProtocol() + " selecionado!");

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
        // Filtro para bitQuantity
        UnaryOperator<TextFormatter.Change> bitQuantityFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change; // Accept change
            }
            return null; // Reject change
        };

        inputSlaveId.setTextFormatter(new TextFormatter<>(slaveFilter));
        inputAddress.setTextFormatter(new TextFormatter<>(enderecoFilter));
        inputBitQuantity.setTextFormatter(new TextFormatter<>(bitQuantityFilter));
    }

    public void sendModbusWMCRequest() throws InterruptedException{
        boolean[] coils = new boolean[coilCheckBoxes.size()];
        for (int i = 0; i < coilCheckBoxes.size(); i++) {
            coils[i] = coilCheckBoxes.get(i).isSelected();
        }
        ModbusWMCRTURequestDTO req = ModbusWMCRTURequestDTO.builder()
                .slaveId(slaveId)
                .address(address)
                .quantity(bitQuantity)
                .coils(coils)
                .build();

        int transactionId = ThreadLocalRandom.current().nextInt(0, 65536);
        ModbusWMCTCPRequestDTO reqTCP = ModbusWMCTCPRequestDTO.builder()
                .transactionId(transactionId)
                .unitId(slaveId)
                .startAddress(address)
                .quantity(bitQuantity)
                .coils(coils)
                .build();
        Alert loading = new Alert(Alert.AlertType.INFORMATION);
        loading.setTitle("Carregando");
        loading.setHeaderText(null);
        loading.setContentText("Escrevendo nas bobinas...");

        loading.show();
        switch(AppContext.getInstance().getTransport().whichProtocol()){
            case TCP:
                Task<ModbusWMCTCPResponseDTO> taskTCP = getModbusWMCResponseTCPDTOTask(reqTCP, loading);
                new Thread(taskTCP).start();
                break;
            case RTU:
                Task<ModbusWMCRTUResponseDTO> taskRTU = getModbusWMCResponseDTOTask(req, loading);
                new Thread(taskRTU).start();
                break;
        }
    }

    private Task<ModbusWMCRTUResponseDTO> getModbusWMCResponseDTOTask(ModbusWMCRTURequestDTO request, Alert loading) {
        Task<ModbusWMCRTUResponseDTO> task =
                new Task<>() {
                    @Override
                    protected ModbusWMCRTUResponseDTO call() throws Exception {
                        SerialPort port = AppContext.getInstance().getSerialPort();
                        return ModbusService.writeMultipleCoils(request, port);
                    }
                };

        task.setOnSucceeded(event -> {
            loading.close();

            ModbusWMCRTUResponseDTO res = task.getValue();
            txtAReceivedData.setText(ModbusResponse.printFrame(res.getReceivedBytes(), res.getReceivedBytes().length));
            StringBuilder errorMessage = new StringBuilder();
            if(res.isHasError()){
                errorMessage.append("Erro, verifique o CRC, modbus, o endereço e/ou a quantidade de bytes. ");
            }
            if(!res.isHasError()) {
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

    private Task<ModbusWMCTCPResponseDTO> getModbusWMCResponseTCPDTOTask(ModbusWMCTCPRequestDTO request, Alert loading) {
        Task<ModbusWMCTCPResponseDTO> task =
                new Task<>() {
                    @Override
                    protected ModbusWMCTCPResponseDTO call() throws Exception {
                        MasterTransport transport = AppContext.getInstance().getTransport();
                        MasterService service = new MasterService(transport);
                        return service.writeMultipleCoilsTCP(request);
                    }
                };

        task.setOnSucceeded(event -> {
            loading.close();

            ModbusWMCTCPResponseDTO res = task.getValue();
            txtAReceivedData.setText(ModbusResponse.printFrame(res.getReceivedBytes(), res.getReceivedBytes().length));
            txtAStatus.setText("Sem erros");
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

    public void generateCoils(){
        coilCheckBoxes.clear();
        gridCoils.getRowConstraints().clear();
        gridCoils.getColumnConstraints().clear();
        gridCoils.getChildren().clear();
        gridCoils.setVgap(3);
        gridCoils.setHgap(6);
        for (int i = 0; i < bitQuantity; i++) {
            CheckBox checkBox = new CheckBox();
            coilCheckBoxes.add(checkBox);
            int row = i % 8;
            int col = i / 8;
            HBox coilHBox = new HBox(
                    6,
                    new Label("Bobina " + i),
                    checkBox
            );
            coilHBox.setMinHeight(30);
            gridCoils.add(coilHBox, col, row);
        }
    }


    public void setBitQuantity(){
        String text = inputBitQuantity.getText();
        bitQuantity = Integer.parseInt(text);
    }

    public void setAddress(){
        String text = inputAddress.getText();
        address = Integer.parseInt(text);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
