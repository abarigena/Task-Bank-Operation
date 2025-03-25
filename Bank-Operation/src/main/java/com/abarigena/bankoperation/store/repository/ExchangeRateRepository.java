package com.abarigena.bankoperation.store.repository;

import com.abarigena.bankoperation.store.entity.ExchangeRate;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends CassandraRepository<ExchangeRate, UUID> {
    @Query("SELECT * FROM exchange_rates WHERE from_currency = ?0 AND to_currency = ?1 ORDER BY date DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRateForCurrencyPair(String fromCurrency, String toCurrency);

    @Query("SELECT * FROM exchange_rates WHERE from_currency = ?0 AND to_currency = ?1 AND date = ?2 LIMIT 1")
    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndDate(String fromCurrency, String toCurrency, LocalDate date);
}
