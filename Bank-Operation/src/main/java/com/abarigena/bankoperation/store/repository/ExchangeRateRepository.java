package com.abarigena.bankoperation.store.repository;

import com.abarigena.bankoperation.store.entity.ExchangeRate;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями Курсов Валют (ExchangeRate),
 * хранящимися в базе данных Apache Cassandra.
 * Использует Spring Data Cassandra.
 */
public interface ExchangeRateRepository extends CassandraRepository<ExchangeRate, UUID> {
    /**
     * Находит самый последний доступный курс обмена для указанной валютной пары.
     * Осуществляет поиск по ключам партиционирования (from_currency, to_currency)
     * и сортирует результаты по кластерному ключу date в порядке убывания,
     * возвращая только первую (самую свежую) запись.
     *
     * @param fromCurrency Код исходной валюты (например, "RUB").
     * @param toCurrency   Код целевой валюты (например, "USD").
     * @return Optional, содержащий самый свежий курс обмена для пары, или пустой Optional, если для этой пары нет ни одной записи.
     */
    @Query("SELECT * FROM exchange_rates WHERE from_currency = ?0 AND to_currency = ?1 ORDER BY date DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRateForCurrencyPair(String fromCurrency, String toCurrency);

    /**
     * Находит курс обмена для указанной валютной пары на конкретную дату.
     * Осуществляет поиск по ключам партиционирования (from_currency, to_currency)
     * и кластерному ключу date.
     * Использует LIMIT 1, так как теоретически могут быть дубликаты,
     * для гарантии возврата только одного результата.
     *
     * @param fromCurrency Код исходной валюты (например, "RUB").
     * @param toCurrency   Код целевой валюты (например, "USD").
     * @param date         Конкретная дата, на которую ищется курс.
     * @return Optional, содержащий курс обмена на указанную дату, или пустой Optional, если запись на эту дату не найдена.
     */
    @Query("SELECT * FROM exchange_rates WHERE from_currency = ?0 AND to_currency = ?1 AND date = ?2 LIMIT 1")
    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndDate(String fromCurrency, String toCurrency, LocalDate date);

    /**
     * Находит все курсы, сохраненные на указанную дату.
     *
     * @param date Дата, на которую нужны курсы.
     * @return Список курсов на указанную дату.
     */
    @Query("SELECT * FROM exchange_rates WHERE date = :rateDate ALLOW FILTERING")
    List<ExchangeRate> findAllByDate(@Param("rateDate") LocalDate date);
}
