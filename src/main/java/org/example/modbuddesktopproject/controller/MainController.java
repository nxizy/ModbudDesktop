package org.example.modbuddesktopproject.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.modbuddesktopproject.HelloApplication;

public class MainController {
    @FXML private TextField modeSelected;
    public void initialize() {
        modeSelected.setText("Modo "+ AppContext.getInstance().getTransport().whichProtocol() + " selecionado!");
    }

    @FXML
    public void irParaHRWindow(ActionEvent event) {
        try{
            Stage janela = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            janela.close();

            Parent root = FXMLLoader.load(HelloApplication.class.getResource("HRWindow.fxml"));

            Stage novaJanela = new Stage();
            novaJanela.setScene(new Scene(root));
            novaJanela.setTitle("Modbud - A Modbus Communication Project!");
            novaJanela.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void irParaReadCoils(ActionEvent event) {
        try{
            Stage janela = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            janela.close();

            Parent root = FXMLLoader.load(HelloApplication.class.getResource("ReadCoils.fxml"));

            Stage novaJanela = new Stage();
            novaJanela.setScene(new Scene(root));
            novaJanela.setTitle("Modbud - A Modbus Communication Project!");
            novaJanela.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void irParaWMCWindow(ActionEvent event) {
        try{
            Stage janela = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            janela.close();

            Parent root = FXMLLoader.load(HelloApplication.class.getResource("WMCWindow.fxml"));

            Stage novaJanela = new Stage();
            novaJanela.setScene(new Scene(root));
            novaJanela.setTitle("Modbud - A Modbus Communication Project!");
            novaJanela.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void irParaMonitorItemWindow(ActionEvent event) {
        try{
            Stage janela = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            janela.close();

            Parent root = FXMLLoader.load(HelloApplication.class.getResource("ItemMonitor.fxml"));

            Stage novaJanela = new Stage();
            novaJanela.setScene(new Scene(root));
            novaJanela.setTitle("Modbud - A Modbus Communication Project!");
            novaJanela.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
