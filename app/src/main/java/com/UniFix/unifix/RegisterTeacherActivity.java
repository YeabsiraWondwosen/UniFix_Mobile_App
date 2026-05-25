package com.UniFix.unifix;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterTeacherActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_FRONT = 1;
    private static final int REQUEST_IMAGE_BACK = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    EditText etStaffId, etPassword, etConfirmPassword;
    LinearLayout layoutIdPrefix, containerIdScanner;
    Button btnVerifyFront, btnVerifyBack, btnSubmit, btnToggleTheme, btnToggleLanguage;
    TextView tvBackToLogin, tvAutoUsername, tvRegTitle, tvRegSubtitle;

    ImageView ivShowPassword, ivShowConfirmPassword;
    boolean isPasswordVisible = false;
    boolean isConfirmPasswordVisible = false;

    FirebaseFirestore db;

    boolean isFrontVerified = false;
    boolean isBackVerified = false;
    int scanAttempts = 0;
    final int MAX_ATTEMPTS = 3;
    int currentScanSide = 0;

    Uri photoURI;

    // --- LANGUAGE LOGIC ---
    boolean isAmharic = false;

    // Translated Strings
    String msgTypeIDFirst = "Please type your Staff ID first to match!";
    String msgCameraNotSupported = "Camera not supported on this device.";
    String msgScanningFront = "SCANNING FRONT...";
    String msgScanningBack = "SCANNING BACK...";
    String msgFailedLoadImg = "Failed to load image file.";
    String msgNameScanned = "Name Scanned Successfully!";
    String msgFrontVerified = "FRONT ✅";
    String msgFrontRetry = "Could not find a valid name. Ensure the image is clear.";
    String msgErrorFront = "Error reading front card.";
    String msgScanFrontBtn = "SCAN FRONT";
    String msgBackVerified = "BACK ✅";
    String msgErrorBack = "Error reading back card.";
    String msgScanBackBtn = "SCAN BACK";
    String msgPolicyError = "Security Policy: You MUST scan the Front and Back of your physical ID first.";
    String msgMandatoryFields = "Please fill all fields";
    String msgPassMismatch = "Passwords do not match!";
    String msgWeakPass = "WEAK PASSWORD! Must be 8+ characters and include: Uppercase, Lowercase, Number, and Special Symbol.";
    String msgChecking = "Checking Availability...";
    String msgAcctExistsTitle = "Account Already Exists! ⚠️";
    String msgAcctExistsBody = "An account with this ID has already been registered.\n\nYour Username is:\n\n";
    String msgLoginBtn = "Go to Login";
    String msgDbError = "Network error checking ID. Try again.";
    String msgSaving = "Saving Account...";
    String msgRegDoneTitle = "Registration Done! 🎉";
    String msgRegDoneBodySuccess = "Your account was successfully verified and created!<br><br>Your Username is:<br><br><b>";
    String msgRegDoneBodyPending = "Your registration was submitted to the Admins for manual review.<br><br>Once approved, your Username will be:<br><br><b>";
    String msgRegDoneBodyEnd = "</b><br><br>Please write this down to log in.";
    String msgRegFailed = "Failed to create account. Check connection.";
    String msgRegisterBtn = "REGISTER ACCOUNT";
    String msgManualReviewBtn = "MANUAL REVIEW";
    String msgManualReviewDialogTitle = "Verification Failed";
    String msgManualReviewDialogBody = "We could not automatically verify your ID card.\n\nYou can still click Register, but your account will require manual approval by an Admin before you can log in.";
    String msgUnderstood = "Understood";
    String msgMaxAttempts = "Max attempts reached. Proceed to register for manual review.";

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
        setContentView(R.layout.activity_register_teacher);

        db = FirebaseFirestore.getInstance();

        // READ LANGUAGE PREFERENCE
        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);

        tvRegTitle = findViewById(R.id.tvRegTitle);
        tvRegSubtitle = findViewById(R.id.tvRegSubtitle);
        layoutIdPrefix = findViewById(R.id.layoutIdPrefix);
        etStaffId = findViewById(R.id.etStaffId);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        containerIdScanner = findViewById(R.id.containerIdScanner);
        btnVerifyFront = findViewById(R.id.btnVerifyFront);
        btnVerifyBack = findViewById(R.id.btnVerifyBack);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        tvAutoUsername = findViewById(R.id.tvAutoUsername);
        btnToggleTheme = findViewById(R.id.btnToggleTheme);
        btnToggleLanguage = findViewById(R.id.btnToggleLanguage); // 🔥 Linked Toggle

        ivShowPassword = findViewById(R.id.ivShowPassword);
        ivShowConfirmPassword = findViewById(R.id.ivShowConfirmPassword);

        styleInputBox(layoutIdPrefix);
        styleInputBox(etPassword);
        styleInputBox(etConfirmPassword);

        GradientDrawable submitShape = new GradientDrawable();
        submitShape.setCornerRadius(30f);
        submitShape.setColor(ContextCompat.getColor(this, R.color.unifix_blue));
        btnSubmit.setBackground(submitShape);

        GradientDrawable yellowShapeFront = new GradientDrawable();
        yellowShapeFront.setCornerRadius(30f);
        yellowShapeFront.setColor(Color.parseColor("#FFc107"));
        btnVerifyFront.setBackground(yellowShapeFront);

        GradientDrawable yellowShapeBack = new GradientDrawable();
        yellowShapeBack.setCornerRadius(30f);
        yellowShapeBack.setColor(Color.parseColor("#FFc107"));
        btnVerifyBack.setBackground(yellowShapeBack);

        makeInteractive(btnSubmit);
        makeInteractive(btnVerifyFront);
        makeInteractive(btnVerifyBack);
        makeInteractive(tvBackToLogin);
        makeInteractive(btnToggleTheme);
        makeInteractive(btnToggleLanguage);


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
            btnToggleLanguage.setOnClickListener(v -> {
                isAmharic = !isAmharic;
                SharedPreferences.Editor editor = getSharedPreferences("UniFixSettings", MODE_PRIVATE).edit();
                editor.putBoolean("isAmharic", isAmharic);
                editor.apply();
                recreate();
            });
        }

        containerIdScanner.setVisibility(View.VISIBLE);

        applyTranslations();

        etStaffId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    tvAutoUsername.setText(isAmharic ? "የእርስዎ የተጠቃሚ ስም: teach... ይሆናል" : "Your username will be: teach...");
                } else {
                    String dynamicText = (isAmharic ? "የእርስዎ የተጠቃሚ ስም: <b>teach" : "Your username will be: <b>teach") + s.toString().trim() + "</b>" + (isAmharic ? " ይሆናል" : "");
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        tvAutoUsername.setText(Html.fromHtml(dynamicText, Html.FROM_HTML_MODE_COMPACT));
                    } else {
                        tvAutoUsername.setText(Html.fromHtml(dynamicText));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivShowPassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivShowPassword.setAlpha(0.4f);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivShowPassword.setAlpha(1.0f);
            }
            etPassword.setSelection(etPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        ivShowConfirmPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivShowConfirmPassword.setAlpha(0.4f);
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivShowConfirmPassword.setAlpha(1.0f);
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        });

        btnVerifyFront.setOnClickListener(v -> {
            if (isFrontVerified) return;
            if (checkManualReviewStatus()) return;

            currentScanSide = 1;
            startCameraCheck();
        });

        btnVerifyBack.setOnClickListener(v -> {
            if (isBackVerified) return;
            if (checkManualReviewStatus()) return;

            String typedId = etStaffId.getText().toString().trim();
            if (typedId.isEmpty()) {
                Toast.makeText(this, msgTypeIDFirst, Toast.LENGTH_SHORT).show();
                return;
            }

            currentScanSide = 2;
            startCameraCheck();
        });

        btnSubmit.setOnClickListener(v -> initiateRegistrationProcess());

        tvBackToLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void applyTranslations() {
        if (isAmharic) {
            tvRegTitle.setText("የመምህራን ምዝገባ");
            tvRegSubtitle.setText("የመታወቂያዎን የፊትና የጀርባ ክፍል ያንሱ። ስምዎ በራስ-ሰር ይነበባል።");
            etStaffId.setHint("የሰራተኛ መታወቂያ (ለምሳሌ 1234)");
            etPassword.setHint("የይለፍ ቃል ይፍጠሩ");
            etConfirmPassword.setHint("የይለፍ ቃል ያረጋግጡ");

            msgScanFrontBtn = "የፊት ክፍል ያንሱ";
            msgScanBackBtn = "የጀርባ ክፍል ያንሱ";
            msgRegisterBtn = "መለያ ይመዝገቡ";

            btnVerifyFront.setText(msgScanFrontBtn);
            btnVerifyBack.setText(msgScanBackBtn);
            btnSubmit.setText(msgRegisterBtn);
            tvBackToLogin.setText("ወደ መግቢያ ተመለስ");
            tvAutoUsername.setText("የእርስዎ የተጠቃሚ ስም: teach... ይሆናል");

            // Variables
            msgTypeIDFirst = "ለማረጋገጥ በመጀመሪያ የሰራተኛ መታወቂያዎን ያስገቡ!";
            msgCameraNotSupported = "ካሜራ በዚህ መሳሪያ ላይ አይደገፍም።";
            msgScanningFront = "የፊት ክፍል በማንበብ ላይ...";
            msgScanningBack = "የጀርባ ክፍል በማንበብ ላይ...";
            msgFailedLoadImg = "የምስል ፋይል መጫን አልተቻለም።";
            msgNameScanned = "ስምዎ በተሳካ ሁኔታ ተነቧል!";
            msgFrontVerified = "የፊት ክፍል ✅";
            msgFrontRetry = "ትክክለኛ ስም ማግኘት አልተቻለም። ምስሉ ግልጽ መሆኑን ያረጋግጡ።";
            msgErrorFront = "የፊት ካርድ በማንበብ ላይ ስህተት ተፈጥሯል።";
            msgBackVerified = "የጀርባ ክፍል ✅";
            msgErrorBack = "የጀርባ ካርድ በማንበብ ላይ ስህተት ተፈጥሯል።";
            msgPolicyError = "የደህንነት ፖሊሲ፡ በመጀመሪያ የመታወቂያዎን የፊት እና የጀርባ ክፍል ማንሳት ግዴታ ነው።";
            msgMandatoryFields = "እባክዎ ሁሉንም መስኮች ይሙሉ";
            msgPassMismatch = "የይለፍ ቃሎቹ አይመሳሰሉም!";
            msgWeakPass = "ደካማ የይለፍ ቃል! ቢያንስ 8 ፊደላት ሆኖ አቢይ ፊደል፣ ትንሽ ፊደል፣ ቁጥር እና ምልክት ማካተት አለበት።";
            msgChecking = "መኖሩን በማረጋገጥ ላይ...";
            msgAcctExistsTitle = "መለያው አስቀድሞ አለ! ⚠️";
            msgAcctExistsBody = "በዚህ መታወቂያ አስቀድሞ መለያ ተመዝግቧል።\n\nየእርስዎ የተጠቃሚ ስም:\n\n";
            msgLoginBtn = "ወደ መግቢያ ሂድ";
            msgDbError = "መታወቂያውን በማረጋገጥ ላይ የኔትወርክ ስህተት ተፈጥሯል።";
            msgSaving = "መለያ በማስቀመጥ ላይ...";
            msgRegDoneTitle = "ምዝገባው ተጠናቅቋል! 🎉";
            msgRegDoneBodySuccess = "መለያዎ በተሳካ ሁኔታ ተረጋግጦ ተፈጥሯል!<br><br>የእርስዎ የተጠቃሚ ስም:<br><br><b>";
            msgRegDoneBodyPending = "ምዝገባዎ ለአስተዳዳሪ ማረጋገጫ ተልኳል።<br><br>ሲረጋገጥ፣ የተጠቃሚ ስምዎ:<br><br><b>";
            msgRegDoneBodyEnd = "</b><br><br>ለመግባት ይህንን መዝግበው ይያዙ።";
            msgRegFailed = "መለያ መፍጠር አልተቻለም። ግንኙነትዎን ያረጋግጡ።";
            msgManualReviewBtn = "በእጅ ማረጋገጫ";
            msgManualReviewDialogTitle = "ማረጋገጫው አልተሳካም";
            msgManualReviewDialogBody = "የመታወቂያ ካርድዎን በራስ-ሰር ማረጋገጥ አልቻልንም።\n\nአሁንም 'መዝግብ'ን ጠቅ ማድረግ ይችላሉ፣ ነገር ግን መግባት ከመቻልዎ በፊት መለያዎ በአስተዳዳሪ መረጋገጥ አለበት።";
            msgUnderstood = "ገብቶኛል";
            msgMaxAttempts = "የሙከራ ገደብ ላይ ደርሰዋል። ለግምገማ መዝገብዎን ይቀጥሉ።";
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private boolean checkManualReviewStatus() {
        if (scanAttempts >= MAX_ATTEMPTS) {
            Toast.makeText(this, msgMaxAttempts, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private void startCameraCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile("ID_SCAN_", ".jpg", storageDir);
            } catch (IOException ex) {
                Toast.makeText(this, "Error setting up camera file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, currentScanSide == 1 ? REQUEST_IMAGE_FRONT : REQUEST_IMAGE_BACK);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && photoURI != null) {
            if (requestCode == REQUEST_IMAGE_FRONT) {
                btnVerifyFront.setText(msgScanningFront);
                btnVerifyFront.setEnabled(false);

                try {
                    InputImage image = InputImage.fromFilePath(this, photoURI);
                    verifyFrontWithMLKit(image);
                } catch (IOException e) {
                    handleFailedScan("Front");
                }
            } else if (requestCode == REQUEST_IMAGE_BACK) {
                btnVerifyBack.setText(msgScanningBack);
                btnVerifyBack.setEnabled(false);

                try {
                    InputImage image = InputImage.fromFilePath(this, photoURI);
                    verifyBackWithMLKit(image);
                } catch (IOException e) {
                    handleFailedScan("Back");
                }
            }
        }
    }

    private void verifyFrontWithMLKit(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String fullTextFound = visionText.getText().toUpperCase();

                    if (fullTextFound.contains("DEBRE BERHAN") || fullTextFound.contains("UNIVERSITY") || fullTextFound.contains("STUDENT") || fullTextFound.contains("STAFF") || fullTextFound.contains("TEACHER")) {
                        isFrontVerified = true;
                        btnVerifyFront.setText(msgFrontVerified);

                        GradientDrawable gd = new GradientDrawable();
                        gd.setCornerRadius(30f);
                        gd.setColor(Color.parseColor("#198754"));
                        btnVerifyFront.setBackground(gd);
                        btnVerifyFront.setTextColor(Color.WHITE);
                    } else {
                        handleFailedScan("Front");
                    }
                })
                .addOnFailureListener(e -> {
                    handleFailedScan("Front");
                });
    }

    private void verifyBackWithMLKit(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String fullTextFound = visionText.getText().toUpperCase();
                    String typedId = etStaffId.getText().toString().toUpperCase().trim();

                    if (fullTextFound.contains(typedId)) {
                        isBackVerified = true;
                        btnVerifyBack.setText(msgBackVerified);

                        GradientDrawable gd = new GradientDrawable();
                        gd.setCornerRadius(30f);
                        gd.setColor(Color.parseColor("#198754"));
                        btnVerifyBack.setBackground(gd);
                        btnVerifyBack.setTextColor(Color.WHITE);
                    } else {
                        handleFailedScan("Back");
                    }
                })
                .addOnFailureListener(e -> {
                    handleFailedScan("Back");
                });
    }

    private void handleFailedScan(String side) {
        scanAttempts++;
        int attemptsLeft = MAX_ATTEMPTS - scanAttempts;

        if (attemptsLeft > 0) {
            String errorMsg = side.equals("Front") ? msgErrorFront : msgErrorBack;
            Toast.makeText(this, errorMsg + " (" + attemptsLeft + " tries left)", Toast.LENGTH_LONG).show();

            if (side.equals("Front")) {
                btnVerifyFront.setText(msgScanFrontBtn);
                btnVerifyFront.setEnabled(true);
            } else {
                btnVerifyBack.setText(msgScanBackBtn);
                btnVerifyBack.setEnabled(true);
            }
        } else {
            btnVerifyFront.setText(msgManualReviewBtn);
            GradientDrawable red1 = new GradientDrawable();
            red1.setCornerRadius(30f);
            red1.setColor(Color.parseColor("#dc3545"));
            btnVerifyFront.setBackground(red1);
            btnVerifyFront.setTextColor(Color.WHITE);
            btnVerifyFront.setEnabled(false);

            btnVerifyBack.setText(msgManualReviewBtn);
            GradientDrawable red2 = new GradientDrawable();
            red2.setCornerRadius(30f);
            red2.setColor(Color.parseColor("#dc3545"));
            btnVerifyBack.setBackground(red2);
            btnVerifyBack.setTextColor(Color.WHITE);
            btnVerifyBack.setEnabled(false);

            isFrontVerified = true;
            isBackVerified = true;

            new AlertDialog.Builder(this)
                    .setTitle(msgManualReviewDialogTitle)
                    .setMessage(msgManualReviewDialogBody)
                    .setPositiveButton(msgUnderstood, null)
                    .show();
        }
    }

    private boolean isStrongPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-]).{8,}$";
        return password != null && password.matches(passwordPattern);
    }

    private void initiateRegistrationProcess() {
        if (!isFrontVerified || !isBackVerified) {
            Toast.makeText(this, msgPolicyError, Toast.LENGTH_LONG).show();
            return;
        }

        String staffId = etStaffId.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (staffId.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, msgMandatoryFields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, msgPassMismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isStrongPassword(password)) {
            Toast.makeText(this, msgWeakPass, Toast.LENGTH_LONG).show();
            return;
        }

        String username = "teach" + staffId;

        btnSubmit.setText(msgChecking);
        btnSubmit.setEnabled(false);

        db.collection("users").document(username).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        new AlertDialog.Builder(RegisterTeacherActivity.this)
                                .setTitle(msgAcctExistsTitle)
                                .setMessage(msgAcctExistsBody + username + (isAmharic ? "\n\nእባክዎ ለመግባት ይህንን ይጠቀሙ።" : "\n\nPlease use this to log in."))
                                .setCancelable(false)
                                .setPositiveButton(msgLoginBtn, (dialog, which) -> {
                                    finish();
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                })
                                .show();
                        resetButton();
                    } else {
                        boolean isPendingManualReview = (scanAttempts >= MAX_ATTEMPTS);
                        registerNewTeacherIntoDatabase(isPendingManualReview);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterTeacherActivity.this, msgDbError, Toast.LENGTH_LONG).show();
                    resetButton();
                });
    }

    private void registerNewTeacherIntoDatabase(boolean isPendingManualReview) {
        btnSubmit.setText(msgSaving);

        String staffId = etStaffId.getText().toString().trim();
        String password = etPassword.getText().toString();
        String user = "teach" + staffId;

        Map<String, Object> newTeacher = new HashMap<>();
        newTeacher.put("fullName", user);
        newTeacher.put("id", staffId);
        newTeacher.put("username", user);
        newTeacher.put("password", password);
        newTeacher.put("role", "Teacher");
        newTeacher.put("warnings", 0);
        newTeacher.put("isBanned", false);
        newTeacher.put("createdAt", System.currentTimeMillis());

        if (!isPendingManualReview) {
            newTeacher.put("isPendingReview", false);
            newTeacher.put("status", "Active");
        } else {
            newTeacher.put("isPendingReview", true);
            newTeacher.put("status", "Pending Admin Approval");
        }

        db.collection("users").document(user).set(newTeacher)
                .addOnSuccessListener(documentReference -> {
                    String messageBody;
                    if (!isPendingManualReview) {
                        messageBody = msgRegDoneBodySuccess + user + msgRegDoneBodyEnd;
                    } else {
                        messageBody = msgRegDoneBodyPending + user + msgRegDoneBodyEnd;
                    }

                    Spanned spannedMessage;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        spannedMessage = Html.fromHtml(messageBody, Html.FROM_HTML_MODE_COMPACT);
                    } else {
                        spannedMessage = Html.fromHtml(messageBody);
                    }

                    new AlertDialog.Builder(RegisterTeacherActivity.this)
                            .setTitle(msgRegDoneTitle)
                            .setMessage(spannedMessage)
                            .setCancelable(false)
                            .setPositiveButton(isAmharic ? "ጨርስ" : "Done", (dialog, which) -> {
                                finish();
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            })
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterTeacherActivity.this, msgRegFailed, Toast.LENGTH_LONG).show();
                    resetButton();
                });
    }

    private void resetButton() {
        btnSubmit.setText(msgRegisterBtn);
        btnSubmit.setEnabled(true);
    }
}