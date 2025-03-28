package com.abarigena.bankoperation.controller;

import com.abarigena.bankoperation.dto.LimitExceededTransactionDTO;
import com.abarigena.bankoperation.dto.TransactionDTO;
import com.abarigena.bankoperation.service.TransactionService;
import com.abarigena.bankoperation.store.entity.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для обработки HTTP-запросов, связанных с банковскими транзакциями.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "API для работы с транзакциями и лимитами")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    /**
     * Обрабатывает POST-запрос для регистрации новой транзакции.
     * Валидирует входящие данные {@link TransactionDTO}. Конвертирует сумму в USD,
     * проверяет на превышение месячного лимита и сохраняет транзакцию.
     *
     * @param transactionDTO DTO с данными новой транзакции, полученный из тела запроса.
     * @return ResponseEntity с сохраненной сущностью транзакции.
     */
    @Operation(summary = "Зарегистрировать новую транзакцию",
            description = "Принимает данные о новой транзакции, конвертирует сумму в USD, проверяет на превышение месячного лимита и сохраняет.")
    @PostMapping
    public ResponseEntity<Transaction> receiveTransaction(@Valid @RequestBody TransactionDTO transactionDTO){
        log.info("Получен запрос на регистрацию транзакции: {}", transactionDTO);
        try {
            Transaction savedTransaction = transactionService.processAndSaveTransaction(transactionDTO);
            log.info("Транзакция успешно обработана и сохранена: {}", savedTransaction.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedTransaction);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка обработки транзакции: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при обработке транзакции", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * GET /api/transactions/exceeded
     * Получает все транзакции, превысившие месячный лимит,
     * включая детали лимита, который действовал на момент транзакции.
     *
     * @return ResponseEntity, содержащий список LimitExceededTransactionDTO или ошибку.
     */
    @Operation(summary = "Получить транзакции, превысившие лимит",
            description = "Возвращает список всех транзакций, которые были помечены как превысившие месячный лимит, " +
                    "вместе с деталями лимита, который действовал на момент совершения каждой транзакции.")
    @GetMapping("/exceeded")
    public ResponseEntity<List<LimitExceededTransactionDTO>> getExceededTransactions() {
        log.info("Получен запрос на получение транзакций, превышающих лимиты.");
        try {
            List<LimitExceededTransactionDTO> transactions = transactionService.getExceededTransactionsWithLimitDetails();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Ошибка при получении транзакций, превысивших лимит", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
