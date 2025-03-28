package com.abarigena.bankoperation.controller;

import com.abarigena.bankoperation.dto.ExchangeRateDTO;
import com.abarigena.bankoperation.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для получения информации о курсах валют.
 */
@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
@Tag(name = "Exchange Rates", description = "API для получения курсов обмена валют")
public class ExchangeRateController {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateController.class);
    private final ExchangeRateService exchangeRateService;

    /**
     * GET /api/rates/today
     * Возвращает список всех доступных курсов обмена на сегодняшний день.
     *
     * @return ResponseEntity со списком ExchangeRateDTO или пустой список.
     */
    @Operation(summary = "Получить курсы валют на сегодня",
            description = "Возвращает список всех доступных курсов обмена, актуальных на текущий день.")
    @GetMapping("/today")
    public ResponseEntity<List<ExchangeRateDTO>> getTodaysRates() {
        log.info("Получен запрос на получение сегодняшних курсов валют.");
        try {
            List<ExchangeRateDTO> rates = exchangeRateService.getTodaysExchangeRates();
            if (rates.isEmpty()) {
                log.warn("Сегодняшние курсы не найдены.");
            }
            return ResponseEntity.ok(rates);
        } catch (Exception e) {
            log.error("Ошибка при получении сегодняшних курсов валют", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
