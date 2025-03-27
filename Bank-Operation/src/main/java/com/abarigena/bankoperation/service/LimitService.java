package com.abarigena.bankoperation.service;

import com.abarigena.bankoperation.dto.LimitDTO;
import com.abarigena.bankoperation.mapper.LimitMapper;
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
    private final LimitMapper limitMapper;

    /**
     * Устанавливает новый лимит расходов для указанной категории.
     * Использует LimitMapper для преобразования DTO в сущность.
     * Текущая дата и время устанавливаются автоматически.
     *
     * @param dto DTO с данными нового лимита (сумма и категория).
     * @return Сохраненная сущность ExpenseLimit.
     */
    @Transactional
    public ExpenseLimit setNewLimit(LimitDTO dto) {
        log.debug("Установка нового лимита {} для категории {}", dto.getLimitSum(), dto.getExpenseCategory());

        ExpenseLimit newLimit = limitMapper.toEntity(dto);
        log.debug("DTO лимита смаплен в сущность: {}", newLimit);

        ExpenseLimit savedLimit = expenseLimitRepository.save(newLimit);
        log.info("Новый лимит {} для категории {} сохранен с ID {}",
                savedLimit.getLimitSum(), savedLimit.getExpenseCategory(), savedLimit.getId());

        return savedLimit;
    }
}
