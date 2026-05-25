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

public class TeacherDashboardActivity extends AppCompatActivity {

    boolean isAmharic = false;

    TextView tvTeacherName, warningBanner;
    Spinner spinnerCategory, spinnerUrgency;
    EditText etPhone, etDescription;
    Button btnSubmitReport, btnSettings, btnTabReport, btnTabHistory, btnAttachPhoto;
    ImageView ivPhotoPreview;
    LinearLayout containerReportForm, containerHistory;

    LinearLayout dynamicMaterials, dynamicClassroom, dynamicOffice, dynamicTech;
    EditText etMatCourse, etMatRoom, etClassRoom, etClassBatch, etOffBlock, etOffNum;

    LinearLayout dynamicIctLab;
    Spinner spinnerIctType;
    EditText etTechDevice, etTechLoc, etIctBuilding, etIctRoom, etIctPlaceName;

    TextView tvGlobalAlertBadge;
    List<DocumentSnapshot> activeSystemAlerts = new ArrayList<>();

    String loggedInUserName = "Unknown Teacher";
    String teacherFullName = "Unknown Teacher";
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
    String msgReqFields = "Please fill all required fields";
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
        setContentView(R.layout.activity_teacher_dashboard);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);

        tvTeacherName = findViewById(R.id.tvTeacherName);
        warningBanner = findViewById(R.id.warningBanner);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUrgency = findViewById(R.id.spinnerUrgency);
        etPhone = findViewById(R.id.etPhone);
        etDescription = findViewById(R.id.etDescription);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        btnSettings = findViewById(R.id.btnSettings);

        btnAttachPhoto = findViewById(R.id.btnAttachPhoto);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);

        btnTabReport = findViewById(R.id.btnTabReport);
        btnTabHistory = findViewById(R.id.btnTabHistory);
        containerReportForm = findViewById(R.id.containerReportForm);
        containerHistory = findViewById(R.id.containerHistory);

        dynamicMaterials = findViewById(R.id.dynamicMaterials);
        dynamicClassroom = findViewById(R.id.dynamicClassroom);
        dynamicOffice = findViewById(R.id.dynamicOffice);
        dynamicTech = findViewById(R.id.dynamicTech);

        etMatCourse = findViewById(R.id.etMatCourse);
        etMatRoom = findViewById(R.id.etMatRoom);
        etClassRoom = findViewById(R.id.etClassRoom);
        etClassBatch = findViewById(R.id.etClassBatch);
        etOffBlock = findViewById(R.id.etOffBlock);
        etOffNum = findViewById(R.id.etOffNum);

        dynamicIctLab = findViewById(R.id.dynamicIctLab);
        spinnerIctType = findViewById(R.id.spinnerIctType);
        etTechDevice = findViewById(R.id.etTechDevice);
        etTechLoc = findViewById(R.id.etTechLoc);
        etIctBuilding = findViewById(R.id.etIctBuilding);
        etIctRoom = findViewById(R.id.etIctRoom);
        etIctPlaceName = findViewById(R.id.etIctPlaceName);

        styleInputBox(spinnerCategory);
        styleInputBox(spinnerUrgency);
        styleInputBox(etPhone);
        styleInputBox(etDescription);
        styleInputBox(etMatCourse);
        styleInputBox(etMatRoom);
        styleInputBox(etClassRoom);
        styleInputBox(etClassBatch);
        styleInputBox(etOffBlock);
        styleInputBox(etOffNum);
        styleInputBox(etTechDevice);
        styleInputBox(etTechLoc);
        styleInputBox(spinnerIctType);
        styleInputBox(etIctBuilding);
        styleInputBox(etIctRoom);
        styleInputBox(etIctPlaceName);

        makeInteractive(btnSubmitReport);
        makeInteractive(btnSettings);
        makeInteractive(btnAttachPhoto);
        makeInteractive(btnTabReport);
        makeInteractive(btnTabHistory);

        String userId = getIntent().getStringExtra("USERNAME");
        String firstName = getIntent().getStringExtra("FULL_NAME");

        if (userId != null) {
            loggedInUserName = userId;
        }

        if (firstName != null && !firstName.isEmpty()) {
            teacherFullName = firstName;
        }

        String shortName = teacherFullName;
        if (shortName.contains(" ")) shortName = shortName.split(" ")[0];
        tvTeacherName.setText((isAmharic ? "ሰላም, " : "Hi, ") + shortName);

        setupGlobalAlertBell();

        applyStaticTranslations();
        setupSpinners();
        setupTabs();
        fetchTeacherStrikes();

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

    private String getTranslatedCategory(String englishCat) {
        if (!isAmharic) return englishCat;
        if (englishCat == null) return "ሌላ";
        switch (englishCat) {
            case "ICT / Technology": return "አይሲቲ / ቴክኖሎጂ";
            case "Academic Resources": return "አካዳሚክ ግብዓቶች";
            case "Human Resources": return "የሰው ኃይል";
            case "Health Center": return "ጤና ጣቢያ";
            case "Campus Security": return "የካምፓስ ደህንነት";
            case "Finance": return "ፋይናንስ";
            case "Administration": return "አስተዳደር";
            case "Department Head": return "የትምህርት ክፍል ኃላፊ";
            case "General Technician": return "አጠቃላይ ቴክኒሻን";
            case "Dean": return "ዲን";
            case "Select...": return "ይምረጡ...";
            default: return "ሌላ";
        }
    }

    private String getEnglishCategory(String amharicCat) {
        if (!isAmharic) return amharicCat;
        switch (amharicCat) {
            case "አይሲቲ / ቴክኖሎጂ": return "ICT / Technology";
            case "አካዳሚክ ግብዓቶች": return "Academic Resources";
            case "የሰው ኃይል": return "Human Resources";
            case "ጤና ጣቢያ": return "Health Center";
            case "የካምፓስ ደህንነት": return "Campus Security";
            case "ፋይናንስ": return "Finance";
            case "አስተዳደር": return "Administration";
            case "የትምህርት ክፍል ኃላፊ": return "Department Head";
            case "አጠቃላይ ቴክኒሻን": return "General Technician";
            case "ዲን": return "Dean";
            case "ይምረጡ...": return "Select...";
            default: return "Other";
        }
    }

    private void applyStaticTranslations() {
        if (!isAmharic) return;

        TextView tvNavTitle = findViewById(R.id.navTitle);
        if(tvNavTitle != null) tvNavTitle.setText("UniFix መምህር");

        btnTabReport.setText("ሪፖርት አድርግ");
        btnTabHistory.setText("የእኔ ታሪክ");

        TextView tvFormTitle = findViewById(R.id.tvFormTitle);
        if(tvFormTitle != null) tvFormTitle.setText("አዲስ ሪፖርት ያስገቡ");

        TextView tvLabelCategory = findViewById(R.id.tvLabelCategory);
        if(tvLabelCategory != null) tvLabelCategory.setText("ምድብ:");

        TextView tvLabelPhone = findViewById(R.id.tvLabelPhone);
        if(tvLabelPhone != null) tvLabelPhone.setText("ስልክ ቁጥር (ግዴታ):");
        etPhone.setHint("ለምሳሌ 09... / 07.../+251");

        TextView tvLabelMaterials = findViewById(R.id.tvLabelMaterials);
        if(tvLabelMaterials != null) tvLabelMaterials.setText("የግብዓት ዝርዝሮች:");
        etMatCourse.setHint("የኮርስ ኮድ/ስም");
        etMatRoom.setHint("የሚፈለግበት ክፍል");

        TextView tvLabelClassroom = findViewById(R.id.tvLabelClassroom);
        if(tvLabelClassroom != null) tvLabelClassroom.setText("የመማሪያ ክፍል ዝርዝሮች:");
        etClassRoom.setHint("ብሎክ እና ክፍል ቁጥር");
        etClassBatch.setHint("ባች/ዓመት");

        TextView tvLabelOffice = findViewById(R.id.tvLabelOffice);
        if(tvLabelOffice != null) tvLabelOffice.setText("የቢሮ ዝርዝሮች:");
        etOffBlock.setHint("ብሎክ ቁጥር");
        etOffNum.setHint("ቢሮ ቁጥር");

        TextView tvLabelIct = findViewById(R.id.tvLabelIct);
        if(tvLabelIct != null) tvLabelIct.setText("አይሲቲ / ቴክኖሎጂ ዝርዝሮች:");
        etTechDevice.setHint("የመሳሪያ ዓይነት (ለምሳሌ ፕሮጀክተር)");
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
        msgReqFields = "እባክዎ ሁሉንም አስፈላጊ መስኮች ይሙሉ";
        msgReqCategory = "እባክዎ ምድብ ይምረጡ";
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
                                teacherFullName = name;
                                String shortName = name;
                                if (shortName.contains(" ")) shortName = shortName.split(" ")[0];
                                tvTeacherName.setText((isAmharic ? "ሰላም, " : "Hi, ") + shortName);
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
            fetchHistory();
        }
    }

    private void fetchTeacherStrikes() {
        db.collection("users")
                .whereEqualTo("username", loggedInUserName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long warnings = doc.getLong("warnings");
                        if (warnings != null && warnings > 0) {
                            warningBanner.setVisibility(View.VISIBLE);
                            warningBanner.setText(isAmharic ? "ማስጠንቀቂያ፡ " + warnings + " ማስጠንቀቂያ አሎት። 3 ሲደርስ መለያዎ ይታገዳል።" : "Warning: You have " + warnings + " strike(s). Reaching 3 results in a ban.");
                        }
                    }
                });
    }

    private String getStaffRoleFromCategory(String shortCategory) {
        if (shortCategory == null) return "Other";
        switch (shortCategory) {
            case "ICT / Technology": return "Staff ICT Manager";
            case "Academic Resources": return "Staff Academic Resources Manager";
            case "Human Resources": return "Staff Human Resource Manager";
            case "Health Center": return "Staff Health Center Manager";
            case "Campus Security": return "Staff Campus Security Manager";
            case "Finance": return "Staff Finance Manager";
            case "Administration": return "Staff University Administration Manager";
            case "Department Head": return "Staff Department Head";
            case "General Technician": return "Staff General Technician";
            case "Dean": return "Staff Dean";
            default: return "Other";
        }
    }

    private String getShortCategoryFromStaffRole(String staffRole) {
        if (staffRole == null) return "Other";
        switch (staffRole) {
            case "Staff ICT Manager": return "ICT / Technology";
            case "Staff Academic Resources Manager": return "Academic Resources";
            case "Staff Human Resource Manager": return "Human Resources";
            case "Staff Health Center Manager": return "Health Center";
            case "Staff Campus Security Manager": return "Campus Security";
            case "Staff Finance Manager": return "Finance";
            case "Staff University Administration Manager": return "Administration";
            case "Staff Department Head": return "Department Head";
            case "Staff General Technician": return "General Technician";
            case "Staff Dean": return "Dean";
            default: return "Other";
        }
    }

    private void setupSpinners() {
        String[] categories;
        if(isAmharic) {
            categories = new String[]{
                    "ይምረጡ...", "አይሲቲ / ቴክኖሎጂ", "አካዳሚክ ግብዓቶች", "የሰው ኃይል", "ጤና ጣቢያ",
                    "የካምፓስ ደህንነት", "ፋይናንስ", "አስተዳደር", "የትምህርት ክፍል ኃላፊ", "አጠቃላይ ቴክኒሻን", "ዲን", "ሌላ"
            };
        } else {
            categories = new String[]{
                    "Select...", "ICT / Technology", "Academic Resources", "Human Resources", "Health Center",
                    "Campus Security", "Finance", "Administration", "Department Head", "General Technician", "Dean", "Other"
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

                if (dynamicMaterials != null) dynamicMaterials.setVisibility(View.GONE);
                if (dynamicClassroom != null) dynamicClassroom.setVisibility(View.GONE);
                if (dynamicOffice != null) dynamicOffice.setVisibility(View.GONE);
                if (dynamicTech != null) dynamicTech.setVisibility(View.GONE);

                if (selected.equals("Academic Resources") || selected.equals("አካዳሚክ ግብዓቶች")) {
                    if (dynamicMaterials != null) dynamicMaterials.setVisibility(View.VISIBLE);
                } else if (selected.equals("Department Head") || selected.equals("የትምህርት ክፍል ኃላፊ") || selected.equals("Dean") || selected.equals("ዲን")) {
                    if (dynamicClassroom != null) dynamicClassroom.setVisibility(View.VISIBLE);
                } else if (selected.equals("Human Resources") || selected.equals("የሰው ኃይል") || selected.equals("Administration") || selected.equals("አስተዳደር")) {
                    if (dynamicOffice != null) dynamicOffice.setVisibility(View.VISIBLE);
                } else if (selected.equals("ICT / Technology") || selected.equals("አይሲቲ / ቴክኖሎጂ")) {
                    if (dynamicTech != null) dynamicTech.setVisibility(View.VISIBLE);
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
                                            saveReportToFirestore(dbCategory, finalDbUrgency, phone, description, uri.toString(), currentDate);
                                        }).addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error getting image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            saveReportToFirestore(dbCategory, finalDbUrgency, phone, description, null, currentDate);
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Storage Blocked Upload: " + e.getMessage() + "\nCheck Firebase Rules!", Toast.LENGTH_LONG).show();
                                        saveReportToFirestore(dbCategory, finalDbUrgency, phone, description, null, currentDate);
                                    });
                        } else {
                            saveReportToFirestore(dbCategory, finalDbUrgency, phone, description, null, currentDate);
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

    private void saveReportToFirestore(String dbCategory, String urgency, String phone, String description, String imageUrl, String currentDate) {
        btnSubmitReport.setText(msgSaving);

        Map<String, Object> report = new HashMap<>();
        report.put("category", dbCategory);
        report.put("urgency", urgency);
        report.put("reporterPhone", phone);
        report.put("description", description);
        report.put("date", currentDate);
        report.put("reporterRole", "Teacher");
        report.put("reporterUsername", loggedInUserName);
        report.put("reporterFullName", teacherFullName);
        report.put("status", "Pending");

        if (imageUrl != null) {
            report.put("imageUrl", imageUrl);
        }

        String[] admins = {"dbu_admin1", "dbu_admin2", "dbu_admin3", "dbu_admin4"};
        int randomIndex = new Random().nextInt(admins.length);
        String assignedAdmin = admins[randomIndex];
        report.put("assignedByAdmin", assignedAdmin);

        Map<String, String> details = new HashMap<>();
        if (dbCategory.equals("Staff Academic Resources Manager")) {
            if (etMatCourse != null) details.put("course", etMatCourse.getText().toString().trim());
            if (etMatRoom != null) details.put("room", etMatRoom.getText().toString().trim());
        } else if (dbCategory.equals("Staff Department Head") || dbCategory.equals("Staff Dean")) {
            if (etClassRoom != null) details.put("room", etClassRoom.getText().toString().trim());
            if (etClassBatch != null) details.put("batch", etClassBatch.getText().toString().trim());
        } else if (dbCategory.equals("Staff Human Resource Manager") || dbCategory.equals("Staff University Administration Manager")) {
            if (etOffBlock != null) details.put("block", etOffBlock.getText().toString().trim());
            if (etOffNum != null) details.put("office", etOffNum.getText().toString().trim());
        } else if (dbCategory.equals("Staff ICT Manager")) {
            if (etTechDevice != null) details.put("device", etTechDevice.getText().toString().trim());

            if (spinnerIctType != null) {
                String ictType = spinnerIctType.getSelectedItem().toString();
                if(ictType.equals("የኮምፒውተር ቤተ-ሙከራ (Lab)")) ictType = "Computer Lab";
                else if(ictType.equals("ሌላ ቦታ / ቢሮ")) ictType = "Other Place / Office";

                details.put("locationType", ictType);

                if (ictType.equals("Computer Lab")) {
                    if (etIctBuilding != null) details.put("building", etIctBuilding.getText().toString().trim());
                    if (etIctRoom != null) details.put("room", etIctRoom.getText().toString().trim());
                } else if (ictType.equals("Other Place / Office")) {
                    if (etIctPlaceName != null) details.put("placeName", etIctPlaceName.getText().toString().trim());
                }
            } else {
                if (etTechLoc != null) details.put("location", etTechLoc.getText().toString().trim());
            }
        }

        if(!details.isEmpty()) {
            report.put("specificDetails", details);
        }

        db.collection("reports").add(report)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, msgReportSubmitted, Toast.LENGTH_LONG).show();

                    String uiCategory = getShortCategoryFromStaffRole(dbCategory);
                    sendInAppNotification(assignedAdmin, "New Ticket Submitted", "A new " + urgency + " " + uiCategory + " issue was just reported by @" + loggedInUserName + ". Please review and approve it.");

                    resetForm();
                    switchToTab("history", true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, isAmharic ? "ሪፖርት ማስገባት አልተቻለም። ግንኙነትዎን ያረጋግጡ።" : "Failed to submit. Check connection.", Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }

    private void resetSubmitButton() {
        btnSubmitReport.setEnabled(true);
        btnSubmitReport.setText(msgSubmitBtn);
    }

    private void resetForm() {
        spinnerCategory.setSelection(0);
        spinnerUrgency.setSelection(0);
        etPhone.setText("");
        etDescription.setText("");

        if (etMatCourse != null) etMatCourse.setText("");
        if (etMatRoom != null) etMatRoom.setText("");
        if (etClassRoom != null) etClassRoom.setText("");
        if (etClassBatch != null) etClassBatch.setText("");
        if (etOffBlock != null) etOffBlock.setText("");
        if (etOffNum != null) etOffNum.setText("");
        if (etTechDevice != null) etTechDevice.setText("");
        if (etTechLoc != null) etTechLoc.setText("");
        if (spinnerIctType != null) spinnerIctType.setSelection(0);
        if (etIctBuilding != null) etIctBuilding.setText("");
        if (etIctRoom != null) etIctRoom.setText("");
        if (etIctPlaceName != null) etIctPlaceName.setText("");

        selectedImageUri = null;
        ivPhotoPreview.setVisibility(View.GONE);
        btnAttachPhoto.setText(msgAttachPhoto);

        resetSubmitButton();
    }

    private void fetchHistory() {
        containerHistory.removeAllViews();

        TextView title = new TextView(this);
        title.setText(isAmharic ? "የእኔ የሪፖርት ታሪክ" : "My Report History");
        title.setTextSize(18);
        title.setTextColor(ContextCompat.getColor(this, R.color.unifix_blue));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 24);
        containerHistory.addView(title);

        long currentTime = System.currentTimeMillis();

        db.collection("reports")
                .whereEqualTo("reporterUsername", loggedInUserName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            TextView empty = new TextView(this);
                            empty.setText(isAmharic ? "ምንም ሪፖርት አልተገኘም።" : "No reports found.");
                            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                            containerHistory.addView(empty);
                            return;
                        }

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("status");
                            String rawCat = document.getString("category");
                            String desc = document.getString("description");
                            String solver = document.getString("assignedTo");
                            String urgency = document.getString("urgency");
                            Long assignedTime = document.getLong("assignedTimestamp");
                            Long resolvedTime = document.getLong("resolvedTimestamp");

                            if (("Finished".equals(status) || "Completed".equals(status)) && resolvedTime != null) {
                                if (System.currentTimeMillis() - resolvedTime > (24L * 60L * 60L * 1000L)) continue;
                            }

                            String uiCat = getShortCategoryFromStaffRole(rawCat);
                            String displayCat = getTranslatedCategory(uiCat);

                            LinearLayout card = new LinearLayout(this);
                            card.setOrientation(LinearLayout.VERTICAL);
                            card.setPadding(40, 40, 40, 40);
                            styleFloatingCard(card);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, 0, 0, 25);
                            card.setLayoutParams(params);

                            TextView tvHeader = new TextView(this);
                            tvHeader.setText((isAmharic ? "ምድብ: " : "Category: ") + displayCat);
                            tvHeader.setTextColor(ContextCompat.getColor(this, R.color.unifix_blue));
                            tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                            card.addView(tvHeader);

                            String statusDisplay = (isAmharic ? "ሁኔታ: " : "Status: ");
                            if ("Pending".equals(status) || "Assigned".equals(status)) statusDisplay += (isAmharic ? "በመጠባበቅ ላይ 🟡" : "Pending 🟡");
                            else if ("In Progress".equals(status)) statusDisplay += (isAmharic ? "በሂደት ላይ 🔵" : "In Progress 🔵");
                            else if ("Finished".equals(status) || "Completed".equals(status)) statusDisplay += (isAmharic ? "ተጠናቋል 🟢" : "Finished 🟢");
                            else if ("Declined".equals(status)) statusDisplay += (isAmharic ? "ውድቅ ተደርጓል 🔴" : "Declined 🔴");

                            TextView tvStatus = new TextView(this);
                            tvStatus.setText(statusDisplay);
                            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                            card.addView(tvStatus);

                            if (assignedTime != null && ("Assigned".equals(status) || "In Progress".equals(status))) {
                                long duration = 48L * 60 * 60 * 1000;
                                if ("Urgent".equals(urgency)) duration = 1L * 60 * 60 * 1000;
                                else if ("High".equals(urgency)) duration = 6L * 60 * 60 * 1000;
                                else if ("Medium".equals(urgency)) duration = 12L * 60 * 60 * 1000;
                                else if ("Low".equals(urgency)) duration = 24L * 60 * 60 * 1000;

                                long timeRemaining = (assignedTime + duration) - currentTime;
                                TextView tvDeadline = new TextView(this);
                                tvDeadline.setPadding(0, 5, 0, 10);

                                if (timeRemaining > 0) {
                                    long hours = timeRemaining / (60 * 60 * 1000);
                                    tvDeadline.setText((isAmharic ? "ቀሪ ጊዜ: " : "Deadline: ") + hours + (isAmharic ? " ሰዓታት ቀርተዋል" : " hours left"));
                                    tvDeadline.setTextColor(Color.parseColor("#fd7e14"));
                                } else {
                                    tvDeadline.setText(isAmharic ? "⚠️ ጊዜው አልፏል (በድጋሚ ይመደባል)" : "⚠️ OVERDUE (Pending Reassignment)");
                                    tvDeadline.setTextColor(Color.parseColor("#dc3545"));
                                    tvDeadline.setTypeface(null, android.graphics.Typeface.BOLD);
                                }
                                card.addView(tvDeadline);
                            }

                            TextView tvInfo = new TextView(this);
                            String infoText = "";
                            if (solver != null && !solver.isEmpty() && !"Pending".equals(status)) {
                                infoText += (isAmharic ? "የተመደበ ባለሙያ: @" : "Expert Assigned: @") + solver + "\n\n";
                            }
                            infoText += (isAmharic ? "መግለጫ:\n" : "Description:\n") + desc;

                            tvInfo.setText(infoText);
                            tvInfo.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                            tvInfo.setPadding(0, 8, 0, 0);
                            tvInfo.setVisibility(View.GONE);

                            ImageView ivAttachment = new ImageView(this);
                            String imageUrl = document.getString("imageUrl");
                            boolean hasImage = imageUrl != null && !imageUrl.isEmpty();

                            if (hasImage) {
                                LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, 500);
                                ivParams.setMargins(0, 20, 0, 0);
                                ivAttachment.setLayoutParams(ivParams);
                                ivAttachment.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                ivAttachment.setVisibility(View.GONE);

                                com.bumptech.glide.Glide.with(this).load(imageUrl).into(ivAttachment);
                            }

                            TextView tvTapToExpand = new TextView(this);
                            tvTapToExpand.setText(isAmharic ? "ዝርዝሮችን ለማየት ይጫኑ ▼" : "Tap to view details ▼");
                            tvTapToExpand.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
                            tvTapToExpand.setTextSize(12);
                            tvTapToExpand.setPadding(0, 10, 0, 10);

                            card.setOnClickListener(v -> {
                                if (tvInfo.getVisibility() == View.GONE) {
                                    tvInfo.setVisibility(View.VISIBLE);
                                    if (hasImage) ivAttachment.setVisibility(View.VISIBLE);
                                    tvTapToExpand.setText(isAmharic ? "ለማጠፍ ይጫኑ ▲" : "Tap to collapse ▲");
                                } else {
                                    tvInfo.setVisibility(View.GONE);
                                    if (hasImage) ivAttachment.setVisibility(View.GONE);
                                    tvTapToExpand.setText(isAmharic ? "ዝርዝሮችን ለማየት ይጫኑ ▼" : "Tap to view details ▼");
                                }
                            });

                            card.addView(tvInfo);
                            if (hasImage) card.addView(ivAttachment);
                            card.addView(tvTapToExpand);

                            if (!"Pending".equals(status)) {
                                Button btnGroupChat = new Button(this);
                                btnGroupChat.setText(isAmharic ? "💬 የትኬት ግሩፕ ቻት ክፈት" : "💬 Open Ticket Group Chat");
                                btnGroupChat.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6f42c1")));
                                btnGroupChat.setTextColor(Color.WHITE);
                                LinearLayout.LayoutParams gcParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                gcParams.setMargins(0, 0, 0, 16);
                                btnGroupChat.setLayoutParams(gcParams);
                                makeInteractive(btnGroupChat);
                                btnGroupChat.setOnClickListener(v -> showTicketGroupChat(document.getId(), displayCat));
                                card.addView(btnGroupChat);
                            }

                            if ("Pending".equals(status)) {
                                LinearLayout btnLayout = new LinearLayout(this);
                                btnLayout.setOrientation(LinearLayout.HORIZONTAL);
                                btnLayout.setPadding(0, 20, 0, 0);

                                Button btnEdit = new Button(this);
                                btnEdit.setText(isAmharic ? "አስተካክል" : "Edit");
                                btnEdit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.unifix_blue)));
                                btnEdit.setTextColor(Color.WHITE);
                                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                                bp.setMargins(0, 0, 8, 0);
                                btnEdit.setLayoutParams(bp);
                                makeInteractive(btnEdit);
                                btnEdit.setOnClickListener(v -> showEditDialog(document.getId(), desc, btnEdit));

                                Button btnDelete = new Button(this);
                                btnDelete.setText(isAmharic ? "ሰርዝ/አንሳ" : "Withdraw");
                                btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
                                btnDelete.setTextColor(Color.WHITE);
                                LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                                bp2.setMargins(8, 0, 0, 0);
                                btnDelete.setLayoutParams(bp2);
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

    private void showEditDialog(String documentId, String currentDesc, Button clickedButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isAmharic ? "የሪፖርት መግለጫን ያስተካክሉ" : "Update Description");
        final EditText input = new EditText(this);
        input.setText(currentDesc);
        styleInputBox(input);
        input.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        input.setPadding(40, 40, 40, 40);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(3);
        input.setMaxLines(8);
        input.setVerticalScrollBarEnabled(true);
        builder.setView(input);

        builder.setPositiveButton(isAmharic ? "ማስተካከያውን አስቀምጥ" : "Save", (dialog, which) -> {
            String newDesc = input.getText().toString().trim();
            if(!newDesc.isEmpty()) {
                clickedButton.setText(isAmharic ? "በማስቀመጥ ላይ..." : "Saving...");
                clickedButton.setEnabled(false);

                db.collection("reports").document(documentId).update("description", newDesc)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, isAmharic ? "ችግሩ በተሳካ ሁኔታ ተስተካክሏል!" : "Report updated!", Toast.LENGTH_SHORT).show();
                            fetchHistory();
                        })
                        .addOnFailureListener(e -> {
                            clickedButton.setText(isAmharic ? "አስተካክል" : "Edit");
                            clickedButton.setEnabled(true);
                            Toast.makeText(this, isAmharic ? "ችግሩን ማስተካከል አልተቻለም።" : "Failed to update report. Check connection.", Toast.LENGTH_SHORT).show();
                        });
            }
        });
        builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null).show();
    }

    private void deleteReport(String documentId, Button clickedButton) {
        new AlertDialog.Builder(this)
                .setTitle(isAmharic ? "ችግሩን ሰርዝ" : "Withdraw Report")
                .setMessage(isAmharic ? "እርግጠኛ ነዎት ይህን ችግር ሙሉ በሙሉ መሰረዝ ይፈልጋሉ? ይህ እርምጃ ሊቀለበስ አይችልም።" : "Delete this issue? This cannot be undone.")
                .setPositiveButton(isAmharic ? "አዎ፣ ሰርዝ" : "Yes, Withdraw", (dialog, which) -> {
                    clickedButton.setText(isAmharic ? "በመሰረዝ ላይ..." : "Withdrawing...");
                    clickedButton.setEnabled(false);

                    db.collection("reports").document(documentId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, isAmharic ? "ችግሩ ተሰርዟል።" : "Issue withdrawn.", Toast.LENGTH_SHORT).show();
                                fetchHistory();
                            })
                            .addOnFailureListener(e -> {
                                clickedButton.setText(isAmharic ? "ሰርዝ/አንሳ" : "Withdraw");
                                clickedButton.setEnabled(true);
                                Toast.makeText(this, isAmharic ? "ችግሩን መሰረዝ አልተቻለም።" : "Failed to withdraw issue.", Toast.LENGTH_SHORT).show();
                            });
                }).setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null).show();
    }

    private void showTicketGroupChat(String reportId, String categoryName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView titleView = new TextView(this);
        titleView.setText((isAmharic ? "ግሩፕ ቻት: " : "Group Chat: ") + categoryName + " Ticket");
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
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 800);
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
        etReply.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etReply.setMaxLines(4);
        LinearLayout.LayoutParams repParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
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
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(120, 120);
        btnSendReply.setLayoutParams(sendParams);
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
                            bubbleWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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

                            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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