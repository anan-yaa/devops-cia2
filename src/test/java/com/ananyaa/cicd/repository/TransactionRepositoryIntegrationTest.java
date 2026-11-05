package com.ananyaa.cicd.repository;

import com.ananyaa.cicd.entity.Transaction;
import com.ananyaa.cicd.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionRepositoryIntegrationTest {
    @TempDir Path tempDir;
    private TransactionRepository repository;

    @BeforeEach
    void createRepository() {
        repository = new TransactionRepository(new ObjectMapper(), tempDir.resolve("transactions.json").toString());
    }

    @Test
    void persistsAndRetrievesTransactionWithMappedFields() {
        Transaction saved = repository.save(transaction("125.70", TransactionType.INCOME, "Salary"));

        Transaction found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getAmount()).isEqualByComparingTo("125.70");
        assertThat(found.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(found.getDate()).isEqualTo(LocalDate.parse("2026-11-01"));
        assertThat(repository.findAllByOrderByDateDescIdDesc()).hasSize(1);
        TransactionRepository restarted = new TransactionRepository(new ObjectMapper(), tempDir.resolve("transactions.json").toString());
        assertThat(restarted.findById(saved.getId()).orElseThrow().getAmount()).isEqualByComparingTo("125.70");
    }

    @Test
    void updatesPersistedRecord() {
        Transaction saved = repository.save(transaction("15.00", TransactionType.EXPENSE, "Food"));
        saved.setAmount(new BigDecimal("25.40"));
        saved.setCategory("Transport");
        saved.setDescription("Bus fare");
        repository.save(saved);

        Transaction updated = repository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getAmount()).isEqualByComparingTo("25.40");
        assertThat(updated.getCategory()).isEqualTo("Transport");
        assertThat(updated.getDescription()).isEqualTo("Bus fare");
    }

    @Test
    void deletesPersistedRecord() {
        Transaction saved = repository.save(transaction("15.00", TransactionType.EXPENSE, "Food"));
        repository.delete(saved);

        assertThat(repository.existsById(saved.getId())).isFalse();
    }

    @Test
    void filtersByTypeCategoryAndBothTogether() {
        repository.save(transaction("100.00", TransactionType.INCOME, "Salary"));
        repository.save(transaction("12.00", TransactionType.EXPENSE, "Food"));
        repository.save(transaction("8.00", TransactionType.EXPENSE, "Transport"));

        assertThat(repository.findByType(TransactionType.EXPENSE)).hasSize(2);
        assertThat(repository.findByCategoryIgnoreCase("food")).hasSize(1);
        assertThat(repository.findByTypeAndCategoryIgnoreCase(TransactionType.EXPENSE, "FOOD"))
                .extracting(Transaction::getCategory).containsExactly("Food");
        assertThat(repository.findAllByOrderByDateDescIdDesc()).hasSize(3);
    }

    @Test
    void sumsAmountsByType() {
        repository.save(transaction("100.25", TransactionType.INCOME, "Salary"));
        repository.save(transaction("25.10", TransactionType.INCOME, "Gift"));
        repository.save(transaction("10.00", TransactionType.EXPENSE, "Food"));

        assertThat(repository.sumAmountByType(TransactionType.INCOME)).isEqualByComparingTo("125.35");
        assertThat(repository.sumAmountByType(TransactionType.EXPENSE)).isEqualByComparingTo("10.00");
    }

    private Transaction transaction(String amount, TransactionType type, String category) {
        return new Transaction(new BigDecimal(amount), type, category, "integration test", LocalDate.parse("2026-11-01"));
    }
}
