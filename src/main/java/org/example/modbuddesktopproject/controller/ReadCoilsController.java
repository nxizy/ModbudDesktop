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
import org.example.modbuddesktopproject.Services.ModbusService;
import org.example.modbuddesktopproject.Services.SerialService;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.*;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsRequestDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsResponseDTO;

import java.util.function.UnaryOperator;

public class ReadCoilsController {

    private final SerialService serialService = new SerialService();

    @FXML
    private ComboBox<String> comboPorts;
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

    public void sendReadCoilsRequest() throws InterruptedException {
        ReadCoilsRequestDTO request = ReadCoilsRequestDTO.builder()
                        .slaveId(slaveId)
                        .address(address)
                        .quantity(bitQuantity)
                        .build();

        Alert loading =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        loading.setTitle("Carregando");
        loading.setHeaderText(null);
        loading.setContentText("Lendo coils...");
        loading.show();

        Task<ReadCoilsResponseDTO> task =
                getReadCoilResponseDTOTask(
                        request,
                        loading
                );

        new Thread(task).start();
    }

    private Task<ReadCoilsResponseDTO> getReadCoilResponseDTOTask(ReadCoilsRequestDTO request, Alert loading){
        Task<ReadCoilsResponseDTO> task = new Task<>() {
            @Override
            protected ReadCoilsResponseDTO call() throws Exception {
                return ModbusService.readCoils(
                        request,
                        selectedPort
                );
            }
        };

        task.setOnSucceeded(event -> {

            loading.close();

            ReadCoilsResponseDTO res = task.getValue();

            txtASentData.setText(ModbusResponse.printFrame(
                    res.getSentBytes(),
                    res.getSentBytes().length
            ));

            txtAReceivedData.setText(
                    ModbusResponse.printFrame(
                            res.getReceivedBytes(),
                            res.getReceivedBytes().length
                    )
            );

            if (res.isHasModbusError() && res.isHasCrcError()) {
                txtAStatus.setText("Erro no CRC e no Modbus");
                return;
            }
            if (res.isHasModbusError()) {
                txtAStatus.setText("Erro no Modbus");
                return;
            }
            if (res.isHasCrcError()) {
                txtAStatus.setText("Erro no CRC");
                return;
            }

            txtAStatus.setText(
                    ModbusResponse.extractCoils(
                            res.getCoilBytes(),
                            res.getAddress(

                            ),
                            res.getQuantity()
                    )
            );


        });

        task.setOnFailed(event -> {

            loading.close();

            task.getException()
                    .printStackTrace();
        });

        return task;
    }

}
