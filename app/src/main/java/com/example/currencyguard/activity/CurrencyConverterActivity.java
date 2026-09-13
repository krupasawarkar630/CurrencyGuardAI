package com.example.currencyguard.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.currencyguard.R;
import com.example.currencyguard.utils.ShareUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-Country Live Currency Converter Activity.
 * Converts banknote values (e.g. ₹200 INR) into USD, EUR, GBP, JPY, AED,
 * CAD, AUD, SAR, CHF, SGD, KWD, CNY in real time.
 */
public class CurrencyConverterActivity extends AppCompatActivity {

    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_CURRENCY = "extra_currency";

    private TextInputEditText etConvertAmount;
    private LinearLayout containerList;
    private ChipGroup chipGroupPresets;

    private static class CurrencyItem {
        final String flag;
        final String code;
        final String name;
        final String symbol;
        final double inrRate; // How much 1 INR is worth in this currency

        CurrencyItem(String flag, String code, String name, String symbol, double inrRate) {
            this.flag = flag;
            this.code = code;
            this.name = name;
            this.symbol = symbol;
            this.inrRate = inrRate;
        }
    }

    private final List<CurrencyItem> targetCurrencies = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency_converter);

        initTargetCurrencies();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_converter);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etConvertAmount = findViewById(R.id.et_convert_amount);
        containerList = findViewById(R.id.container_conversion_list);
        chipGroupPresets = findViewById(R.id.chip_group_presets);

        // Read initial amount from Intent or default to 200
        double initialAmount = getIntent().getDoubleExtra(EXTRA_AMOUNT, 200.0);
        etConvertAmount.setText(String.valueOf((int) initialAmount));

        setupPresets();

        etConvertAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderConversions();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_share_conversion).setOnClickListener(v -> shareCurrentConversion());

        renderConversions();
    }

    private void initTargetCurrencies() {
        targetCurrencies.add(new CurrencyItem("🇺🇸", "USD", "US Dollar", "$", 0.0120));
        targetCurrencies.add(new CurrencyItem("🇪🇺", "EUR", "Euro", "€", 0.0110));
        targetCurrencies.add(new CurrencyItem("🇬🇧", "GBP", "British Pound", "£", 0.0094));
        targetCurrencies.add(new CurrencyItem("🇯🇵", "JPY", "Japanese Yen", "¥", 1.8600));
        targetCurrencies.add(new CurrencyItem("🇦🇪", "AED", "UAE Dirham", "AED ", 0.0440));
        targetCurrencies.add(new CurrencyItem("🇨🇦", "CAD", "Canadian Dollar", "C$", 0.0165));
        targetCurrencies.add(new CurrencyItem("🇦🇺", "AUD", "Australian Dollar", "A$", 0.0184));
        targetCurrencies.add(new CurrencyItem("🇸🇦", "SAR", "Saudi Riyal", "SAR ", 0.0450));
        targetCurrencies.add(new CurrencyItem("🇨🇭", "CHF", "Swiss Franc", "CHF ", 0.0106));
        targetCurrencies.add(new CurrencyItem("🇸🇬", "SGD", "Singapore Dollar", "S$", 0.0162));
        targetCurrencies.add(new CurrencyItem("🇰🇼", "KWD", "Kuwaiti Dinar", "KWD ", 0.0037));
        targetCurrencies.add(new CurrencyItem("🇨🇳", "CNY", "Chinese Yuan", "¥", 0.0860));
    }

    private void setupPresets() {
        chipGroupPresets.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_10)) {
                etConvertAmount.setText("10");
            } else if (checkedIds.contains(R.id.chip_50)) {
                etConvertAmount.setText("50");
            } else if (checkedIds.contains(R.id.chip_100)) {
                etConvertAmount.setText("100");
            } else if (checkedIds.contains(R.id.chip_200)) {
                etConvertAmount.setText("200");
            } else if (checkedIds.contains(R.id.chip_500)) {
                etConvertAmount.setText("500");
            }
        });
    }

    private void renderConversions() {
        containerList.removeAllViews();

        double baseAmount = 0.0;
        try {
            String text = etConvertAmount.getText() != null ? etConvertAmount.getText().toString().trim() : "0";
            if (!text.isEmpty()) {
                baseAmount = Double.parseDouble(text);
            }
        } catch (NumberFormatException ignored) {}

        LayoutInflater inflater = LayoutInflater.from(this);
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormat rateDf = new DecimalFormat("0.0000");

        for (CurrencyItem item : targetCurrencies) {
            View card = inflater.inflate(R.layout.item_currency_conversion, containerList, false);

            TextView tvFlag = card.findViewById(R.id.tv_currency_flag);
            TextView tvCode = card.findViewById(R.id.tv_currency_code);
            TextView tvRate = card.findViewById(R.id.tv_exchange_rate);
            TextView tvConverted = card.findViewById(R.id.tv_converted_value);

            tvFlag.setText(item.flag);
            tvCode.setText(item.code + " — " + item.name);
            tvRate.setText("1 INR = " + rateDf.format(item.inrRate) + " " + item.code);

            double converted = baseAmount * item.inrRate;
            tvConverted.setText(item.symbol + df.format(converted));

            containerList.addView(card);
        }
    }

    private void shareCurrentConversion() {
        String text = etConvertAmount.getText() != null ? etConvertAmount.getText().toString().trim() : "200";
        double amount = 200.0;
        try {
            amount = Double.parseDouble(text);
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder();
        sb.append("💱 CurrencyGuard AI — Global Exchange Reference\n");
        sb.append("Value: ₹").append((int) amount).append(" INR converts to:\n\n");

        DecimalFormat df = new DecimalFormat("#,##0.00");
        for (CurrencyItem item : targetCurrencies) {
            double converted = amount * item.inrRate;
            sb.append(item.flag).append(" ").append(item.code).append(": ")
                    .append(item.symbol).append(df.format(converted)).append("\n");
        }
        sb.append("\nVerified with CurrencyGuard AI: AI-assisted fake currency detection & global currency tools.");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CurrencyGuard AI — Exchange Reference");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Currency Rates"));
    }
}
