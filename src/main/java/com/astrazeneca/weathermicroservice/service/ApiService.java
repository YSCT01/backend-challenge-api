package com.astrazeneca.weathermicroservice.service;

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

    @Autowired
    private RequestDoneService requestDoneService; 

    public ApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @CircuitBreaker(name = "weatherService", fallbackMethod = "weatherServiceFallback")
    @Retry(name = "weatherService")
    public List<String> getPlaylistByCity(String city){
        try {
            String weatherUrl = String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, this.openWeatherApiKey);

            // API call to OpenWeather
            @SuppressWarnings("unchecked")
            Map<String, Object> weatherData = webClient.get()
                    .uri(weatherUrl)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .blockFirst();

            double temperature = getTemperature(weatherData);
            List<String> playlist = getPlaylist(temperature);
            requestDoneService.saveRequest(city, temperature, playlist);
            return playlist;

        } catch (Exception e) {
            logger.error("Error fetching weather data from OpenWeather API for city: {}. Error: {}", city, e.getMessage());
            throw new RuntimeException(e.getMessage());
            
        }
    }

    @CircuitBreaker(name = "weatherService", fallbackMethod = "weatherServiceFallback")
    @Retry(name = "weatherService")
    public List<String> getPlaylistByCoordinates(double lat, double lon) {
        try {
            String weatherUrl = String.format("http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric", lat, lon, this.openWeatherApiKey);

            // API call to OpenWeather
            @SuppressWarnings("unchecked")
            Map<String, Object> weatherData = webClient.get()
                    .uri(weatherUrl)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .blockFirst();

            double temperature = getTemperature(weatherData);
            List<String> playlist = getPlaylist(temperature);
            requestDoneService.saveRequest("Lat: " + lat + ", Lon: " + lon, temperature, playlist);
            return playlist;

        } catch (Exception e) {
            logger.error("Error fetching weather data from OpenWeather API for coordinates: {}, {}. Error: {}", lat, lon, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @CircuitBreaker(name = "spotifyService", fallbackMethod = "spotifyServiceFallback")
    @Retry(name = "spotifyService")
    private List<String> getPlaylist(double temperature) {
        int limit = 10;
        try {
            if (temperature > 30) {
                return getPlaylistSongs("party", limit);
            } else if (temperature >= 15 && temperature <= 30) {
                return getPlaylistSongs("pop", limit);
            } else if (temperature >= 10 && temperature < 15) {
                return getPlaylistSongs("rock", limit);
            } else {
                return getPlaylistSongs("classical", limit);
            }
        } catch (Exception e) {
            logger.error("Error fetching playlist for temperature: {}. Error: {}", temperature, e.getMessage());
            throw new RuntimeException("Spotify service is unavailable at the moment");
        }
    }

    @CircuitBreaker(name = "spotifyService", fallbackMethod = "spotifyServiceFallback")
    @Retry(name = "spotifyService")
    private List<String> getPlaylistSongs(String genre, int limit) {
        try {
            String accessToken = getSpotifyAccessToken();
            String spotifyUrl = String.format("https://api.spotify.com/v1/search?q=%s&type=track&limit=%s", genre, limit);

            // API call to Spotify
            @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        Map<String, Object> main = (Map<String, Object>) weatherData.get("main");
        return (double) main.get("temp");
    }

    private List<String> getSongs(Map<String, Object> songsData) {
        List<String> songNames = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Map<String, Object> tracks = (Map<String, Object>) songsData.get("tracks");
        @SuppressWarnings("unchecked")
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
            @SuppressWarnings("unchecked")
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

    public List<String> cityServiceFallback(String city, Throwable t) throws Exception{
        logger.error("Fallback triggered for weather service. Error: {}", t.getMessage());
        throw new Exception(t.getMessage());
    }

    public List<String> coordinatesServiceFallback(double lat, double lon, Throwable t) throws Exception{
        logger.error("Fallback triggered for weather service. Error: {}", t.getMessage());
        throw new Exception(t.getMessage());
    }

    public List<String> spotifyServiceFallback(String genre, Throwable t) throws Exception{
        logger.error("Fallback triggered for Spotify service. Error: {}", t.getMessage());
        throw new Exception(t.getMessage());
    }
}


