package com.ananyaa.cicd.dto;

import com.ananyaa.cicd.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotNull TransactionType type,
        @NotBlank @Size(max = 100) String category,
        @Size(max = 500) String description,
        @NotNull LocalDate date) {}
