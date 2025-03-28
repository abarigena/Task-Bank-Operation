package com.abarigena.bankoperation.service;

import com.abarigena.bankoperation.dto.ExchangeRateDTO;
import com.abarigena.bankoperation.mapper.ExchangeRateMapper;
import com.abarigena.bankoperation.store.entity.ExchangeRate;
import com.abarigena.bankoperation.store.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper exchangeRateMapper;


    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    // Определяем точность и округление для кросс-курса
    private static final int CROSS_RATE_SCALE = 8; // Больше знаков для промежуточных расчетов
    private static final RoundingMode CROSS_RATE_ROUNDING = RoundingMode.HALF_UP;

    /**
     * Сохраняет курс обмена, если для данной валютной пары и даты его еще нет.
     *
     * @param rate Курс для сохранения.
     * @return true, если курс был сохранен, false, если он уже существовал.
     */
    public boolean saveExchangeRateIfNotExists(ExchangeRate rate) {
        // Проверяем существование записи для данной валютной пары и даты
        Optional<ExchangeRate> existingRate = exchangeRateRepository
                .findByFromCurrencyAndToCurrencyAndDate(
                        rate.getFromCurrency(),
                        rate.getToCurrency(),
                        rate.getDate()
                );

        if (existingRate.isPresent()) {
            log.info("Курс для {} -> {} на дату {} уже существует",
                    rate.getFromCurrency(),
                    rate.getToCurrency(),
                    rate.getDate());
            return false;
        }

        // Если записи нет, сохраняем новую
        exchangeRateRepository.save(rate);
        log.info("Сохранен курс валют: валютная пара {} -> {}, курс: {}",
                rate.getFromCurrency(),
                rate.getToCurrency(),
                rate.getClosePrice());
        return true;
    }

    /**
     * Возвращает курс обмена для указанной пары валют на заданную дату.
     * Сначала ищет курс на точную дату. Если не находит, использует последний доступный курс (fallback).
     * Если запрашивается курс к USD, и прямой курс XXX/USD не найден,
     * пытается вычислить его через кросс-курс (на данный момент реализовано для KZT/USD через KZT/RUB и RUB/USD).
     *
     * @param fromCurrency Валюта, ИЗ которой конвертируем.
     * @param toCurrency   Валюта, В которую конвертируем (ожидается "USD").
     * @param date         Дата, на которую нужен курс.
     * @return Курс обмена (сколько единиц toCurrency за 1 единицу fromCurrency).
     * @throws IllegalArgumentException если курс не найден или не может быть вычислен.
     * @throws UnsupportedOperationException если целевая валюта не USD (можно расширить в будущем).
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency, LocalDate date) {
        log.debug("Запрос курса обмена для пары {} -> {} на дату {}", fromCurrency, toCurrency, date);

        // Если конвертируем X в X, курс равен 1
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // 1. Попытка найти прямой курс XXX/YYY
        Optional<BigDecimal> directRate = findRateOptional(fromCurrency, toCurrency, date);
        if (directRate.isPresent()) {
            log.debug("Найден прямой курс {}/{} на {}: {}", fromCurrency, toCurrency, date, directRate.get());
            return directRate.get();
        }
        log.warn("Прямой курс {}/{} на {} не найден в базе.", fromCurrency, toCurrency, date);

        // Если прямой курс не найден И исходная валюта KZT, пытаемся вычислить кросс-курс
        if("USD".equals(toCurrency) && "KZT".equals(fromCurrency)) {
            log.info("Пытаемся вычислить кросс-курс KZT/USD через KZT/RUB и RUB/USD на дату {}", date);
            Optional<BigDecimal> kztRubRateOpt = findRateOptional("KZT", "RUB", date);
            Optional<BigDecimal> rubUsdRateOpt = findRateOptional("RUB", "USD", date);

            // Вычисляем KZT/USD = KZT/RUB * RUB/USD
            if(kztRubRateOpt.isPresent() && rubUsdRateOpt.isPresent()) {
                BigDecimal kztRubRate = kztRubRateOpt.get();
                BigDecimal rubUsdRate = rubUsdRateOpt.get();

                BigDecimal calculatedKztUsdRate = kztRubRate.multiply(rubUsdRate)
                        .setScale(CROSS_RATE_SCALE, CROSS_RATE_ROUNDING); // Округление

                log.info("Кросс-курс KZT/USD на {} вычислен: {} (из KZT/RUB={} * RUB/USD={})",
                        date, calculatedKztUsdRate, kztRubRate, rubUsdRate);

                cacheCalculatedRate("KZT", "USD", date, calculatedKztUsdRate, kztRubRate, rubUsdRate);

                return calculatedKztUsdRate;
            }else {
                String missing = "";
                if (kztRubRateOpt.isEmpty()) missing += " KZT/RUB";
                if (rubUsdRateOpt.isEmpty()) missing += " RUB/USD";
                log.error("Невозможно вычислить кросс-курс KZT/USD на {}: отсутствуют промежуточные курсы:{}.", date, missing.trim());
                throw new IllegalArgumentException("Не найдены необходимые промежуточные курсы для расчета KZT/USD на " + date);
            }
        }
        log.error("Не удалось найти или вычислить курс для {}/{} на {}", fromCurrency, toCurrency, date);
        throw new IllegalArgumentException("Курс обмена не найден для " + fromCurrency + "/" + toCurrency + " на " + date);
    }

    /**
     * Вспомогательный метод для поиска курса.
     * Сначала ищет на точную дату, потом последний доступный (fallback).
     *
     * @param from Валюта ИЗ
     * @param to   Валюта В
     * @param date Дата
     * @return Optional с курсом или пустой Optional.
     */
    public Optional<BigDecimal> findRateOptional(String from, String to, LocalDate date) {
        // Ищем на конкретную дату
        Optional<ExchangeRate> rateOnDate = exchangeRateRepository.findByFromCurrencyAndToCurrencyAndDate(from, to, date);
        if (rateOnDate.isPresent()) {
            return Optional.of(rateOnDate.get().getClosePrice());
        }

        // Если на дату нет, ищем последний доступный (fallback)
        Optional<ExchangeRate> latestRate = exchangeRateRepository.findLatestRateForCurrencyPair(from, to);
        if (latestRate.isPresent()) {
            log.warn("Курс для {}/{} не найден на дату {}. Используется последний доступный курс от {}",
                    from, to, date, latestRate.get().getDate());
            return Optional.of(latestRate.get().getClosePrice());
        }

        // Если вообще ничего нет
        return Optional.empty();
    }

    /**
     * Сохраняет вычисленный кросс-курс в базу данных для кеширования.
     * Использует курсы, на основе которых он был вычислен, как close и previous_close
     *
     * @param fromCurrency Валюта ИЗ (напр., "KZT")
     * @param toCurrency   Валюта В (напр., "USD")
     * @param date         Дата курса
     * @param calculatedRate Вычисленный курс
     * @param intermediateRate1 Первый промежуточный курс (напр., KZT/RUB)
     * @param intermediateRate2 Второй промежуточный курс (напр., RUB/USD)
     */
    private void cacheCalculatedRate(String fromCurrency, String toCurrency, LocalDate date,
                                     BigDecimal calculatedRate, BigDecimal intermediateRate1, BigDecimal intermediateRate2){
        try {
            ExchangeRate rateToCache = ExchangeRate.builder()
                    .id(UUID.randomUUID())
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .date(date)
                    .closePrice(calculatedRate)
                    .previousClosePrice(intermediateRate1.multiply(intermediateRate2).setScale(CROSS_RATE_SCALE, CROSS_RATE_ROUNDING)) // Просто пересчитываем
                    .build();

            boolean saved = saveExchangeRateIfNotExists(rateToCache);
            if (saved) {
                log.info("Вычисленный кросс-курс {}/{} на {} сохранен в кеш (базу данных).", fromCurrency, toCurrency, date);
            }
        } catch (Exception e) {
            log.error("Ошибка при попытке кеширования вычисленного кросс-курса {}/{} на {}: {}",
                    fromCurrency, toCurrency, date, e.getMessage());
        }
    }

    /**
     * Получает все доступные курсы обмена на сегодняшний день.
     *
     * @return Список DTO курсов обмена на сегодня.
     */
    public List<ExchangeRateDTO> getTodaysExchangeRates(){
        LocalDate today = LocalDate.now();
        log.info("Запрос всех курсов обмена на дату: {}", today);

        List<ExchangeRate> ratesEntities = exchangeRateRepository.findAllByDate(today);

        if (ratesEntities.isEmpty()) {
            log.warn("Курсы на дату {} не найдены.", today);
            return List.of();
        }

        log.info("Найдено {} курсов на дату {}", ratesEntities.size(), today);

        return exchangeRateMapper.toDtoList(ratesEntities);
    }
}
