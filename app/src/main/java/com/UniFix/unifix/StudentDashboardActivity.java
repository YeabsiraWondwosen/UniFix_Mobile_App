package com.UniFix.unifix;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.UUID;

public class StudentDashboardActivity extends AppCompatActivity {

    boolean isAmharic = false;

    Spinner spinnerCategory, spinnerUrgency;
    EditText etPhone, etDescription, etDormBlock, etDormRoom;
    Button btnSubmitReport, btnSettings, btnTabReport, btnTabHistory, btnAttachPhoto;
    ImageView ivPhotoPreview;
    LinearLayout dynamicDormitory, containerReportForm, containerHistory;
    TextView tvStudentName;

    TextView tvGlobalAlertBadge;
    List<DocumentSnapshot> activeSystemAlerts = new ArrayList<>();

    LinearLayout dynamicIct, dynamicIctLab;
    Spinner spinnerIctType;
    EditText etIctBuilding, etIctRoom, etIctPlaceName;

    String loggedInUserName = "Unknown Student";
    FirebaseFirestore db;
    FirebaseStorage storage;
    Uri selectedImageUri = null;

    private static final int PICK_IMAGE_REQUEST = 100;

    Stack<String> tabHistory = new Stack<>();
    String currentTab = "report";
    long backPressedTime = 0;

    String msgProcessing = "Processing...";
    String msgUploading = "Uploading Image...";
    String msgSaving = "Saving Report...";
    String msgSubmitBtn = "Submit Report";
    String msgAttachPhoto = "📎 Attach Photo (Optional)";
    String msgChangePhoto = "✏️ Change Photo";
    String msgReportSubmitted = "Report Submitted! 🟡 Awaiting Admin Approval.";
    String msgReqFields = "Phone and Description are required";
    String msgReqCategory = "Please select a Category";

