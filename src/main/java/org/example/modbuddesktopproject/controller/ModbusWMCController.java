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
import org.example.modbuddesktopproject.Services.ModbusService;
import org.example.modbuddesktopproject.Services.SerialService;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCRequestDTO;
import org.example.modbuddesktopproject.models.WriteMultipleCoils.ModbusWMCResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class ModbusWMCController {
    private final SerialService serialService = new SerialService();

    @FXML private ComboBox<String> comboPorts;
    @FXML private Button btnRefreshPorts;
    @FXML private TextField inputSlaveId;
    @FXML private TextField inputAddress;
    @FXML private TextField inputBitQuantity;
    @FXML private TextArea txtASentData;
    @FXML private TextArea txtAReceivedData;
    @FXML private TextArea txtAStatus;
    @FXML private GridPane gridCoils;
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
        ModbusWMCRequestDTO req = ModbusWMCRequestDTO.builder()
                .slaveId(slaveId)
                .address(address)
                .quantity(bitQuantity)
                .coils(coils)
                .build();
        Alert loading = new Alert(Alert.AlertType.INFORMATION);
        loading.setTitle("Carregando");
        loading.setHeaderText(null);
        loading.setContentText("Escrevendo nas bobinas...");
        loading.show();
        Task<ModbusWMCResponseDTO> task = getModbusWMCResponseDTOTask(req, loading);
        new Thread(task).start();
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
