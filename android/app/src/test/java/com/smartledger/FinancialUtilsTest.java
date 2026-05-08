package com.smartledger;

import com.smartledger.models.Expense;
import com.smartledger.utils.FinancialUtils;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit Tests για την επαλήθευση της λογικής υπολογισμού εξόδων και budgets.
 * Καλύπτει κανονικές περιπτώσεις (Happy Path) καθώς και ακραίες (Edge Cases).
 */
public class FinancialUtilsTest {

    @Test
    public void testCalculateTotalSpent_withValidExpenses() {
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("u1", 50.50, "Food", "Lunch", new Date()));
        expenses.add(new Expense("u1", 10.00, "Transport", "Bus", new Date()));
        expenses.add(new Expense("u1", 100.00, "Rent", "Utility", new Date()));

        double total = FinancialUtils.calculateTotalSpent(expenses);
        
        // Ελέγχουμε αν το σύνολο είναι ακριβώς 160.50 (με μικρή ανοχή 0.001 στα δεκαδικά)
        assertEquals(160.50, total, 0.001);
    }

    @Test
    public void testCalculateTotalSpent_withEmptyList() {
        List<Expense> emptyList = new ArrayList<>();
        double total = FinancialUtils.calculateTotalSpent(emptyList);
        assertEquals(0.0, total, 0.001);
    }

    @Test
    public void testCalculateTotalSpent_withNullList() {
        double total = FinancialUtils.calculateTotalSpent(null);
        assertEquals(0.0, total, 0.001);
    }

    @Test
    public void testCalculateBudgetPercentage_normalValues() {
        double percentage = FinancialUtils.calculateBudgetPercentage(2500.0, 5000.0);
        assertEquals(50.0, percentage, 0.001);
    }

    @Test
    public void testCalculateBudgetPercentage_zeroBudget() {
        double percentage = FinancialUtils.calculateBudgetPercentage(100.0, 0.0);
        // Πρέπει να επιστρέψει 0 για να μη "σκάσει" η εφαρμογή (Infinity/NaN error)
        assertEquals(0.0, percentage, 0.001); 
    }
}
