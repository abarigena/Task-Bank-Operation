package com.abarigena.bankoperation.controller;

import com.abarigena.bankoperation.dto.LimitDTO;
import com.abarigena.bankoperation.service.LimitService;
import com.abarigena.bankoperation.store.entity.ExpenseLimit;
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
@RequestMapping("/api/limits")
@RequiredArgsConstructor
public class LimitController {
    private static final Logger log = LoggerFactory.getLogger(LimitController.class);
    private final LimitService limitService;

    @PostMapping
    public ResponseEntity<ExpenseLimit> setNewLimit(@Valid @RequestBody LimitDTO limitDTO) {
        log.info("Получен запрос на установку нового лимита: {}", limitDTO);
        try {
            ExpenseLimit savedLimit = limitService.setNewLimit(limitDTO);
            log.info("Новый лимит успешно установлен: {}", savedLimit.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLimit);
        } catch (Exception e) {
            log.error("Ошибка при установке нового лимита", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
