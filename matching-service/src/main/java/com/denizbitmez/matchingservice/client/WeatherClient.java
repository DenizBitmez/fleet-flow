package com.denizbitmez.matchingservice.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class WeatherClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true";

    public WeatherState getCurrentWeather(double latitude, double longitude) {
        try {
            OpenMeteoResponse response = restTemplate.getForObject(API_URL, OpenMeteoResponse.class, latitude, longitude);
            if (response != null && response.getCurrent_weather() != null) {
                int code = response.getCurrent_weather().getWeathercode();
                return mapWmoCodeToState(code);
            }
        } catch (Exception e) {
            log.error("Failed to fetch weather from Open-Meteo. Defaulting to CLEAR.", e);
        }
        return WeatherState.CLEAR;
    }

    private WeatherState mapWmoCodeToState(int code) {
        if (code >= 95) return WeatherState.STORM; // 95, 96, 99 Thunderstorm
        if (code >= 71 && code <= 86) return WeatherState.SNOW; // 71-75 Snow, 77 Snow grains, 85-86 Snow showers
        if (code >= 51 && code <= 67) return WeatherState.RAIN; // 51-55 Drizzle, 61-65 Rain, 66-67 Freezing Rain
        if (code >= 80 && code <= 82) return WeatherState.RAIN; // Rain showers
        return WeatherState.CLEAR; // 0-3 Clear/Cloudy, 45-48 Fog
    }

    public enum WeatherState {
        CLEAR, RAIN, SNOW, STORM
    }

    @Data
    public static class OpenMeteoResponse {
        private CurrentWeather current_weather;
    }

    @Data
    public static class CurrentWeather {
        private double temperature;
        private int weathercode;
    }
}
