package com.astrazeneca.weathermicroservice.service;

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

@Service
public class ApiService {

    @Value("${openwheater-api-key}")
    private String openWeatherApiKey;

    @Value("${spotify-api-key}")
    private String spotifyApiKey;

    private String spotifyApiSecret;

    private final WebClient webClient;

    //Constructor
    @Autowired
    public ApiService(WebClient.Builder webClientBuilder){
        this.webClient = webClientBuilder.build();
    }

    //Weather & Spotify services
    //Weather
    public List<String> getPlaylistByCity(String city){

        String weatherUrl = String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, this.openWeatherApiKey);

        //API call
        Map<String, Object> weatherData = webClient.get()
            .uri(weatherUrl)
            .retrieve()
            .bodyToFlux(Map.class)
            .blockFirst();

        double temperature = getTemperature(weatherData);
        return getPlaylist(temperature);
    }

    public List<String> getPlaylistByCoordinates(double lat, double lon) {
        // Llamada asíncrona a OpenWeather API con WebClient
        String weatherUrl = String.format("http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric", lat, lon, this.openWeatherApiKey);

        //API call
        Map<String, Object> weatherData = webClient.get()
            .uri(weatherUrl)
            .retrieve()
            .bodyToFlux(Map.class)
            .blockFirst();


        double temperature = getTemperature(weatherData);
        return getPlaylist(temperature);
    }

    private double getTemperature(Map<String, Object> weatherData) {
        Map<String, Object> main = (Map<String, Object>) weatherData.get("main");
        return (double) main.get("temp");
    }

    //Spotify
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

        //API call
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

        //Creates list of song for every track in playlist
        Map<String, Object> tracks = (Map<String, Object>) songsData.get("tracks");
        List<Map<String, Object>> items = (List<Map<String, Object>>) tracks.get("items");

        for (Map<String, Object> item : items) {
            songsNames.add((String) item.get("name"));
        }

        return songsNames;
    }

    private String getSpotifyAccessToken() {
        String spotifyTokenUrl = "https://accounts.spotify.com/api/token";
    
        //Request parameters configuration
        String credentials = this.spotifyApiKey + ":" + this.spotifyApiSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
    
        //API call
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
}
