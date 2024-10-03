# Weather-Based Playlist Microservice

This project is a microservice developed in **Java** using the **Spring Boot framework**. It accepts either the name of a city or its geographical coordinates (latitude and longitude) as a parameter, retrieves the current temperature of the city from the **OpenWeather API**, and suggests a playlist from **Spotify** based on the following rules:

- If the temperature is above 30°C: Suggests party music.
- If the temperature is between 15°C and 30°C: Suggests pop music.
- If the temperature is between 10°C and 14°C: Suggests rock music.
- If the temperature is below 10°C: Suggests classical music.

The microservice also tracks the number of requests, including timestamps and track information for each request.

## Architecture

The project follows a **microservice architecture** with RESTful endpoints. It uses the following components and patterns:

### Main Components

1. **Spring Boot**: The main framework used to develop the microservice, offering a lightweight and easy-to-configure structure.
2. **Spring WebClient**: Used to make non-blocking HTTP requests to external APIs (OpenWeather and Spotify), improving performance.
3. **OpenWeather API**: External service used to get the current temperature for the specified city.
4. **Spotify API**: External service used to fetch a playlist based on the music genre determined by the temperature.
5. **In-Memory Database (H2)**: Used to store statistics of the requests, including timestamps and track details.

### Project Structure

```
├── src
│   ├── main
│   │   ├── java
│   │   │   ├── com.astrazeneca.weathermicroservice
│   │   │   │   ├── controller
│   │   │   │   │   └── ApiController.java    // REST Controller
│   │   │   │   ├── service
│   │   │   │   │   └── ApiService.java       // Services
│   │   │   │   ├── model
│   │   │   │   │   └── RequestDone.java      // Model
│   └── test                                      // Unit tests
├── pom.xml                                       // Maven configuration
└── README.md                                     // Project documentation
```

### Patterns Used

- **Dependency Injection**: DI pattern provided by Spring to manage components like services, controllers, and HTTP clients.
- **Non-blocking REST Client**: `WebClient` is used to optimize performance by handling HTTP calls asynchronously.
- **Repository Pattern**: Used for storing and retrieving statistics from the H2 in-memory database.

## Frameworks and Libraries

- **Spring Boot**: For developing Java-based microservices.
- **Spring WebFlux (WebClient)**: For making asynchronous, non-blocking HTTP requests.
- **H2 Database**: In-memory database to store request statistics.
- **MockWebServer**: For mocking HTTP servers during unit tests.
- **JUnit 5** and **Mockito**: For writing unit tests for the service.

## Requirements

To run this project, you will need:

- **Java 17** or higher.
- **Maven** 3.x (for dependency management).
- An active internet connection (to access external APIs).

## External API Setup

1. **OpenWeather API**:

   - The OpenWeather API is used to fetch the current temperature of a city. You’ll need an API Key to use this service.
   - You can sign up at [OpenWeather](https://openweathermap.org/) to get a free API Key.
   - This API Key should be configured in the application properties (see "Configuration").
2. **Spotify API**:

   - The Spotify API is used to fetch playlists based on the genre. You’ll need a **Client ID** and **Client Secret** from Spotify.
   - Sign up at [Spotify for Developers](https://developer.spotify.com/) to get your credentials (Client ID and Client Secret).

## Configuration

The API credentials should be set in the `application.properties` file:

```properties
# OpenWeather API Key
openwheater-api-key=YOUR_OPENWEATHER_API_KEY

# Spotify API credentials
spotify-api-key=YOUR_SPOTIFY_KEY
spotify-api-secret=YOUR_SPOTIFY_CLIENT_SECRET
```

Replace `YOUR_OPENWEATHER_API_KEY`, `YOUR_SPOTIFY_CLIENT_ID`, and `YOUR_SPOTIFY_CLIENT_SECRET` with your actual credentials.

## Running the Project

### 1. Build and run the project

```bash
mvn clean install
mvn spring-boot:run
```

### 2. Access the microservice

Once the service is running, you can access the endpoints via `http://localhost:8080`.

#### Example endpoint:

```http
GET http://localhost:8080/api/weather/playlist/city?city=zapopan
```

This endpoint accepts the city name as a parameter (`city`) and returns a list of suggested Spotify tracks based on the current temperature in that city.

## Potential Improvements

- **Scalability**: Implement caching for API responses to reduce requests to OpenWeather and Spotify.
- **Resilience**: Implement patterns like **Circuit Breaker** using **Resilience4j** to handle failures from external services more effectively.
- **Security**: Add OAuth2 or another authentication system to protect the REST endpoints.
