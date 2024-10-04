package com.astrazeneca.weathermicroservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

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

    // Circuit Breaker and Retry for Weather service
    @CircuitBreaker(name = "weatherService", fallbackMethod = "weatherServiceFallback")
    @Retry(name = "weatherService")
    public List<String> getPlaylistByCity(String city){
        try {
            String weatherUrl = String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, this.openWeatherApiKey);

            // API call to OpenWeather
            Map<String, Object> weatherData = webClient.get()
                    .uri(weatherUrl)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .blockFirst();

            double temperature = getTemperature(weatherData);
            List<String> playlist = getPlaylist(temperature);
            persistRequest(city, temperature, playlist);
            return playlist;

        } catch (Exception e) {
            logger.error("Error fetching weather data from OpenWeather API for city: {}. Error: {}", city, e.getMessage());
            List<String> error = new ArrayList<>();
            error.add(0, e.getMessage());
            // throw new Exception(e.getMessage());
            return error;
            
        }
    }

    @CircuitBreaker(name = "weatherService", fallbackMethod = "weatherServiceFallback")
    @Retry(name = "weatherService")
    public List<String> getPlaylistByCoordinates(double lat, double lon) {
        try {
            String weatherUrl = String.format("http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric", lat, lon, this.openWeatherApiKey);

            // API call to OpenWeather
            Map<String, Object> weatherData = webClient.get()
                    .uri(weatherUrl)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .blockFirst();

            double temperature = getTemperature(weatherData);
            List<String> playlist = getPlaylist(temperature);
            persistRequest("Lat: " + lat + ", Lon: " + lon, temperature, playlist);
            return playlist;

        } catch (Exception e) {
            logger.error("Error fetching weather data from OpenWeather API for coordinates: {}, {}. Error: {}", lat, lon, e.getMessage());
            List<String> error = new ArrayList<>();
            error.add(0, e.getMessage());
            // throw new Exception(e.getMessage());
            return error;
        }
    }

    @CircuitBreaker(name = "spotifyService", fallbackMethod = "spotifyServiceFallback")
    @Retry(name = "spotifyService")
    private List<String> getPlaylist(double temperature) {
        try {
            if (temperature > 30) {
                return getPlaylistSongs("party");
            } else if (temperature >= 15 && temperature <= 30) {
                return getPlaylistSongs("pop");
            } else if (temperature >= 10 && temperature < 15) {
                return getPlaylistSongs("rock");
            } else {
                return getPlaylistSongs("classical");
            }
        } catch (Exception e) {
            logger.error("Error fetching playlist for temperature: {}. Error: {}", temperature, e.getMessage());
            throw new RuntimeException("Spotify service is unavailable at the moment");
        }
    }

    @CircuitBreaker(name = "spotifyService", fallbackMethod = "spotifyServiceFallback")
    @Retry(name = "spotifyService")
    private List<String> getPlaylistSongs(String genre) {
        try {
            String accessToken = getSpotifyAccessToken();
            String spotifyUrl = String.format("https://api.spotify.com/v1/search?q=%s&type=track&limit=10", genre);

            // API call to Spotify
            Map<String, Object> songsData = webClient.get()
                    .uri(spotifyUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .blockFirst();

            return getSongs(songsData);

        } catch (Exception e) {
            logger.error("Error fetching playlist from Spotify API for genre: {}. Error: {}", genre, e.getMessage());
            throw new RuntimeException("Spotify service is unavailable at the moment");
        }
    }

    // Helper methods

    private double getTemperature(Map<String, Object> weatherData) {
        Map<String, Object> main = (Map<String, Object>) weatherData.get("main");
        return (double) main.get("temp");
    }

    private List<String> getSongs(Map<String, Object> songsData) {
        List<String> songNames = new ArrayList<>();

        Map<String, Object> tracks = (Map<String, Object>) songsData.get("tracks");
        List<Map<String, Object>> items = (List<Map<String, Object>>) tracks.get("items");

        for (Map<String, Object> item : items) {
            songNames.add((String) item.get("name"));
        }

        return songNames;
    }

    // Spotify token fetching with error handling
    @CircuitBreaker(name = "spotifyService", fallbackMethod = "spotifyServiceFallback")
    @Retry(name = "spotifyService")
    private String getSpotifyAccessToken() {
        try {
            String spotifyTokenUrl = "https://accounts.spotify.com/api/token";

            String credentials = this.spotifyApiKey + ":" + this.spotifyApiSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");

            // API call to Spotify token service
            Map<String, Object> response = webClient.post()
                    .uri(spotifyTokenUrl)
                    .header("Authorization", "Basic " + encodedCredentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return (String) response.get("access_token");

        } catch (Exception e) {
            logger.error("Error fetching Spotify access token. Error: {}", e.getMessage());
            throw new RuntimeException("Spotify authentication service is unavailable at the moment");
        }
    }

    // Fallback methods

    public List<String> weatherServiceFallback(String city, Throwable t) {
        logger.error("Fallback triggered for weather service. Error: {}", t.getMessage());
        return new ArrayList<>();
    }

    public List<String> weatherServiceFallback(double lat, double lon, Throwable t) {
        logger.error("Fallback triggered for weather service. Error: {}", t.getMessage());
        return new ArrayList<>();
    }

    public List<String> spotifyServiceFallback(String genre, Throwable t) {
        logger.error("Fallback triggered for Spotify service. Error: {}", t.getMessage());
        return new ArrayList<>();
    }

    //RequestDoneService
    public void persistRequest(String cityName, double temperature, List<String> playlist) {
        RequestDone requestDone = new RequestDone();
        requestDone.setCityName(cityName);
        requestDone.setTemperature(temperature);
        requestDone.setTime(LocalDateTime.now());
        requestDone.setPlaylist(playlist);

        requestDoneService.saveRequest(requestDone);  
    }
}


