package com.example.currencyguard.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.currencyguard.R;
import com.example.currencyguard.fragment.HistoryFragment;
import com.example.currencyguard.fragment.HomeFragment;
import com.example.currencyguard.fragment.LearnFragment;
import com.example.currencyguard.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity hosting the primary dashboard and BottomNavigationView tabs:
 * Home, Scan, History, Learn, Profile.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mandatory Account Gate: Require sign in / sign up before using app
        if (!com.example.currencyguard.firebase.FirebaseAuthManager.isUserLoggedIn(this)) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_scan) {
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
                return false; // Don't highlight tab since it's an activity
            } else if (itemId == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;
            } else if (itemId == R.id.nav_learn) {
                loadFragment(new LearnFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // Default to HomeFragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
