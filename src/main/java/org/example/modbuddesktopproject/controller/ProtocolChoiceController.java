package org.example.modbuddesktopproject.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.modbuddesktopproject.HelloApplication;
import org.example.modbuddesktopproject.Services.MasterTransport;
import org.example.modbuddesktopproject.Services.RTUTransport;
import org.example.modbuddesktopproject.Services.SerialService;
import org.example.modbuddesktopproject.Services.TCPTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class ProtocolChoiceController {
    @FXML
    private ComboBox<String> comboPorts;
    @FXML
    private Button btnRefreshPorts;
    @FXML
    private CheckBox rtuCheckBox;
    @FXML
    private CheckBox tcpCheckBox;
    @FXML
    private TextField txtIpAddress;
    @FXML
    private TextField txtTcpPort;
    private SerialPort selectedPort;
    private final List<CheckBox> coilCheckBoxes = new ArrayList<>();
    private final SerialService serialService = new SerialService();

    public void initialize() {
        //Filtro para o IP
        UnaryOperator<TextFormatter.Change> ipFilter = change -> {

            String newText = change.getControlNewText();

            if (newText.matches("^\\d{0,3}(\\.\\d{0,3}){0,3}$")) {
                return change;
            }

            return null;
        };

        //Filtro para Porta
        UnaryOperator<TextFormatter.Change> filter = change -> {

            String text = change.getControlNewText();

            if (text.matches("\\d{0,5}")) {
                return change;
            }

            return null;
        };

        txtTcpPort.setTextFormatter(new TextFormatter<>(filter));
        txtIpAddress.setTextFormatter(new TextFormatter<>(ipFilter));

        btnRefreshPorts.setOnAction(e -> loadPorts());
        comboPorts.setOnAction(e -> setSelectedPort());
        loadPorts();
    }


    @FXML
    public void irParaMain(ActionEvent event){
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

    public void tcpChoice() {
        String ip = txtIpAddress.getText();
        String port = txtTcpPort.getText();
        if (!isValidIp(ip)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("IP inválido");
            alert.showAndWait();
        }
        if(!isValidPort(port)){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Porta inválida");
            alert.showAndWait();
        }
        rtuCheckBox.setSelected(false);
        AppContext.getInstance().connectTCP(ip, Integer.parseInt(port));
    }

    public void rtuChoice() {
        tcpCheckBox.setSelected(false);
        if(selectedPort == null) {
            rtuCheckBox.setSelected(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Erro ao habilitar o modo RTU.");
            alert.setHeaderText(null);
            alert.setContentText("Selecione primeiro uma porta COM para se comunicar com o mestre.");
            alert.showAndWait();
            return;
        }
        AppContext.getInstance().connectRTU(selectedPort);
    }

    public void setSelectedPort() {
        if (selectedPort != null && selectedPort.isOpen()) {
            SerialService.disconnect(selectedPort);
        }
        selectedPort = serialService.getPort(comboPorts.getValue());
        SerialService.connect(selectedPort);
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

    private boolean isValidIp(String ip){

        return ip.matches(
                "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}"
                        + "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"
        );

    }

    private boolean isValidPort(String portText) {

        try {

            int port = Integer.parseInt(portText);

            return port >= 1 && port <= 65535;

        } catch (NumberFormatException e) {

            return false;

        }
    }
}
