package com.abarigena.bankoperation.service;

import com.abarigena.bankoperation.dto.LimitDTO;
import com.abarigena.bankoperation.mapper.LimitMapper;
import com.abarigena.bankoperation.store.entity.ExpenseLimit;
import com.abarigena.bankoperation.store.entity.Transaction;
import com.abarigena.bankoperation.store.repository.ExpenseLimitRepository;
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
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitServiceTest {

    @Mock
    private ExpenseLimitRepository expenseLimitRepository;
    @Mock
    private LimitMapper limitMapper; // Мокаем маппер

    @InjectMocks
    private LimitService limitService;

    @Captor
    ArgumentCaptor<ExpenseLimit> limitCaptor;

    private LimitDTO limitDTO;
    private ExpenseLimit mappedLimit;
    private ExpenseLimit savedLimit;

    @BeforeEach
    void setUp() {
        limitDTO = new LimitDTO();
        limitDTO.setLimitSum(new BigDecimal("1500.00"));
        limitDTO.setExpenseCategory(Transaction.ExpenseCategory.SERVICE);

        // Настройка мока маппера
        mappedLimit = new ExpenseLimit();
        mappedLimit.setLimitSum(limitDTO.getLimitSum());
        mappedLimit.setExpenseCategory(limitDTO.getExpenseCategory());
        mappedLimit.setLimitCurrencyShortname("USD"); // Ожидаем, что маппер установит USD
        // Ожидаем, что маппер установит время близкое к now()
        // В тесте мы не будем проверять точное время, а проверим через captor
        mappedLimit.setLimitDateTime(ZonedDateTime.now()); // Просто для примера
        when(limitMapper.toEntity(any(LimitDTO.class))).thenReturn(mappedLimit);

        // Настройка мока сохранения
        savedLimit = new ExpenseLimit();
        savedLimit.setId(UUID.randomUUID()); // Имитируем генерацию ID
        savedLimit.setLimitSum(mappedLimit.getLimitSum());
        savedLimit.setExpenseCategory(mappedLimit.getExpenseCategory());
        savedLimit.setLimitCurrencyShortname(mappedLimit.getLimitCurrencyShortname());
        savedLimit.setLimitDateTime(mappedLimit.getLimitDateTime());
        when(expenseLimitRepository.save(any(ExpenseLimit.class))).thenReturn(savedLimit);
    }

    @Test
    @DisplayName("Установка нового лимита: должен смапить DTO, сохранить и вернуть сущность")
    void setNewLimit_shouldMapAndSaveLimit() {
        // Arrange
        ZonedDateTime timeBeforeSave = ZonedDateTime.now(); // Время до вызова метода

        // Act
        ExpenseLimit result = limitService.setNewLimit(limitDTO);
        ZonedDateTime timeAfterSave = ZonedDateTime.now(); // Время после вызова

        // Assert
        // Проверяем, что маппер был вызван с правильным DTO
        verify(limitMapper, times(1)).toEntity(limitDTO);

        // Проверяем, что репозиторий был вызван для сохранения сущности, ПОЛУЧЕННОЙ ОТ МАППЕРА
        verify(expenseLimitRepository, times(1)).save(limitCaptor.capture());
        ExpenseLimit capturedLimit = limitCaptor.getValue();

        // Проверяем поля захваченной сущности (той, что передали в save)
        assertThat(capturedLimit.getLimitSum()).isEqualTo(limitDTO.getLimitSum());
        assertThat(capturedLimit.getExpenseCategory()).isEqualTo(limitDTO.getExpenseCategory());
        assertThat(capturedLimit.getLimitCurrencyShortname()).isEqualTo("USD");
        // Проверяем, что время установки находится между временем до и после вызова
        // Это подтверждает, что маппер установил ZonedDateTime.now()
        assertThat(capturedLimit.getLimitDateTime()).isBetween(timeBeforeSave.minusSeconds(1), timeAfterSave.plusSeconds(1)); // Добавляем секунду для надежности
        assertThat(capturedLimit.getId()).isNull(); // ID еще не должен быть установлен перед сохранением

        // Проверяем, что возвращенный результат - это сущность, которую вернул мок репозитория
        assertThat(result).isEqualTo(savedLimit);
        assertThat(result.getId()).isNotNull(); // У возвращенной сущности должен быть ID
    }
}
