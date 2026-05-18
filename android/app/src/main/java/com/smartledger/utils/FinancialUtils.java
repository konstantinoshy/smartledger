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
     * Υπολογίζει το συνολικό ποσό εξόδων για τον τρέχοντα μήνα
     */
    public static double calculateCurrentMonthSpent(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return 0.0;
        }
        double total = 0;
        java.util.Calendar currentCal = java.util.Calendar.getInstance();
        int currentMonth = currentCal.get(java.util.Calendar.MONTH);
        int currentYear = currentCal.get(java.util.Calendar.YEAR);
        
        for (Expense e : expenses) {
            if (e.getDate() != null) {
                java.util.Calendar expCal = java.util.Calendar.getInstance();
                expCal.setTime(e.getDate());
                if (expCal.get(java.util.Calendar.MONTH) == currentMonth &&
                    expCal.get(java.util.Calendar.YEAR) == currentYear) {
                    total += e.getAmount();
                }
            }
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
