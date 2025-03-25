package com.abarigena.bankoperation.store.repository;

import com.abarigena.bankoperation.store.entity.ExchangeRate;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends CassandraRepository<ExchangeRate, UUID> {

    // Поиск последнего курса обмена для пары валют
    Optional<ExchangeRate> findFirstByFromCurrencyAndToCurrencyOrderByDateDesc(String fromCurrency, String toCurrency);

    // Поиск курса обмена для конкретной даты
    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndDate(String fromCurrency, String toCurrency, LocalDate date);
}
