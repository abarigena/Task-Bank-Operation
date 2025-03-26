package com.abarigena.bankoperation.service;

import com.abarigena.bankoperation.dto.TransactionDTO;
import com.abarigena.bankoperation.store.entity.ExpenseLimit;
import com.abarigena.bankoperation.store.entity.Transaction;
import com.abarigena.bankoperation.store.repository.ExpenseLimitRepository;
import com.abarigena.bankoperation.store.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;
    private final ExpenseLimitRepository expenseLimitRepository;

    // Константа для дефолтного лимита
    private static final BigDecimal DEFAULT_MONTHLY_LIMIT_USD = new BigDecimal("1000.00");
    // Константа для базовой валюты лимитов
    private static final String LIMIT_CURRENCY = "USD";
    // Масштаб и округление для операций с USD
    private static final int USD_SCALE = 2;
    private static final RoundingMode USD_ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Обрабатывает входящую транзакцию: конвертирует в USD, проверяет лимит и сохраняет.
     * @param dto Данные транзакции из запроса.
     * @return Сохраненная сущность транзакции.
     * @throws IllegalArgumentException если не найден курс валют.
     */
    @Transactional
    public Transaction processAndSaveTransaction(TransactionDTO dto) {
        log.debug("Начало обработки транзакции для счета {}", dto.getAccountFrom());

        Transaction transaction = mapDtoToEntity(dto);
        LocalDate transactionDate = transaction.getDateTime().toLocalDate();

        // Рассчитываем сумму в USD
        BigDecimal sumInUsd = convertToUsd(dto.getSum(), dto.getCurrencyShortname(), transactionDate);
        transaction.setSumInUsd(sumInUsd);
        log.debug("Сумма в USD рассчитана: {}", sumInUsd);

        // Определяем действующий лимит на момент транзакции
        BigDecimal applicableLimit = findApplicableLimit(transaction.getExpenseCategory(), transaction.getDateTime());
        log.debug("Действующий лимит на момент транзакции: {} USD", applicableLimit);

        // Рассчитываем траты за месяц до текущей транзакции
        ZonedDateTime monthStart = transaction.getDateTime().with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        ZonedDateTime currentTransactionTime = transaction.getDateTime();

        log.debug("Расчет трат для категории {} за период с {} по {}",
                transaction.getExpenseCategory(), monthStart, currentTransactionTime);

        // Берем начало текущего месяца и время начала транзакции dto.getDatetime()
        BigDecimal spentInMonthBeforeCurrent = transactionRepository.calculateSpendingInUsdForPeriod(
                transaction.getExpenseCategory(),
                monthStart,
                transaction.getDateTime() // Суммируем все ДО момента текущей транзакции
        );
        log.debug("Потрачено в текущем месяце до этой транзакции: {} USD", spentInMonthBeforeCurrent);

        // Проверяем превышение лимита
        BigDecimal totalSpendingIncludingCurrent = spentInMonthBeforeCurrent.add(transaction.getSumInUsd());
        boolean limitExceeded = totalSpendingIncludingCurrent.compareTo(applicableLimit) > 0;
        transaction.setLimitExceeded(limitExceeded);
        log.debug("Общие траты с учетом текущей: {} USD. Лимит превышен: {}", totalSpendingIncludingCurrent, limitExceeded);

        // 6. Сохраняем транзакцию
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Транзакция {} сохранена с флагом limitExceeded={}", savedTransaction.getId(), limitExceeded);

        return savedTransaction;

    }

    /**
     * Вспомогательный метод для маппинга DTO на сущность.
     */
    private Transaction mapDtoToEntity(TransactionDTO dto) {
        Transaction transaction = new Transaction();
        transaction.setAccountFrom(dto.getAccountFrom());
        transaction.setAccountTo(dto.getAccountTo());
        transaction.setCurrencyShortname(dto.getCurrencyShortname());
        transaction.setSum(dto.getSum());
        transaction.setExpenseCategory(dto.getExpenseCategory());

        ZonedDateTime transactionTime;
        if (dto.getDateTime() != null) {
            transactionTime = dto.getDateTime();
            log.debug("Используем dateTime из DTO: {}", transactionTime);
        } else {
            transactionTime = ZonedDateTime.now();
            log.debug("Поле dateTime в DTO не указано, используем текущее время: {}", transactionTime);
        }
        transaction.setDateTime(transactionTime);

        return transaction;
    }

    /**
     * Конвертирует сумму из указанной валюты в USD по курсу на заданную дату.
     */
    private BigDecimal convertToUsd(BigDecimal amount, String currency, LocalDate date) {
        if (currency.equals(LIMIT_CURRENCY)) {
            return amount.setScale(USD_SCALE, USD_ROUNDING_MODE); // Если уже в USD, просто округляем
        }

        // Получаем курс (сколько USD стоит 1 единица 'currency', т.е. USD/'currency')
        try{
            BigDecimal exchangeRate = exchangeRateService.getExchangeRate(currency, LIMIT_CURRENCY, date);
            if(exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) < 0) {
                log.error("Получен невалидный курс для {}/{} на {}: {}", currency, LIMIT_CURRENCY, date, exchangeRate);
                throw new IllegalArgumentException("Невалидный курс обмена для " + currency + " на " + date);
            }

            BigDecimal amountInUsd = amount.multiply(exchangeRate)
                    .setScale(USD_SCALE, USD_ROUNDING_MODE);

            log.debug("Результат конвертации: {} {} -> {} USD", amount, currency, amountInUsd);
            return amountInUsd;

        } catch (IllegalArgumentException e) {

            log.error("Не удалось получить курс для {}/{} на {}: {}", currency, LIMIT_CURRENCY, date, e.getMessage());
            throw e; // Перебрасываем исключение, чтобы контроллер мог его обработать
        } catch (ArithmeticException e) {

            log.error("Ошибка деления при конвертации {} {} в USD на дату {}", amount, currency, date, e);
            throw new IllegalArgumentException("Ошибка расчета суммы в USD для " + currency + " на " + date);
        }
    }

    /**
     * Находит лимит расходов в USD, действующий для данной категории на указанную дату и время.
     * Возвращает DEFAULT_MONTHLY_LIMIT_USD, если лимит не установлен.
     */
    private BigDecimal findApplicableLimit(Transaction.ExpenseCategory category, ZonedDateTime dateTime) {
        Optional<ExpenseLimit> limitOptional = expenseLimitRepository
                .findLimitValidAtDateTime(category, dateTime);

        return limitOptional
                .map(ExpenseLimit::getLimitSum) // Если лимит найден, берем его сумму
                .orElse(DEFAULT_MONTHLY_LIMIT_USD); // Иначе используем дефолтное значение
    }
}
