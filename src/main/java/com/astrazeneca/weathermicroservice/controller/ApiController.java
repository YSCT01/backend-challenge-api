package com.astrazeneca.weathermicroservice.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.astrazeneca.weathermicroservice.service.ApiService;

@RestController
@RequestMapping("/api/weather")
public class ApiController {

    @Autowired
    public ApiService apiService;

    @GetMapping("/helloWorld")
    public String helloWorld(){
        return "Hello World!";
    }

    @GetMapping("/playlist/city")
    public ResponseEntity<List<String>> getPlaylistByCity(@RequestParam String city){
        List<String> playlist = new ArrayList<String>();
        return new ResponseEntity<>(playlist,
            HttpStatus.OK
        );
    }

    @GetMapping("/playlist/coordinates")
    public ResponseEntity<List<String>> getPlaylistByCoordinates(@RequestParam double lat, @RequestParam double lon){
        List<String> playlist = new ArrayList<String>();
        return new ResponseEntity<>(playlist,
            HttpStatus.OK
        );
    }
}
