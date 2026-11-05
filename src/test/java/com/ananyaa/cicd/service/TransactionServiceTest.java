package com.ananyaa.cicd.service;

import com.ananyaa.cicd.dto.TransactionRequest;
import com.ananyaa.cicd.dto.TransactionResponse;
import com.ananyaa.cicd.entity.Transaction;
import com.ananyaa.cicd.entity.TransactionType;
import com.ananyaa.cicd.repository.TransactionRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock TransactionRepository repository;
    private TransactionService service;
    private Validator validator;

    @BeforeEach
    void setUp() {
        service = new TransactionService(repository);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createsValidIncomeAndExpenseTransactions() {
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse income = service.create(request("2500.00", TransactionType.INCOME, "Salary"));
        TransactionResponse expense = service.create(request("42.50", TransactionType.EXPENSE, "Food"));

        assertThat(income.type()).isEqualTo(TransactionType.INCOME);
        assertThat(income.amount()).isEqualByComparingTo("2500.00");
        assertThat(expense.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(expense.amount()).isEqualByComparingTo("42.50");
        verify(repository, times(2)).save(any(Transaction.class));
    }

    @Test
    void validationRejectsZeroNegativeAndMissingCategory() {
        assertThat(validator.validate(request("0", TransactionType.EXPENSE, "Food")))
                .extracting(v -> v.getPropertyPath().toString()).contains("amount");
        assertThat(validator.validate(request("-0.01", TransactionType.EXPENSE, "Food")))
                .extracting(v -> v.getPropertyPath().toString()).contains("amount");
        assertThat(validator.validate(request("10", TransactionType.EXPENSE, " ")))
                .extracting(v -> v.getPropertyPath().toString()).contains("category");
        verifyNoInteractions(repository);
    }

    @Test
    void retrievesAnExistingTransaction() {
        Transaction transaction = transaction("19.95", TransactionType.EXPENSE, "Books");
        when(repository.findById(7L)).thenReturn(Optional.of(transaction));

        TransactionResponse result = service.get(7L);

        assertThat(result.amount()).isEqualByComparingTo("19.95");
        assertThat(result.category()).isEqualTo("Books");
    }

    @Test
    void throwsNotFoundForMissingTransaction() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(TransactionNotFoundException.class, () -> service.get(99L));
    }

    @Test
    void updatesAllMutableTransactionDetails() {
        Transaction existing = transaction("50", TransactionType.EXPENSE, "Food");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        TransactionResponse updated = service.update(3L,
                new TransactionRequest(new BigDecimal("75.25"), TransactionType.INCOME, "Bonus", "Refund", LocalDate.parse("2026-10-01")));

        assertThat(updated.amount()).isEqualByComparingTo("75.25");
        assertThat(updated.type()).isEqualTo(TransactionType.INCOME);
        assertThat(updated.category()).isEqualTo("Bonus");
        assertThat(updated.description()).isEqualTo("Refund");
        assertThat(updated.date()).isEqualTo(LocalDate.parse("2026-10-01"));
        verify(repository).save(existing);
    }

    @Test
    void deletesExistingTransaction() {
        Transaction existing = transaction("50", TransactionType.EXPENSE, "Food");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));

        service.delete(3L);

        verify(repository).delete(existing);
    }

    @Test
    void dashboardCalculatesIncomeExpensesBalanceAndRecentTransactions() {
        when(repository.sumAmountByType(TransactionType.INCOME)).thenReturn(new BigDecimal("3000.00"));
        when(repository.sumAmountByType(TransactionType.EXPENSE)).thenReturn(new BigDecimal("725.45"));
        when(repository.findAllByOrderByDateDescIdDesc())
                .thenReturn(List.of(transaction("20", TransactionType.EXPENSE, "Food")));

        var dashboard = service.dashboard();

        assertThat(dashboard.totalIncome()).isEqualByComparingTo("3000.00");
        assertThat(dashboard.totalExpenses()).isEqualByComparingTo("725.45");
        assertThat(dashboard.currentBalance()).isEqualByComparingTo("2274.55");
        assertThat(dashboard.recentTransactions()).hasSize(1);
        verify(repository).sumAmountByType(TransactionType.INCOME);
        verify(repository).sumAmountByType(TransactionType.EXPENSE);
    }

    @Test
    void dashboardTotalsChangeAfterTransactionUpdateAndDelete() {
        Transaction existingExpense = transaction("100.00", TransactionType.EXPENSE, "Rent");
        List<Transaction> stored = new java.util.ArrayList<>(List.of(existingExpense));
        when(repository.findById(11L)).thenReturn(Optional.of(existingExpense));
        when(repository.save(existingExpense)).thenReturn(existingExpense);
        when(repository.findAllByOrderByDateDescIdDesc()).thenAnswer(invocation -> stored);
        when(repository.sumAmountByType(any(TransactionType.class))).thenAnswer(invocation -> {
            TransactionType type = invocation.getArgument(0);
            return stored.stream().filter(item -> item.getType() == type)
                    .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        });
        doAnswer(invocation -> { stored.remove(invocation.getArgument(0)); return null; })
                .when(repository).delete(existingExpense);

        assertThat(service.dashboard().totalExpenses()).isEqualByComparingTo("100.00");
        service.update(11L, new TransactionRequest(new BigDecimal("150.00"), TransactionType.INCOME,
                "Refund", "Rent reimbursement", LocalDate.parse("2026-11-01")));
        assertThat(service.dashboard().totalIncome()).isEqualByComparingTo("150.00");
        assertThat(service.dashboard().totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(service.dashboard().currentBalance()).isEqualByComparingTo("150.00");

        service.delete(11L);
        assertThat(service.dashboard().totalIncome()).isEqualByComparingTo("0.00");
        assertThat(service.dashboard().currentBalance()).isEqualByComparingTo("0.00");
    }

    private TransactionRequest request(String amount, TransactionType type, String category) {
        return new TransactionRequest(new BigDecimal(amount), type, category, "test entry", LocalDate.parse("2026-11-01"));
    }

    private Transaction transaction(String amount, TransactionType type, String category) {
        return new Transaction(new BigDecimal(amount), type, category, "test entry", LocalDate.parse("2026-11-01"));
    }
}
