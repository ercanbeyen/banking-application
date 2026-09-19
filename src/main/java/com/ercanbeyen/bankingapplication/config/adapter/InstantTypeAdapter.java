package com.ercanbeyen.bankingapplication.config.adapter;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class InstantTypeAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }

        try {
            return Instant.parse(jsonElement.getAsString());
        } catch (DateTimeParseException exception) {
            throw new JsonParseException("Invalid Instant value: " + jsonElement.getAsString(), exception);
        }
    }

    @Override
    public JsonElement serialize(Instant instant, Type type, JsonSerializationContext jsonSerializationContext) {
        if (instant == null) {
            return JsonNull.INSTANCE;
        }

        return new JsonPrimitive(instant.toString());
    }
}
