package com.smartledger.utils;

import com.smartledger.models.Expense;
import java.util.List;

public class FinancialUtils {

    /**
     * Υπολογίζει το συνολικό ποσό εξόδων από μια λίστα (Business Logic)
     */
    public static double calculateTotalSpent(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return 0.0;
        }
        double total = 0;
        for (Expense e : expenses) {
            total += e.getAmount();
        }
        return total;
    }

    /**
     * Υπολογίζει το ποσοστό του budget που έχει ξοδευτεί.
     */
    public static double calculateBudgetPercentage(double totalSpent, double budget) {
        if (budget <= 0) {
            return 0.0; // Προστασία από διαίρεση με το 0
        }
        return (totalSpent / budget) * 100.0;
    }
}
