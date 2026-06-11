package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.modbuddesktopproject.HelloApplication;
import org.example.modbuddesktopproject.Scheduler.FunctionRequestHandler;
import org.example.modbuddesktopproject.Scheduler.models.MonitorItem;
import org.example.modbuddesktopproject.Scheduler.models.MonitorResponse;
import org.example.modbuddesktopproject.Services.SerialService;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
    @FXML private TextField inputScanDelay;
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
    @FXML
    private VBox requestsContainer;
    @FXML
    private Button btnAddRequest;
    private final Map<Long, MonitorItem> monitorItemsRequest = new HashMap<>();
    private final ObservableList<MonitorResponse<?>> monitorData = FXCollections.observableArrayList();
    private final Map<Long, MonitorResponse<?>> monitorItemsResponse = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private SerialPort selectedPort;
    private int selectedFunction;
    private String name;
    private int slaveId;
    private int bitQuantity;
    private int address;
    private int scanDelay;

    public void initialize(){
        System.out.println(this);
        btnRefreshPorts.setOnAction(e -> loadPorts());
        comboPorts.setOnAction(e -> setSelectedPort());
        comboFunctions.setOnAction(e -> setSelectedFunction());
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
        // Filtro para o scan delay
        UnaryOperator<TextFormatter.Change> scanDelayFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change; // Accept change
            }
            return null; // Reject change
        };

        inputSlaveId.setTextFormatter(new TextFormatter<>(slaveFilter));
        inputAddress.setTextFormatter(new TextFormatter<>(enderecoFilter));
        inputBitQuantity.setTextFormatter(new TextFormatter<>(bitQuantityFilter));
        inputScanDelay.setTextFormatter(new TextFormatter<>(scanDelayFilter));
    }

    @FXML
    public void irParaMain(ActionEvent event) {
        try{
            Stage janela = (Stage) ((Node) event.getSource()).getScene().getWindow();
            janela.close();

            Parent root = FXMLLoader.load(HelloApplication.class.getResource("Main.fxml"));

            handler.shutdown();
            SerialService.disconnect(selectedPort);
            Stage novaJanela = new Stage();
            novaJanela.setScene(new Scene(root));
            novaJanela.setTitle("Modbud - A Modbus Communication Project!");
            novaJanela.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MonitorItem addMonitorItems(){
        Long id = idGenerator.getAndIncrement();
        MonitorItem item = MonitorItem.builder()
                .id(id)
                .name(name)
                .slaveId(slaveId)
                .functionCode(selectedFunction)
                .address(address)
                .quantity(bitQuantity)
                .msDelay(scanDelay)
                .build();
        monitorItemsRequest.put(id, item);
        return item;
    }

    public void sendModbusMonitoring() {
        handler.stopAll();
        monitorData.clear();
        for(MonitorItem monitorItem: monitorItemsRequest.values()){
            handler.addMonitorItem(
                    monitorItem,
                    response -> {
                        Long responseId = response.getMonitorItem().getId();
                        monitorItemsResponse.put(responseId, response);
                        monitorData.setAll(
                                monitorItemsResponse.values()
                        );
                    }
            );
        }
    }

    @FXML
    private void addRequest() {

        MonitorItem addedMonitorItem = addMonitorItems();

        HBox request = new HBox();
        request.setSpacing(12);
        request.setAlignment(Pos.CENTER_LEFT);

        request.setMinHeight(49);
        request.setPrefHeight(49);
        request.setMaxHeight(49);
        request.setPadding(new Insets(0, 12, 0, 12));

        request.setUserData(addedMonitorItem.getId());

        Button btnDelete = new Button("X");
        btnDelete.setFont(Font.font("System", FontWeight.BOLD, 12));
        btnDelete.setOnAction(event -> deleteRequest(request));

        TextField nome = new TextField();
        nome.setText(name);
        nome.setPrefWidth(100);

        TextField slave = new TextField();
        slave.setText(String.valueOf(slaveId));
        slave.setPrefWidth(100);

        TextField endereco = new TextField();
        endereco.setText(String.valueOf(address));
        endereco.setPrefWidth(100);

        TextField quantidade = new TextField();
        quantidade.setText(String.valueOf(bitQuantity));
        quantidade.setPrefWidth(100);

        TextField tempo = new TextField();
        tempo.setText(String.valueOf(scanDelay));
        tempo.setPrefWidth(100);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setFont(Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 12));
        btnAtualizar.setOnAction(e -> updateRequest(
                request,
                nome,
                slave,
                endereco,
                quantidade,
                tempo
                ));

        request.getChildren().addAll(
                btnDelete,
                nome,
                slave,
                endereco,
                quantidade,
                tempo,
                btnAtualizar
        );

        requestsContainer.getChildren().add(request);
    }

    private void deleteRequest(HBox row) {
        Long id = (Long) row.getUserData();
        MonitorItem item = monitorItemsRequest.get(id);
        monitorItemsRequest.remove(id);
        monitorItemsResponse.remove(id);
        handler.removeMonitorItem(item);
        requestsContainer.getChildren().remove(row);
        sendModbusMonitoring();
    }

    private void updateRequest(
            HBox row,
            TextField name,
            TextField slaveId,
            TextField address,
            TextField quantity,
            TextField msDelay
    ){
        Long id = (Long) row.getUserData();
        MonitorItem previousItem = monitorItemsRequest.get(id);
        if(previousItem != null){
            MonitorItem newItem = MonitorItem.builder()
                    .id(previousItem.getId())
                    .functionCode(previousItem.getFunctionCode())
                    .name(name.getText())
                    .slaveId(Integer.parseInt(slaveId.getText()))
                    .address(Integer.parseInt(address.getText()))
                    .quantity(Integer.parseInt(quantity.getText()))
                    .msDelay(Integer.parseInt(msDelay.getText()))
                    .build();
            handler.updateMonitorItem(
                    previousItem,
                    newItem,
                    response -> {
                        Long responseId = response.getMonitorItem().getId();
                        monitorItemsResponse.put(responseId, response);
                        monitorData.setAll(
                                monitorItemsResponse.values()
                        );
                    }
            );
            monitorItemsRequest.put(previousItem.getId(), newItem);
            sendModbusMonitoring();
        }
    }
    public void setScanDelay() {
        scanDelay = Integer.parseInt(inputScanDelay.getText());
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
        handler.stopAll();
        if(selectedPort != null){
            SerialService.disconnect(selectedPort);
        }
        selectedPort = serialService.getPort(comboPorts.getValue());
        handler.setCurrentPort(selectedPort);
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
