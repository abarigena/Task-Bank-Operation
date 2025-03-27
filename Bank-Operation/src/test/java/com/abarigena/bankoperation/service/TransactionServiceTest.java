package com.abarigena.bankoperation.service;

import com.abarigena.bankoperation.dto.LimitExceededTransactionDTO;
import com.abarigena.bankoperation.dto.TransactionDTO;
import com.abarigena.bankoperation.mapper.TransactionMapper;
import com.abarigena.bankoperation.store.entity.ExpenseLimit;
import com.abarigena.bankoperation.store.entity.Transaction;
import com.abarigena.bankoperation.store.repository.ExpenseLimitRepository;
import com.abarigena.bankoperation.store.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private ExpenseLimitRepository expenseLimitRepository;
    @Mock
    private TransactionMapper transactionMapper; // Мокаем маппер

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    ArgumentCaptor<Transaction> transactionCaptor;

    private TransactionDTO transactionDTO;
    private Transaction transactionEntity;
    private ExpenseLimit currentLimit;
    private final ZonedDateTime transactionTime = ZonedDateTime.of(2024, 3, 15, 10, 30, 0, 0, ZoneId.systemDefault());
    private final BigDecimal defaultLimit = new BigDecimal("1000.00");
    private final BigDecimal customLimitAmount = new BigDecimal("1500.00");
    private final BigDecimal rubExchangeRate = new BigDecimal("0.011"); // Курс RUB/USD

    @BeforeEach
    void setUp() {
        transactionDTO = new TransactionDTO();
        transactionDTO.setAccountFrom("123");
        transactionDTO.setAccountTo("456");
        transactionDTO.setCurrencyShortname("RUB");
        transactionDTO.setSum(new BigDecimal("100000.00")); // 100,000 RUB
        transactionDTO.setExpenseCategory(Transaction.ExpenseCategory.PRODUCT);
        transactionDTO.setDateTime(transactionTime);

        transactionEntity = new Transaction();
        transactionEntity.setAccountFrom(transactionDTO.getAccountFrom());
        transactionEntity.setAccountTo(transactionDTO.getAccountTo());
        transactionEntity.setCurrencyShortname(transactionDTO.getCurrencyShortname());
        transactionEntity.setSum(transactionDTO.getSum());
        transactionEntity.setExpenseCategory(transactionDTO.getExpenseCategory());
        transactionEntity.setDateTime(transactionTime);
        lenient().when(transactionMapper.toEntity(any(TransactionDTO.class))).thenReturn(transactionEntity);

        currentLimit = new ExpenseLimit();
        currentLimit.setId(UUID.randomUUID());
        currentLimit.setLimitSum(customLimitAmount); // 1500 USD
        currentLimit.setExpenseCategory(Transaction.ExpenseCategory.PRODUCT);
        currentLimit.setLimitDateTime(transactionTime.minusDays(10));
        currentLimit.setLimitCurrencyShortname("USD");

        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
    }

    // Вспомогательный метод для мока курса RUB/USD
    private void mockExchangeRateRub() {
        when(exchangeRateService.getExchangeRate(eq("RUB"), eq("USD"), eq(transactionTime.toLocalDate())))
                .thenReturn(rubExchangeRate);
    }

    private void mockApplicableLimit(Optional<ExpenseLimit> limitOptional) {
        when(expenseLimitRepository.findLimitValidAtDateTime(
                eq(Transaction.ExpenseCategory.PRODUCT),
                eq(transactionTime)))
                .thenReturn(limitOptional);
    }

    private void mockSpendingCalculation(BigDecimal spentAmount) {
        ZonedDateTime monthStart = transactionTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        when(transactionRepository.calculateSpendingInUsdForPeriod(
                eq(Transaction.ExpenseCategory.PRODUCT),
                eq(monthStart),
                eq(transactionTime)))
                .thenReturn(spentAmount);
    }

    // --- Тесты с RUB ---

    @Test
    @DisplayName("Обработка транзакции (RUB): лимит НЕ превышен (с использованием кастомного лимита)")
    void processAndSaveTransaction_Rub_shouldNotExceedLimit_withCustomLimit() {
        // Arrange
        mockExchangeRateRub();
        mockApplicableLimit(Optional.of(currentLimit)); // Действует лимит 1500 USD
        mockSpendingCalculation(new BigDecimal("150.00")); // Потрачено 150 USD до этой транзакции

        // Сумма текущей транзакции в USD = 100000 * 0.011 = 1100.00 USD
        // Ожидаемая сумма в USD после округления до 2 знаков
        BigDecimal expectedUsdSum = new BigDecimal("1100.00");
        // Итого = 150 + 1100 = 1250 USD, что < 1500 USD

        // Act
        Transaction saved = transactionService.processAndSaveTransaction(transactionDTO);

        // Assert
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction captured = transactionCaptor.getValue();

        assertThat(saved.getLimitExceeded()).isFalse();
        assertThat(captured.getLimitExceeded()).isFalse();
        // Проверяем расчет USD
        assertThat(captured.getSumInUsd()).isEqualByComparingTo(expectedUsdSum);
        verify(expenseLimitRepository).findLimitValidAtDateTime(any(), any());
        verify(transactionRepository).calculateSpendingInUsdForPeriod(any(), any(), any());
        // Проверяем вызов сервиса курса для RUB
        verify(exchangeRateService).getExchangeRate(eq("RUB"), eq("USD"), any(LocalDate.class));
    }

    @Test
    @DisplayName("Обработка транзакции (RUB): лимит ПРЕВЫШЕН (с использованием кастомного лимита)")
    void processAndSaveTransaction_Rub_shouldExceedLimit_withCustomLimit() {
        // Arrange
        mockExchangeRateRub();
        mockApplicableLimit(Optional.of(currentLimit)); // Действует лимит 1500 USD
        mockSpendingCalculation(new BigDecimal("500.00")); // Потрачено 500 USD до этой транзакции

        // Сумма текущей транзакции в USD = 100000 * 0.011 = 1100.00 USD
        BigDecimal expectedUsdSum = new BigDecimal("1100.00");
        // Итого = 500 + 1100 = 1600 USD, что > 1500 USD

        // Act
        Transaction saved = transactionService.processAndSaveTransaction(transactionDTO);

        // Assert
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction captured = transactionCaptor.getValue();

        assertThat(saved.getLimitExceeded()).isTrue();
        assertThat(captured.getLimitExceeded()).isTrue();
        assertThat(captured.getSumInUsd()).isEqualByComparingTo(expectedUsdSum);
    }

    @Test
    @DisplayName("Обработка транзакции (RUB): лимит НЕ превышен (с использованием ДЕФОЛТНОГО лимита)")
    void processAndSaveTransaction_Rub_shouldNotExceedLimit_withDefaultLimit() {
        // Arrange
        mockExchangeRateRub();
        mockApplicableLimit(Optional.empty()); // Кастомный лимит не найден, используется дефолтный 1000 USD
        transactionDTO.setSum(new BigDecimal("80000.00")); // 80,000 RUB
        transactionEntity.setSum(new BigDecimal("80000.00")); // Обновляем и в сущности для мока
        when(transactionMapper.toEntity(any(TransactionDTO.class))).thenReturn(transactionEntity); // Перенастраиваем мок

        mockSpendingCalculation(new BigDecimal("100.00")); // Потрачено 100 USD

        // Сумма текущей транзакции в USD = 80000 * 0.011 = 880.00 USD
        BigDecimal expectedUsdSum = new BigDecimal("880.00");
        // Итого = 100 + 880 = 980 USD, что < 1000 USD

        // Act
        Transaction saved = transactionService.processAndSaveTransaction(transactionDTO);

        // Assert
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction captured = transactionCaptor.getValue();
        assertThat(captured.getLimitExceeded()).isFalse();
        assertThat(captured.getSumInUsd()).isEqualByComparingTo(expectedUsdSum);
    }


    @Test
    @DisplayName("Обработка транзакции (RUB): лимит ПРЕВЫШЕН (с использованием ДЕФОЛТНОГО лимита)")
    void processAndSaveTransaction_Rub_shouldExceedLimit_withDefaultLimit() {
        // Arrange
        mockExchangeRateRub();
        mockApplicableLimit(Optional.empty()); // Дефолтный лимит 1000 USD
        mockSpendingCalculation(new BigDecimal("0.00")); // Потрачено 0 USD

        // Сумма текущей транзакции в USD = 100000 * 0.011 = 1100.00 USD (используем сумму из setUp)
        BigDecimal expectedUsdSum = new BigDecimal("1100.00");
        // Итого = 0 + 1100 = 1100 USD, что > 1000 USD

        // Act
        Transaction saved = transactionService.processAndSaveTransaction(transactionDTO);

        // Assert
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction captured = transactionCaptor.getValue();
        assertThat(captured.getLimitExceeded()).isTrue();
        assertThat(captured.getSumInUsd()).isEqualByComparingTo(expectedUsdSum);
    }

    @Test
    @DisplayName("Обработка транзакции: валюта уже USD, курс не запрашивается")
    void processAndSaveTransaction_shouldNotCallExchangeRate_whenCurrencyIsUsd() {
        // Arrange
        transactionDTO.setCurrencyShortname("USD");
        transactionDTO.setSum(new BigDecimal("300.00"));
        transactionEntity.setCurrencyShortname("USD");
        transactionEntity.setSum(new BigDecimal("300.00"));
        when(transactionMapper.toEntity(any(TransactionDTO.class))).thenReturn(transactionEntity);

        // Используем кастомный лимит 1500 USD из setUp
        mockApplicableLimit(Optional.of(currentLimit));
        mockSpendingCalculation(new BigDecimal("1000.00")); // Потрачено 1000

        // Итого = 1000 + 300 = 1300 USD < 1500 USD

        // Act
        Transaction saved = transactionService.processAndSaveTransaction(transactionDTO);

        // Assert
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction captured = transactionCaptor.getValue();

        assertThat(captured.getLimitExceeded()).isFalse();
        assertThat(captured.getSumInUsd()).isEqualByComparingTo(new BigDecimal("300.00")); // Сумма в USD равна исходной

        // Главная проверка: сервис курсов НЕ вызывался
        verify(exchangeRateService, never()).getExchangeRate(anyString(), anyString(), any(LocalDate.class));
    }


    @Test
    @DisplayName("Обработка транзакции (RUB): Ошибка получения курса валют")
    void processAndSaveTransaction_Rub_shouldThrowException_whenExchangeRateNotFound() {
        // Arrange
        // Настраиваем мок сервиса курсов на выброс исключения для RUB
        when(exchangeRateService.getExchangeRate(eq("RUB"), eq("USD"), eq(transactionTime.toLocalDate())))
                .thenThrow(new IllegalArgumentException("Курс RUB не найден"));

        when(transactionMapper.toEntity(any(TransactionDTO.class))).thenReturn(transactionEntity);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.processAndSaveTransaction(transactionDTO);
        });

        assertThat(exception.getMessage()).contains("Курс RUB не найден");

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(expenseLimitRepository, never()).findLimitValidAtDateTime(any(), any());
        verify(transactionRepository, never()).calculateSpendingInUsdForPeriod(any(), any(), any());
    }

    @Test
    @DisplayName("Получение транзакций, превысивших лимит: должен вызывать репозиторий")
    void getExceededTransactionsWithLimitDetails_shouldCallRepository() {
        // Arrange
        List<LimitExceededTransactionDTO> expectedList = List.of(
                new LimitExceededTransactionDTO(UUID.randomUUID(), "111", "222", "RUB", BigDecimal.TEN, BigDecimal.ONE, Transaction.ExpenseCategory.SERVICE, ZonedDateTime.now(), BigDecimal.TEN, ZonedDateTime.now(), "USD")
        );
        when(transactionRepository.findExceededTransactionsWithLimitDetails()).thenReturn(expectedList);

        // Act
        List<LimitExceededTransactionDTO> actualList = transactionService.getExceededTransactionsWithLimitDetails();

        // Assert
        assertThat(actualList).isEqualTo(expectedList);
        verify(transactionRepository, times(1)).findExceededTransactionsWithLimitDetails();
    }

}
