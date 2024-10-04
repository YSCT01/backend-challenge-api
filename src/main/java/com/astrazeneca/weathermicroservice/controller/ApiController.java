package com.astrazeneca.weathermicroservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.astrazeneca.weathermicroservice.service.ApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/weather")
public class ApiController {

    @Autowired
    public ApiService apiService;

    @GetMapping("/helloWorld")
    public String helloWorld(){
        return "Hello World!";
    }

    @Operation(summary = "Get a playlist suggestion based on city name and current temperature",
               description = "Fetches the current temperature for the given city and suggests a playlist based on predefined rules for different temperature ranges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", 
                         content = {@Content(mediaType = "application/json",
                         schema = @Schema(implementation = String.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid city name provided",
                         content = @Content),
            @ApiResponse(responseCode = "404", description = "City not found",
                         content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                         content = @Content)
    })
    @GetMapping("/playlist/city")
    public ResponseEntity<List<String>> getPlaylistByCity(@RequestParam String city){
        
        List<String> playlist = apiService.getPlaylistByCity(city);
        
        if (playlist.size() > 1) {
            return new ResponseEntity<>(playlist, HttpStatus.OK);
        }
    
        if (playlist.get(0).contains("404")) {
            // Error 404 Not Found
            return new ResponseEntity<>(playlist, HttpStatus.NOT_FOUND);
        } else if (playlist.get(0).contains("500")) {
            // Error 500 Internal Server Error
            return new ResponseEntity<>(playlist, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            // Error 400 Bad Request
            return new ResponseEntity<>(playlist, HttpStatus.BAD_REQUEST);
        }
        
    }

    @Operation(summary = "Get a playlist suggestion based on coordinates",
           description = "Fetches the current temperature for the given latitude and longitude and suggests a playlist based on predefined rules for different temperature ranges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", 
                        content = {@Content(mediaType = "application/json",
                        schema = @Schema(implementation = String.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates provided",
                        content = @Content),
            @ApiResponse(responseCode = "404", description = "Location not found",
                        content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                        content = @Content)
    })
    @GetMapping("/playlist/coordinates")
    public ResponseEntity<List<String>> getPlaylistByCoordinates(@RequestParam double lat, @RequestParam double lon){
        
        List<String> playlist = apiService.getPlaylistByCoordinates(lat, lon);

        if (playlist.size() > 1) {
            return new ResponseEntity<>(playlist, HttpStatus.OK);
        }
    
        if (playlist.get(0).contains("404")) {
            // Error 404 Not Found
            return new ResponseEntity<>(playlist, HttpStatus.NOT_FOUND);
        } else if (playlist.get(0).contains("500")) {
            // Error 500 Internal Server Error
            return new ResponseEntity<>(playlist, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            // Error 400 Bad Request
            return new ResponseEntity<>(playlist, HttpStatus.BAD_REQUEST);
        }

    }
}
