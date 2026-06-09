package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.modbuddesktopproject.HelloApplication;
import org.example.modbuddesktopproject.Scheduler.FunctionRequestHandler;
import org.example.modbuddesktopproject.Scheduler.models.MonitorItem;
import org.example.modbuddesktopproject.Scheduler.models.MonitorResponse;
import org.example.modbuddesktopproject.Services.SerialService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

public class ItemMonitorController {
    private final SerialService serialService = new SerialService();
    private final FunctionRequestHandler handler = new FunctionRequestHandler();

    @FXML
    private ComboBox<String> comboPorts;
    @FXML
    private ComboBox<String> comboFunctions;
    @FXML private Button btnRefreshPorts;
    @FXML private Button btnMonitor;
    @FXML private TextField inputSlaveId;
    @FXML private TextField inputAddress;
    @FXML private TextField inputBitQuantity;
    @FXML private TextField inputMonitorName;
    @FXML
    private TableView<MonitorResponse<?>> tblResponse;
    @FXML
    private TableColumn<MonitorResponse<?>, String> nameColumn;
    @FXML
    private TableColumn<MonitorResponse<?>, Integer> slaveColumn;
    @FXML
    private TableColumn<MonitorResponse<?>, Integer> functionColumn;
    @FXML
    private TableColumn<MonitorResponse<?>, Integer> addressColumn;
    @FXML
    private TableColumn<MonitorResponse<?>, String> valueColumn;
    @FXML
    private TableColumn<MonitorResponse<?>, String> timestampColumn;
    private final List<MonitorItem> monitorItems = new CopyOnWriteArrayList<>();;
    private final ObservableList<MonitorResponse<?>> monitorData = FXCollections.observableArrayList();
    private SerialPort selectedPort;
    private int selectedFunction;
    private String name;
    private int slaveId;
    private int bitQuantity;
    private int address;
    private boolean monitoringStarted = false;

    public void initialize(){
        System.out.println(this);
        btnRefreshPorts.setOnAction(e -> loadPorts());
        comboPorts.setOnAction(e -> setSelectedPort());
        comboFunctions.setOnAction(e -> setSelectedFunction());
        btnMonitor.setOnAction(e -> sendModbusMonitoring());
        loadPorts();
        loadFunctions();

        //Configuração das colunas
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue()
                                .getMonitorItem()
                                .getName()
                )
        );

        slaveColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(
                        (int) cell.getValue()
                                .getMonitorItem()
                                .getSlaveId()
                )
        );

        functionColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(
                        (int) cell.getValue()
                                .getMonitorItem()
                                .getFunctionCode()
                )
        );

        addressColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(
                        cell.getValue()
                                .getMonitorItem()
                                .getAddress()
                )
        );

        valueColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue()
                                .getValues()
                                .toString()
                )
        );

        timestampColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue()
                                .getTimestamp()
                                .toLocalTime()
                                .toString()
                )
        );

        tblResponse.setItems(monitorData);
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

    @FXML
    public void irParaMain(ActionEvent event) {
        try{
            Stage janela = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            janela.close();

            Parent root = FXMLLoader.load(HelloApplication.class.getResource("Main.fxml"));

            handler.stopMonitoring();
            SerialService.disconnect(selectedPort);
            Stage novaJanela = new Stage();
            novaJanela.setScene(new Scene(root));
            novaJanela.setTitle("Modbud - A Modbus Communication Project!");
            novaJanela.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendModbusMonitoring() {
        MonitorItem item = MonitorItem.builder()
                .name(name)
                .slaveId(slaveId)
                .functionCode(selectedFunction)
                .address(address)
                .quantity(bitQuantity)
                .build();
        monitorItems.add(item);
        if (!monitoringStarted) {
            handler.startMonitoring(
                    monitorItems,
                    selectedPort,
                    400,
                    responses -> {
                        Platform.runLater(() -> {
                            monitorData.setAll(responses);
                        });
                    }
            );
            monitoringStarted = true;
        }
    }

    public void setName(){
        name = inputMonitorName.getText(0, inputMonitorName.getText().length());
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

    public void setSelectedPort() {
        if (selectedPort != null && selectedPort.isOpen()) {
            selectedPort.closePort();
        }
        selectedPort = serialService.getPort(comboPorts.getValue());

        SerialService.connect(selectedPort);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Porta selecionada");
        alert.setHeaderText(null);
        alert.setContentText(comboPorts.getValue());
        alert.showAndWait();
    }

    public void setSelectedFunction() {
        if(Objects.equals(comboFunctions.getValue(), "01 - Read Multiple Coils")){
            selectedFunction = 1;
        }
        if(Objects.equals(comboFunctions.getValue(), "03 - Holding Registers")){
            selectedFunction = 3;
        }
    }

    private void loadFunctions(){
        comboFunctions.getItems().clear();

        List<String> functions = new ArrayList<>();
        functions.add("01 - Read Multiple Coils");
        functions.add("03 - Holding Registers");

        comboFunctions.getItems().addAll(functions);
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
