package com.abarigena.bankoperation.controller;

import com.abarigena.bankoperation.dto.LimitDTO;
import com.abarigena.bankoperation.service.LimitService;
import com.abarigena.bankoperation.store.entity.ExpenseLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Контроллер для обработки HTTP-запросов, связанных с управлением лимитами расходов.
 */
@RestController
@RequestMapping("/api/limits")
@RequiredArgsConstructor
@Tag(name = "Expense Limits", description = "API для управления месячными лимитами расходов")
public class LimitController {
    private static final Logger log = LoggerFactory.getLogger(LimitController.class);
    private final LimitService limitService;

    /**
     * Обрабатывает POST-запрос для установки нового месячного лимита расходов.
     * Валидирует входящие данные {@link LimitDTO}.
     *
     * @param limitDTO DTO с данными нового лимита (сумма и категория), полученный из тела запроса.
     * @return ResponseEntity с созданной сущностью лимита или ResponseEntity с ошибкой.
     */
    @Operation(summary = "Установить новый лимит расходов",
            description = "Устанавливает новый месячный лимит для указанной категории расходов (товары или услуги). " +
                    "Дата установки лимита выставляется автоматически на текущий момент. " +
                    "Старые лимиты не удаляются, система всегда использует последний установленный лимит для категории на момент транзакции.")
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
