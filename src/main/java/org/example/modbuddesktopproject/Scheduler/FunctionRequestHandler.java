package org.example.modbuddesktopproject.Scheduler;

import com.fazecast.jSerialComm.SerialPort;
import org.example.modbuddesktopproject.Modbus.ModbusResponse;
import org.example.modbuddesktopproject.Scheduler.models.MonitorItem;
import org.example.modbuddesktopproject.Scheduler.models.MonitorResponse;
import org.example.modbuddesktopproject.Services.ModbusService;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsRequestDTO;
import org.example.modbuddesktopproject.models.ReadCoils.ReadCoilsResponseDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusRequestDTO;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.ModbusResponseDTO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class FunctionRequestHandler {
    private volatile SerialPort currentPort;
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final int delayAntiOverlap = 50;

    public MonitorResponse<?> executeItem(MonitorItem monitorItem, SerialPort port) throws InterruptedException, IOException {
        switch(monitorItem.getFunctionCode()){
            case 1:
                ReadCoilsRequestDTO coilsRequestDTO = ReadCoilsRequestDTO.builder()
                        .slaveId(monitorItem.getSlaveId())
                        .address(monitorItem.getAddress())
                        .quantity(monitorItem.getQuantity())
                        .build();
                //Para não dar overlap?
                Thread.sleep(delayAntiOverlap);
                ReadCoilsResponseDTO coilsResponseDTO = ModbusService.readCoils(coilsRequestDTO, port);
                List<Boolean> coilsList = ModbusService.extractCoils(
                        coilsResponseDTO.getCoilBytes(),
                        coilsResponseDTO.getQuantity()
                );
                return MonitorResponse.<Boolean>builder()
                        .monitorItem(monitorItem)
                        .values(coilsList)
                        .timestamp(LocalDateTime.now())
                        .build();
            case 3:
               ModbusRequestDTO hrRequestDTO = ModbusRequestDTO.builder()
                       .slaveId(monitorItem.getSlaveId())
                       .address(monitorItem.getAddress())
                       .quantity(monitorItem.getQuantity())
                       .build();
                //Para não dar overlap?
                Thread.sleep(delayAntiOverlap);
               ModbusResponseDTO hrResponseDTO = ModbusService.readHoldingRegisters(hrRequestDTO, port);
               return MonitorResponse.<Integer>builder()
                       .monitorItem(monitorItem)
                       .values(List.of(hrResponseDTO.getValue()))
                       .timestamp(LocalDateTime.now())
                       .build();
            default:
                throw new IllegalArgumentException();
        }
    }

    public void removeMonitorItem(
            MonitorItem item
    ) {
        ScheduledFuture<?> future = tasks.remove(item.getId());
        if (future != null) {
            future.cancel(true);
        }
    }

    public void addMonitorItem(
            MonitorItem monitorItem,
            Consumer<MonitorResponse<?>> onUpdate
    ) {
        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(() -> {
            try {
                SerialPort port = currentPort;
                if(port == null) {
                    return;
                }
                MonitorResponse<?> response = executeItem(monitorItem, port);
                System.out.println("Resposta vindo do schedule addMonitorItem" +response);
                onUpdate.accept(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, monitorItem.getMsDelay() - delayAntiOverlap, TimeUnit.MILLISECONDS);
//        Long newMonitorItemId = idGenerator.getAndIncrement();
//        monitorItem.setId(newMonitorItemId);
        tasks.put(monitorItem.getId(), future);
    }

    public void updateMonitorItem(
            MonitorItem monitorItemRequest,
            MonitorItem actualItem,
            Consumer<MonitorResponse<?>> onUpdate
    ){
        removeMonitorItem(actualItem);
        MonitorItem newMonitorItem = MonitorItem.builder()
                .id(actualItem.getId())
                .name(monitorItemRequest.getName())
                .slaveId(monitorItemRequest.getSlaveId())
                .functionCode(monitorItemRequest.getFunctionCode())
                .address(monitorItemRequest.getAddress())
                .quantity(monitorItemRequest.getQuantity())
                .msDelay(monitorItemRequest.getMsDelay())
                .build();
        addMonitorItem(newMonitorItem, onUpdate);
    }

    public void stopAll() {
        tasks.values().forEach(
                future -> future.cancel(true)
        );
        tasks.clear();
    }

    public void shutdown(){
        stopAll();
        executor.shutdown();
    }

    public void setCurrentPort(
            SerialPort port
    ) {
        this.currentPort = port;
    }
}
