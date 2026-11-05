package com.ananyaa.cicd.dto;

import com.ananyaa.cicd.entity.Transaction;
import com.ananyaa.cicd.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(Long id, BigDecimal amount, TransactionType type,
                                 String category, String description, LocalDate date) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAmount(), transaction.getType(),
                transaction.getCategory(), transaction.getDescription(), transaction.getDate());
    }
}
