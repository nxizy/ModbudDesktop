package org.example.modbuddesktopproject.Scheduler;

import org.example.modbuddesktopproject.Scheduler.models.MonitorItem;
import org.example.modbuddesktopproject.Scheduler.models.MonitorResponse;
import org.example.modbuddesktopproject.Services.ENUMs.Protocol;
import org.example.modbuddesktopproject.Services.MasterService;
import org.example.modbuddesktopproject.Services.RTUTransport;
import org.example.modbuddesktopproject.controller.AppContext;
import org.example.modbuddesktopproject.models.ReadCoils.*;
import org.example.modbuddesktopproject.models.ReadHoldingRegisters.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class FunctionRequestHandler {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final int delayAntiOverlap = 50;

    private MasterService getService() {
        return new MasterService(AppContext.getInstance().getTransport());
    }

    public MonitorResponse<?> executeItem(MonitorItem monitorItem)
            throws IOException, InterruptedException {
        Thread.sleep(delayAntiOverlap);
        switch (monitorItem.getFunctionCode()) {
            case 1 -> {
                if (AppContext.getInstance().getTransport().whichProtocol() == Protocol.RTU) {
                    ReadCoilsRTURequestDTO req = ReadCoilsRTURequestDTO.builder()
                                    .slaveId(monitorItem.getSlaveId())
                                    .address(monitorItem.getAddress())
                                    .quantity(monitorItem.getQuantity())
                                    .build();

                    ReadCoilsRTUResponseDTO res = getService().readCoilsRTU(req);

                    return MonitorResponse.<Boolean>builder()
                            .monitorItem(monitorItem)
                            .values(
                                    RTUTransport.extractCoils(
                                            res.getCoilBytes(),
                                            monitorItem.getQuantity()))
                            .timestamp(LocalDateTime.now())
                            .build();

                } else {
                    ReadCoilsTCPRequestDTO req = ReadCoilsTCPRequestDTO.builder()
                                    .transactionId(ThreadLocalRandom.current().nextInt(65536))
                                    .unitId(monitorItem.getSlaveId())
                                    .startAddress(monitorItem.getAddress())
                                    .quantity(monitorItem.getQuantity())
                                    .build();

                    ReadCoilsTCPResponseDTO res = getService().readCoilsTCP(req);

                    return MonitorResponse.<Boolean>builder()
                            .monitorItem(monitorItem)
                            .values(
                                    RTUTransport.extractCoils(
                                            res.getCoilBytes(),
                                            monitorItem.getQuantity()))
                            .timestamp(LocalDateTime.now())
                            .build();
                }
            }
            case 3 -> {
                if (AppContext.getInstance().getTransport().whichProtocol() == Protocol.RTU) {
                    ModbusRHRRTURequestDTO req = ModbusRHRRTURequestDTO.builder()
                                    .slaveId(monitorItem.getSlaveId())
                                    .address(monitorItem.getAddress())
                                    .quantity(monitorItem.getQuantity())
                                    .build();

                    ModbusRHRRTUResponseDTO res = getService().readHoldingRegistersRTU(req);

                    return MonitorResponse.<Integer>builder()
                            .monitorItem(monitorItem)
                            .values(List.of(res.getValue()))
                            .timestamp(LocalDateTime.now())
                            .build();

                } else {
                    ModbusRHRTCPRequestDTO req = ModbusRHRTCPRequestDTO.builder()
                                    .transactionId(ThreadLocalRandom.current().nextInt(65536))
                                    .unitId(monitorItem.getSlaveId())
                                    .startAddress(monitorItem.getAddress())
                                    .quantity(monitorItem.getQuantity())
                                    .build();

                    ModbusRHRTCPResponseDTO res = getService().readHoldingRegistersTCP(req);

                    return MonitorResponse.<Integer>builder()
                            .monitorItem(monitorItem)
                            .values(List.of(res.getValue()))
                            .timestamp(LocalDateTime.now())
                            .build();
                }
            }

            default -> throw new IllegalArgumentException("Function Code inválido");
        }
    }

    public void removeMonitorItem(MonitorItem item) {

        ScheduledFuture<?> future = tasks.remove(item.getId());

        if (future != null) {
            future.cancel(true);
        }
    }

    public void addMonitorItem(
            MonitorItem monitorItem,
            Consumer<MonitorResponse<?>> onUpdate
    ) {
        System.out.println("Delay = " + monitorItem.getMsDelay());
        System.out.println("Delay real = " + (monitorItem.getMsDelay() - delayAntiOverlap));
        ScheduledFuture<?> future =
                executor.scheduleWithFixedDelay(() -> {
                            try {
                                MonitorResponse<?> response =
                                        executeItem(monitorItem);
                                onUpdate.accept(response);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }, 0,
                        monitorItem.getMsDelay(),
                        TimeUnit.MILLISECONDS);

        tasks.put(monitorItem.getId(), future);
    }

    public void updateMonitorItem(
            MonitorItem monitorItemRequest,
            MonitorItem actualItem,
            Consumer<MonitorResponse<?>> onUpdate
    ) {

        removeMonitorItem(actualItem);

        MonitorItem newMonitorItem =
                MonitorItem.builder()
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

        tasks.values().forEach(future -> future.cancel(true));

        tasks.clear();
    }

    public void shutdown() {

        stopAll();

        executor.shutdown();
    }

}