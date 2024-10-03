package com.astrazeneca.weathermicroservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.astrazeneca.weathermicroservice.model.RequestDone;
import com.astrazeneca.weathermicroservice.model.RequestDone.RequestDoneRepository;

public class RequestDoneService {

    private final RequestDoneRepository requestDoneRepository;

    @Autowired
    public RequestDoneService(RequestDoneRepository requestDoneRepository) {
        this.requestDoneRepository = requestDoneRepository;
    }

    public List<RequestDone> getAllRequests() {
        return requestDoneRepository.findAll();
    }

    public RequestDone saveRequest(RequestDone requestDone) {
        return requestDoneRepository.save(requestDone);
    }
}
