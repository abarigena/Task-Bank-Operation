package com.abarigena.bankoperation.service;

import com.abarigena.bankoperation.store.entity.ExchangeRate;
import com.abarigena.bankoperation.store.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final ExchangeRateRepository exchangeRateRepository;

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);


    public boolean saveExchangeRateIfNotExists(ExchangeRate rate) {
        // Проверяем существование записи для данной валютной пары и даты
        Optional<ExchangeRate> existingRate = exchangeRateRepository
                .findByFromCurrencyAndToCurrencyAndDate(
                        rate.getFromCurrency(),
                        rate.getToCurrency(),
                        rate.getDate()
                );

        if (existingRate.isPresent()) {
            log.info("Курс для {} -> {} на дату {} уже существует",
                    rate.getFromCurrency(),
                    rate.getToCurrency(),
                    rate.getDate());
            return false;
        }

        // Если записи нет, сохраняем новую
        exchangeRateRepository.save(rate);
        log.info("Сохранен курс валют: валютная пара {} -> {}, курс: {}",
                rate.getFromCurrency(),
                rate.getToCurrency(),
                rate.getClosePrice());
        return true;
    }

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency, LocalDate date) {
        log.debug("Запрос курса обмена для пары {} -> {} на дату {}",
                fromCurrency, toCurrency, date);

        // Попытка найти курс по дате
        Optional<ExchangeRate> rateOptional = exchangeRateRepository
                .findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, date);

        if (rateOptional.isPresent()) {
            return rateOptional.get().getClosePrice();
        }

        // Если курса нет, берем последний доступный курс
        Optional<ExchangeRate> latestRateOptional = exchangeRateRepository
                .findLatestRateForCurrencyPair(fromCurrency, toCurrency);

        if (latestRateOptional.isPresent()) {
            log.warn("Курс для {} -> {} не найден на указанную дату. Используем последний доступный курс.",
                    fromCurrency, toCurrency);
            return latestRateOptional.get().getClosePrice();
        }

        // Если совсем нет курсов
        log.error("Курс не найден для валютной пары {} -> {}", fromCurrency, toCurrency);
        throw new IllegalArgumentException("Курс не найден для данной валютной пары");
    }
}
