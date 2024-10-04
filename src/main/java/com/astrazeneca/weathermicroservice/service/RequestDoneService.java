package com.astrazeneca.weathermicroservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.astrazeneca.weathermicroservice.model.RequestDone;
import com.astrazeneca.weathermicroservice.model.RequestDoneRepository;

@Service
public class RequestDoneService {

    @Autowired
    private RequestDoneRepository requestDoneRepository;

    public RequestDone saveRequest(String cityName, double temperature, List<String> playlist) {
        RequestDone requestDone = new RequestDone();
        requestDone.setCityName(cityName);
        requestDone.setTemperature(temperature);
        requestDone.setTime(LocalDateTime.now()); 
        requestDone.setPlaylist(playlist);

        return requestDoneRepository.save(requestDone);
    }

    public List<RequestDone> getAllRequests() {
        return requestDoneRepository.findAll();
    }

    public RequestDone getRequestById(int id) {
        return requestDoneRepository.findById(id).orElse(null);
    }
}