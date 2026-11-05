package com.ananyaa.cicd.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(BigDecimal totalIncome, BigDecimal totalExpenses,
                                BigDecimal currentBalance, List<TransactionResponse> recentTransactions) {}
