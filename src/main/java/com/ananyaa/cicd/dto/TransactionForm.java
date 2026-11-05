package com.ananyaa.cicd.dto;

import com.ananyaa.cicd.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionForm {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    @NotBlank
    @Size(max = 100)
    private String category;

    @Size(max = 500)
    private String description;

    @NotNull
    private LocalDate date;

    public TransactionRequest toRequest() {
        return new TransactionRequest(amount, type, category, description, date);
    }

    public static TransactionForm from(TransactionResponse transaction) {
        TransactionForm form = new TransactionForm();
        form.setAmount(transaction.amount());
        form.setType(transaction.type());
        form.setCategory(transaction.category());
        form.setDescription(transaction.description());
        form.setDate(transaction.date());
        return form;
    }

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
