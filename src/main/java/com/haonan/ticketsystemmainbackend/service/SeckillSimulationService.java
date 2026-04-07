package com.haonan.ticketsystemmainbackend.service;

import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationRequest;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationProgress;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationResponse;
import java.util.function.Consumer;

public interface SeckillSimulationService {

    SeckillSimulationResponse runSimulation(SeckillSimulationRequest request);

    void streamSimulation(SeckillSimulationRequest request, Consumer<SeckillSimulationProgress> progressConsumer);
}
