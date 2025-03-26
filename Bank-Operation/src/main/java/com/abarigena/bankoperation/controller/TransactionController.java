package com.abarigena.bankoperation.controller;

import com.abarigena.bankoperation.dto.TransactionDTO;
import com.abarigena.bankoperation.service.TransactionService;
import com.abarigena.bankoperation.store.entity.Transaction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
