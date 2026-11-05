package com.ananyaa.cicd.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {
    private Long id;

    private BigDecimal amount;

    private TransactionType type;

    private String category;

    private String description;

    private LocalDate date;

    public Transaction() {}

    public Transaction(BigDecimal amount, TransactionType type, String category, String description, LocalDate date) {
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
