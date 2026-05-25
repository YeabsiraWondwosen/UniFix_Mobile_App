package com.UniFix.unifix;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.TextView;

public class WelcomeActivity extends Activity {

    boolean isAmharic = false;

    Button btnToggleLanguage, btnTopSignIn, btnReportIssue;
    TextView tvBadge, tvMainTitle, tvMainDesc;
    TextView tvFeat1Title, tvFeat1Desc, tvFeat2Title, tvFeat2Desc, tvFeat3Title, tvFeat3Desc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnToggleLanguage = findViewById(R.id.btnToggleLanguage);
        btnTopSignIn = findViewById(R.id.btnTopSignIn);
        btnReportIssue = findViewById(R.id.btnReportIssue);

        tvBadge = findViewById(R.id.tvBadge);
        tvMainTitle = findViewById(R.id.tvMainTitle);
        tvMainDesc = findViewById(R.id.tvMainDesc);

        tvFeat1Title = findViewById(R.id.tvFeat1Title);
        tvFeat1Desc = findViewById(R.id.tvFeat1Desc);
        tvFeat2Title = findViewById(R.id.tvFeat2Title);
        tvFeat2Desc = findViewById(R.id.tvFeat2Desc);
        tvFeat3Title = findViewById(R.id.tvFeat3Title);
        tvFeat3Desc = findViewById(R.id.tvFeat3Desc);

        View.OnClickListener goToLogin = v -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
        };

        if (tvBadge != null) {
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(100f);
            badgeBg.setColor(Color.parseColor("#33FFFFFF"));
            tvBadge.setBackground(badgeBg);
        }

        // 3. Style Buttons and Add Click Navigation
        if (btnTopSignIn != null) {
            GradientDrawable topBtn = new GradientDrawable();
            topBtn.setCornerRadius(30f);
            topBtn.setColor(Color.parseColor("#0d6efd"));
            btnTopSignIn.setBackground(topBtn);
            btnTopSignIn.setOnClickListener(goToLogin);
            makeInteractive(btnTopSignIn);
        }

        if (btnReportIssue != null) {
            GradientDrawable mainBtn = new GradientDrawable();
            mainBtn.setCornerRadius(50f);
            mainBtn.setColor(Color.parseColor("#0d6efd"));
            btnReportIssue.setBackground(mainBtn);
            btnReportIssue.setOnClickListener(goToLogin);
            makeInteractive(btnReportIssue);
        }

        if (btnToggleLanguage != null) {
            GradientDrawable langBtn = new GradientDrawable();
            langBtn.setCornerRadius(30f);
            langBtn.setColor(Color.parseColor("#44FFFFFF"));
            btnToggleLanguage.setBackground(langBtn);
            makeInteractive(btnToggleLanguage);

            // Toggle Click Listener
            btnToggleLanguage.setOnClickListener(v -> {
                isAmharic = !isAmharic;
                saveLanguagePreference(isAmharic);
                updateLanguageUI();
            });
        }

        // 4. APPLY FLOATING GLASS EFFECT
        styleFloatingCard(R.id.featCard1);
        styleFloatingCard(R.id.featCard2);
        styleFloatingCard(R.id.featCard3);

        styleIconCircle(R.id.featIcon1);
        styleIconCircle(R.id.featIcon2);
        styleIconCircle(R.id.featIcon3);

        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);
        updateLanguageUI();
    }


    // TRANSLATION ENGINE

    private void saveLanguagePreference(boolean amharicSelected) {
        SharedPreferences.Editor editor = getSharedPreferences("UniFixSettings", MODE_PRIVATE).edit();
        editor.putBoolean("isAmharic", amharicSelected);
        editor.apply();
    }

    private void updateLanguageUI() {
        if (isAmharic) {

            btnToggleLanguage.setText("🌐 English");
            btnTopSignIn.setText("ግባ");
            btnReportIssue.setText("ችግር ሪፖርት ያድርጉ");

            tvBadge.setText("በኢትዮጵያ ቁጥር 1 የካምፓስ ችግር ሪፖርት ማቅረቢያ");
            tvMainTitle.setText("አንድ ሪፖርት።\nእውነተኛ መፍትሄዎች።\nየተሻለ ካምፓስ።");
            tvMainDesc.setText("የዩኒቨርሲቲ ካምፓስ ችግሮችን በአንድ ቦታ ሪፖርት ያድርጉ፣ ይከታተሉ እና ይፍቱ።");

            tvFeat1Title.setText("ፈጣን ምላሽ");
            tvFeat1Desc.setText("ትኬቶች በፍጥነት ይፈታሉ");

            tvFeat2Title.setText("ግልጽነት ያለው");
            tvFeat2Desc.setText("እያንዳንዱን እርምጃ በቀጥታ ይከታተሉ");

            tvFeat3Title.setText("ለሁሉም ሰው");
            tvFeat3Desc.setText("ለተማሪዎች እና ለመምህራን");
        } else {

            btnToggleLanguage.setText("🌐 አማርኛ");
            btnTopSignIn.setText("Sign In");
            btnReportIssue.setText("Report an Issue");

            tvBadge.setText("Ethiopia's #1 Campus Issue Reporting Platform");
            tvMainTitle.setText("One Report.\nReal Solutions.\nA Better Campus.");
            tvMainDesc.setText("Report, track and resolve university campus problems in one place.");

            tvFeat1Title.setText("Fast Response");
            tvFeat1Desc.setText("Tickets resolved quickly");

            tvFeat2Title.setText("Transparent");
            tvFeat2Desc.setText("Track every step live");

            tvFeat3Title.setText("For Everyone");
            tvFeat3Desc.setText("Students & Teachers");
        }
    }


    private void styleFloatingCard(int id) {
        View v = findViewById(id);
        if (v != null) {
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(40f);
            shape.setColor(Color.parseColor("#33FFFFFF"));
            v.setBackground(shape);
            v.setElevation(8f);
            v.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            v.setClipToOutline(true);
        }
    }

    private void styleIconCircle(int id) {
        View v = findViewById(id);
        if (v != null) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor("#330d6efd"));
            v.setBackground(circle);
        }
    }

    private void makeInteractive(View view) {
        if (view != null) {
            view.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        break;
                }
                return false;
            });
        }
    }
}