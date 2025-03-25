package com.abarigena.bankoperation.service;

import com.abarigena.bankoperation.store.entity.ExchangeRate;
import com.abarigena.bankoperation.store.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final ExchangeRateRepository exchangeRateRepository;

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    public void saveExchangeRate(ExchangeRate rate) {
        // Сохраняем курс в базу
        exchangeRateRepository.save(rate);
        log.info("Сохранен курс валют: валютная пара {} -> {}, курс: {}",
                rate.getFromCurrency(),
                rate.getToCurrency(),
                rate.getClosePrice());
    }

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency, LocalDate date) {
        log.debug("Запрос курса обмена для пары {} -> {} на дату {}",
                fromCurrency, toCurrency, date);

        // Попытка найти курс по дате
        return exchangeRateRepository
                .findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, date)
                .map(rate -> {
                    // Если курс есть, используем его
                    log.info("Найден курс для {} -> {}: {}",
                            fromCurrency, toCurrency, rate.getClosePrice());
                    return rate.getClosePrice();
                })
                .orElseGet(() -> {
                    // Если курса нет, берем последний доступный курс
                    ExchangeRate lastRate = exchangeRateRepository
                            .findFirstByFromCurrencyAndToCurrencyOrderByDateDesc(fromCurrency, toCurrency)
                            .orElseThrow(() -> new IllegalArgumentException("Курс не найден для данной валютной пары"));

                    // Используем предыдущую цену закрытия
                    log.warn("Курс для {} -> {} не найден на указанную дату. Используем последний доступный курс: {}",
                            fromCurrency, toCurrency, lastRate.getClosePrice());
                    return lastRate.getClosePrice();
                });
    }
}
