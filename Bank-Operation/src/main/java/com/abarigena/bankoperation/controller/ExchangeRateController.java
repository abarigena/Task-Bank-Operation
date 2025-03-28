package com.abarigena.bankoperation.controller;

import com.abarigena.bankoperation.dto.ExchangeRateDTO;
import com.abarigena.bankoperation.service.ExchangeRateService;
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
@RequestMapping("/api/rates") // Используем префикс /api/rates
@RequiredArgsConstructor
public class ExchangeRateController {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateController.class);
    private final ExchangeRateService exchangeRateService;

    /**
     * GET /api/rates/today
     * Возвращает список всех доступных курсов обмена на сегодняшний день.
     *
     * @return ResponseEntity со списком ExchangeRateDTO или пустой список.
     */
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
