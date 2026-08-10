package com.ercanbeyen.bankingapplication.service;


import java.time.ZoneId;
import java.util.Optional;

public interface TimeZoneService {
    Optional<ZoneId> getZoneId(String country, String city);
    void checkZoneId(String country, String city);
}
