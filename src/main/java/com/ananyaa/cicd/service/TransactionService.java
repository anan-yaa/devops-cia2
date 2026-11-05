package com.ananyaa.cicd.service;

import com.ananyaa.cicd.dto.DashboardResponse;
import com.ananyaa.cicd.dto.TransactionRequest;
import com.ananyaa.cicd.dto.TransactionResponse;
import com.ananyaa.cicd.entity.Transaction;
import com.ananyaa.cicd.entity.TransactionType;
import com.ananyaa.cicd.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) { this.repository = repository; }

    public TransactionResponse create(TransactionRequest request) {
        Transaction saved = repository.save(new Transaction(request.amount(), request.type(), request.category().trim(),
                request.description(), request.date()));
        log.info("Created {} transaction id={}", saved.getType(), saved.getId());
        return TransactionResponse.from(saved);
    }

    public List<TransactionResponse> list(TransactionType type, String category) {
        List<Transaction> items;
        if (type != null && category != null && !category.isBlank())
            items = repository.findByTypeAndCategoryIgnoreCase(type, category.trim());
        else if (type != null) items = repository.findByType(type);
        else if (category != null && !category.isBlank()) items = repository.findByCategoryIgnoreCase(category.trim());
        else items = repository.findAllByOrderByDateDescIdDesc();
        return items.stream().map(TransactionResponse::from).toList();
    }

    public TransactionResponse get(Long id) { return TransactionResponse.from(find(id)); }

    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = find(id);
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(request.category().trim());
        transaction.setDescription(request.description());
        transaction.setDate(request.date());
        log.info("Updated transaction id={}", id);
        return TransactionResponse.from(repository.save(transaction));
    }

    public void delete(Long id) {
        Transaction transaction = find(id);
        repository.delete(transaction);
        log.info("Deleted transaction id={}", id);
    }

    public DashboardResponse dashboard() {
        BigDecimal income = zeroIfNull(repository.sumAmountByType(TransactionType.INCOME));
        BigDecimal expenses = zeroIfNull(repository.sumAmountByType(TransactionType.EXPENSE));
        List<TransactionResponse> recent = repository.findAllByOrderByDateDescIdDesc().stream().limit(5)
                .map(TransactionResponse::from).toList();
        return new DashboardResponse(income, expenses, income.subtract(expenses), recent);
    }

    private Transaction find(Long id) {
        return repository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private BigDecimal zeroIfNull(BigDecimal value) { return value == null ? ZERO : value; }
}
