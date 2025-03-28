package com.abarigena.bankoperation.client;

import com.abarigena.bankoperation.dto.TwelveDataExchangeRateDTO;
import com.abarigena.bankoperation.service.ExchangeRateService;
import com.abarigena.bankoperation.store.entity.ExchangeRate;
import com.abarigena.bankoperation.store.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TwelveDataExchangeRateClient {
    private final WebClient webClient;
    private final ExchangeRateService exchangeRateService;

    private static final Logger log = LoggerFactory.getLogger(TwelveDataExchangeRateClient.class);

    @Value("${twelvedata.api.key}")
    private String apiKey;

    @Value("${app.exchange.currencies:EUR/USD,RUB/USD}")
    private List<String> currencies;

    @Scheduled(cron = "0 0 8 * * MON-FRI")
    @PostConstruct
    public void updateExchangeRates() {

        log.info("Начало обновления курсов валют для {} пар", currencies.size());

        currencies.forEach(pair -> {
            String[] currencyPair = pair.split("/");
            log.debug("Загрузка курса для валютной пары: {}", pair);

            fetchExchangeRate(currencyPair[0], currencyPair[1])
                    .subscribe(
                            exchangeRate -> {
                                boolean saved = exchangeRateService.saveExchangeRateIfNotExists(exchangeRate);
                                if (saved) {
                                    log.info("Новый курс сохранен для пары {}", pair);
                                }
                            },
                            error -> log.error("Ошибка получения курса валют для пары {}: {}",
                                    pair, error.getMessage())
                    );
        });

        log.info("Завершено обновление курсов валют");
    }

    Mono<ExchangeRate> fetchExchangeRate(String fromCurrency, String toCurrency) {
        log.debug("Запрос курса для {} -> {}", fromCurrency, toCurrency);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/time_series")
                        .queryParam("symbol", fromCurrency + "/" + toCurrency)
                        .queryParam("interval", "1day")
                        .queryParam("outputsize", "1")
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(TwelveDataExchangeRateDTO.class)
                .flatMap(dto -> {
                    if (dto == null || dto.getValues() == null || dto.getValues().isEmpty()) {
                        log.warn("API Twelve Data не вернуло данных (или вернуло null) для {} -> {}. Попытка использовать fallback.", fromCurrency, toCurrency);
                        // Если данных от API нет, используем последний доступный курс (fallback)
                        try {
                            // Используем findRateOptional, который включает fallback
                            Optional<BigDecimal> lastClosePriceOpt = exchangeRateService.findRateOptional(
                                    fromCurrency,
                                    toCurrency,
                                    LocalDate.now() // Ищем на сегодня
                            );

                            if (lastClosePriceOpt.isEmpty()) {
                                log.error("Fallback невозможен: Нет доступных данных о курсе для {} -> {}", fromCurrency, toCurrency);
                                return Mono.empty(); // Если и fallback не дал результата
                            }

                            BigDecimal lastClosePrice = lastClosePriceOpt.get();
                            ExchangeRate fallbackRate = new ExchangeRate(
                                    fromCurrency,
                                    toCurrency,
                                    LocalDate.now(),
                                    UUID.randomUUID(),
                                    lastClosePrice,
                                    lastClosePrice
                            );
                            log.info("Использован fallback курс для {} -> {}: {}", fromCurrency, toCurrency, lastClosePrice);
                            return Mono.just(fallbackRate); // Возвращаем fallback

                        } catch (Exception ex) { // Ловим любые исключения при поиске fallback
                            log.error("Ошибка при поиске fallback курса для {} -> {}: {}", fromCurrency, toCurrency, ex.getMessage());
                            return Mono.empty();
                        }
                    }

                    // --- Если данные от API есть
                    TwelveDataExchangeRateDTO.Value value = dto.getValues().get(0);
                    // Добавим проверку на null и для самого close
                    if (value.getClose() == null) {
                        log.error("API Twelve Data вернуло запись, но поле 'close' равно null для {} -> {}. Обработка невозможна.", fromCurrency, toCurrency);
                        return Mono.empty(); // Или можно попытаться использовать fallback здесь тоже
                    }
                    BigDecimal closePrice = new BigDecimal(value.getClose());

                    // Получаем предыдущий курс для previousClosePrice, используя findRateOptional
                    BigDecimal previousClosePrice = exchangeRateService.findRateOptional(
                                    fromCurrency,
                                    toCurrency,
                                    LocalDate.now().minusDays(1)
                            )
                            .orElse(closePrice); // Если не найден, используем текущий

                    ExchangeRate rate = new ExchangeRate(
                            fromCurrency,
                            toCurrency,
                            LocalDate.now(),
                            UUID.randomUUID(),
                            closePrice,
                            previousClosePrice
                    );

                    log.info("Получен курс от API для {} -> {}: закрытие={}, предыдущее закрытие={}",
                            fromCurrency, toCurrency, rate.getClosePrice(), rate.getPreviousClosePrice());

                    return Mono.just(rate);
                })
                .onErrorResume(ex -> {
                    log.error("Ошибка при получении курса: {}", ex.getMessage());
                    return Mono.empty();
                });
    }
}
