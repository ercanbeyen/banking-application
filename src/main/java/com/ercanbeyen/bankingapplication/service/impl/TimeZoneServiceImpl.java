package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.dto.CountryCity;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.service.TimeZoneService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
public class TimeZoneServiceImpl implements TimeZoneService {
    private final Map<String, String> countryToCode = new HashMap<>();
    private final Map<CountryCity, ZoneId> countryAndCityToZones = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        loadCountries();
        log.info("loadCountries method completed!");
        loadTimeZones();
        log.info("loadTimeZones method completed!");

        log.info("countryToCode: {}", countryToCode);
        log.info("countryAndCityToZones: {}", countryAndCityToZones);
    }

    @Override
    public Optional<ZoneId> getZoneId(String country, String city) {
        if (country == null || city == null) {
            return Optional.empty();
        }

        String countryCode = countryToCode.get(country);

        if (countryCode == null) {
            return Optional.empty();
        }

        CountryCity key = new CountryCity(countryCode, city);

        return Optional.ofNullable(countryAndCityToZones.get(key));
    }

    @Override
    public void checkZoneId(String country, String city) {
        getZoneId(country, city).ifPresentOrElse(
                _ -> log.info("Time zone for country and city is found"),
                () -> {
                    log.error("Time zone for country and city is not found");
                    throw new BadRequestException("Country and city are not included in the address coverage");
                }
        );
    }

    private void loadCountries() {
        for (String code : Locale.getISOCountries()) {
            Locale locale = Locale.of("", code);
            String country = locale.getDisplayCountry(Locale.ENGLISH);

            if (!country.isBlank()) {
                countryToCode.put(normalize(country), code);
            }
        }
    }

    private void loadTimeZones() throws IOException {
        ClassPathResource resource = new ClassPathResource("dataset/city-zone.csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;

            reader.readLine(); // Pass header

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(",", -1);

                if (columns.length >= 3) {
                    String countryCode = normalize(columns[0]);
                    String city = normalize(columns[1]);
                    ZoneId zoneId = ZoneId.of(columns[2]);

                    CountryCity key = new CountryCity(countryCode, city);
                    countryAndCityToZones.put(key, zoneId);
                }
            }
        }
    }

    private String normalize(String value) {
        return value.trim()
                .replace('_', ' ')
                .replace('-', ' ');
    }
}
