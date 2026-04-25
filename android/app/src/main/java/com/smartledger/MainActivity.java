package com.smartledger;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_NAV_ITEM = "selected_nav_item";
    private static final String TAG_DASHBOARD = "dashboard";
    private static final String TAG_EXPENSES = "expenses";
    private static final String TAG_SPLITS = "splits";
    private static final String TAG_CRYPTO = "crypto";

    private int selectedNavItemId = R.id.nav_dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        if (savedInstanceState != null) {
            selectedNavItemId = savedInstanceState.getInt(KEY_SELECTED_NAV_ITEM, R.id.nav_dashboard);
        }

        setupBottomNavigation();
        showFragment(selectedNavItemId);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(selectedNavItemId);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            boolean handled = showFragment(itemId);
            if (handled) {
                selectedNavItemId = itemId;
            }
            return handled;
        });
    }

    private boolean showFragment(int itemId) {
        String targetTag = getTagForItem(itemId);
        if (targetTag == null) {
            return false;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment target = fragmentManager.findFragmentByTag(targetTag);

        hideFragmentIfPresent(fragmentManager, transaction, TAG_DASHBOARD);
        hideFragmentIfPresent(fragmentManager, transaction, TAG_EXPENSES);
        hideFragmentIfPresent(fragmentManager, transaction, TAG_SPLITS);
        hideFragmentIfPresent(fragmentManager, transaction, TAG_CRYPTO);

        if (target == null) {
            target = createFragment(itemId);
            transaction.add(R.id.fragment_container, target, targetTag);
        } else {
            transaction.show(target);
        }

        transaction.commit();
        return true;
    }

    private void hideFragmentIfPresent(
            FragmentManager fragmentManager,
            FragmentTransaction transaction,
            String tag
    ) {
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment != null) {
            transaction.hide(fragment);
        }
    }

    private Fragment createFragment(int itemId) {
        if (itemId == R.id.nav_dashboard) {
            return new DashboardFragment();
        } else if (itemId == R.id.nav_expenses) {
            return new ExpensesFragment();
        } else if (itemId == R.id.nav_splits) {
            return new SplitsFragment();
        } else if (itemId == R.id.nav_crypto) {
            return new CryptoFragment();
        }
        throw new IllegalArgumentException("Unknown navigation item: " + itemId);
    }

    private String getTagForItem(int itemId) {
        if (itemId == R.id.nav_dashboard) {
            return TAG_DASHBOARD;
        } else if (itemId == R.id.nav_expenses) {
            return TAG_EXPENSES;
        } else if (itemId == R.id.nav_splits) {
            return TAG_SPLITS;
        } else if (itemId == R.id.nav_crypto) {
            return TAG_CRYPTO;
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_NAV_ITEM, selectedNavItemId);
        super.onSaveInstanceState(outState);
    }
}