    private void styleFloatingCard(View v) {
        if (v != null) {
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(40f);
            shape.setColor(ContextCompat.getColor(this, R.color.input_background));
            v.setBackground(shape);
            v.setElevation(4f);
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
        setContentView(R.layout.activity_student_dashboard);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUrgency = findViewById(R.id.spinnerUrgency);
        etPhone = findViewById(R.id.etPhone);
        etDescription = findViewById(R.id.etDescription);
        etDormBlock = findViewById(R.id.etDormBlock);
        etDormRoom = findViewById(R.id.etDormRoom);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        btnSettings = findViewById(R.id.btnSettings);
        dynamicDormitory = findViewById(R.id.dynamicDormitory);
        tvStudentName = findViewById(R.id.tvStudentName);

        dynamicIct = findViewById(R.id.dynamicIct);
        dynamicIctLab = findViewById(R.id.dynamicIctLab);
        spinnerIctType = findViewById(R.id.spinnerIctType);
        etIctBuilding = findViewById(R.id.etIctBuilding);
        etIctRoom = findViewById(R.id.etIctRoom);
        etIctPlaceName = findViewById(R.id.etIctPlaceName);

        styleInputBox(spinnerCategory);
        styleInputBox(spinnerUrgency);
        styleInputBox(etPhone);
        styleInputBox(etDescription);
        styleInputBox(etDormBlock);
        styleInputBox(etDormRoom);
        styleInputBox(spinnerIctType);
        styleInputBox(etIctBuilding);
        styleInputBox(etIctRoom);
        styleInputBox(etIctPlaceName);

        makeInteractive(btnSubmitReport);
        makeInteractive(btnSettings);

        String userName = getIntent().getStringExtra("FULL_NAME");
        if (userName != null && !userName.isEmpty()) {
            loggedInUserName = userName;
        }
        String userRaw = getIntent().getStringExtra("USERNAME");
        if(userRaw != null) {
            loggedInUserName = userRaw;
        }

        String shortName = loggedInUserName;
        if (shortName.contains(" ")) shortName = shortName.split(" ")[0];
        tvStudentName.setText((isAmharic ? "ሰላም, " : "Hi, ") + shortName);

        setupGlobalAlertBell();

        btnAttachPhoto = findViewById(R.id.btnAttachPhoto);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        makeInteractive(btnAttachPhoto);

        btnTabReport = findViewById(R.id.btnTabReport);
        btnTabHistory = findViewById(R.id.btnTabHistory);
        makeInteractive(btnTabReport);
        makeInteractive(btnTabHistory);

        containerReportForm = findViewById(R.id.containerReportForm);
        containerHistory = findViewById(R.id.containerHistory);

        applyStaticTranslations();
        setupSpinners();
        setupTabs();

        btnSettings.setOnClickListener(v -> showSettingsMenu());
        btnSubmitReport.setOnClickListener(v -> submitReport());
        btnAttachPhoto.setOnClickListener(v -> openGallery());
    }

    private void setupGlobalAlertBell() {
        Button btnBell = findViewById(R.id.btnBell);
        tvGlobalAlertBadge = findViewById(R.id.tvGlobalAlertBadge);
        View badgeContainer = findViewById(R.id.badgeContainer);

        if (btnBell != null && tvGlobalAlertBadge != null && badgeContainer != null) {
            makeInteractive(btnBell);
            btnBell.setOnClickListener(v -> showSystemAlertsDialog());

            db.collection("user_messages")
                    .whereEqualTo("recipient", loggedInUserName)
                    .whereEqualTo("sender", "System Alerts")
                    .whereEqualTo("status", "Unread")
                    .addSnapshotListener((value, error) -> {
                        if (error != null || value == null) return;
                        activeSystemAlerts = value.getDocuments();
                        if (!activeSystemAlerts.isEmpty()) {
                            tvGlobalAlertBadge.setText(String.valueOf(activeSystemAlerts.size()));
                            badgeContainer.setVisibility(View.VISIBLE);
                        } else {
                            badgeContainer.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void showSystemAlertsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isAmharic ? "የስርዓት ማንቂያዎች 🚨" : "System Alerts 🚨");

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));

        if (activeSystemAlerts.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(isAmharic ? "ምንም አዲስ ማንቂያ የለም።" : "No new alerts.");
            tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            layout.addView(tvEmpty);
        } else {
            for (DocumentSnapshot doc : activeSystemAlerts) {
                TextView tvAlert = new TextView(this);
                tvAlert.setText(doc.getString("text"));
                tvAlert.setTextColor(Color.parseColor("#dc3545"));
                tvAlert.setPadding(20, 20, 20, 20);
                tvAlert.setTypeface(null, Typeface.BOLD);

                GradientDrawable alertBg = new GradientDrawable();
                alertBg.setCornerRadius(20f);
                alertBg.setColor(ContextCompat.getColor(this, R.color.input_background));
                alertBg.setStroke(3, Color.parseColor("#dc3545"));
                tvAlert.setBackground(alertBg);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 0, 0, 20);
                tvAlert.setLayoutParams(lp);

                layout.addView(tvAlert);

                db.collection("user_messages").document(doc.getId()).update("status", "Read");
            }
        }

        scroll.addView(layout);
        builder.setView(scroll);
        builder.setPositiveButton(isAmharic ? "ዝጋ" : "Close", null);
        builder.show();
    }

    private void applyStaticTranslations() {
        if (!isAmharic) return;

        TextView tvNavTitle = findViewById(R.id.navTitle);
        if(tvNavTitle != null) tvNavTitle.setText("UniFix ተማሪ");

        btnTabReport.setText("ሪፖርት አድርግ");
        btnTabHistory.setText("የእኔ ታሪክ");

        TextView tvFormTitle = findViewById(R.id.tvFormTitle);
        if(tvFormTitle != null) tvFormTitle.setText("አዲስ ሪፖርት ያስገቡ");

        TextView tvLabelCategory = findViewById(R.id.tvLabelCategory);
        if(tvLabelCategory != null) tvLabelCategory.setText("ምድብ:");

        TextView tvLabelPhone = findViewById(R.id.tvLabelPhone);
        if(tvLabelPhone != null) tvLabelPhone.setText("ስልክ ቁጥር (ግዴታ):");
        etPhone.setHint("ለምሳሌ 09... / 07.../+251");

        TextView tvLabelDorm = findViewById(R.id.tvLabelDorm);
        if(tvLabelDorm != null) tvLabelDorm.setText("የመኝታ ክፍል ዝርዝሮች:");
        etDormBlock.setHint("ብሎክ ቁጥር");
        etDormRoom.setHint("ክፍል ቁጥር");

        TextView tvLabelIct = findViewById(R.id.tvLabelIct);
        if(tvLabelIct != null) tvLabelIct.setText("አይሲቲ / ቴክኖሎጂ ዝርዝሮች:");
        etIctBuilding.setHint("የህንፃ ስም/ቁጥር");
        etIctRoom.setHint("ክፍል ቁጥር");
        etIctPlaceName.setHint("የቦታው/ቢሮው ስም");

        TextView tvLabelUrgency = findViewById(R.id.tvLabelUrgency);
        if(tvLabelUrgency != null) tvLabelUrgency.setText("የአስቸኳይነት ደረጃ:");

        TextView tvLabelDesc = findViewById(R.id.tvLabelDesc);
        if(tvLabelDesc != null) tvLabelDesc.setText("ዝርዝር መግለጫ:");
        etDescription.setHint("እባክዎ ችግሩን በግልጽ ያብራሩ...");

        btnAttachPhoto.setText("📎 ፎቶ አያይዝ (አማራጭ)");
        btnSubmitReport.setText("ሪፖርት አስገባ");

        msgProcessing = "በማቀነባበር ላይ...";
        msgUploading = "ፎቶ በመጫን ላይ...";
        msgSaving = "ሪፖርት በማስቀመጥ ላይ...";
        msgSubmitBtn = "ሪፖርት አስገባ";
        msgAttachPhoto = "📎 ፎቶ አያይዝ (አማራጭ)";
        msgChangePhoto = "✏️ ፎቶ ቀይር";
        msgReportSubmitted = "ሪፖርት ገብቷል! 🟡 የአስተዳዳሪ ማረጋገጫ በመጠባበቅ ላይ።";
        msgReqFields = "ስልክ ቁጥር እና መግለጫ ማስገባት ግዴታ ነው";
        msgReqCategory = "እባክዎ ምድብ ይምረጡ";
    }

    private String getTranslatedCategory(String englishCat) {
        if (!isAmharic) return englishCat;
        if (englishCat == null) return "ሌላ";
        switch (englishCat) {
            case "ICT / Technology": return "አይሲቲ / ቴክኖሎጂ";
            case "Dormitory": return "መኝታ ክፍል";
            case "Academic Resources": return "አካዳሚክ ግብዓቶች";
            case "Cafeteria": return "ካፌ";
            case "Human Resources": return "የሰው ኃይል";
            case "Health Center": return "ጤና ጣቢያ";
            case "Campus Security": return "የካምፓስ ደህንነት";
            case "Finance": return "ፋይናንስ";
            case "Administration": return "አስተዳደር";
            case "Department Head": return "የትምህርት ክፍል ኃላፊ";
            case "Select...": return "ይምረጡ...";
            default: return "ሌላ";
        }
    }

    private String getEnglishCategory(String amharicCat) {
        if (!isAmharic) return amharicCat;
        switch (amharicCat) {
            case "አይሲቲ / ቴክኖሎጂ": return "ICT / Technology";
            case "መኝታ ክፍል": return "Dormitory";
            case "አካዳሚክ ግብዓቶች": return "Academic Resources";
            case "ካፌ": return "Cafeteria";
            case "የሰው ኃይል": return "Human Resources";
            case "ጤና ጣቢያ": return "Health Center";
            case "የካምፓስ ደህንነት": return "Campus Security";
            case "ፋይናንስ": return "Finance";
            case "አስተዳደር": return "Administration";
            case "የትምህርት ክፍል ኃላፊ": return "Department Head";
            case "ይምረጡ...": return "Select...";
            default: return "Other";
        }
    }

    @Override
    public void onBackPressed() {
        if (!tabHistory.isEmpty()) {
            String previousTab = tabHistory.pop();
            switchToTab(previousTab, false);
        } else {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                backPressedTime = System.currentTimeMillis();
                Toast.makeText(this, isAmharic ? "በፍጥነት ለመውጣት የጀርባ ቁልፍን በድጋሚ ይጫኑ" : "Press back again to exit quickly", Toast.LENGTH_SHORT).show();
                confirmLogout();
            }
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(isAmharic ? "ከመተግበሪያው ውጣ" : "Exit App")
                .setMessage(isAmharic ? "እርግጠኛ ነዎት መውጣት ይፈልጋሉ?" : "Are you sure you want to log out and exit?")
                .setPositiveButton(isAmharic ? "አዎ፣ ውጣ" : "Yes, Exit", (dialog, which) -> {
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                })
                .setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null)
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            ivPhotoPreview.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            ivPhotoPreview.setClipToOutline(true);
            GradientDrawable imgShape = new GradientDrawable();
            imgShape.setCornerRadius(20f);
            ivPhotoPreview.setBackground(imgShape);

            ivPhotoPreview.setImageURI(selectedImageUri);
            ivPhotoPreview.setVisibility(View.VISIBLE);
            btnAttachPhoto.setText(msgChangePhoto);
        }
    }

    private void showSettingsMenu() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        String themeText = isNight ? (isAmharic ? "☀️ ብሩህ ገጽታ" : "☀️ Light Theme") : (isAmharic ? "🌙 ጨለማ ገጽታ" : "🌙 Dark Theme");
        String langText = isAmharic ? "🌐 English" : "🌐 አማርኛ";

        String[] options = {
                isAmharic ? "መገለጫ / ስልክ / የይለፍ ቃል ያርትዑ" : "Edit Profile Info",
                themeText,
                langText,
                isAmharic ? "ውጣ" : "Logout",
                isAmharic ? "ሰርዝ" : "Cancel"
        };

        new AlertDialog.Builder(this).setTitle(isAmharic ? "⚙️ ቅንብሮች" : "⚙️ Settings").setItems(options, (dialog, which) -> {
            if (which == 0) showEditProfileDialog();
            else if (which == 1) {
                if (isNight) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            else if (which == 2) {
                isAmharic = !isAmharic;
                SharedPreferences.Editor editor = getSharedPreferences("UniFixSettings", MODE_PRIVATE).edit();
                editor.putBoolean("isAmharic", isAmharic);
                editor.apply();
                recreate();
            }
            else if (which == 3) confirmLogout();
            else dialog.dismiss();
        }).show();
    }

    private void showEditProfileDialog() {
        db.collection("users").document(loggedInUserName).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(isAmharic ? "የመገለጫ ቅንብሮች ⚙️" : "Edit Profile ⚙️");
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 40, 50, 40);
                layout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));

                TextView tvLockedUser = new TextView(this);
                tvLockedUser.setText((isAmharic ? "የተጠቃሚ ስም: @" : "Username: @") + loggedInUserName + (isAmharic ? " (የተቆለፈ)" : " (Locked)"));
                tvLockedUser.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                tvLockedUser.setPadding(0, 0, 0, 20);
                layout.addView(tvLockedUser);

                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
                p.setMargins(0, 0, 0, 20);

                final EditText etName = new EditText(this);
                etName.setHint(isAmharic ? "ሙሉ ስም" : "Full Name");
                etName.setText(doc.getString("fullName"));
                styleInputBox(etName);
                etName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                etName.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
                etName.setPadding(40, 40, 40, 40);
                etName.setLayoutParams(p);
                layout.addView(etName);

                final EditText etPhoneEdit = new EditText(this);
                etPhoneEdit.setHint(isAmharic ? "ስልክ ቁጥር" : "Phone Number");
                if (doc.getString("phone") != null) etPhoneEdit.setText(doc.getString("phone"));
                etPhoneEdit.setInputType(InputType.TYPE_CLASS_PHONE);
                styleInputBox(etPhoneEdit);
                etPhoneEdit.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                etPhoneEdit.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
                etPhoneEdit.setPadding(40, 40, 40, 40);
                etPhoneEdit.setLayoutParams(p);
                layout.addView(etPhoneEdit);

                final EditText etNewPass = new EditText(this);
                etNewPass.setHint(isAmharic ? "አዲስ የይለፍ ቃል (የአሁኑን ለማቆየት ባዶ ይተዉት)" : "New Password (Leave blank to keep current)");
                styleInputBox(etNewPass);
                etNewPass.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                etNewPass.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
                etNewPass.setPadding(40, 40, 40, 40);
                etNewPass.setLayoutParams(p);
                layout.addView(etNewPass);

                builder.setView(layout);
                builder.setPositiveButton(isAmharic ? "ለውጦችን አስቀምጥ" : "Save Changes", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhoneEdit.getText().toString().trim();
                    String pass = etNewPass.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, isAmharic ? "ስም ባዶ መሆን አይችልም።" : "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!phone.isEmpty() && !phone.matches("^((09|07)\\d{8})|(\\+251(9|7)\\d{8})$")) {
                        Toast.makeText(this, isAmharic ? "ስልክ ቁጥሩ ትክክል አይደለም። (09.., 07.., +2519.., ወይንም +2517.. ይጠቀሙ)" : "Invalid phone. Use 09.., 07.., +2519.., or +2517.. (8 digits)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fullName", name);
                    if (!phone.isEmpty()) updates.put("phone", phone);

                    if (!pass.isEmpty() && pass.length() >= 6) {
                        updates.put("password", pass);
                    }

                    db.collection("users").document(loggedInUserName).update(updates)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, isAmharic ? "የመገለጫ መረጃ ዘምኗል! ✅" : "Profile Info Updated! ✅", Toast.LENGTH_LONG).show();
                                String shortName = name;
                                if (shortName.contains(" ")) shortName = shortName.split(" ")[0];
                                tvStudentName.setText((isAmharic ? "ሰላም, " : "Hi, ") + shortName);
                            });
                });
                builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null);
                builder.show();
            }
        });
    }

    private void setupTabs() {
        btnTabReport.setOnClickListener(v -> switchToTab("report", true));
        btnTabHistory.setOnClickListener(v -> switchToTab("history", true));
        switchToTab("report", false);
    }

    private void switchToTab(String tab, boolean addToHistory) {
        if (currentTab.equals(tab) && (containerReportForm.getVisibility() == View.VISIBLE || containerHistory.getVisibility() == View.VISIBLE)) {
            return;
        }

        if (addToHistory) {
            tabHistory.push(currentTab);
        }
        currentTab = tab;

        Slide slide = new Slide();
        if (tab.equals("history")) {
            slide.setSlideEdge(Gravity.END);
        } else {
            slide.setSlideEdge(Gravity.START);
        }
        slide.setDuration(250);
        TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), slide);

        containerReportForm.setVisibility(View.GONE);
        containerHistory.setVisibility(View.GONE);

        int inactiveBg = ContextCompat.getColor(this, R.color.input_background);
        int inactiveText = ContextCompat.getColor(this, R.color.text_primary);
        btnTabReport.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
        btnTabReport.setTextColor(inactiveText);
        btnTabHistory.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
        btnTabHistory.setTextColor(inactiveText);

        int activeBg = ContextCompat.getColor(this, R.color.unifix_blue);

        if (tab.equals("report")) {
            containerReportForm.setVisibility(View.VISIBLE);
            btnTabReport.setBackgroundTintList(ColorStateList.valueOf(activeBg));
            btnTabReport.setTextColor(Color.WHITE);
        } else if (tab.equals("history")) {
            containerHistory.setVisibility(View.VISIBLE);
            btnTabHistory.setBackgroundTintList(ColorStateList.valueOf(activeBg));
            btnTabHistory.setTextColor(Color.WHITE);
            loadMyHistory();
        }
    }

    private void loadMyHistory() {
        containerHistory.removeAllViews();

        db.collection("reports")
                .whereEqualTo("reporterUsername", loggedInUserName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        containerHistory.removeAllViews();

                        TextView title = new TextView(this);
                        title.setText(isAmharic ? "የእኔ የሪፖርት ታሪክ" : "My Report History");
                        title.setTextSize(18);
                        title.setTextColor(ContextCompat.getColor(this, R.color.unifix_blue));
                        title.setTypeface(null, android.graphics.Typeface.BOLD);
                        title.setPadding(0, 0, 0, 16);
                        containerHistory.addView(title);

                        if (task.getResult().isEmpty()) {
                            TextView empty = new TextView(this);
                            empty.setText(isAmharic ? "ምንም ሪፖርት አልተገኘም።" : "No reports found.");
                            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                            containerHistory.addView(empty);
                            return;
                        }

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("status");
                            Long resolvedTime = document.getLong("resolvedTimestamp");

                            if (("Finished".equals(status) || "Completed".equals(status)) && resolvedTime != null) {
                                long oneDay = 24L * 60L * 60L * 1000L;
                                if (System.currentTimeMillis() - resolvedTime > oneDay) {
                                    continue;
                                }
                            }

                            String rawCat = document.getString("category");
                            String cat = getShortCategoryFromStaffRole(rawCat);
                            String displayCat = getTranslatedCategory(cat);

                            String desc = document.getString("description");
                            String solver = document.getString("assignedTo");

                            LinearLayout card = new LinearLayout(this);
                            card.setOrientation(LinearLayout.VERTICAL);
                            card.setPadding(40, 40, 40, 40);
                            styleFloatingCard(card);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                            params.setMargins(0, 0, 0, 20);
                            card.setLayoutParams(params);

                            String statusDisplay = (isAmharic ? "ሁኔታ: " : "Status: ");
                            if ("Pending".equals(status)) statusDisplay += (isAmharic ? "በመጠባበቅ ላይ 🟡" : "Pending 🟡");
                            else if ("Assigned".equals(status) || "Delegated_Pending".equals(status)) statusDisplay += (isAmharic ? "ተመድቧል 🟠" : "Assigned 🟠");
                            else if ("In Progress".equals(status)) statusDisplay += (isAmharic ? "በሂደት ላይ 🔵" : "In Progress 🔵");
                            else if ("Finished".equals(status) || "Completed".equals(status)) statusDisplay += (isAmharic ? "ተጠናቋል 🟢" : "Finished 🟢");
                            else if ("Appealed".equals(status)) statusDisplay += (isAmharic ? "ይግባኝ ተጠይቋል ⚠️" : "Appealed ⚠️");
                            else if ("Declined".equals(status)) statusDisplay += (isAmharic ? "ውድቅ ተደርጓል 🔴" : "Declined 🔴");

                            TextView tvInfo = new TextView(this);
                            String infoText = (isAmharic ? "ምድብ: " : "Category: ") + displayCat + "\n" + statusDisplay;
                            if (solver != null && !solver.isEmpty() && !"Pending".equals(status)) {
                                infoText += (isAmharic ? "\nየተመደበ ባለሙያ: @" : "\nExpert Assigned: @") + solver;
                            }
                            infoText += (isAmharic ? "\n\nመግለጫ: " : "\n\nDescription: ") + desc;

                            tvInfo.setText(infoText);
                            tvInfo.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                            tvInfo.setPadding(0, 0, 0, 16);
                            card.addView(tvInfo);

                            ImageView ivAttachment = new ImageView(this);
                            String imageUrl = document.getString("imageUrl");
                            boolean hasImage = imageUrl != null && !imageUrl.isEmpty();

                            if (hasImage) {
                                LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(-1, 500);
                                ivParams.setMargins(0, 0, 0, 16);
                                ivAttachment.setLayoutParams(ivParams);
                                ivAttachment.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                ivAttachment.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                                ivAttachment.setClipToOutline(true);
                                GradientDrawable imgShape = new GradientDrawable();
                                imgShape.setCornerRadius(20f);
                                ivAttachment.setBackground(imgShape);

                                Glide.with(this).load(imageUrl).into(ivAttachment);
                                card.addView(ivAttachment);
                            }

                            if (!"Pending".equals(status)) {
                                Button btnGroupChat = new Button(this);
                                btnGroupChat.setText(isAmharic ? "💬 የትኬት ግሩፕ ቻት ክፈት" : "💬 Open Ticket Group Chat");
                                btnGroupChat.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6f42c1")));
                                btnGroupChat.setTextColor(Color.WHITE);
                                LinearLayout.LayoutParams gcParams = new LinearLayout.LayoutParams(-1, -2);
                                gcParams.setMargins(0, 0, 0, 16);
                                btnGroupChat.setLayoutParams(gcParams);
                                makeInteractive(btnGroupChat);
                                btnGroupChat.setOnClickListener(v -> showTicketGroupChat(document.getId(), displayCat));
                                card.addView(btnGroupChat);
                            }

                            if ("Pending".equals(status)) {
                                LinearLayout btnLayout = new LinearLayout(this);
                                btnLayout.setOrientation(LinearLayout.HORIZONTAL);

                                Button btnEdit = new Button(this);
                                btnEdit.setText(isAmharic ? "አስተካክል" : "Edit");
                                btnEdit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.unifix_blue)));
                                btnEdit.setTextColor(Color.WHITE);
                                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, -2, 1);
                                btnParams.setMargins(0, 0, 8, 0);
                                btnEdit.setLayoutParams(btnParams);
                                makeInteractive(btnEdit);
                                btnEdit.setOnClickListener(v -> showEditDialog(document.getId(), desc, btnEdit));

                                Button btnDelete = new Button(this);
                                btnDelete.setText(isAmharic ? "ሰርዝ/አንሳ" : "Withdraw");
                                btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
                                btnDelete.setTextColor(Color.WHITE);
                                LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(0, -2, 1);
                                btnParams2.setMargins(8, 0, 0, 0);
                                btnDelete.setLayoutParams(btnParams2);
                                makeInteractive(btnDelete);
                                btnDelete.setOnClickListener(v -> deleteReport(document.getId(), btnDelete));

                                btnLayout.addView(btnEdit);
                                btnLayout.addView(btnDelete);
                                card.addView(btnLayout);
                            } else {
                                TextView tvLocked = new TextView(this);
                                tvLocked.setText(isAmharic ? "🔒 ተግባሩ ተቆልፏል (በሂደት ላይ ነው)" : "🔒 Task is locked (Action in progress)");
                                tvLocked.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                                tvLocked.setTextSize(12);
                                tvLocked.setPadding(0, 5, 0, 0);
                                tvLocked.setTypeface(null, android.graphics.Typeface.ITALIC);
                                card.addView(tvLocked);
                            }

                            containerHistory.addView(card);
                        }
                    }
                });
    }

    private void showTicketGroupChat(String reportId, String categoryName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView titleView = new TextView(this);
        titleView.setText((isAmharic ? "ግሩፕ ቻት: " : "Group Chat: ") + categoryName);
        titleView.setPadding(40, 40, 40, 40);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.WHITE);
        titleView.setBackgroundColor(Color.parseColor("#6f42c1"));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        builder.setCustomTitle(titleView);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        container.setPadding(20, 20, 20, 20);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 800);
        scrollParams.weight = 1;
        scrollView.setLayoutParams(scrollParams);

        LinearLayout chatLayout = new LinearLayout(this);
        chatLayout.setOrientation(LinearLayout.VERTICAL);
        chatLayout.setPadding(10, 10, 10, 10);
        scrollView.addView(chatLayout);
        container.addView(scrollView);

        LinearLayout replyContainer = new LinearLayout(this);
        replyContainer.setOrientation(LinearLayout.HORIZONTAL);
        replyContainer.setPadding(10, 20, 10, 10);
        replyContainer.setGravity(Gravity.CENTER_VERTICAL);

        EditText etReply = new EditText(this);
        etReply.setHint(isAmharic ? "ለአስተዳዳሪ/ባለሙያ መልዕክት ይጻፉ..." : "Message Admin/Solver...");
        styleInputBox(etReply);
        etReply.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        etReply.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
        etReply.setPadding(40, 30, 40, 30);
        LinearLayout.LayoutParams repParams = new LinearLayout.LayoutParams(0, -2, 1);
        repParams.setMarginEnd(20);
        etReply.setLayoutParams(repParams);

        Button btnSendReply = new Button(this);
        btnSendReply.setText("➤");
        btnSendReply.setTextSize(18);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#6f42c1"));
        btnBg.setShape(GradientDrawable.OVAL);
        btnSendReply.setBackground(btnBg);
        btnSendReply.setTextColor(Color.WHITE);
        btnSendReply.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        makeInteractive(btnSendReply);

        replyContainer.addView(etReply);
        replyContainer.addView(btnSendReply);
        container.addView(replyContainer);

        builder.setView(container);
        builder.setNegativeButton(isAmharic ? "ዝጋ" : "Close", null);
        AlertDialog dialog = builder.create();

        Runnable loadGroupMessages = () -> {
            db.collection("report_chats").whereEqualTo("reportId", reportId).get()
                    .addOnSuccessListener(snaps -> {
                        chatLayout.removeAllViews();
                        List<DocumentSnapshot> msgs = new ArrayList<>(snaps.getDocuments());
                        Collections.sort(msgs, (d1, d2) -> {
                            Long t1 = d1.getLong("timestamp");
                            Long t2 = d2.getLong("timestamp");
                            if(t1==null) t1=0L; if(t2==null) t2=0L;
                            return t1.compareTo(t2);
                        });

                        for (DocumentSnapshot m : msgs) {
                            String msgSender = m.getString("sender");
                            boolean isMe = loggedInUserName.equals(msgSender);

                            LinearLayout bubbleWrapper = new LinearLayout(this);
                            bubbleWrapper.setOrientation(LinearLayout.VERTICAL);
                            bubbleWrapper.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                            bubbleWrapper.setGravity(isMe ? Gravity.END : Gravity.START);

                            if (!isMe) {
                                TextView tvName = new TextView(this);
                                tvName.setText("@" + msgSender);
                                tvName.setTextSize(10);
                                tvName.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                                tvName.setPadding(10,0,0,0);
                                bubbleWrapper.addView(tvName);
                            }

                            TextView bubble = new TextView(this);
                            bubble.setText(m.getString("text"));
                            bubble.setPadding(40, 25, 40, 25);
                            bubble.setTextSize(16);

                            GradientDrawable gd = new GradientDrawable();
                            gd.setCornerRadius(40f);
                            if (isMe) {
                                gd.setColor(Color.parseColor("#6f42c1"));
                                bubble.setTextColor(Color.WHITE);
                            } else {
                                gd.setColor(ContextCompat.getColor(this, R.color.input_background));
                                bubble.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                                bubble.setElevation(2f);
                            }
                            bubble.setBackground(gd);

                            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
                            bp.setMargins(isMe ? 150 : 0, 5, isMe ? 0 : 150, 15);
                            bubble.setLayoutParams(bp);

                            bubbleWrapper.addView(bubble);
                            chatLayout.addView(bubbleWrapper);
                        }
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    });
        };

        loadGroupMessages.run();

        btnSendReply.setOnClickListener(v -> {
            String txt = etReply.getText().toString().trim();
            if (!txt.isEmpty()) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("reportId", reportId);
                msg.put("sender", loggedInUserName);
                msg.put("text", txt);
                msg.put("timestamp", System.currentTimeMillis());

                db.collection("report_chats").add(msg).addOnSuccessListener(a -> {
                    etReply.setText("");
                    loadGroupMessages.run();
                });
            }
        });

        dialog.show();
    }

    private void showEditDialog(String documentId, String currentDesc, Button clickedButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isAmharic ? "የሪፖርት መግለጫን ያስተካክሉ" : "Edit Report Description");

        final EditText input = new EditText(this);
        input.setText(currentDesc);
        styleInputBox(input);
        input.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        input.setPadding(40, 40, 40, 40);
        builder.setView(input);

        builder.setPositiveButton(isAmharic ? "ማስተካከያውን አስቀምጥ" : "Save Update", (dialog, which) -> {
            String newDesc = input.getText().toString().trim();
            if(!newDesc.isEmpty()) {
                clickedButton.setText(isAmharic ? "በማስቀመጥ ላይ..." : "Saving...");
                clickedButton.setEnabled(false);

                db.collection("reports").document(documentId)
                        .update("description", newDesc)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, isAmharic ? "ችግሩ በተሳካ ሁኔታ ተስተካክሏል!" : "Issue updated successfully!", Toast.LENGTH_SHORT).show();
                            loadMyHistory();
                        })
                        .addOnFailureListener(e -> {
                            clickedButton.setText(isAmharic ? "አስተካክል" : "Edit");
                            clickedButton.setEnabled(true);
                            Toast.makeText(this, isAmharic ? "ችግሩን ማስተካከል አልተቻለም።" : "Failed to update issue. Check connection.", Toast.LENGTH_LONG).show();
                        });
            }
        });
        builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void deleteReport(String documentId, Button clickedButton) {
        new AlertDialog.Builder(this)
                .setTitle(isAmharic ? "ችግሩን ሰርዝ" : "Withdraw Issue")
                .setMessage(isAmharic ? "እርግጠኛ ነዎት ይህን ችግር ሙሉ በሙሉ መሰረዝ ይፈልጋሉ? ይህ እርምጃ ሊቀለበስ አይችልም።" : "Are you sure you want to completely withdraw this issue? This cannot be undone.")
                .setPositiveButton(isAmharic ? "አዎ፣ ሰርዝ" : "Yes, Withdraw", (dialog, which) -> {
                    clickedButton.setText(isAmharic ? "በመሰረዝ ላይ..." : "Withdrawing...");
                    clickedButton.setEnabled(false);

                    db.collection("reports").document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, isAmharic ? "ችግሩ ተሰርዟል።" : "Issue withdrawn.", Toast.LENGTH_SHORT).show();
                                loadMyHistory();
                            })
                            .addOnFailureListener(e -> {
                                clickedButton.setText(isAmharic ? "ሰርዝ/አንሳ" : "Withdraw");
                                clickedButton.setEnabled(true);
                                Toast.makeText(this, isAmharic ? "ችግሩን መሰረዝ አልተቻለም።" : "Failed to withdraw issue. Check connection.", Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null)
                .show();
    }

    private String getStaffRoleFromCategory(String shortCategory) {
        if (shortCategory == null) return "Other";
        switch (shortCategory) {
            case "ICT / Technology": return "Staff ICT Manager";
            case "Dormitory": return "Staff Dormitory Manager";
            case "Academic Resources": return "Staff Academic Resources Manager";
            case "Cafeteria": return "Staff Cafeteria Manager";
            case "Human Resources": return "Staff Human Resource Manager";
            case "Health Center": return "Staff Health Center Manager";
            case "Campus Security": return "Staff Campus Security Manager";
            case "Finance": return "Staff Finance Manager";
            case "Administration": return "Staff University Administration Manager";
            case "Department Head": return "Staff Department Head";
            default: return "Other";
        }
    }

    private String getShortCategoryFromStaffRole(String staffRole) {
        if (staffRole == null) return "Other";
        switch (staffRole) {
            case "Staff ICT Manager": return "ICT / Technology";
            case "Staff Dormitory Manager": return "Dormitory";
            case "Staff Academic Resources Manager": return "Academic Resources";
            case "Staff Cafeteria Manager": return "Cafeteria";
            case "Staff Human Resource Manager": return "Human Resources";
            case "Staff Health Center Manager": return "Health Center";
            case "Staff Campus Security Manager": return "Campus Security";
            case "Staff Finance Manager": return "Finance";
            case "Staff University Administration Manager": return "Administration";
            case "Staff Department Head": return "Department Head";
            default: return "Other";
        }
    }

    private void setupSpinners() {
        String[] categories;
        if(isAmharic) {
            categories = new String[]{
                    "ይምረጡ...", "አይሲቲ / ቴክኖሎጂ", "መኝታ ክፍል", "አካዳሚክ ግብዓቶች", "ካፌ",
                    "የሰው ኃይል", "ጤና ጣቢያ", "የካምፓስ ደህንነት", "ፋይናንስ", "አስተዳደር", "የትምህርት ክፍል ኃላፊ", "ሌላ"
            };
        } else {
            categories = new String[]{
                    "Select...", "ICT / Technology", "Dormitory", "Academic Resources", "Cafeteria",
                    "Human Resources", "Health Center", "Campus Security", "Finance", "Administration", "Department Head", "Other"
            };
        }

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);

        String[] urgencies;
        if(isAmharic) {
            urgencies = new String[]{"ዝቅተኛ", "መካከለኛ", "አስቸኳይ", "በጣም አስቸኳይ"};
        } else {
            urgencies = new String[]{"Low", "Medium", "High", "Urgent"};
        }
        ArrayAdapter<String> urgAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, urgencies);
        spinnerUrgency.setAdapter(urgAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = categories[position];

                if (dynamicDormitory != null) dynamicDormitory.setVisibility(View.GONE);
                if (dynamicIct != null) dynamicIct.setVisibility(View.GONE);

                if (selected.equals("Dormitory") || selected.equals("መኝታ ክፍል")) {
                    if (dynamicDormitory != null) dynamicDormitory.setVisibility(View.VISIBLE);
                } else if (selected.equals("ICT / Technology") || selected.equals("አይሲቲ / ቴክኖሎጂ")) {
                    if (dynamicIct != null) dynamicIct.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (spinnerIctType != null) {
            String[] ictTypes;
            if(isAmharic) {
                ictTypes = new String[]{"የቦታ ዓይነት ይምረጡ...", "የኮምፒውተር ቤተ-ሙከራ (Lab)", "ሌላ ቦታ / ቢሮ"};
            } else {
                ictTypes = new String[]{"Select Location Type...", "Computer Lab", "Other Place / Office"};
            }
            ArrayAdapter<String> ictTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ictTypes);
            spinnerIctType.setAdapter(ictTypeAdapter);

            spinnerIctType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 1) { // Computer Lab
                        if (dynamicIctLab != null) dynamicIctLab.setVisibility(View.VISIBLE);
                        if (etIctPlaceName != null) etIctPlaceName.setVisibility(View.GONE);
                    } else if (position == 2) { // Other Place
                        if (dynamicIctLab != null) dynamicIctLab.setVisibility(View.GONE);
                        if (etIctPlaceName != null) etIctPlaceName.setVisibility(View.VISIBLE);
                    } else {
                        if (dynamicIctLab != null) dynamicIctLab.setVisibility(View.GONE);
                        if (etIctPlaceName != null) etIctPlaceName.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void submitReport() {
        String uiCategory = spinnerCategory.getSelectedItem().toString();
        String uiUrgency = spinnerUrgency.getSelectedItem().toString();
        String phone = etPhone.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (uiCategory.equals("Select...") || uiCategory.equals("ይምረጡ...")) {
            Toast.makeText(this, msgReqCategory, Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, msgReqFields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("^((09|07)\\d{8})|(\\+251(9|7)\\d{8})$")) {
            Toast.makeText(this, isAmharic ? "ስልክ ቁጥሩ ትክክል አይደለም። (09.., 07.., +2519.., ወይንም +2517.. ይጠቀሙ)" : "Invalid phone. Use 09.., 07.., +2519.., or +2517.. (8 digits)", Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmitReport.setText(msgProcessing);
        btnSubmitReport.setEnabled(false);

        String currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());

        String dbCategory = getStaffRoleFromCategory(getEnglishCategory(uiCategory));

        String dbUrgency = uiUrgency;
        if(isAmharic) {
            if(uiUrgency.equals("በጣም አስቸኳይ")) dbUrgency = "Urgent";
            else if(uiUrgency.equals("አስቸኳይ")) dbUrgency = "High";
            else if(uiUrgency.equals("መካከለኛ")) dbUrgency = "Medium";
            else if(uiUrgency.equals("ዝቅተኛ")) dbUrgency = "Low";
        }

        final String finalDbUrgency = dbUrgency;

        db.collection("users").document(loggedInUserName).get().addOnSuccessListener(userDoc -> {

            if (userDoc.exists() && userDoc.getString("phone") == null) {
                db.collection("users").document(loggedInUserName).update("phone", phone);
            }

            db.collection("reports")
                    .whereEqualTo("reporterUsername", loggedInUserName)
                    .whereEqualTo("date", currentDate)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.size() >= 3) {
                            Toast.makeText(this, isAmharic ? "ዕለታዊ ገደብ ላይ ደርሰዋል! በቀን 3 ሪፖርቶችን ብቻ ማስገባት ይችላሉ።" : "Daily Limit Reached! You can only submit 3 reports per day.", Toast.LENGTH_LONG).show();
                            resetSubmitButton();
                            return;
                        }

                        if (selectedImageUri != null) {
                            btnSubmitReport.setText(msgUploading);
                            StorageReference imageRef = storage.getReference().child("report_images/" + UUID.randomUUID().toString() + ".jpg");

                            imageRef.putFile(selectedImageUri)
                                    .addOnSuccessListener(taskSnapshot -> {
                                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                            saveReportToFirestore(dbCategory, finalDbUrgency, phone, description, currentDate, uri.toString());
                                        }).addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error getting image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            saveReportToFirestore(dbCategory, finalDbUrgency, phone, description, currentDate, null);
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Storage Blocked Upload: " + e.getMessage() + "\nCheck Firebase Rules!", Toast.LENGTH_LONG).show();
                                        saveReportToFirestore(dbCategory, finalDbUrgency, phone, description, currentDate, null);
                                    });
                        } else {
                            saveReportToFirestore(dbCategory, finalDbUrgency, phone, description, currentDate, null);
                        }

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, isAmharic ? "ዕለታዊ ገደቡን ማረጋገጥ አልተቻለም። የኔትወርክ ስህተት።" : "Network error verifying daily limit. Try again.", Toast.LENGTH_LONG).show();
                        resetSubmitButton();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, isAmharic ? "የተጠቃሚ መገለጫውን ማረጋገጥ አልተቻለም።" : "Network error verifying user profile. Try again.", Toast.LENGTH_LONG).show();
            resetSubmitButton();
        });
    }

    private void saveReportToFirestore(String dbCategory, String urgency, String phone, String description, String date, String imageUrl) {
        btnSubmitReport.setText(msgSaving);

        Map<String, Object> report = new HashMap<>();
        report.put("category", dbCategory);
        report.put("urgency", urgency);
        report.put("reporterPhone", phone);
        report.put("description", description);
        report.put("date", date);
        report.put("reporterRole", "Student");
        report.put("reporterUsername", loggedInUserName);

        String nameValue = tvStudentName.getText().toString();
        if(isAmharic) nameValue = nameValue.replace("ሰላም, ", "");
        else nameValue = nameValue.replace("Hi, ", "");
        report.put("reporterFullName", nameValue);
        report.put("status", "Pending");

        if (imageUrl != null) {
            report.put("imageUrl", imageUrl);
        }

        String[] admins = {"dbu_admin1", "dbu_admin2", "dbu_admin3", "dbu_admin4"};
        int randomIndex = new Random().nextInt(admins.length);
        String assignedAdmin = admins[randomIndex];
        report.put("assignedByAdmin", assignedAdmin);

        Map<String, String> specificDetails = new HashMap<>();
        if (dbCategory.equals("Staff Dormitory Manager")) {
            if (etDormBlock != null) specificDetails.put("block", etDormBlock.getText().toString().trim());
            if (etDormRoom != null) specificDetails.put("room", etDormRoom.getText().toString().trim());
        } else if (dbCategory.equals("Staff ICT Manager") && spinnerIctType != null) {
            String ictType = spinnerIctType.getSelectedItem().toString();
            if(ictType.equals("የኮምፒውተር ቤተ-ሙከራ (Lab)")) ictType = "Computer Lab";
            else if(ictType.equals("ሌላ ቦታ / ቢሮ")) ictType = "Other Place / Office";

            specificDetails.put("locationType", ictType);

            if (ictType.equals("Computer Lab")) {
                if (etIctBuilding != null) specificDetails.put("building", etIctBuilding.getText().toString().trim());
                if (etIctRoom != null) specificDetails.put("room", etIctRoom.getText().toString().trim());
            } else if (ictType.equals("Other Place / Office")) {
                if (etIctPlaceName != null) specificDetails.put("placeName", etIctPlaceName.getText().toString().trim());
            }
        }

        if (!specificDetails.isEmpty()) {
            report.put("specificDetails", specificDetails);
        }

        db.collection("reports").add(report)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(StudentDashboardActivity.this, msgReportSubmitted, Toast.LENGTH_LONG).show();

                    String uiCategory = getShortCategoryFromStaffRole(dbCategory);
                    sendInAppNotification(assignedAdmin, "New Ticket Submitted", "A new " + urgency + " " + uiCategory + " issue was just reported by @" + loggedInUserName + ". Please review and approve it.");

                    resetForm();
                    switchToTab("history", true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentDashboardActivity.this, isAmharic ? "ሪፖርት ማስገባት አልተቻለም። ግንኙነትዎን ያረጋግጡ።" : "Failed to submit report. Check connection.", Toast.LENGTH_LONG).show();
                    resetSubmitButton();
                });
    }

    private void resetSubmitButton() {
        btnSubmitReport.setText(msgSubmitBtn);
        btnSubmitReport.setEnabled(true);
    }

    private void resetForm() {
        spinnerCategory.setSelection(0);
        spinnerUrgency.setSelection(0);
        etPhone.setText("");
        etDescription.setText("");

        if (etDormBlock != null) etDormBlock.setText("");
        if (etDormRoom != null) etDormRoom.setText("");
        if (spinnerIctType != null) spinnerIctType.setSelection(0);
        if (etIctBuilding != null) etIctBuilding.setText("");
        if (etIctRoom != null) etIctRoom.setText("");
        if (etIctPlaceName != null) etIctPlaceName.setText("");

        selectedImageUri = null;
        ivPhotoPreview.setVisibility(View.GONE);
        btnAttachPhoto.setText(msgAttachPhoto);

        resetSubmitButton();
    }

    private void sendInAppNotification(String targetUsername, String subject, String messageBody) {
        Map<String, Object> inAppMsg = new HashMap<>();
        inAppMsg.put("sender", "System Alerts");
        inAppMsg.put("recipient", targetUsername);
        inAppMsg.put("text", "🔔 " + subject + "\n\n" + messageBody);
        inAppMsg.put("timestamp", System.currentTimeMillis());
        inAppMsg.put("status", "Unread");

        db.collection("users").document(targetUsername).get().addOnSuccessListener(targetDoc -> {
            if (targetDoc.exists()) {
                String role = targetDoc.getString("role");

                if ("Admin".equals(role)) {
                    db.collection("admin_messages").add(inAppMsg);
                } else if ("Solver".equals(role)) {
                    db.collection("solver_inbox").add(inAppMsg);
                } else {
                    db.collection("user_messages").add(inAppMsg);
                }
            }
        });
    }
}