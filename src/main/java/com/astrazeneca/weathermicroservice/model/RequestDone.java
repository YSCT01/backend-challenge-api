package com.astrazeneca.weathermicroservice.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class RequestDone {

    //Properties
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;
    private String cityName;
    private double temperature;
    private LocalDateTime time;
    private List<String> playlist;

    //Getters and Setters
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getCityName() {
        return this.cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public double getTemperature() {
        return this.temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public LocalDateTime getTime() {
        return this.time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public List<String> getPlaylist() {
        return this.playlist;
    }

    public void setPlaylist(List<String> playlist) {
        this.playlist = playlist;
    }


    //Repository Interface to persist data
    @Repository
    public interface RequestDoneRepository extends JpaRepository<RequestDone, Integer>{}
}
