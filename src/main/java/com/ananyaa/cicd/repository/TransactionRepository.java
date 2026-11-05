package com.ananyaa.cicd.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ananyaa.cicd.entity.Transaction;
import com.ananyaa.cicd.entity.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TransactionRepository {
    private static final TypeReference<List<Transaction>> TRANSACTION_LIST = new TypeReference<>() {};
    private final ObjectMapper objectMapper;
    private final Path dataFile;
    private final Map<Long, Transaction> transactions = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public TransactionRepository(ObjectMapper objectMapper,
                                 @Value("${app.data.file:./data/transactions.json}") String dataFile) {
        this.objectMapper = objectMapper.findAndRegisterModules();
        this.dataFile = Path.of(dataFile).toAbsolutePath();
        load();
    }

    public synchronized Transaction save(Transaction transaction) {
        if (transaction.getId() == null) transaction.setId(nextId.getAndIncrement());
        else nextId.accumulateAndGet(transaction.getId() + 1, Math::max);
        transactions.put(transaction.getId(), transaction);
        persist();
        return transaction;
    }

    public synchronized Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(transactions.get(id));
    }

    public synchronized List<Transaction> findAll() {
        return transactions.values().stream().sorted(newestFirst()).toList();
    }

    public synchronized List<Transaction> findByTypeAndCategoryIgnoreCase(TransactionType type, String category) {
        return transactions.values().stream()
                .filter(transaction -> transaction.getType() == type)
                .filter(transaction -> transaction.getCategory().equalsIgnoreCase(category))
                .sorted(newestFirst()).toList();
    }

    public synchronized List<Transaction> findByType(TransactionType type) {
        return transactions.values().stream().filter(transaction -> transaction.getType() == type)
                .sorted(newestFirst()).toList();
    }

    public synchronized List<Transaction> findByCategoryIgnoreCase(String category) {
        return transactions.values().stream()
                .filter(transaction -> transaction.getCategory().equalsIgnoreCase(category))
                .sorted(newestFirst()).toList();
    }

    public synchronized List<Transaction> findAllByOrderByDateDescIdDesc() { return findAll(); }

    public synchronized BigDecimal sumAmountByType(TransactionType type) {
        return transactions.values().stream().filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public synchronized long count() { return transactions.size(); }
    public synchronized boolean existsById(Long id) { return transactions.containsKey(id); }

    public synchronized void delete(Transaction transaction) {
        if (transactions.remove(transaction.getId()) != null) persist();
    }

    public synchronized void deleteAll() {
        transactions.clear();
        nextId.set(1);
        persist();
    }

    private Comparator<Transaction> newestFirst() {
        return Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getId).reversed();
    }

    private void load() {
        if (!Files.exists(dataFile)) return;
        try {
            List<Transaction> loaded = objectMapper.readValue(dataFile.toFile(), TRANSACTION_LIST);
            for (Transaction transaction : loaded) {
                transactions.put(transaction.getId(), transaction);
                nextId.accumulateAndGet(transaction.getId() + 1, Math::max);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load expense data from " + dataFile, exception);
        }
    }

    private void persist() {
        Path parent = dataFile.getParent();
        Path temporaryFile = null;
        try {
            Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, "transactions-", ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), new ArrayList<>(transactions.values()));
            try {
                Files.move(temporaryFile, dataFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save expense data to " + dataFile, exception);
        } finally {
            if (temporaryFile != null) {
                try { Files.deleteIfExists(temporaryFile); }
                catch (IOException ignored) { /* Best effort cleanup after a failed write. */ }
            }
        }
    }
}
