package com.example.currencyguard.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.currencyguard.R;
import com.example.currencyguard.firebase.FirebaseAuthManager;
import com.example.currencyguard.fragment.HistoryFragment;
import com.example.currencyguard.fragment.HomeFragment;
import com.example.currencyguard.fragment.ProfileFragment;
import com.example.currencyguard.utils.ShareUtils;
import com.example.currencyguard.whatsapp.WhatsAppBotDemoActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

/**
 * MainActivity hosting the primary dashboard, DrawerLayout, custom elevated top bar,
 * and floating modern BottomNavigationView tabs.
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private BottomNavigationView bottomNav;
    private TextView tvTopTitle;
    private TextView tvTopSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mandatory Account Gate: Require sign in / sign up before using app
        if (!FirebaseAuthManager.isUserLoggedIn(this)) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_navigation);
        tvTopTitle = findViewById(R.id.tv_main_top_title);
        tvTopSubtitle = findViewById(R.id.tv_main_top_subtitle);

        // Window Insets for status bar
        com.google.android.material.appbar.AppBarLayout appBar = findViewById(R.id.app_bar_layout);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, insets.top, 0, 0);
                return windowInsets;
            });
        }

        // 1. Navigation Drawer Toggle Button
        ImageView btnDrawerToggle = findViewById(R.id.btn_drawer_toggle);
        if (btnDrawerToggle != null) {
            btnDrawerToggle.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        // 2. Top Right Action 1: Gemini AI Copilot
        ImageView btnAiCopilot = findViewById(R.id.btn_top_ai_copilot);
        if (btnAiCopilot != null) {
            btnAiCopilot.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ChatAssistantActivity.class));
            });
        }

        // 3. Top Right Action 2: WhatsApp Chatbot
        ImageView btnWhatsApp = findViewById(R.id.btn_top_whatsapp);
        if (btnWhatsApp != null) {
            btnWhatsApp.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, WhatsAppBotDemoActivity.class));
            });
        }

        // 4. Populate Drawer Header with user info
        setupDrawerHeader();

        // 5. Drawer Menu Item Click Handling
        if (navView != null) {
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }

                if (id == R.id.drawer_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                    return true;
                } else if (id == R.id.drawer_scan) {
                    startActivity(new Intent(MainActivity.this, ScanActivity.class));
                    return true;
                } else if (id == R.id.drawer_wallet) {
                    bottomNav.setSelectedItemId(R.id.nav_history);
                    return true;
                } else if (id == R.id.drawer_ai_copilot) {
                    startActivity(new Intent(MainActivity.this, ChatAssistantActivity.class));
                    return true;
                } else if (id == R.id.drawer_whatsapp_bot) {
                    startActivity(new Intent(MainActivity.this, WhatsAppBotDemoActivity.class));
                    return true;
                } else if (id == R.id.drawer_converter) {
                    startActivity(new Intent(MainActivity.this, CurrencyConverterActivity.class));
                    return true;
                } else if (id == R.id.drawer_settings) {
                    bottomNav.setSelectedItemId(R.id.nav_profile);
                    return true;
                } else if (id == R.id.drawer_share) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CurrencyGuard AI");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Protect yourself from counterfeit currency with CurrencyGuard AI — download the app today!");
                    startActivity(Intent.createChooser(shareIntent, "Share CurrencyGuard AI"));
                    return true;
                } else if (id == R.id.drawer_about) {
                    Toast.makeText(MainActivity.this, "CurrencyGuard AI Enterprise Forensics Engine v2.4. Compliant with central bank specifications.", Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            });
        }

        // 6. Floating Bottom Navigation Handling
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                updateTopTitles(getString(R.string.app_name), "AI Verification Platform");
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_scan) {
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
                return false;
            } else if (itemId == R.id.nav_history) {
                updateTopTitles("Smart Note Wallet", "Banknote Audit Ledger");
                loadFragment(new HistoryFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                updateTopTitles("Settings & Diagnostics", "Engine Config & AI Diagnostics");
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // Default to HomeFragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            updateTopTitles(getString(R.string.app_name), "AI Verification Platform");
        }
    }

    private void setupDrawerHeader() {
        if (navView == null) return;
        View headerView = navView.getHeaderView(0);
        if (headerView != null) {
            TextView tvName = headerView.findViewById(R.id.tv_drawer_user_name);
            TextView tvEmail = headerView.findViewById(R.id.tv_drawer_user_email);

            FirebaseAuthManager.UserProfile profile = FirebaseAuthManager.getUserProfile(this);
            if (profile != null) {
                if (tvName != null && profile.getName() != null && !profile.getName().isEmpty()) {
                    tvName.setText(profile.getName());
                }
                if (tvEmail != null && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                    tvEmail.setText(profile.getEmail());
                }
            }
        }
    }

    private void updateTopTitles(String title, String subtitle) {
        if (tvTopTitle != null) tvTopTitle.setText(title);
        if (tvTopSubtitle != null) tvTopSubtitle.setText(subtitle);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
