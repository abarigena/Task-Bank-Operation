package com.abarigena.bankoperation.controller;

import com.abarigena.bankoperation.dto.LimitExceededTransactionDTO;
import com.abarigena.bankoperation.dto.TransactionDTO;
import com.abarigena.bankoperation.service.TransactionService;
import com.abarigena.bankoperation.store.entity.Transaction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

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
            // Обработка других непредвиденных ошибок
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
