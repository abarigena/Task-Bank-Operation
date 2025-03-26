package com.abarigena.bankoperation.service;

import com.abarigena.bankoperation.dto.LimitDTO;
import com.abarigena.bankoperation.store.entity.ExpenseLimit;
import com.abarigena.bankoperation.store.repository.ExpenseLimitRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class LimitService {

    private static final Logger log = LoggerFactory.getLogger(LimitService.class);
    private final ExpenseLimitRepository expenseLimitRepository;

    private static final String LIMIT_CURRENCY = "USD";

    @Transactional
    public ExpenseLimit setNewLimit(LimitDTO dto) {
        log.debug("Установка нового лимита {} для категории {}", dto.getLimitSum(), dto.getExpenseCategory());

        ExpenseLimit newLimit = new ExpenseLimit();
        newLimit.setLimitSum(dto.getLimitSum());
        newLimit.setExpenseCategory(dto.getExpenseCategory());
        newLimit.setLimitCurrencyShortname(LIMIT_CURRENCY);
        newLimit.setLimitDateTime(ZonedDateTime.now());

        ExpenseLimit savedLimit = expenseLimitRepository.save(newLimit);
        log.info("Новый лимит {} для категории {} сохранен с ID {}",
                savedLimit.getLimitSum(), savedLimit.getExpenseCategory(), savedLimit.getId());

        return savedLimit;
    }
}
