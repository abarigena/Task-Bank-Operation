package com.abarigena.bankoperation.service;


import com.abarigena.bankoperation.store.entity.ExchangeRate;
import com.abarigena.bankoperation.store.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRate sampleRate;
    private final String fromCurrency = "EUR";
    private final String toCurrency = "USD";
    private final LocalDate today = LocalDate.now();
    private final LocalDate yesterday = today.minusDays(1);
    private final BigDecimal rateValue = new BigDecimal("1.10");
    private final BigDecimal previousRateValue = new BigDecimal("1.09");

    @BeforeEach
    void setUp() {
        sampleRate = ExchangeRate.builder()
                .id(UUID.randomUUID())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .date(today)
                .closePrice(rateValue)
                .previousClosePrice(previousRateValue)
                .build();
    }

    // --- Тесты для saveExchangeRateIfNotExists ---

    @Test
    @DisplayName("saveExchangeRateIfNotExists должен сохранить курс, если он еще не существует")
    void saveExchangeRateIfNotExists_shouldSave_whenRateDoesNotExist() {
        // Arrange
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenReturn(sampleRate);

        // Act
        boolean result = exchangeRateService.saveExchangeRateIfNotExists(sampleRate);

        // Assert
        // 1. Проверяем, что метод вернул true (указывая, что сохранение произошло).
        assertTrue(result);
        // 2. Проверяем (verify), что метод репозитория был вызван ровно 1 раз
        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today);
        // 3. Проверяем, что метод save репозитория был вызван ровно 1 раз
        verify(exchangeRateRepository, times(1)).save(sampleRate);
    }

    @Test
    @DisplayName("saveExchangeRateIfNotExists НЕ должен сохранять курс, если он уже существует")
    void saveExchangeRateIfNotExists_shouldNotSave_whenRateExists() {
        // Arrange
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today))
                .thenReturn(Optional.of(sampleRate));

        // Act
        boolean result = exchangeRateService.saveExchangeRateIfNotExists(sampleRate);

        // Assert
        // 1. Проверяем, что метод вернул false (указывая, что сохранение НЕ произошло).
        assertFalse(result);
        // 2. Проверяем, что метод репозитория был вызван ровно 1 раз
        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today);
        // 3. Проверяем, что метод save репозитория НЕ БЫЛ вызван НИ РАЗУ.
        verify(exchangeRateRepository, never()).save(any(ExchangeRate.class));
    }

    // --- Тесты для getExchangeRate ---

    @Test
    @DisplayName("getExchangeRate должен вернуть курс, если он существует для указанной даты")
    void getExchangeRate_shouldReturnRate_whenExistsForDate() {
        // Arrange
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today))
                .thenReturn(Optional.of(sampleRate));

        // Act
        BigDecimal result = exchangeRateService.getExchangeRate(fromCurrency, toCurrency, today);

        // Assert
        // 1. Проверяем, что возвращенное значение BigDecimal равно ожидаемому курсу rateValue.
        assertThat(result).isEqualTo(rateValue);
        // 2. Проверяем, что метод был вызван 1 раз.
        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today);
        // 3. Проверяем, что метод findLatestRateForCurrencyPair НЕ вызывался.
        verify(exchangeRateRepository, never()).findLatestRateForCurrencyPair(anyString(), anyString());
    }

    @Test
    @DisplayName("getExchangeRate должен вернуть ПОСЛЕДНИЙ курс, если курс на указанную дату не найден, но есть более старые")
    void getExchangeRate_shouldReturnLatestRate_whenNotExistsForDateButLatestExists() {
        // Arrange
        ExchangeRate latestRate = ExchangeRate.builder()
                .id(UUID.randomUUID())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .date(yesterday) // Вчерашний курс
                .closePrice(previousRateValue)
                .build();
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestRateForCurrencyPair(fromCurrency, toCurrency))
                .thenReturn(Optional.of(latestRate));

        // Act
        BigDecimal result = exchangeRateService.getExchangeRate(fromCurrency, toCurrency, today);

        // Assert
        // 1. Проверяем, что возвращенное значение равно курсу из latestRate (т.е. previousRateValue),
        assertThat(result).isEqualTo(previousRateValue);
        // 2. Проверяем, что сначала был вызван поиск по точной дате.
        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today);
        // 3. Проверяем, что затем был вызван поиск последнего курса (fallback сработал).
        verify(exchangeRateRepository, times(1)).findLatestRateForCurrencyPair(fromCurrency, toCurrency);
    }

    @Test
    @DisplayName("getExchangeRate должен выбросить исключение, если курс не найден ни на дату, ни вообще для пары")
    void getExchangeRate_shouldThrowException_whenNoRateExistsAtAll() {
        // Arrange
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestRateForCurrencyPair(fromCurrency, toCurrency))
                .thenReturn(Optional.empty());

        // Act & Assert
        // Используем assertThrows для проверки, что вызов метода сервиса приводит к выбросу
        // ожидаемого исключения (IllegalArgumentException).
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            exchangeRateService.getExchangeRate(fromCurrency, toCurrency, today);
        });

        String expectedMessagePart1 = "Курс обмена не найден для " + fromCurrency + "/" + toCurrency;
        String expectedMessagePart2 = " на " + today;
        assertThat(exception.getMessage())
                .contains(expectedMessagePart1)
                .contains(expectedMessagePart2);

        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, today);
        verify(exchangeRateRepository, times(1)).findLatestRateForCurrencyPair(fromCurrency, toCurrency);
    }

    @Test
    @DisplayName("getExchangeRate должен выбросить IllegalArgumentException, если прямой курс для не-USD валюты не найден")
    void getExchangeRate_shouldThrowIllegalArgument_whenDirectRateForNonUsdNotFound() {
        // Arrange
        String from = "JPY";
        String to = "EUR"; // Целевая валюта НЕ USD
        LocalDate testDate = LocalDate.now();

        // Имитируем, что findRateOptional НЕ нашел курс
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(from, to, testDate))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestRateForCurrencyPair(from, to))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            exchangeRateService.getExchangeRate(from, to, testDate);
        });

        // Проверяем сообщение об ошибке, которое теперь генерируется в конце метода
        assertThat(exception.getMessage())
                .isEqualTo("Курс обмена не найден для " + from + "/" + to + " на " + testDate);

        // Проверяем, что методы репозитория вызывались для поиска
        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(from, to, testDate);
        verify(exchangeRateRepository, times(1)).findLatestRateForCurrencyPair(from, to);
    }

    @Test
    @DisplayName("getExchangeRate должен выбросить исключение для KZT, если промежуточные курсы не найдены")
    void getExchangeRate_shouldThrowException_whenKztCrossRateDependenciesMissing() {
        // Arrange
        String kzt = "KZT";
        String rub = "RUB";
        String usd = "USD";

        // Имитируем отсутствие прямого KZT/USD
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(kzt, usd, today)).thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestRateForCurrencyPair(kzt, usd)).thenReturn(Optional.empty());

        // Имитируем отсутствие ОДНОГО из промежуточных курсов (например, KZT/RUB)
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(kzt, rub, today)).thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestRateForCurrencyPair(kzt, rub)).thenReturn(Optional.empty());
        // А второй (RUB/USD) пусть будет найден
        ExchangeRate rubUsdRate = ExchangeRate.builder().closePrice(new BigDecimal("0.01")).date(today).build();
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(rub, usd, today)).thenReturn(Optional.of(rubUsdRate));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            exchangeRateService.getExchangeRate(kzt, usd, today);
        });

        assertThat(exception.getMessage()).contains("Не найдены необходимые промежуточные курсы для расчета KZT/USD");

        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(kzt, usd, today); // Прямой
        verify(exchangeRateRepository, times(1)).findLatestRateForCurrencyPair(kzt, usd);      // Прямой (fallback)
        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(kzt, rub, today); // Промежуточный 1
        verify(exchangeRateRepository, times(1)).findLatestRateForCurrencyPair(kzt, rub);      // Промежуточный 1 (fallback)
        verify(exchangeRateRepository, times(1)).findByFromCurrencyAndToCurrencyAndDate(rub, usd, today); // Промежуточный 2
    }
}