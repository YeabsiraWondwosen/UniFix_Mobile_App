package com.UniFix.unifix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewOutlineProvider;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgotPasswordActivity extends AppCompatActivity {

    // UI Elements
    EditText etUsername, etNewPassword;
    Button btnCheckUser, btnScanFront, btnScanBack, btnResetPassword, btnToggleTheme, btnToggleLanguage;
    LinearLayout layoutIdScanner, layoutResetPassword;
    TextView tvStatus, tvRecoveryTitle;
    ImageView imgFront, imgBack;

    FirebaseFirestore db;
    String targetUsername = "";
    String userRole = "";

    boolean isFrontScanned = false;
    boolean isBackScanned = false;

    // Language State
    boolean isAmharic = false;

    // Translated Strings
    String msgEmptyInput = "Enter your Username or ID";
    String msgSearching = "Searching...";
    String msgVerifyBtn = "Verify User";
    String msgUserNotFound = "Account not found. Check your spelling.";
    String msgPendingStatus = "Status: Pending Verification";
    String msgStaffVerified = "Staff Account Verified. Proceed to reset.";
    String msgIdRequiredPopup = "Security Policy: Physical ID required for password reset.";
    String msgUnknownRole = "Account role not recognized.";
    String msgIdRequiredStatus = "Security: Physical ID Verification Required.";
    String msgCaptureFront = "📸 Capture Front of ID";
    String msgCaptureBack = "📸 Capture Back of ID";
    String msgCameraError = "Camera not supported on this device.";
    String msgFrontCaptured = "Front ID Captured ✅";
    String msgBackCaptured = "Back ID Captured ✅";
    String msgVerificationComplete = "ID Verification Complete ✅";
    String msgPassLength = "Password must be at least 6 characters.";
    String msgUpdating = "Updating...";
    String msgResetSuccess = "Password reset successful! Please log in.";
    String msgConfirmReset = "Confirm Reset";
    String msgDbError = "Database Error.";

    private static final int REQUEST_IMAGE_FRONT = 1;
    private static final int REQUEST_IMAGE_BACK = 2;

    // --- 🎨 UI HELPER METHODS ---
    private void styleInputBox(View v) {
        if (v != null) {
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(30f);
            shape.setColor(ContextCompat.getColor(this, R.color.input_background));
            v.setBackground(shape);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = FirebaseFirestore.getInstance();

        // READ LANGUAGE PREFERENCE FIRST
        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);

        // 1. Link all elements
        tvRecoveryTitle = findViewById(R.id.tvRecoveryTitle);
        etUsername = findViewById(R.id.etUsername);
        btnCheckUser = findViewById(R.id.btnCheckUser);
        layoutIdScanner = findViewById(R.id.layoutIdScanner);
        layoutResetPassword = findViewById(R.id.layoutResetPassword);
        btnScanFront = findViewById(R.id.btnScanFront);
        btnScanBack = findViewById(R.id.btnScanBack);
        imgFront = findViewById(R.id.imgFront);
        imgBack = findViewById(R.id.imgBack);
        tvStatus = findViewById(R.id.tvStatus);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnToggleTheme = findViewById(R.id.btnToggleTheme);
        btnToggleLanguage = findViewById(R.id.btnToggleLanguage); // 🔥 Linked Toggle

        // Apply Premium Styling
        styleInputBox(etUsername);
        styleInputBox(etNewPassword);
        makeInteractive(btnCheckUser);
        makeInteractive(btnScanFront);
        makeInteractive(btnScanBack);
        makeInteractive(btnResetPassword);
        makeInteractive(btnToggleTheme);


        if (btnToggleTheme != null) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                btnToggleTheme.setText("☀️");
            } else {
                btnToggleTheme.setText("🌙");
            }

            btnToggleTheme.setOnClickListener(v -> {
                int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
            });
        }


        if (btnToggleLanguage != null) {
            btnToggleLanguage.setText(isAmharic ? "🌐 ENG" : "🌐 አማ");
            makeInteractive(btnToggleLanguage);
            btnToggleLanguage.setOnClickListener(v -> {
                isAmharic = !isAmharic;
                SharedPreferences.Editor editor = getSharedPreferences("UniFixSettings", MODE_PRIVATE).edit();
                editor.putBoolean("isAmharic", isAmharic);
                editor.apply();
                recreate();
            });
        }

        GradientDrawable btnShape = new GradientDrawable();
        btnShape.setCornerRadius(30f);
        btnShape.setColor(ContextCompat.getColor(this, R.color.unifix_blue));
        btnCheckUser.setBackground(btnShape);

        GradientDrawable resetShape = new GradientDrawable();
        resetShape.setCornerRadius(30f);
        resetShape.setColor(Color.parseColor("#198754"));
        btnResetPassword.setBackground(resetShape);

        GradientDrawable scanShape1 = new GradientDrawable();
        scanShape1.setCornerRadius(30f);
        scanShape1.setColor(Color.parseColor("#6c757d"));
        btnScanFront.setBackground(scanShape1);

        GradientDrawable scanShape2 = new GradientDrawable();
        scanShape2.setCornerRadius(30f);
        scanShape2.setColor(Color.parseColor("#6c757d"));
        btnScanBack.setBackground(scanShape2);

        applyTranslation();

        // 2. Set Click Listeners
        btnCheckUser.setOnClickListener(v -> verifyUser());
        btnScanFront.setOnClickListener(v -> captureImage(REQUEST_IMAGE_FRONT));
        btnScanBack.setOnClickListener(v -> captureImage(REQUEST_IMAGE_BACK));
        btnResetPassword.setOnClickListener(v -> executePasswordReset());
    }

    private void applyTranslation() {
        if (isAmharic) {
            tvRecoveryTitle.setText("መለያ መልሶ ማግኘት");
            etUsername.setHint("የተጠቃሚ ስምዎን ወይም መታወቂያዎን ያስገቡ");
            btnCheckUser.setText("ተጠቃሚውን አረጋግጥ");
            btnScanFront.setText("📸 የመታወቂያውን የፊት ክፍል ያንሱ");
            btnScanBack.setText("📸 የመታወቂያውን የጀርባ ክፍል ያንሱ");
            etNewPassword.setHint("አዲስ የይለፍ ቃል ይፍጠሩ");
            btnResetPassword.setText("ማደሱን አረጋግጥ");

            // Variables
            msgEmptyInput = "እባክዎ የተጠቃሚ ስም ወይም መታወቂያ ያስገቡ";
            msgSearching = "በመፈለግ ላይ...";
            msgVerifyBtn = "ተጠቃሚውን አረጋግጥ";
            msgUserNotFound = "መለያው አልተገኘም። የፊደል አጻጻፍዎን ያረጋግጡ።";
            msgPendingStatus = "ሁኔታ: ማረጋገጫ በመጠባበቅ ላይ";
            msgStaffVerified = "የሰራተኛ መለያ ተረጋግጧል። ወደ ማደስ ይቀጥሉ።";
            msgIdRequiredPopup = "የደህንነት ፖሊሲ፡ የይለፍ ቃል ለማደስ አካላዊ መታወቂያ ያስፈልጋል።";
            msgUnknownRole = "የመለያው ሚና አልታወቀም።";
            msgIdRequiredStatus = "ደህንነት፡ የአካላዊ መታወቂያ ማረጋገጫ ያስፈልጋል።";
            msgCaptureFront = "📸 የመታወቂያውን የፊት ክፍል ያንሱ";
            msgCaptureBack = "📸 የመታወቂያውን የጀርባ ክፍል ያንሱ";
            msgCameraError = "ካሜራ በዚህ መሣሪያ ላይ አይደገፍም።";
            msgFrontCaptured = "የፊት መታወቂያ ተነስቷል ✅";
            msgBackCaptured = "የጀርባ መታወቂያ ተነስቷል ✅";
            msgVerificationComplete = "የመታወቂያ ማረጋገጫ ተጠናቅቋል ✅";
            msgPassLength = "የይለፍ ቃል ቢያንስ 6 ቁምፊዎች መሆን አለበት።";
            msgUpdating = "በማዘመን ላይ...";
            msgResetSuccess = "የይለፍ ቃል ማደስ ተሳክቷል! እባክዎ ይግቡ።";
            msgConfirmReset = "ማደሱን አረጋግጥ";
            msgDbError = "የዳታቤዝ ስህተት።";
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void verifyUser() {
        String input = etUsername.getText().toString().trim().toLowerCase();
        if (input.isEmpty()) {
            Toast.makeText(this, msgEmptyInput, Toast.LENGTH_SHORT).show();
            return;
        }

        btnCheckUser.setText(msgSearching);
        btnCheckUser.setEnabled(false);

        db.collection("users").document(input).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                targetUsername = input;
                processUserFound(task.getResult());
            } else {
                db.collection("users").whereEqualTo("id", input).get().addOnCompleteListener(idTask -> {
                    if (idTask.isSuccessful() && !idTask.getResult().isEmpty()) {
                        DocumentSnapshot doc = idTask.getResult().getDocuments().get(0);
                        targetUsername = doc.getId();
                        processUserFound(doc);
                    } else {
                        btnCheckUser.setText(msgVerifyBtn);
                        btnCheckUser.setEnabled(true);
                        Toast.makeText(this, msgUserNotFound, Toast.LENGTH_LONG).show();
                        layoutIdScanner.setVisibility(View.GONE);
                        layoutResetPassword.setVisibility(View.GONE);
                        tvStatus.setText(msgPendingStatus);
                        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    }
                });
            }
        });
    }

    private void processUserFound(DocumentSnapshot doc) {
        btnCheckUser.setText(msgVerifyBtn);
        btnCheckUser.setEnabled(true);

        userRole = doc.getString("role");

        if ("Admin".equals(userRole) || "Solver".equals(userRole)) {
            tvStatus.setText(msgStaffVerified);
            tvStatus.setTextColor(Color.parseColor("#198754"));
            layoutIdScanner.setVisibility(View.GONE);
            layoutResetPassword.setVisibility(View.VISIBLE);
        } else if ("Teacher".equals(userRole) || "Student".equals(userRole)) {
            Toast.makeText(this, msgIdRequiredPopup, Toast.LENGTH_LONG).show();
            setupIdScannerUI();
        } else {
            Toast.makeText(this, msgUnknownRole, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupIdScannerUI() {
        tvStatus.setText(msgIdRequiredStatus);
        tvStatus.setTextColor(Color.parseColor("#dc3545"));
        layoutIdScanner.setVisibility(View.VISIBLE);
        layoutResetPassword.setVisibility(View.GONE);

        isFrontScanned = false;
        isBackScanned = false;
        imgFront.setVisibility(View.GONE);
        imgBack.setVisibility(View.GONE);

        btnScanFront.setText(msgCaptureFront);
        ((GradientDrawable)btnScanFront.getBackground()).setColor(Color.parseColor("#6c757d"));

        btnScanBack.setText(msgCaptureBack);
        ((GradientDrawable)btnScanBack.getBackground()).setColor(Color.parseColor("#6c757d"));
    }

    private void captureImage(int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, requestCode);
        } else {
            Toast.makeText(this, msgCameraError, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            if (requestCode == REQUEST_IMAGE_FRONT) {
                imgFront.setImageBitmap(imageBitmap);

                imgFront.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                imgFront.setClipToOutline(true);
                GradientDrawable imgShape = new GradientDrawable();
                imgShape.setCornerRadius(20f);
                imgFront.setBackground(imgShape);

                imgFront.setVisibility(View.VISIBLE);
                isFrontScanned = true;
                btnScanFront.setText(msgFrontCaptured);
                ((GradientDrawable)btnScanFront.getBackground()).setColor(Color.parseColor("#198754"));
            } else if (requestCode == REQUEST_IMAGE_BACK) {
                imgBack.setImageBitmap(imageBitmap);

                imgBack.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                imgBack.setClipToOutline(true);
                GradientDrawable imgShape = new GradientDrawable();
                imgShape.setCornerRadius(20f);
                imgBack.setBackground(imgShape);

                imgBack.setVisibility(View.VISIBLE);
                isBackScanned = true;
                btnScanBack.setText(msgBackCaptured);
                ((GradientDrawable)btnScanBack.getBackground()).setColor(Color.parseColor("#198754"));
            }

            if (isFrontScanned && isBackScanned) {
                tvStatus.setText(msgVerificationComplete);
                tvStatus.setTextColor(Color.parseColor("#198754"));
                layoutResetPassword.setVisibility(View.VISIBLE);
            }
        }
    }

    private void executePasswordReset() {
        String newPass = etNewPassword.getText().toString().trim();
        if (newPass.length() < 6) {
            Toast.makeText(this, msgPassLength, Toast.LENGTH_SHORT).show();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText(msgUpdating);

        db.collection("users").document(targetUsername).update("password", newPass)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, msgResetSuccess, Toast.LENGTH_LONG).show();
                    finish();
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                })
                .addOnFailureListener(e -> {
                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText(msgConfirmReset);
                    Toast.makeText(this, msgDbError, Toast.LENGTH_SHORT).show();
                });
    }
}