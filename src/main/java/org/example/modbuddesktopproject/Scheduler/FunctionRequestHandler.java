package org.example.modbuddesktopproject.Scheduler;

import com.fazecast.jSerialComm.SerialPort;
import org.example.modbuddesktopproject.Scheduler.models.MonitorItem;
import org.example.modbuddesktopproject.Scheduler.models.MonitorResponse;
import org.example.modbuddesktopproject.Services.ModbusService;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsRequestDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsResponseDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRequestDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusResponseDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FunctionRequestHandler {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> monitoringTask;

    public List<MonitorResponse<?>> functionHandler(List<MonitorItem> monitorItemsReq, SerialPort port) throws InterruptedException {
        List<MonitorResponse<?>> responses = new ArrayList<>();
        for(MonitorItem monitorItem: monitorItemsReq){
            switch(monitorItem.getFunctionCode()){
                case 1:
                    ReadCoilsRequestDTO coilsRequestDTO = ReadCoilsRequestDTO.builder()
                            .slaveId(monitorItem.getSlaveId())
                            .address(monitorItem.getAddress())
                            .quantity(monitorItem.getQuantity())
                            .build();
                    ReadCoilsResponseDTO coilsResponseDTO = ModbusService.readCoils(coilsRequestDTO, port);
                    System.out.println("Bytes recebidos:" + coilsResponseDTO.getCoilBytes().length);
                    System.out.println(
                            "quantity = "
                                    + coilsResponseDTO.getQuantity()
                    );
                    List<Boolean> coilsList = ModbusService.extractCoils(
                            coilsResponseDTO.getCoilBytes(),
                            coilsResponseDTO.getQuantity()
                    );
                    MonitorResponse<Boolean> coilsResponse = MonitorResponse.<Boolean>builder()
                            .monitorItem(monitorItem)
                            .values(coilsList)
                            .timestamp(LocalDateTime.now())
                            .build();
                    responses.add(coilsResponse);
                    Thread.sleep(50);
                    break;

                case 3:
                    ModbusRequestDTO hrRequestDTO = ModbusRequestDTO.builder()
                            .slaveId(monitorItem.getSlaveId())
                            .address(monitorItem.getAddress())
                            .quantity(monitorItem.getQuantity())
                            .build();
                    ModbusResponseDTO hrResponseDTO = ModbusService.readHoldingRegisters(hrRequestDTO, port);
                    MonitorResponse<Integer> holdingResponse = MonitorResponse.<Integer>builder()
                            .monitorItem(monitorItem)
                            .values(List.of(hrResponseDTO.getValue()))
                            .timestamp(LocalDateTime.now())
                            .build();
                    responses.add(holdingResponse);
                    Thread.sleep(50);
                    break;
            }
        }
        return responses;
    }

    public void startMonitoring(
            List<MonitorItem> monitorItems,
            SerialPort port,
            int timeDelayInMS,
            Consumer<List<MonitorResponse<?>>> onUpdate
    ) {
        System.out.println("Monitoramento iniciado");
        monitoringTask = executor.scheduleWithFixedDelay(() -> {
            try {
                List<MonitorResponse<?>> responses = functionHandler(monitorItems, port);
                System.out.println(responses);
                onUpdate.accept(responses);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, timeDelayInMS, TimeUnit.MILLISECONDS);
    }

    public void stopMonitoring(){
        if (monitoringTask != null) {
            monitoringTask.cancel(true);
        }
    }
}
