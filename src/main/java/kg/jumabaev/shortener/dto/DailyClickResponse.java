package kg.jumabaev.shortener.dto;

import java.time.LocalDate;

public record DailyClickResponse(
        LocalDate date,
        long clicks
) {
}
