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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterStudentActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    // UI Elements
    EditText etStudentIdNumbers, etPassword, etConfirmPassword, etManualDept;
    LinearLayout layoutIdPrefix, containerIdScanner;
    Spinner spDept;
    Button btnVerifyFront, btnVerifyBack, btnSubmit, btnToggleTheme, btnToggleLanguage;
    TextView tvBackToLogin, tvAutoUsername, tvRegTitle, tvRegSubtitle;

    ImageView ivShowPassword, ivShowConfirmPassword;
    boolean isPasswordVisible = false;
    boolean isConfirmPasswordVisible = false;

    FirebaseFirestore db;

    // TWO-STEP VERIFICATION TRACKERS
    boolean isFrontVerified = false;
    boolean isBackVerified = false;
    int scanAttempts = 0;
    final int MAX_ATTEMPTS = 3;
    int currentScanSide = 0; // 1 = Front, 2 = Back

    Uri photoURI;
    String autoExtractedName = "Student";

    // --- LANGUAGE LOGIC ---
    boolean isAmharic = false;

    // Translated Strings
    String msgTypeIDFirst = "Please type your ID numbers first so we can verify the back!";
    String msgCameraNotSupported = "Camera not supported on this device.";
    String msgScanningFront = "SCANNING FRONT...";
    String msgScanningBarcode = "SCANNING BARCODE...";
    String msgFailedLoadImg = "Failed to load image file.";
    String msgNameScanned = "Name Scanned Successfully!";
    String msgFrontVerified = "FRONT VERIFIED ✅";
    String msgFrontRetry = "Could not find a valid name. Ensure the image is clear.";
    String msgErrorFront = "Error reading front card.";
    String msgScanFrontBtn = "SCAN FRONT";
    String msgBarcodeVerified = "Barcode Verified securely!";
    String msgBackVerified = "BACK VERIFIED ✅";
    String msgBarcodeMismatch = "Barcode does NOT match the ID you typed! Try again.";
    String msgErrorBack = "Error scanning barcode.";
    String msgScanBackBtn = "SCAN BACK";
    String msgPolicyError = "Security Policy: You MUST scan the Front and Back of your physical ID first.";
    String msgManualDeptReq = "Please type your department/college name!";
    String msgMandatoryFields = "Please fill all mandatory fields";
    String msgPassMismatch = "Passwords do not match! Please check and try again.";
    String msgWeakPass = "WEAK PASSWORD! Must be 8+ characters and include: Uppercase, Lowercase, Number, and Symbol.";
    String msgChecking = "Checking Availability...";
    String msgAcctExistsTitle = "Account Already Exists! ⚠️";
    String msgAcctExistsBody = "An account with this ID has already been registered.\n\nYour Username is:\n\n";
    String msgLoginBtn = "Go to Login";
    String msgDbError = "Database Error";
    String msgFinalizing = "Finalizing Registration...";
    String msgRegisterBtn = "REGISTER ACCOUNT";
    String msgRegDoneTitle = "Registration Done! 🎉";
    String msgRegDoneBodyStart = "Your account was successfully verified and created!<br><br>Your official Login Username is:<br><br><b>";
    String msgRegDoneBodyPending = "Your registration was submitted to the Admins for manual review.<br><br>Once approved, your Username will be:<br><br><b>";
    String msgRegDoneBodyEnd = "</b><br><br>Please write this down to log in.";
    String msgRegFailed = "Failed to create account";
    String msgManualReviewBtn = "MANUAL REVIEW";
    String msgManualReviewDialogTitle = "Verification Failed";
    String msgManualReviewDialogBody = "We could not automatically verify your ID card.\n\nYou can still click Register, but your account will require manual approval by an Admin before you can log in.";
    String msgUnderstood = "Understood";
    String msgMaxAttempts = "Max attempts reached. Proceed to register for manual review.";

    // --- 🎨 UI HELPER METHODS ---
    private void styleFloatingCard(View v) {
        if (v != null) {
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(30f);
            shape.setColor(ContextCompat.getColor(this, R.color.card_background));
            v.setBackground(shape);
            v.setElevation(8f);
            v.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            v.setClipToOutline(true);
        }
    }

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
        setContentView(R.layout.activity_register_student);

        db = FirebaseFirestore.getInstance();

        // READ LANGUAGE PREFERENCE
        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);

        // Link UI Elements
        tvRegTitle = findViewById(R.id.tvRegTitle);
        tvRegSubtitle = findViewById(R.id.tvRegSubtitle);
        layoutIdPrefix = findViewById(R.id.layoutIdPrefix);
        etStudentIdNumbers = findViewById(R.id.etStudentIdNumbers);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etManualDept = findViewById(R.id.etManualDept);
        spDept = findViewById(R.id.spDept);

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

        // Apply Premium Styling
        styleInputBox(layoutIdPrefix);
        styleInputBox(etPassword);
        styleInputBox(etConfirmPassword);
        styleInputBox(etManualDept);
        styleInputBox(spDept);

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

        containerIdScanner.setVisibility(View.VISIBLE);

        applyTranslations();
        setupDropdowns();
        setupPasswordVisibilityToggles();

        etStudentIdNumbers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    tvAutoUsername.setText(isAmharic ? "የእርስዎ የተጠቃሚ ስም: stud... ይሆናል" : "Your username will be: stud and the id number only...");
                } else {
                    String dynamicText = (isAmharic ? "የእርስዎ የተጠቃሚ ስም: <b>stud" : "Your username will be: <b>stud") + s.toString().trim() + "</b>" + (isAmharic ? " ይሆናል" : "");
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

        btnVerifyFront.setOnClickListener(v -> {
            if (isFrontVerified) return;
            if (checkManualReviewStatus()) return;
            currentScanSide = 1;
            startCameraCheck();
        });

        btnVerifyBack.setOnClickListener(v -> {
            if (isBackVerified) return;
            if (checkManualReviewStatus()) return;

            String numbersOnly = etStudentIdNumbers.getText().toString().trim();
            if (numbersOnly.isEmpty()) {
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
            tvRegTitle.setText("የተማሪ ምዝገባ");
            tvRegSubtitle.setText("የመታወቂያዎን የፊትና የጀርባ ክፍል ያንሱ። ስምዎ በራስ-ሰር ይነበባል።");
            etStudentIdNumbers.setHint("የመታወቂያ ቁጥር (ለምሳሌ 1601218)");
            etPassword.setHint("የይለፍ ቃል ይፍጠሩ");
            etConfirmPassword.setHint("የይለፍ ቃል ያረጋግጡ");
            etManualDept.setHint("ለምሳሌ ማህበራዊ ሳይንስ፣ ቴክኖሎጂ");

            msgScanFrontBtn = "የፊት ክፍል ያንሱ";
            msgScanBackBtn = "የጀርባ ክፍል ያንሱ";
            msgRegisterBtn = "መለያ ይመዝገቡ";

            btnVerifyFront.setText(msgScanFrontBtn);
            btnVerifyBack.setText(msgScanBackBtn);
            btnSubmit.setText(msgRegisterBtn);
            tvBackToLogin.setText("ወደ መግቢያ ተመለስ");
            tvAutoUsername.setText("የእርስዎ የተጠቃሚ ስም: stud... ይሆናል");

            // Variable Translations
            msgTypeIDFirst = "የጀርባውን ክፍል ለማረጋገጥ በመጀመሪያ የመታወቂያ ቁጥርዎን ያስገቡ!";
            msgCameraNotSupported = "ካሜራ በዚህ መሳሪያ ላይ አይደገፍም።";
            msgScanningFront = "የፊት ክፍል በማንበብ ላይ...";
            msgScanningBarcode = "ባርኮድ በማንበብ ላይ...";
            msgFailedLoadImg = "የምስል ፋይል መጫን አልተቻለም።";
            msgNameScanned = "ስምዎ በተሳካ ሁኔታ ተነቧል!";
            msgFrontVerified = "የፊት ክፍል ተረጋግጧል ✅";
            msgFrontRetry = "ትክክለኛ ስም ማግኘት አልተቻለም። ምስሉ ግልጽ መሆኑን ያረጋግጡ።";
            msgErrorFront = "የፊት ካርድ በማንበብ ላይ ስህተት ተፈጥሯል።";
            msgBarcodeVerified = "ባርኮድ በተሳካ ሁኔታ ተረጋግጧል!";
            msgBackVerified = "የጀርባ ክፍል ተረጋግጧል ✅";
            msgBarcodeMismatch = "ባርኮዱ ካስገቡት መታወቂያ ጋር አይመሳሰልም! እንደገና ይሞክሩ።";
            msgErrorBack = "ባርኮድ በማንበብ ላይ ስህተት ተፈጥሯል።";
            msgPolicyError = "የደህንነት ፖሊሲ፡ በመጀመሪያ የመታወቂያዎን የፊት እና የጀርባ ክፍል ማንሳት ግዴታ ነው።";
            msgManualDeptReq = "እባክዎ የክፍል/ኮሌጅዎን ስም ያስገቡ!";
            msgMandatoryFields = "እባክዎ ሁሉንም አስፈላጊ መስኮች ይሙሉ";
            msgPassMismatch = "የይለፍ ቃሎቹ አይመሳሰሉም! እባክዎ አረጋግጠው እንደገና ይሞክሩ።";
            msgWeakPass = "ደካማ የይለፍ ቃል! ቢያንስ 8 ፊደላት ሆኖ አቢይ ፊደል፣ ትንሽ ፊደል፣ ቁጥር እና ምልክት ማካተት አለበት።";
            msgChecking = "መኖሩን በማረጋገጥ ላይ...";
            msgAcctExistsTitle = "መለያው አስቀድሞ አለ! ⚠️";
            msgAcctExistsBody = "በዚህ መታወቂያ አስቀድሞ መለያ ተመዝግቧል።\n\nየእርስዎ የተጠቃሚ ስም:\n\n";
            msgLoginBtn = "ወደ መግቢያ ሂድ";
            msgDbError = "የዳታቤዝ ስህተት";
            msgFinalizing = "ምዝገባን በማጠናቀቅ ላይ...";
            msgRegDoneTitle = "ምዝገባው ተጠናቅቋል! 🎉";
            msgRegDoneBodyStart = "መለያዎ በተሳካ ሁኔታ ተረጋግጦ ተፈጥሯል!<br><br>የእርስዎ መግቢያ የተጠቃሚ ስም:<br><br><b>";
            msgRegDoneBodyPending = "ምዝገባዎ ለአስተዳዳሪ ማረጋገጫ ተልኳል።<br><br>ሲረጋገጥ፣ የተጠቃሚ ስምዎ:<br><br><b>";
            msgRegDoneBodyEnd = "</b><br><br>ለመግባት ይህንን መዝግበው ይያዙ።";
            msgRegFailed = "መለያ መፍጠር አልተቻለም";
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

    private void setupPasswordVisibilityToggles() {
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
    }

    private void setupDropdowns() {
        String[] departments;
        if (isAmharic) {
            departments = new String[]{
                    "ክፍል/ኮሌጅ ይምረጡ...",
                    "ኮምፒውቲንግ፡ ኢንፎርሜሽን ቴክኖሎጂ", "ኮምፒውቲንግ፡ ኢንፎርሜሽን ሲስተምስ", "ኮምፒውቲንግ፡ ሶፍትዌር ምህንድስና", "ኮምፒውቲንግ፡ ዳታ ሳይንስ",
                    "ምህንድስና፡ ሲቪል ምህንድስና", "ምህንድስና፡ COTM", "ምህንድስና፡ ኬሚካል ምህንድስና",
                    "ማህበራዊ ሳይንስ፡ አካውንቲንግ", "ማህበራዊ ሳይንስ፡ ማርኬቲንግ", "ማህበራዊ ሳይንስ፡ ማኔጅመንት", "ማህበራዊ ሳይንስ፡ ሎጂስቲክስ",
                    "ጤና፡ ህክምና", "ጤና፡ ሜዲካል ላብራቶሪ", "ጤና፡ ነርሲንግ",
                    "ሌላ (በእጅ ያስገቡ)"
            };
        } else {
            departments = new String[]{
                    "Select Department/College...",
                    "Computing: Information Technology", "Computing: Information Systems", "Computing: Software Engineering", "Computing: Data Science",
                    "Engineering: Civil Engineering", "Engineering: COTM", "Engineering: Chemical Engineering",
                    "Social Science: Accounting", "Social Science: Marketing", "Social Science: Management", "Social Science: Logistics",
                    "Health: Medicine", "Health: Medical Lab", "Health: Nursing",
                    "Other (Type manually)"
            };
        }

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, departments);
        spDept.setAdapter(deptAdapter);

        spDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (departments[position].equals("Other (Type manually)") || departments[position].equals("ሌላ (በእጅ ያስገቡ)")) {
                    etManualDept.setVisibility(View.VISIBLE);
                } else {
                    etManualDept.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private String getEnglishDepartment(String selectedDept) {
        if (!isAmharic) return selectedDept;

        switch (selectedDept) {
            case "ኮምፒውቲንግ፡ ኢንፎርሜሽን ቴክኖሎጂ": return "Computing: Information Technology";
            case "ኮምፒውቲንግ፡ ኢንፎርሜሽን ሲስተምስ": return "Computing: Information Systems";
            case "ኮምፒውቲንግ፡ ሶፍትዌር ምህንድስና": return "Computing: Software Engineering";
            case "ኮምፒውቲንግ፡ ዳታ ሳይንስ": return "Computing: Data Science";
            case "ምህንድስና፡ ሲቪል ምህንድስና": return "Engineering: Civil Engineering";
            case "ምህንድስና፡ COTM": return "Engineering: COTM";
            case "ምህንድስና፡ ኬሚካል ምህንድስና": return "Engineering: Chemical Engineering";
            case "ማህበራዊ ሳይንስ፡ አካውንቲንግ": return "Social Science: Accounting";
            case "ማህበራዊ ሳይንስ፡ ማርኬቲንግ": return "Social Science: Marketing";
            case "ማህበራዊ ሳይንስ፡ ማኔጅመንት": return "Social Science: Management";
            case "ማህበራዊ ሳይንስ፡ ሎጂስቲክስ": return "Social Science: Logistics";
            case "ጤና፡ ህክምና": return "Health: Medicine";
            case "ጤና፡ ሜዲካል ላብራቶሪ": return "Health: Medical Lab";
            case "ጤና፡ ነርሲንግ": return "Health: Nursing";
            case "ሌላ (በእጅ ያስገቡ)": return "Other (Type manually)";
            default: return selectedDept;
        }
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
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && photoURI != null) {
            if (currentScanSide == 1) btnVerifyFront.setText(msgScanningFront);
            if (currentScanSide == 2) btnVerifyBack.setText(msgScanningBarcode);

            try {
                InputImage image = InputImage.fromFilePath(this, photoURI);
                verifyIdWithMLKit(image);
            } catch (IOException e) {
                handleFailedScan("Front");
            }
        }
    }

    private void verifyIdWithMLKit(InputImage image) {
        if (currentScanSide == 1) {
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String[] lines = visionText.getText().split("\n");
                        boolean foundHeader = false;
                        autoExtractedName = "";

                        for (String line : lines) {
                            String cleanLine = line.trim().toUpperCase();
                            if (cleanLine.contains("DEBRE BERHAN UNIVERSITY")) {
                                foundHeader = true;
                                continue;
                            }
                            if (foundHeader && cleanLine.length() > 5 && !cleanLine.matches(".*\\d.*")
                                    && !cleanLine.contains("GENDER") && !cleanLine.contains("STUDENT")) {
                                autoExtractedName = line.trim();
                                break;
                            }
                        }

                        if (!autoExtractedName.isEmpty()) {
                            isFrontVerified = true;
                            btnVerifyFront.setText(msgFrontVerified);
                            GradientDrawable gd = new GradientDrawable();
                            gd.setCornerRadius(30f);
                            gd.setColor(Color.parseColor("#198754"));
                            btnVerifyFront.setBackground(gd);
                            btnVerifyFront.setTextColor(Color.WHITE);
                            btnVerifyFront.setEnabled(false);
                            Toast.makeText(this, msgNameScanned, Toast.LENGTH_SHORT).show();
                        } else {
                            handleFailedScan("Front");
                        }
                    })
                    .addOnFailureListener(e -> {
                        handleFailedScan("Front");
                    });
        } else if (currentScanSide == 2) {
            BarcodeScanner barcodeScanner = BarcodeScanning.getClient();
            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        String typedId = etStudentIdNumbers.getText().toString().trim();
                        boolean match = false;
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && rawValue.contains(typedId)) {
                                match = true;
                                break;
                            }
                        }

                        if (match) {
                            isBackVerified = true;
                            btnVerifyBack.setText(msgBackVerified);
                            GradientDrawable gd = new GradientDrawable();
                            gd.setCornerRadius(30f);
                            gd.setColor(Color.parseColor("#198754"));
                            btnVerifyBack.setBackground(gd);
                            btnVerifyBack.setTextColor(Color.WHITE);
                            btnVerifyBack.setEnabled(false);
                            Toast.makeText(this, msgBarcodeVerified, Toast.LENGTH_SHORT).show();
                        } else {
                            handleFailedScan("Back");
                        }
                    })
                    .addOnFailureListener(e -> {
                        handleFailedScan("Back");
                    });
        }
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

        String idNumbers = etStudentIdNumbers.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        String selectedDept = spDept.getSelectedItem().toString();
        if (selectedDept.equals("Other (Type manually)") || selectedDept.equals("ሌላ (በእጅ ያስገቡ)")) {
            if (etManualDept.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, msgManualDeptReq, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (idNumbers.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || selectedDept.contains("Select") || selectedDept.contains("ይምረጡ")) {
            Toast.makeText(this, msgMandatoryFields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, msgPassMismatch, Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStrongPassword(password)) {
            Toast.makeText(this, msgWeakPass, Toast.LENGTH_LONG).show();
            return;
        }

        String generatedUsername = "stud" + idNumbers;

        btnSubmit.setText(msgChecking);
        btnSubmit.setEnabled(false);

        db.collection("users").document(generatedUsername).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            new AlertDialog.Builder(RegisterStudentActivity.this)
                                    .setTitle(msgAcctExistsTitle)
                                    .setMessage(msgAcctExistsBody + generatedUsername + (isAmharic ? "\n\nእባክዎ ለመግባት ይህንን ይጠቀሙ።" : "\n\nPlease use this to log in."))
                                    .setCancelable(false)
                                    .setPositiveButton(msgLoginBtn, (dialog, which) -> {
                                        finish();
                                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                    })
                                    .show();
                            resetButton();
                        } else {
                            boolean isPendingManualReview = (scanAttempts >= MAX_ATTEMPTS);
                            registerNewStudentIntoDatabase(isPendingManualReview);
                        }
                    } else {
                        Toast.makeText(RegisterStudentActivity.this, msgDbError, Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                });
    }

    private void registerNewStudentIntoDatabase(boolean isPendingManualReview) {
        btnSubmit.setText(msgFinalizing);

        String idNumbers = etStudentIdNumbers.getText().toString().trim();
        String fullStudentId = "DBU" + idNumbers;
        String password = etPassword.getText().toString();
        String selectedDept = spDept.getSelectedItem().toString();

        String englishDept = getEnglishDepartment(selectedDept);
        String finalDept = englishDept.equals("Other (Type manually)") ? etManualDept.getText().toString().trim() : englishDept;

        String generatedUsername = "stud" + idNumbers;

        long currentMillis = System.currentTimeMillis();
        long fiveYearsMillis = 5L * 365L * 24L * 60L * 60L * 1000L;
        long expirationMillis = currentMillis + fiveYearsMillis;

        Map<String, Object> newStudent = new HashMap<>();
        newStudent.put("fullName", autoExtractedName);
        newStudent.put("id", fullStudentId);
        newStudent.put("username", generatedUsername);
        newStudent.put("password", password);
        newStudent.put("role", "Student");
        newStudent.put("dept", finalDept);
        newStudent.put("warnings", 0);
        newStudent.put("isBanned", false);
        newStudent.put("createdAt", currentMillis);
        newStudent.put("expiresAt", expirationMillis);

        if (!isPendingManualReview) {
            newStudent.put("isPendingReview", false);
            newStudent.put("status", "Active");
        } else {
            newStudent.put("isPendingReview", true);
            newStudent.put("status", "Pending Admin Approval");
        }

        db.collection("users").document(generatedUsername).set(newStudent)
                .addOnSuccessListener(documentReference -> {
                    String messageBody;
                    if (!isPendingManualReview) {
                        messageBody = msgRegDoneBodyStart + generatedUsername + msgRegDoneBodyEnd;
                    } else {
                        messageBody = msgRegDoneBodyPending + generatedUsername + msgRegDoneBodyEnd;
                    }

                    Spanned spannedMessage;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        spannedMessage = Html.fromHtml(messageBody, Html.FROM_HTML_MODE_COMPACT);
                    } else {
                        spannedMessage = Html.fromHtml(messageBody);
                    }

                    new AlertDialog.Builder(RegisterStudentActivity.this)
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
                    Toast.makeText(RegisterStudentActivity.this, msgRegFailed, Toast.LENGTH_LONG).show();
                    resetButton();
                });
    }

    private void resetButton() {
        btnSubmit.setText(msgRegisterBtn);
        btnSubmit.setEnabled(true);
    }
}