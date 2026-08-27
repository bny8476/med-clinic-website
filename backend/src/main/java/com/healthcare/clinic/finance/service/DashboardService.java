package com.healthcare.clinic.finance.service;

import com.healthcare.clinic.finance.dto.DashboardResponse;
import com.healthcare.clinic.finance.entity.BranchBudget;
import com.healthcare.clinic.finance.entity.Expense;
import com.healthcare.clinic.finance.entity.ExpenseStatus;
import com.healthcare.clinic.finance.entity.InsuranceClaim;
import com.healthcare.clinic.finance.repository.BranchBudgetRepository;
import com.healthcare.clinic.finance.repository.ExpenseRepository;
import com.healthcare.clinic.finance.repository.InsuranceClaimRepository;
import com.healthcare.clinic.finance.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final InsuranceClaimRepository claimRepository;
    private final BranchBudgetRepository budgetRepository;
    private final PnLService pnLService;

    @Transactional(readOnly = true)
    public DashboardResponse getRealtimeDashboardData(Long branchId, LocalDate startDate, LocalDate endDate) {
        
        // PnL uses existing double-entry logic
        com.healthcare.clinic.finance.dto.PnLResponse pnl = pnLService.generatePnLStatement(branchId, startDate, endDate);
        BigDecimal totalRevenue = pnl.getTotalRevenue();
        BigDecimal totalExpenses = pnl.getTotalExpenses();
        BigDecimal netProfit = pnl.getNetProfit();

        // Claims Stats
        List<InsuranceClaim> claims = claimRepository.findAll();
        Map<String, Long> claimsByStatus = claims.stream()
                .collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));

        // Pending Expenses
        long pendingApprovals = expenseRepository.countByStatus(ExpenseStatus.PENDING_APPROVAL);

        // Budget Status (current month if not specified, else simple approach uses all for now)
        List<BranchBudget> budgets = budgetRepository.findAll();
        Map<String, BigDecimal> budgetStatus = new HashMap<>();
        for (BranchBudget b : budgets) {
            String key = b.getBranch().getName() + " (" + b.getBudgetYear() + "-" + b.getBudgetMonth() + ")";
            budgetStatus.put(key, b.getRemainingAmount());
        }

        return DashboardResponse.builder()
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netProfit(netProfit)
                .claimsByStatus(claimsByStatus)
                .pendingExpenseApprovals(pendingApprovals)
                .branchBudgetStatus(budgetStatus)
                .build();
    }
}
