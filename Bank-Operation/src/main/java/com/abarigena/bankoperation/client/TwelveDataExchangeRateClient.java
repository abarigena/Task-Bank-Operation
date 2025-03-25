package com.abarigena.bankoperation.client;

import com.abarigena.bankoperation.dto.TwelveDataExchangeRateDTO;
import com.abarigena.bankoperation.service.ExchangeRateService;
import com.abarigena.bankoperation.store.entity.ExchangeRate;
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


    /*@Scheduled(cron = "0 0 8 * * MON-FRI")*/
    @PostConstruct
    public void updateExchangeRates() {
        List<String> currencies = List.of("EUR/USD", "RUB/USD");

        log.info("Начало обновления курсов валют для {} пар", currencies.size());

        currencies.forEach(pair -> {
            String[] currencyPair = pair.split("/");
            log.debug("Загрузка курса для валютной пары: {}", pair);

            fetchExchangeRate(currencyPair[0], currencyPair[1])
                    .subscribe(
                            exchangeRateService::saveExchangeRate,
                            error -> log.error("Ошибка получения курса валют для пары {}: {}",
                                    pair, error.getMessage())
                    );
        });

        log.info("Завершено обновление курсов валют");
    }

    private Mono<ExchangeRate> fetchExchangeRate(String fromCurrency, String toCurrency) {
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
                        // Если данных от API нет
                        log.error("API вернуло пустые данные для курса валют: {} -> {}", fromCurrency, toCurrency);

                        // Используем метод getExchangeRate для получения последнего доступного курса
                        BigDecimal closePrice = exchangeRateService.getExchangeRate(fromCurrency, toCurrency, LocalDate.now());

                        // Создаем новый объект ExchangeRate с текущей датой и предыдущей ценой
                        ExchangeRate updatedRate = new ExchangeRate(
                                UUID.randomUUID(),
                                fromCurrency,
                                toCurrency,
                                LocalDate.now(),
                                closePrice,  // Используем полученный курс
                                closePrice   // Текущий курс будет также предыдущей ценой
                        );

                        log.warn("API не предоставило данные, используя последний курс для валютной пары: {} -> {}",
                                fromCurrency, toCurrency);

                        return Mono.just(updatedRate);
                    }

                    TwelveDataExchangeRateDTO.Value value = dto.getValues().get(0);

                    // Получаем курс за предыдущий день
                    LocalDate previousDay = LocalDate.now().minusDays(1);
                    BigDecimal previousClosePrice = exchangeRateService.getExchangeRate(fromCurrency, toCurrency, previousDay);

                    // Преобразуем полученные данные в объект ExchangeRate
                    ExchangeRate rate = new ExchangeRate(
                            UUID.randomUUID(),
                            fromCurrency,
                            toCurrency,
                            LocalDate.now(),
                            new BigDecimal(value.getClose()),
                            previousClosePrice
                    );

                    log.info("Получен курс для {} -> {}: закрытие={}",
                            fromCurrency, toCurrency, rate.getClosePrice());

                    return Mono.just(rate);
                });
    }
}
