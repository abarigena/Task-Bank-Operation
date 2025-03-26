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
                    if (dto.getValues().isEmpty() || dto.getValues().get(0).getClose() == null) {
                        // Если данных от API нет, используем последний доступный курс
                        try {
                            BigDecimal lastClosePrice = exchangeRateService.getExchangeRate(
                                    fromCurrency,
                                    toCurrency,
                                    LocalDate.now()
                            );

                            ExchangeRate fallbackRate = new ExchangeRate(
                                    fromCurrency,
                                    toCurrency,
                                    LocalDate.now(),
                                    UUID.randomUUID(),
                                    lastClosePrice,
                                    lastClosePrice
                            );

                            log.warn("API не вернуло данные. Использован последний доступный курс.");
                            return Mono.just(fallbackRate);
                        } catch (IllegalArgumentException ex) {
                            log.error("Нет доступных данных о курсе для {} -> {}", fromCurrency, toCurrency);
                            return Mono.empty();
                        }
                    }

                    TwelveDataExchangeRateDTO.Value value = dto.getValues().get(0);
                    BigDecimal closePrice = new BigDecimal(value.getClose());

                    // Пытаемся получить предыдущий курс для previousClosePrice
                    BigDecimal previousClosePrice;
                    try {
                        previousClosePrice = exchangeRateService.getExchangeRate(
                                fromCurrency,
                                toCurrency,
                                LocalDate.now().minusDays(1)
                        );
                    } catch (IllegalArgumentException ex) {
                        // Если нет предыдущего курса, используем текущий
                        previousClosePrice = closePrice;
                    }

                    ExchangeRate rate = new ExchangeRate(
                            fromCurrency,
                            toCurrency,
                            LocalDate.now(),
                            UUID.randomUUID(),
                            closePrice,
                            previousClosePrice
                    );

                    log.info("Получен курс для {} -> {}: закрытие={}, предыдущее закрытие={}",
                            fromCurrency, toCurrency, rate.getClosePrice(), rate.getPreviousClosePrice());

                    return Mono.just(rate);
                })
                .onErrorResume(ex -> {
                    log.error("Ошибка при получении курса: {}", ex.getMessage());
                    return Mono.empty();
                });
    }
}
