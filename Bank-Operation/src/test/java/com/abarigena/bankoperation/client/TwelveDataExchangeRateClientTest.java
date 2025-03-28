package com.abarigena.bankoperation.client;

import com.abarigena.bankoperation.dto.TwelveDataExchangeRateDTO;
import com.abarigena.bankoperation.service.ExchangeRateService;
import com.abarigena.bankoperation.store.entity.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwelveDataExchangeRateClientTest {

    @Mock
    private WebClient webClient; // Мокаем внешний клиент
    @Mock
    private ExchangeRateService exchangeRateService; // Мокаем сервис

    // Их нужно мокать, чтобы симулировать вызов .get().uri().retrieve().bodyToMono()
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private TwelveDataExchangeRateClient twelveDataExchangeRateClient;

    private final String fromCurrencyEur = "EUR";
    private final String toCurrencyUsd = "USD";
    private final String fromCurrencyRub = "RUB";
    private final List<String> configuredCurrencies = List.of("EUR/USD", "RUB/USD");
    private final String apiKey = "test-api-key";
    private final LocalDate today = LocalDate.now();
    private final LocalDate yesterday = today.minusDays(1);
    private final BigDecimal currentClosePrice = new BigDecimal("1.12");
    private final BigDecimal previousClosePrice = new BigDecimal("1.11");
    private final BigDecimal fallbackPrice = new BigDecimal("1.09");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(twelveDataExchangeRateClient, "apiKey", apiKey);
        ReflectionTestUtils.setField(twelveDataExchangeRateClient, "currencies", configuredCurrencies);

        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        lenient().when(exchangeRateService.findRateOptional(anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
    }

    // --- Тесты для метода fetchExchangeRate ---

    @Test
    @DisplayName("fetchExchangeRate должен вернуть курс, когда API отвечает успешно")
    void fetchExchangeRate_shouldReturnRate_whenApiCallSuccessful() {
        // Arrange
        TwelveDataExchangeRateDTO dto = new TwelveDataExchangeRateDTO(
                List.of(new TwelveDataExchangeRateDTO.Value(today.toString(), currentClosePrice.toString()))
        );
        when(responseSpec.bodyToMono(eq(TwelveDataExchangeRateDTO.class))).thenReturn(Mono.just(dto));
        // Мокаем ответ сервиса для получения предыдущего курса через findRateOptional
        when(exchangeRateService.findRateOptional(fromCurrencyEur, toCurrencyUsd, yesterday))
                .thenReturn(Optional.of(previousClosePrice));

        // Act
        Mono<ExchangeRate> resultMono = twelveDataExchangeRateClient.fetchExchangeRate(fromCurrencyEur, toCurrencyUsd);

        // Assert
        StepVerifier.create(resultMono)
                .expectNextMatches(rate ->
                        rate.getFromCurrency().equals(fromCurrencyEur) &&
                                rate.getToCurrency().equals(toCurrencyUsd) &&
                                rate.getDate().equals(today) &&
                                rate.getClosePrice().compareTo(currentClosePrice) == 0 &&
                                rate.getPreviousClosePrice().compareTo(previousClosePrice) == 0 && // Проверяем, что предыдущий курс использован
                                rate.getId() != null
                )
                .verifyComplete();

        // Verify: Проверяем, что был вызван findRateOptional для предыдущего курса
        verify(exchangeRateService, times(1)).findRateOptional(fromCurrencyEur, toCurrencyUsd, yesterday);
        verify(webClient).get();
    }

    @Test
    @DisplayName("fetchExchangeRate должен использовать текущий как предыдущий, если предыдущий не найден")
    void fetchExchangeRate_shouldUseCurrentAsPrevious_whenPreviousRateNotFound() {
        // Arrange
        TwelveDataExchangeRateDTO dto = new TwelveDataExchangeRateDTO(
                List.of(new TwelveDataExchangeRateDTO.Value(today.toString(), currentClosePrice.toString()))
        );
        when(responseSpec.bodyToMono(eq(TwelveDataExchangeRateDTO.class))).thenReturn(Mono.just(dto));
        // Имитируем, что findRateOptional НЕ нашел предыдущий курс
        when(exchangeRateService.findRateOptional(fromCurrencyEur, toCurrencyUsd, yesterday))
                .thenReturn(Optional.empty()); // Возвращаем пустой Optional

        // Act
        Mono<ExchangeRate> resultMono = twelveDataExchangeRateClient.fetchExchangeRate(fromCurrencyEur, toCurrencyUsd);

        // Assert
        StepVerifier.create(resultMono)
                .expectNextMatches(rate ->
                        rate.getClosePrice().compareTo(currentClosePrice) == 0 &&
                                // Главная проверка: previousClosePrice равен currentClosePrice
                                rate.getPreviousClosePrice().compareTo(currentClosePrice) == 0
                )
                .verifyComplete();

        // Verify
        verify(exchangeRateService, times(1)).findRateOptional(fromCurrencyEur, toCurrencyUsd, yesterday);
    }


    @Test
    @DisplayName("fetchExchangeRate должен вернуть fallback курс, когда API возвращает пустые данные")
    void fetchExchangeRate_shouldReturnFallbackRate_whenApiReturnsEmptyValues() {
        // Arrange
        TwelveDataExchangeRateDTO emptyDto = new TwelveDataExchangeRateDTO(Collections.emptyList());
        when(responseSpec.bodyToMono(eq(TwelveDataExchangeRateDTO.class))).thenReturn(Mono.just(emptyDto));
        // Имитируем, что findRateOptional НАШЕЛ fallback курс для СЕГОДНЯ
        when(exchangeRateService.findRateOptional(fromCurrencyEur, toCurrencyUsd, today))
                .thenReturn(Optional.of(fallbackPrice));

        // Act
        Mono<ExchangeRate> resultMono = twelveDataExchangeRateClient.fetchExchangeRate(fromCurrencyEur, toCurrencyUsd);

        // Assert
        StepVerifier.create(resultMono)
                .expectNextMatches(rate ->
                        rate.getClosePrice().compareTo(fallbackPrice) == 0 && // Курс - fallback
                                rate.getPreviousClosePrice().compareTo(fallbackPrice) == 0 // Предыдущий тоже fallback
                )
                .verifyComplete(); // Теперь ожидаем результат, а не onComplete()

        // Verify: Проверяем, что был вызван findRateOptional для получения fallback-курса
        verify(exchangeRateService, times(1)).findRateOptional(fromCurrencyEur, toCurrencyUsd, today);
        // Проверяем, что НЕ вызывался для вчерашнего дня
        verify(exchangeRateService, never()).findRateOptional(fromCurrencyEur, toCurrencyUsd, yesterday);
    }

    @Test
    @DisplayName("fetchExchangeRate должен вернуть пустой Mono, когда API пусто и fallback не найден")
    void fetchExchangeRate_shouldReturnEmpty_whenApiReturnsEmptyValuesAndNoFallbackExists() {
        // Arrange
        TwelveDataExchangeRateDTO emptyDto = new TwelveDataExchangeRateDTO(Collections.emptyList());
        when(responseSpec.bodyToMono(eq(TwelveDataExchangeRateDTO.class))).thenReturn(Mono.just(emptyDto));
        // Имитируем, что findRateOptional НЕ нашел fallback-курс
        when(exchangeRateService.findRateOptional(fromCurrencyEur, toCurrencyUsd, today))
                .thenReturn(Optional.empty()); // Возвращаем пустой Optional

        // Act
        Mono<ExchangeRate> resultMono = twelveDataExchangeRateClient.fetchExchangeRate(fromCurrencyEur, toCurrencyUsd);

        // Assert
        StepVerifier.create(resultMono)
                .expectNextCount(0) // Ожидаем пустой Mono
                .verifyComplete();

        // Verify: Проверяем, что вызывался findRateOptional
        verify(exchangeRateService, times(1)).findRateOptional(fromCurrencyEur, toCurrencyUsd, today);
        verify(exchangeRateService, never()).findRateOptional(fromCurrencyEur, toCurrencyUsd, yesterday);
    }


    @Test
    @DisplayName("fetchExchangeRate должен вернуть пустой Mono, когда API возвращает ошибку")
    void fetchExchangeRate_shouldReturnEmpty_whenApiCallFails() {
        // Arrange
        // Имитируем ошибку от WebClient
        when(responseSpec.bodyToMono(eq(TwelveDataExchangeRateDTO.class)))
                .thenReturn(Mono.error(new RuntimeException("API connection failed")));

        // Act
        Mono<ExchangeRate> resultMono = twelveDataExchangeRateClient.fetchExchangeRate(fromCurrencyEur, toCurrencyUsd);

        // Assert: Проверяем, что onErrorResume обработал ошибку и вернул Mono.empty()
        StepVerifier.create(resultMono)
                .expectNextCount(0)
                .verifyComplete();

        // Verify: Сервис не должен был вызываться, так как ошибка произошла раньше
        verify(exchangeRateService, never()).getExchangeRate(anyString(), anyString(), any(LocalDate.class));
    }

    // --- Тест для метода updateExchangeRates ---

    @Test
    @DisplayName("updateExchangeRates должен вызывать fetchExchangeRate для каждой сконфигурированной валютной пары")
    void updateExchangeRates_shouldCallFetchForEachCurrencyPair() {
        // --- Arrange ---

        // Мы хотим вызвать реальный updateExchangeRates(), но замокать fetchExchangeRate(), чтобы не делать реальных вызовов API.
        TwelveDataExchangeRateClient spiedClient = spy(twelveDataExchangeRateClient);

        // Возвращаем Mono.empty(), потому что в этом тесте нам не важен результат fetchExchangeRate,
        // нам важно только то, что он был ВЫЗВАН для каждой пары.
        doReturn(Mono.empty()).when(spiedClient).fetchExchangeRate(anyString(), anyString());

        // Мы знаем, что из-за Mono.empty() этот метод НЕ будет вызван.
        // Чтобы Mockito не выдал ошибку UnnecessaryStubbingException, помечаем этот мок как lenient().
        lenient().when(exchangeRateService.saveExchangeRateIfNotExists(any())).thenReturn(true);

        // --- Act ---
        // Вызываем РЕАЛЬНЫЙ метод updateExchangeRates() на ШПИОНЕ.
        // Внутри него будет выполнен цикл forEach, который будет вызывать ЗАМОКАННЫЙ fetchExchangeRate().
        spiedClient.updateExchangeRates();

        // --- Assert (Verify) ---
        // Проверяем, что метод fetchExchangeRate() был вызван на шпионе РОВНО по одному разу
        // для каждой валютной пары из списка configuredCurrencies.
        verify(spiedClient, times(1)).fetchExchangeRate("EUR", "USD");
        verify(spiedClient, times(1)).fetchExchangeRate("RUB", "USD");

        // Дополнительно проверяем, что другие методы НЕ вызывались (как и ожидалось):
        // Сервис сохранения не должен был вызваться, так как fetchExchangeRate вернул Mono.empty().
        verify(exchangeRateService, never()).saveExchangeRateIfNotExists(any());
        // WebClient не должен был вызываться, так как реальный код fetchExchangeRate не выполнялся (он был замокан).
        verify(webClient, never()).get();
    }
}