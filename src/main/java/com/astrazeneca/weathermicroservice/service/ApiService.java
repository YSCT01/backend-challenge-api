package com.astrazeneca.weathermicroservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.astrazeneca.weathermicroservice.model.RequestDone;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ApiService {

    @Value("${openwheater-api-key}")
    private String openWeatherApiKey;

    @Value("${spotify-api-key}")
    private String spotifyApiKey;

    @Value("${spotify-api-secret}")
    private String spotifyApiSecret;

    private final WebClient webClient;
    private RequestDoneService requestDoneService; 

    @Autowired
    public ApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.requestDoneService = requestDoneService;
    }

    //Weather & Spotify services
    //Weather
    @CircuitBreaker(name = "apiService", fallbackMethod = "getFallbackPlaylistByCity")
    @Retry(name = "apiService")
    public List<String> getPlaylistByCity(String city) {
        String weatherUrl = String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, this.openWeatherApiKey);

        //API call
        Map<String, Object> weatherData = webClient.get()
            .uri(weatherUrl)
            .retrieve()
            .bodyToFlux(Map.class)
            .blockFirst();

        double temperature = getTemperature(weatherData);
        List<String> playlist = getPlaylist(temperature);

        persistRequest(city, temperature, playlist);

        return playlist;
    }

    @CircuitBreaker(name = "apiService", fallbackMethod = "getFallbackPlaylistByCoordinates")
    @Retry(name = "apiService")
    public List<String> getPlaylistByCoordinates(double lat, double lon) {
        String weatherUrl = String.format("http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric", lat, lon, this.openWeatherApiKey);

        //API call
        Map<String, Object> weatherData = webClient.get()
            .uri(weatherUrl)
            .retrieve()
            .bodyToFlux(Map.class)
            .blockFirst();

        double temperature = getTemperature(weatherData);
        List<String> playlist = getPlaylist(temperature);
        
        persistRequest("Lat: " + lat + ", Lon: " + lon, temperature, playlist);

        return playlist;
    }

    //Fallback methods
    public List<String> getFallbackPlaylistByCity(String city, Throwable t) {
        // Código de respuesta de emergencia (Fallback)
        return List.of("Fallback song 1", "Fallback song 2");
    }

    public List<String> getFallbackPlaylistByCoordinates(double lat, double lon, Throwable t) {
        // Código de respuesta de emergencia (Fallback)
        return List.of("Fallback song 1", "Fallback song 2");
    }

    private double getTemperature(Map<String, Object> weatherData) {
        Map<String, Object> main = (Map<String, Object>) weatherData.get("main");
        return (double) main.get("temp");
    }

    private List<String> getPlaylist(double temperature){
        if (temperature > 30) {
            return getPlaylistSongs("party");
        } else if (temperature >= 15 && temperature <= 30) {
            return getPlaylistSongs("pop");
        } else if (temperature >= 10 && temperature < 15) {
            return getPlaylistSongs("rock");
        } else {
            return getPlaylistSongs("classical");
        }
    }

    private List<String> getPlaylistSongs(String genre){
        String accessToken = getSpotifyAccessToken();
        String spotifyUrl = String.format("https://api.spotify.com/v1/search?q=%s&type=track&limit=10", genre);

        Map<String, Object> songsData = webClient.get()
                .uri(spotifyUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToFlux(Map.class)
                .blockFirst();

        return getSongs(songsData);
    }

    private List<String> getSongs(Map<String, Object> songsData){
        List<String> songsNames = new ArrayList<>();
        Map<String, Object> tracks = (Map<String, Object>) songsData.get("tracks");
        List<Map<String, Object>> items = (List<Map<String, Object>>) tracks.get("items");

        for (Map<String, Object> item : items) {
            songsNames.add((String) item.get("name"));
        }

        return songsNames;
    }

    private String getSpotifyAccessToken() {
        String spotifyTokenUrl = "https://accounts.spotify.com/api/token";
    
        String credentials = this.spotifyApiKey + ":" + this.spotifyApiSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        Map<String, Object> response = webClient.post()
                .uri(spotifyTokenUrl)
                .header("Authorization", "Basic " + encodedCredentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }

    //RequestDoneService
    public void persistRequest(String cityName, double temperature, List<String> playlist) {
        RequestDone requestDone = new RequestDone();
        requestDone.setCityName(cityName);
        requestDone.setTemperature(temperature);
        requestDone.setTime(LocalDateTime.now());
        requestDone.setPlaylist(playlist);
        requestDone.setCount(1);

        requestDoneService.saveRequest(requestDone);  
    }
}
