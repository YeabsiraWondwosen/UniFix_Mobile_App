package com.UniFix.unifix;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class SolverDashboardActivity extends AppCompatActivity {

    // Language State
    boolean isAmharic = false;

    TextView tvExpertName, tvExpertDept, tvActiveCount, tvTotalCompleted, tvEmptyActive;
    Button btnTabActive, btnTabHistory, btnTabMessages, btnContactAdmin, btnLoadOlderHistory, btnSettings;
    LinearLayout containerActive, containerHistory, containerMessages;
    LinearLayout listActive, listHistory, solverInboxList;
    TextView tvMessageBadge;

    // Global Alerts Bell
    TextView tvGlobalAlertBadge;
    List<DocumentSnapshot> activeSystemAlerts = new ArrayList<>();

    EditText etSearchTasks;
    Spinner spinnerFilter;

    FirebaseFirestore db;
    String mySolverUsername = "";
    String mySolverDept = "";

    boolean showAllHistory = false;
    static boolean hasShownWelcomePopup = false;
    List<DocumentSnapshot> allFetchedTasks = new ArrayList<>();

    // Tab History tracking
    Stack<String> tabHistory = new Stack<>();
    String currentTab = "active";
    long backPressedTime = 0;

    // --- Translated Action Strings ---
    String msgStartTask = "Start Task";
    String msgMarkFinished = "Mark Finished 🟢";
    String msgDelegate = "Delegate 🤝";
    String msgAppeal = "Appeal ⚠️";
    String msgAcceptTask = "Accept Task";
    String msgDeclineTask = "Decline";
    String msgRemoveHistory = "Remove from History";
    String msgOpenChat = "💬 Open Ticket Group Chat";

    // --- 🎨 UI HELPER METHODS ---
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
        setContentView(R.layout.activity_solver_dashboard);

        db = FirebaseFirestore.getInstance();
        mySolverUsername = getIntent().getStringExtra("USERNAME");

        // READ LANGUAGE PREFERENCE
        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);

        tvExpertName = findViewById(R.id.tvExpertName);
        tvExpertDept = findViewById(R.id.tvExpertDept);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvTotalCompleted = findViewById(R.id.tvTotalCompleted);
        tvEmptyActive = findViewById(R.id.tvEmptyActive);

        btnTabActive = findViewById(R.id.btnTabActive);
        btnTabHistory = findViewById(R.id.btnTabHistory);
        btnTabMessages = findViewById(R.id.btnTabMessages);
        tvMessageBadge = findViewById(R.id.tvMessageBadge);
        btnSettings = findViewById(R.id.btnSettings);
        btnContactAdmin = findViewById(R.id.btnContactAdmin);
        btnLoadOlderHistory = findViewById(R.id.btnLoadOlderHistory);

        makeInteractive(btnTabActive);
        makeInteractive(btnTabHistory);
        makeInteractive(btnTabMessages);
        makeInteractive(btnSettings);
        makeInteractive(btnLoadOlderHistory);

        setupGlobalAlertBell();

        containerActive = findViewById(R.id.containerActive);
        containerHistory = findViewById(R.id.containerHistory);
        containerMessages = findViewById(R.id.containerMessages);

        listActive = findViewById(R.id.listActive);
        listHistory = findViewById(R.id.listHistory);
        solverInboxList = findViewById(R.id.solverInboxList);

        etSearchTasks = findViewById(R.id.etSearchTasks);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        styleInputBox(etSearchTasks);
        styleInputBox(spinnerFilter);

        btnTabActive.setOnClickListener(v -> switchToTab("active", true));
        btnTabHistory.setOnClickListener(v -> switchToTab("history", true));
        if (btnTabMessages != null) btnTabMessages.setOnClickListener(v -> switchToTab("messages", true));

        // Setting Gear triggers the unified Settings Menu
        btnSettings.setOnClickListener(v -> showSettingsMenu());

        if (btnContactAdmin != null) {
            btnContactAdmin.setOnClickListener(v -> showComposeMessageDialog(""));
        }

        btnLoadOlderHistory.setOnClickListener(v -> {
            showAllHistory = true;
            btnLoadOlderHistory.setText(isAmharic ? "ሁሉንም አሳይ" : "Showing All");
            btnLoadOlderHistory.setEnabled(false);
            applyFiltersAndRender();
        });

        applyStaticTranslations();
        setupSearchAndFilter();

        if (mySolverUsername != null && !mySolverUsername.isEmpty()) {
            fetchSolverProfile();
            loadTasksFromFirebase();
            loadSolverMessages();
        } else {
            Toast.makeText(this, isAmharic ? "ስህተት፡ የባለሙያው ማንነት አልታወቀም" : "Error: Solver Identity Unknown", Toast.LENGTH_LONG).show();
        }

        switchToTab("active", false);
    }

    private void setupGlobalAlertBell() {
        Button btnBell = findViewById(R.id.btnBell);
        tvGlobalAlertBadge = findViewById(R.id.tvGlobalAlertBadge);
        View badgeContainer = findViewById(R.id.badgeContainer);

        if (btnBell != null && tvGlobalAlertBadge != null && badgeContainer != null) {
            makeInteractive(btnBell);
            btnBell.setOnClickListener(v -> showSystemAlertsDialog());

            db.collection("solver_inbox") // Solvers use solver_inbox
                    .whereEqualTo("recipient", mySolverUsername)
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
                alertBg.setStroke(3, Color.parseColor("#dc3545")); // Highlight Red Border
                tvAlert.setBackground(alertBg);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 0, 0, 20);
                tvAlert.setLayoutParams(lp);

                layout.addView(tvAlert);

                db.collection("solver_inbox").document(doc.getId()).update("status", "Read");
            }
        }

        scroll.addView(layout);
        builder.setView(scroll);
        builder.setPositiveButton(isAmharic ? "ዝጋ" : "Close", null);
        builder.show();
    }


    private String getShortDeptName(String fullDept) {
        if (fullDept == null) return "Other";
        if (fullDept.contains("ICT")) return isAmharic ? "አይሲቲ (ICT)" : "ICT";
        if (fullDept.contains("Dormitory")) return isAmharic ? "መኝታ ክፍል" : "Dormitory";
        if (fullDept.contains("Academic")) return isAmharic ? "አካዳሚክ" : "Academic";
        if (fullDept.contains("Cafeteria")) return isAmharic ? "ካፌ" : "Cafeteria";
        if (fullDept.contains("Human Resource")) return isAmharic ? "የሰው ኃይል" : "HR";
        if (fullDept.contains("Health")) return isAmharic ? "ጤና" : "Health";
        if (fullDept.contains("Security")) return isAmharic ? "ደህንነት" : "Security";
        if (fullDept.contains("Finance")) return isAmharic ? "ፋይናንስ" : "Finance";
        if (fullDept.contains("Administration")) return isAmharic ? "አስተዳደር" : "Admin";
        if (fullDept.contains("Technician")) return isAmharic ? "ቴክኒሻን" : "Technician";
        return isAmharic ? "ሌላ" : "Other";
    }

    private String getTranslatedCategory(String englishCat) {
        if (!isAmharic) return englishCat;
        if (englishCat == null) return "ሌላ";
        switch (englishCat) {
            case "Staff ICT Manager": return "የአይሲቲ (ICT) ኃላፊ";
            case "Staff Dormitory Manager": return "የመኝታ ክፍል ኃላፊ";
            case "Staff Academic Resources Manager": return "የአካዳሚክ ግብዓቶች ኃላፊ";
            case "Staff Cafeteria Manager": return "የካፌ ኃላፊ";
            case "Staff Human Resource Manager": return "የሰው ኃይል ኃላፊ";
            case "Staff Health Center Manager": return "የጤና ጣቢያ ኃላፊ";
            case "Staff Campus Security Manager": return "የካምፓስ ደህንነት ኃላፊ";
            case "Staff Finance Manager": return "የፋይናንስ ኃላፊ";
            case "Staff University Administration Manager": return "የዩኒቨርሲቲ አስተዳደር";
            case "Staff Department Head": return "የትምህርት ክፍል ኃላፊ";
            case "Staff General Technician": return "አጠቃላይ ቴክኒሻን";
            case "Staff Dean": return "ዲን";
            case "All Categories": return "ሁሉም ምድቦች";
            default: return "ሌላ";
        }
    }

    private String getEnglishCategory(String amharicCat) {
        if (!isAmharic) return amharicCat;
        switch (amharicCat) {
            case "የአይሲቲ (ICT) ኃላፊ": return "Staff ICT Manager";
            case "የመኝታ ክፍል ኃላፊ": return "Staff Dormitory Manager";
            case "የአካዳሚክ ግብዓቶች ኃላፊ": return "Staff Academic Resources Manager";
            case "የካፌ ኃላፊ": return "Staff Cafeteria Manager";
            case "የሰው ኃይል ኃላፊ": return "Staff Human Resource Manager";
            case "የጤና ጣቢያ ኃላፊ": return "Staff Health Center Manager";
            case "የካምፓስ ደህንነት ኃላፊ": return "Staff Campus Security Manager";
            case "የፋይናንስ ኃላፊ": return "Staff Finance Manager";
            case "የዩኒቨርሲቲ አስተዳደር": return "Staff University Administration Manager";
            case "የትምህርት ክፍል ኃላፊ": return "Staff Department Head";
            case "አጠቃላይ ቴክኒሻን": return "Staff General Technician";
            case "ዲን": return "Staff Dean";
            case "ሁሉም ምድቦች": return "All Categories";
            default: return "Other";
        }
    }

    private void applyStaticTranslations() {
        if (!isAmharic) return;

        TextView tvActiveLabel = findViewById(R.id.tvActiveLabel);
        if(tvActiveLabel != null) tvActiveLabel.setText("ንቁ");

        TextView tvSolvedLabel = findViewById(R.id.tvSolvedLabel);
        if(tvSolvedLabel != null) tvSolvedLabel.setText("የተፈቱ");

        btnTabActive.setText("ንቁ");
        btnTabHistory.setText("ታሪክ");
        btnTabMessages.setText("መልዕክቶች");

        TextView tvActiveJobsTitle = findViewById(R.id.tvActiveJobsTitle);
        if(tvActiveJobsTitle != null) tvActiveJobsTitle.setText("የእኔ ንቁ ተግባራት");

        if(btnContactAdmin != null) btnContactAdmin.setText("ለአስተዳዳሪ መልዕክት ላክ");

        etSearchTasks.setHint("ችግር ወይም ተጠቃሚ ይፈልጉ...");
        tvEmptyActive.setText("ምንም የተመደበልዎት ንቁ ተግባር የለም።");

        TextView tvRecentHistoryTitle = findViewById(R.id.tvRecentHistoryTitle);
        if(tvRecentHistoryTitle != null) tvRecentHistoryTitle.setText("የቅርብ ጊዜ ታሪክ");

        btnLoadOlderHistory.setText("ሁሉንም አሳይ");

        TextView tvSolverInboxTitle = findViewById(R.id.tvSolverInboxTitle);
        if(tvSolverInboxTitle != null) tvSolverInboxTitle.setText("የባለሙያ መልዕክት ሳጥን");

        msgStartTask = "ስራ ጀምር";
        msgMarkFinished = "ተጠናቋል ብለህ መዝግብ 🟢";
        msgDelegate = "ውክልና ስጥ 🤝";
        msgAppeal = "ይግባኝ ጠይቅ ⚠️";
        msgAcceptTask = "ተግባሩን ተቀበል";
        msgDeclineTask = "አትቀበል";
        msgRemoveHistory = "ከታሪክ አስወግድ";
        msgOpenChat = "💬 የትኬት ግሩፕ ቻት ክፈት";
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
                Toast.makeText(this, isAmharic ? "ከመተግበሪያው ለመውጣት የጀርባ ቁልፍን በድጋሚ ይጫኑ" : "Press back again to exit quickly", Toast.LENGTH_SHORT).show();
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

    private int getTabIndex(String tabName) {
        switch (tabName) {
            case "active": return 0;
            case "history": return 1;
            case "messages": return 2;
            default: return 0;
        }
    }

    private void switchToTab(String tab, boolean addToHistory) {
        if (currentTab.equals(tab) && (containerActive.getVisibility() == View.VISIBLE || containerHistory.getVisibility() == View.VISIBLE)) {
            return;
        }

        int oldIndex = getTabIndex(currentTab);
        int newIndex = getTabIndex(tab);

        if (addToHistory) {
            tabHistory.push(currentTab);
        }
        currentTab = tab;

        Slide slide = new Slide();
        if (newIndex > oldIndex) {
            slide.setSlideEdge(Gravity.END);
        } else {
            slide.setSlideEdge(Gravity.START);
        }
        slide.setDuration(250);
        TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), slide);

        if (containerActive != null) containerActive.setVisibility(View.GONE);
        if (containerHistory != null) containerHistory.setVisibility(View.GONE);
        if (containerMessages != null) containerMessages.setVisibility(View.GONE);

        int inactiveBg = ContextCompat.getColor(this, R.color.input_background);
        int inactiveText = ContextCompat.getColor(this, R.color.text_primary);
        if (btnTabActive != null) {
            btnTabActive.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
            btnTabActive.setTextColor(inactiveText);
        }
        if (btnTabHistory != null) {
            btnTabHistory.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
            btnTabHistory.setTextColor(inactiveText);
        }
        if (btnTabMessages != null) {
            btnTabMessages.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
            btnTabMessages.setTextColor(inactiveText);
        }

        int activeBg = ContextCompat.getColor(this, R.color.unifix_blue);
        int activeText = ContextCompat.getColor(this, R.color.white);

        if (tab.equals("active")) {
            if (containerActive != null) containerActive.setVisibility(View.VISIBLE);
            if (btnTabActive != null) {
                btnTabActive.setBackgroundTintList(ColorStateList.valueOf(activeBg));
                btnTabActive.setTextColor(activeText);
            }
        } else if (tab.equals("history")) {
            if (containerHistory != null) containerHistory.setVisibility(View.VISIBLE);
            if (btnTabHistory != null) {
                btnTabHistory.setBackgroundTintList(ColorStateList.valueOf(activeBg));
                btnTabHistory.setTextColor(activeText);
            }
        } else if (tab.equals("messages")) {
            if (containerMessages != null) containerMessages.setVisibility(View.VISIBLE);
            if (btnTabMessages != null) {
                btnTabMessages.setBackgroundTintList(ColorStateList.valueOf(activeBg));
                btnTabMessages.setTextColor(activeText);
            }
            loadSolverMessages();
        }
    }

    private void showWelcomePopup(int totalTasks) {
        String title = isAmharic ? "🔔 እርምጃ ያስፈልጋል" : "🔔 Action Required";
        String msg = isAmharic ? "እንኳን ደህና መጡ, @" + mySolverUsername + "!\n\nለእርስዎ የተመደቡ " + totalTasks + " የሚጠብቁ/ንቁ ተግባራት አሉ።"
                : "Welcome back, @" + mySolverUsername + "!\n\nYou have " + totalTasks + " pending/active tasks assigned to you.";
        String btnText = isAmharic ? "እንጀምር" : "Let's Go";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(btnText, null)
                .setCancelable(false)
                .show();
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
        db.collection("users").document(mySolverUsername).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(isAmharic ? "የመገለጫ ቅንብሮች ⚙️" : "Edit Profile ⚙️");
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 40, 50, 40);
                layout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));

                TextView tvLockedUser = new TextView(this);
                tvLockedUser.setText((isAmharic ? "የተጠቃሚ ስም: @" : "Username: @") + mySolverUsername + (isAmharic ? " (የተቆለፈ)" : " (Locked)"));
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

                    db.collection("users").document(mySolverUsername).update(updates)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, isAmharic ? "የመገለጫ ቅንብሮች ዘምነዋል! ✅" : "Profile Updated! ✅", Toast.LENGTH_LONG).show();

                                String shortName = name;
                                if (name.contains(" ")) shortName = name.split(" ")[0];
                                tvExpertName.setText((isAmharic ? "ሰላም, " : "Hi, ") + shortName);
                            });
                });
                builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null);
                builder.show();
            }
        });
    }

    private void setupSearchAndFilter() {
        String[] filters;
        if (isAmharic) {
            filters = new String[]{
                    "ሁሉም ምድቦች", "የአይሲቲ (ICT) ኃላፊ", "የመኝታ ክፍል ኃላፊ", "የአካዳሚክ ግብዓቶች ኃላፊ",
                    "የካፌ ኃላፊ", "የሰው ኃይል ኃላፊ", "የጤና ጣቢያ ኃላፊ", "የካምፓስ ደህንነት ኃላፊ",
                    "የፋይናንስ ኃላፊ", "የዩኒቨርሲቲ አስተዳደር", "የትምህርት ክፍል ኃላፊ", "አጠቃላይ ቴክኒሻን", "ዲን", "ሌላ"
            };
        } else {
            filters = new String[]{
                    "All Categories", "Staff ICT Manager", "Staff Dormitory Manager", "Staff Academic Resources Manager",
                    "Staff Cafeteria Manager", "Staff Human Resource Manager", "Staff Health Center Manager",
                    "Staff Campus Security Manager", "Staff Finance Manager", "Staff University Administration Manager",
                    "Staff Department Head", "Staff General Technician", "Staff Dean", "Other"
            };
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        spinnerFilter.setAdapter(adapter);

        etSearchTasks.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFiltersAndRender(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFiltersAndRender(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchSolverProfile() {
        db.collection("users").whereEqualTo("username", mySolverUsername).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String fullName = doc.getString("fullName");
                        mySolverDept = doc.getString("dept");

                        if (fullName != null) {
                            String shortName = fullName;
                            if (fullName.contains(" ")) shortName = fullName.split(" ")[0];
                            tvExpertName.setText((isAmharic ? "ሰላም, " : "Hi, ") + shortName);
                        }
                        if (mySolverDept != null) {
                            tvExpertDept.setText((isAmharic ? "ክፍል: " : "Dept: ") + getShortDeptName(mySolverDept));
                        }
                    }
                });
    }

    private void loadTasksFromFirebase() {
        db.collection("reports").whereEqualTo("assignedTo", mySolverUsername).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allFetchedTasks = task.getResult().getDocuments();

                        Collections.sort(allFetchedTasks, (doc1, doc2) -> {
                            int urg1 = getUrgencyValue(doc1.getString("urgency"));
                            int urg2 = getUrgencyValue(doc2.getString("urgency"));
                            if (urg1 != urg2) return Integer.compare(urg2, urg1);

                            String d1 = doc1.getString("date") != null ? doc1.getString("date") : "";
                            String d2 = doc2.getString("date") != null ? doc2.getString("date") : "";
                            return d2.compareTo(d1);
                        });

                        applyFiltersAndRender();
                    } else {
                        Toast.makeText(this, isAmharic ? "ተግባራትን መጫን አልተቻለም።" : "Failed to load tasks.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFiltersAndRender() {
        if (listActive != null) listActive.removeAllViews();
        if (listHistory != null) listHistory.removeAllViews();

        String searchQuery = etSearchTasks.getText().toString().toLowerCase().trim();
        String selectedFilter = spinnerFilter.getSelectedItem().toString();
        String dbSearchCat = getEnglishCategory(selectedFilter);

        int activeCounter = 0;
        int solvedCounter = 0;
        long currentTime = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;

        for (DocumentSnapshot document : allFetchedTasks) {
            String status = document.getString("status");
            String cat = document.getString("category");
            String desc = document.getString("description");
            String reporterUsername = document.getString("reporterUsername");
            String reporterFullName = document.getString("reporterFullName");
            Long resolvedAt = document.getLong("resolvedTimestamp");

            if (!dbSearchCat.equals("All Categories") && cat != null && !cat.equals(dbSearchCat)) continue;

            boolean matchesSearch = false;
            if (searchQuery.isEmpty()) matchesSearch = true;
            else {
                if (desc != null && desc.toLowerCase().contains(searchQuery)) matchesSearch = true;
                if (reporterUsername != null && reporterUsername.toLowerCase().contains(searchQuery)) matchesSearch = true;
                if (reporterFullName != null && reporterFullName.toLowerCase().contains(searchQuery)) matchesSearch = true;
            }

            if (!matchesSearch) continue;

            if ("Assigned".equals(status) || "In Progress".equals(status) || "Delegated_Pending".equals(status)) {
                addTaskCardToUI(document, listActive, status);
                activeCounter++;
            } else if ("Finished".equals(status) || "Completed".equals(status) || "Appealed".equals(status)) {
                boolean isRecent = resolvedAt != null && (currentTime - resolvedAt) < oneDayMillis;
                if (isRecent || showAllHistory) {
                    addTaskCardToUI(document, listHistory, status);
                    solvedCounter++;
                }
            }
        }

        tvActiveCount.setText(String.valueOf(activeCounter));
        tvTotalCompleted.setText(String.valueOf(solvedCounter));
        tvEmptyActive.setVisibility(activeCounter == 0 ? View.VISIBLE : View.GONE);

        if (!hasShownWelcomePopup && activeCounter > 0) {
            showWelcomePopup(activeCounter);
            hasShownWelcomePopup = true;
        } else if (!hasShownWelcomePopup) {
            hasShownWelcomePopup = true;
        }
    }

    private int getUrgencyValue(String urgency) {
        if (urgency == null) return 0;
        switch (urgency) {
            case "Urgent": return 4;
            case "High": return 3;
            case "Medium": return 2;
            case "Low": return 1;
            default: return 0;
        }
    }

    private void addTaskCardToUI(DocumentSnapshot doc, LinearLayout targetList, String currentStatus) {
        if (targetList == null) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 40, 40, 40);
        styleFloatingCard(card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 30);
        card.setLayoutParams(params);

        String cat = doc.getString("category");
        String urg = doc.getString("urgency");
        String desc = doc.getString("description");
        String date = doc.getString("date");
        String repName = doc.getString("reporterFullName");
        String repUser = doc.getString("reporterUsername");
        Long assignedTime = doc.getLong("assignedTimestamp");
        if(repName == null) repName = isAmharic ? "ያልታወቀ" : "Unknown";
        if(repUser == null) repUser = isAmharic ? "ያልታወቀ" : "Unknown";

        String translatedCat = getTranslatedCategory(cat);
        String translatedUrg = urg;
        if(isAmharic) {
            if("Urgent".equals(urg)) translatedUrg = "በጣም አስቸኳይ";
            else if("High".equals(urg)) translatedUrg = "አስቸኳይ";
            else if("Medium".equals(urg)) translatedUrg = "መካከለኛ";
            else if("Low".equals(urg)) translatedUrg = "ዝቅተኛ";
        }

        TextView tvTitle = new TextView(this);
        tvTitle.setText(translatedCat + " [" + translatedUrg + "]");
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        if ("Urgent".equals(urg)) tvTitle.setTextColor(Color.parseColor("#dc3545"));
        else if ("High".equals(urg)) tvTitle.setTextColor(Color.parseColor("#fd7e14"));
        else tvTitle.setTextColor(ContextCompat.getColor(this, R.color.unifix_blue));

        TextView tvReporter = new TextView(this);
        tvReporter.setText((isAmharic ? "ሪፖርት አድራጊ: " : "Reported by: ") + repName + " (@" + repUser + ")");
        tvReporter.setTextColor(Color.parseColor("#198754"));
        tvReporter.setTextSize(12);
        tvReporter.setPadding(0, 0, 0, 8);
        tvReporter.setTypeface(null, android.graphics.Typeface.ITALIC);

        String statusDisplay = (isAmharic ? "ሁኔታ: " : "Status: ");
        if ("Pending".equals(currentStatus)) statusDisplay += (isAmharic ? "በመጠባበቅ ላይ" : "Pending") + " 🟡";
        else if ("In Progress".equals(currentStatus)) statusDisplay += (isAmharic ? "በሂደት ላይ" : "In Progress") + " 🔵";
        else if ("Finished".equals(currentStatus) || "Completed".equals(currentStatus)) statusDisplay += (isAmharic ? "ተጠናቋል" : "Finished") + " 🟢";
        else if ("Appealed".equals(currentStatus)) statusDisplay += (isAmharic ? "ይግባኝ ተጠይቋል ⚠️ (ወደ አስተዳዳሪ ተልኳል)" : "Appealed ⚠️ (Sent to Admin)");
        else if ("Delegated_Pending".equals(currentStatus)) statusDisplay += (isAmharic ? "ማረጋገጫዎን በመጠባበቅ ላይ 🤝" : "Pending Your Acceptance 🤝");
        else if ("Assigned".equals(currentStatus)) statusDisplay += (isAmharic ? "ተመድቧል" : "Assigned");

        TextView tvStatus = new TextView(this);
        tvStatus.setText(statusDisplay);
        tvStatus.setTextSize(12);
        tvStatus.setPadding(0, 4, 0, 8);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        card.addView(tvTitle);
        card.addView(tvReporter);
        card.addView(tvStatus);

        // 🔥 UPGRADED: DYNAMIC DEADLINES (1h, 6h, 12h, 24h) FOR SOLVER 🔥
        if (assignedTime != null && ("Assigned".equals(currentStatus) || "In Progress".equals(currentStatus))) {
            long duration = 48L * 60 * 60 * 1000;
            if ("Urgent".equals(urg)) duration = 1L * 60 * 60 * 1000;
            else if ("High".equals(urg)) duration = 6L * 60 * 60 * 1000;
            else if ("Medium".equals(urg)) duration = 12L * 60 * 60 * 1000;
            else if ("Low".equals(urg)) duration = 24L * 60 * 60 * 1000;

            long timeRemaining = (assignedTime + duration) - System.currentTimeMillis();
            TextView tvDeadline = new TextView(this);
            tvDeadline.setPadding(0, 0, 0, 8);

            if (timeRemaining > 0) {
                long hours = timeRemaining / (60 * 60 * 1000);
                tvDeadline.setText((isAmharic ? "ቀሪ ጊዜ: " : "Deadline: ") + hours + (isAmharic ? " ሰዓታት ቀርተዋል" : " hours left"));
                tvDeadline.setTextColor(Color.parseColor("#fd7e14"));
            } else {
                tvDeadline.setText(isAmharic ? "⚠️ ጊዜው አልፏል" : "⚠️ OVERDUE");
                tvDeadline.setTextColor(Color.parseColor("#dc3545"));
                tvDeadline.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            card.addView(tvDeadline);
        }

        // 🔥 SPECIFIC DETAILS FORMATTER 🔥
        StringBuilder extraDetails = new StringBuilder();
        Map<String, Object> specifics = (Map<String, Object>) doc.get("specificDetails");
        if (specifics != null && !specifics.isEmpty()) {
            extraDetails.append(isAmharic ? "\n\n📍 የተወሰኑ ዝርዝሮች:\n" : "\n\n📍 Specific Details:\n");
            for (Map.Entry<String, Object> entry : specifics.entrySet()) {
                String key = entry.getKey();
                key = key.substring(0, 1).toUpperCase() + key.substring(1);
                key = key.replaceAll("([A-Z])", " $1").trim();
                extraDetails.append("• ").append(key).append(": ").append(entry.getValue()).append("\n");
            }
        }

        TextView tvDetails = new TextView(this);
        tvDetails.setText((isAmharic ? "መግለጫ: " : "Details: ") + desc + extraDetails.toString() + (isAmharic ? "\nሪፖርት የተደረገበት: " : "\nReported: ") + date);
        tvDetails.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvDetails.setPadding(0, 0, 0, 20);
        tvDetails.setVisibility(View.GONE);

        // --- SOLVER GLIDE IMAGE LOADING LOGIC ---
        ImageView ivAttachment = new ImageView(this);
        String imageUrl = doc.getString("imageUrl");
        boolean hasImage = imageUrl != null && !imageUrl.isEmpty();

        if (hasImage) {
            LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 600);
            ivParams.setMargins(0, 20, 0, 20);
            ivAttachment.setLayoutParams(ivParams);
            ivAttachment.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivAttachment.setVisibility(View.GONE);

            ivAttachment.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
            ivAttachment.setClipToOutline(true);
            android.graphics.drawable.GradientDrawable imgShape = new android.graphics.drawable.GradientDrawable();
            imgShape.setCornerRadius(20f);
            ivAttachment.setBackground(imgShape);

            Glide.with(this).load(imageUrl).into(ivAttachment);
        }

        TextView tvTapToExpand = new TextView(this);
        tvTapToExpand.setText(isAmharic ? "ዝርዝሮችን ለማየት ይጫኑ ▼" : "Tap to view details ▼");
        tvTapToExpand.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
        tvTapToExpand.setTextSize(12);
        tvTapToExpand.setPadding(0, 10, 0, 20);

        card.setOnClickListener(v -> {
            if (tvDetails.getVisibility() == View.GONE) {
                tvDetails.setVisibility(View.VISIBLE);
                if (hasImage) ivAttachment.setVisibility(View.VISIBLE);
                tvTapToExpand.setText(isAmharic ? "ለማጠፍ ይጫኑ ▲" : "Tap to collapse ▲");
            } else {
                tvDetails.setVisibility(View.GONE);
                if (hasImage) ivAttachment.setVisibility(View.GONE);
                tvTapToExpand.setText(isAmharic ? "ዝርዝሮችን ለማየት ይጫኑ ▼" : "Tap to view details ▼");
            }
        });

        card.addView(tvDetails);
        if (hasImage) card.addView(ivAttachment);
        card.addView(tvTapToExpand);

        Button btnGroupChat = new Button(this);
        btnGroupChat.setText(msgOpenChat);
        btnGroupChat.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6f42c1"))); // Purple
        btnGroupChat.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams gcParams = new LinearLayout.LayoutParams(-1, -2);
        gcParams.setMargins(0, 0, 0, 20);
        btnGroupChat.setLayoutParams(gcParams);
        makeInteractive(btnGroupChat);
        btnGroupChat.setOnClickListener(v -> showTicketGroupChat(doc.getId(), translatedCat));
        card.addView(btnGroupChat);

        if (targetList == listActive) setupActiveButtons(card, doc, currentStatus);
        else setupHistoryButtons(card, doc);

        targetList.addView(card);
    }

    private void setupActiveButtons(LinearLayout card, DocumentSnapshot doc, String currentStatus) {
        Boolean appealRejected = doc.getBoolean("appealRejected");
        if (Boolean.TRUE.equals(appealRejected)) {
            GradientDrawable alertShape = new GradientDrawable();
            alertShape.setCornerRadius(40f);
            alertShape.setColor(Color.parseColor("#44dc3545"));
            card.setBackground(alertShape);

            TextView tvRejected = new TextView(this);
            tvRejected.setText(isAmharic ? "⚠️ አስተዳዳሪው ይግባኝዎን ውድቅ አድርጎታል እና ማስጠንቀቂያ ሰጥቶዎታል። ይህን ተግባር ማጠናቀቅ አለብዎት።" : "⚠️ Admin rejected your appeal and issued a formal warning. You must complete this task.");
            tvRejected.setTextColor(Color.parseColor("#dc3545"));
            tvRejected.setTypeface(null, android.graphics.Typeface.BOLD);
            tvRejected.setPadding(0, 0, 0, 20);
            card.addView(tvRejected);
        }

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        if ("Delegated_Pending".equals(currentStatus)) {
            String delegatedFrom = doc.getString("delegatedFrom");

            Button btnAccept = new Button(this);
            btnAccept.setText(msgAcceptTask);
            btnAccept.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
            btnAccept.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
            p1.setMargins(0, 0, 10, 0);
            btnAccept.setLayoutParams(p1);
            makeInteractive(btnAccept);
            btnAccept.setOnClickListener(v -> processDelegation(doc.getId(), true, delegatedFrom));

            Button btnDecline = new Button(this);
            btnDecline.setText(msgDeclineTask);
            btnDecline.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
            btnDecline.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1);
            btnDecline.setLayoutParams(p2);
            makeInteractive(btnDecline);
            btnDecline.setOnClickListener(v -> processDelegation(doc.getId(), false, delegatedFrom));

            btnLayout.addView(btnAccept);
            btnLayout.addView(btnDecline);
            card.addView(btnLayout);

        } else {
            Button btnAction = new Button(this);
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
            p1.setMargins(0, 0, 10, 0);
            btnAction.setLayoutParams(p1);
            makeInteractive(btnAction);

            if ("Assigned".equals(currentStatus)) {
                btnAction.setText(msgStartTask);
                btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.unifix_blue)));
                btnAction.setTextColor(Color.WHITE);
                btnAction.setOnClickListener(v -> updateJobStatus(doc.getId(), "In Progress", btnAction));
            } else {
                btnAction.setText(msgMarkFinished);
                btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
                btnAction.setTextColor(Color.WHITE);
                btnAction.setOnClickListener(v -> updateJobStatus(doc.getId(), "Finished", btnAction));
            }

            Button btnDelegate = new Button(this);
            btnDelegate.setText(msgDelegate);
            btnDelegate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffc107")));
            btnDelegate.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1);
            p2.setMargins(0, 0, 10, 0);
            btnDelegate.setLayoutParams(p2);
            makeInteractive(btnDelegate);
            btnDelegate.setOnClickListener(v -> showDelegateDialog(doc.getId(), doc.getString("category")));

            Button btnAppeal = new Button(this);
            btnAppeal.setText(msgAppeal);
            btnAppeal.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
            btnAppeal.setTextColor(Color.WHITE);
            btnAppeal.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            makeInteractive(btnAppeal);
            btnAppeal.setOnClickListener(v -> appealTask(doc.getId(), btnAppeal));

            btnLayout.addView(btnAction);
            btnLayout.addView(btnDelegate);
            btnLayout.addView(btnAppeal);
            card.addView(btnLayout);
        }
    }

    private void setupHistoryButtons(LinearLayout card, DocumentSnapshot doc) {
        Button btnDelete = new Button(this);
        btnDelete.setText(msgRemoveHistory);
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6c757d")));
        btnDelete.setTextColor(Color.WHITE);
        makeInteractive(btnDelete);
        btnDelete.setOnClickListener(v -> deleteReport(doc.getId(), btnDelete));
        card.addView(btnDelete);
    }

    private void showDelegateDialog(String reportId, String category) {
        db.collection("users").whereEqualTo("role", "Solver").whereEqualTo("isBanned", false).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> names = new ArrayList<>();
                    List<String> usernames = new ArrayList<>();

                    for (QueryDocumentSnapshot user : queryDocumentSnapshots) {
                        String uName = user.getString("username");
                        String d = user.getString("dept");
                        if (uName != null && !uName.equals(mySolverUsername)) {
                            if (d != null && (d.equals(category) || d.equals("Staff General Technician"))) {
                                names.add(user.getString("fullName") + " (" + getTranslatedCategory(d) + ")");
                                usernames.add(uName);
                            }
                        }
                    }

                    if (names.isEmpty()) {
                        Toast.makeText(this, isAmharic ? "ምንም ብቁ ባለሙያ አልተገኘም።" : "No eligible solvers found to delegate to.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Spinner spinner = new Spinner(this);
                    spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
                    styleInputBox(spinner);
                    spinner.setPadding(40,40,40,40);

                    new AlertDialog.Builder(this)
                            .setTitle(isAmharic ? "ተግባር ውክልና ስጥ" : "Delegate Task")
                            .setMessage(isAmharic ? "ይህን ተግባር የሚያስተላልፉለትን ባለሙያ ይምረጡ። እነሱ መቀበል አለባቸው።" : "Select an expert to pass this task to. They must accept it.")
                            .setView(spinner)
                            .setPositiveButton(isAmharic ? "ጥያቄ ላክ" : "Send Request", (dialog, which) -> {
                                String targetUser = usernames.get(spinner.getSelectedItemPosition());

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("status", "Delegated_Pending");
                                updates.put("assignedTo", targetUser);
                                updates.put("delegatedFrom", mySolverUsername);

                                db.collection("reports").document(reportId).update(updates)
                                        .addOnSuccessListener(a -> {
                                            Toast.makeText(this, (isAmharic ? "ውክልና ተልኳል ለ " : "Delegation sent to ") + targetUser, Toast.LENGTH_SHORT).show();
                                            sendInAppNotification(targetUser, isAmharic ? "የተግባር ውክልና ጥያቄ" : "Task Delegation Request",
                                                    isAmharic ? "ባለሙያ @" + mySolverUsername + " አንድ ተግባር ውክልና ሊሰጥዎ ይፈልጋል።" : "Solver @" + mySolverUsername + " wants to delegate a task to you.");
                                            loadTasksFromFirebase();
                                        });
                            })
                            .setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null)
                            .show();
                });
    }

    private void processDelegation(String reportId, boolean isAccepted, String originalSolver) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Assigned");
        updates.put("delegatedFrom", null);

        if (isAccepted) {
            updates.put("assignedTo", mySolverUsername);
            sendInAppNotification(originalSolver, isAmharic ? "ውክልና ተቀባይነት አግኝቷል" : "Delegation Accepted",
                    isAmharic ? "ባለሙያ @" + mySolverUsername + " የውክልና ጥያቄዎን ተቀብሏል።" : "Solver @" + mySolverUsername + " accepted your task delegation.");
        } else {
            updates.put("assignedTo", originalSolver);
            sendInAppNotification(originalSolver, isAmharic ? "ውክልና ተቀባይነት አላገኘም" : "Delegation Declined",
                    isAmharic ? "ባለሙያ @" + mySolverUsername + " የውክልና ጥያቄዎን አልተቀበለም።" : "Solver @" + mySolverUsername + " declined your task delegation.");
            Toast.makeText(this, (isAmharic ? "ተግባር ተመልሷል ለ " : "Task bounced back to ") + originalSolver, Toast.LENGTH_SHORT).show();
        }

        db.collection("reports").document(reportId).update(updates)
                .addOnSuccessListener(a -> loadTasksFromFirebase());
    }

    private void deleteReport(String documentId, Button clickedButton) {
        clickedButton.setText(isAmharic ? "በመሰረዝ ላይ..." : "Deleting...");
        clickedButton.setEnabled(false);
        db.collection("reports").document(documentId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isAmharic ? "ከታሪክ ተሰርዟል" : "Record deleted", Toast.LENGTH_SHORT).show();
                    loadTasksFromFirebase();
                });
    }

    private void updateJobStatus(String docId, String status, Button clickedButton) {
        clickedButton.setText(isAmharic ? "በማዘመን ላይ..." : "Updating...");
        clickedButton.setEnabled(false);

        Map<String, Object> up = new HashMap<>();
        up.put("status", status);
        if ("Finished".equals(status)) {
            up.put("resolvedTimestamp", System.currentTimeMillis());
        }

        db.collection("reports").document(docId).update(up)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, (isAmharic ? "ተግባሩ ዘምኗል: " : "Task Updated to ") + status, Toast.LENGTH_SHORT).show();

                    db.collection("reports").document(docId).get().addOnSuccessListener(doc -> {
                        String reporter = doc.getString("reporterUsername");
                        String admin = doc.getString("assignedByAdmin");
                        String cat = doc.getString("category");
                        String translatedCat = getTranslatedCategory(cat);

                        if (reporter != null) {
                            sendInAppNotification(reporter, isAmharic ? "የችግር ማሻሻያ፡ " + status : "Issue Update: " + status,
                                    isAmharic ? "የእርስዎ " + translatedCat + " ችግር አሁን በ @" + mySolverUsername + " ወደ " + status + " ተቀይሯል።"
                                            : "Your " + cat + " issue is now marked as: " + status + " by @" + mySolverUsername + ".");
                        }
                        if (admin != null) {
                            sendInAppNotification(admin, isAmharic ? "የተግባር ሂደት፡ " + status : "Task Progress: " + status,
                                    isAmharic ? "ባለሙያ @" + mySolverUsername + " አንድ የ " + translatedCat + " ተግባር ወደ " + status + " ቀይሮታል።"
                                            : "Solver @" + mySolverUsername + " has marked a " + cat + " task as: " + status + ".");
                        }
                    });

                    loadTasksFromFirebase();
                })
                .addOnFailureListener(e -> {
                    clickedButton.setEnabled(true);
                    clickedButton.setText(status.equals("In Progress") ? msgStartTask : msgMarkFinished);
                    Toast.makeText(this, isAmharic ? "ማዘመን አልተሳካም። ግንኙነትዎን ያረጋግጡ።" : "Update Failed: Check your connection.", Toast.LENGTH_LONG).show();
                });
    }

    private void appealTask(String docId, Button clickedButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isAmharic ? "የተግባር ይግባኝ" : "Appeal Task Assignment");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));

        TextView tvDesc = new TextView(this);
        tvDesc.setText(isAmharic ? "ለአስተዳዳሪው ትክክለኛ ምክንያት ያቅርቡ። ውድቅ ከተደረገ ማስጠንቀቂያ ሊደርስዎት ይችላል።" : "Provide a valid reason for the Admin. If rejected, you may receive a warning.");
        tvDesc.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvDesc.setPadding(0, 0, 0, 20);
        layout.addView(tvDesc);

        final EditText inputReason = new EditText(this);
        inputReason.setHint(isAmharic ? "የይግባኝ ምክንያት..." : "Reason for appeal...");
        styleInputBox(inputReason);
        inputReason.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        inputReason.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
        inputReason.setPadding(40, 40, 40, 40);
        inputReason.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputReason.setMinLines(3);
        inputReason.setMaxLines(8);
        inputReason.setVerticalScrollBarEnabled(true);
        inputReason.setGravity(Gravity.TOP | Gravity.START);
        layout.addView(inputReason);

        builder.setView(layout);

        builder.setPositiveButton(isAmharic ? "ይግባኝ አስገባ" : "Submit Appeal", (dialog, which) -> {
            String reason = inputReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, isAmharic ? "ይግባኝ ለማለት ምክንያት ማቅረብ አለብዎት።" : "You must provide a reason to appeal.", Toast.LENGTH_SHORT).show();
                return;
            }

            clickedButton.setText(isAmharic ? "በመጠየቅ ላይ..." : "Appealing...");
            clickedButton.setEnabled(false);

            Map<String, Object> up = new HashMap<>();
            up.put("status", "Appealed");
            up.put("appealReason", reason);
            up.put("appealRejected", false);
            up.put("resolvedTimestamp", System.currentTimeMillis());

            db.collection("reports").document(docId).update(up)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, isAmharic ? "ይግባኝ ተጠይቋል። ወደ አስተዳዳሪ ተልኳል።" : "Task Appealed. Sent to Admin.", Toast.LENGTH_LONG).show();

                        db.collection("reports").document(docId).get().addOnSuccessListener(doc -> {
                            String admin = doc.getString("assignedByAdmin");
                            if (admin != null) {
                                sendInAppNotification(admin, isAmharic ? "ተግባር ይግባኝ ተጠይቋል" : "Task Appealed",
                                        isAmharic ? "ባለሙያ @" + mySolverUsername + " አንድ የተግባር ምደባ ላይ ይግባኝ ብሏል። ምክንያት: " + reason
                                                : "Solver @" + mySolverUsername + " has appealed a task assignment. Reason: " + reason);
                            }
                        });

                        loadTasksFromFirebase();
                    });
        });
        builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null);
        builder.show();
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
        etReply.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etReply.setMaxLines(4);
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
                            boolean isMe = mySolverUsername.equals(msgSender);

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
                msg.put("sender", mySolverUsername);
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

    private void loadSolverMessages() {
        if (solverInboxList == null) return;

        db.collection("solver_inbox")
                .whereEqualTo("recipient", mySolverUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    solverInboxList.removeAllViews();
                    int totalUnreadCount = 0;

                    Button btnCompose = new Button(this);
                    btnCompose.setText(isAmharic ? "➕ አዲስ የግል መልዕክት ጻፍ" : "➕ Compose New Private Message");
                    btnCompose.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
                    btnCompose.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams composeParams = new LinearLayout.LayoutParams(-1, -2);
                    composeParams.setMargins(0, 0, 0, 30);
                    btnCompose.setLayoutParams(composeParams);
                    btnCompose.setPadding(0, 30, 0, 30);
                    makeInteractive(btnCompose);
                    btnCompose.setOnClickListener(v -> showComposeMessageDialog(""));
                    solverInboxList.addView(btnCompose);

                    List<DocumentSnapshot> allDocs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
                    Collections.sort(allDocs, (d1, d2) -> {
                        Long t1 = d1.getLong("timestamp");
                        Long t2 = d2.getLong("timestamp");
                        if (t1 == null) t1 = 0L;
                        if (t2 == null) t2 = 0L;
                        return t2.compareTo(t1);
                    });

                    Map<String, List<DocumentSnapshot>> threads = new HashMap<>();
                    for (DocumentSnapshot doc : allDocs) {
                        String sender = doc.getString("sender");
                        String status = doc.getString("status");
                        if (sender == null) sender = "Unknown";
                        if ("Unread".equals(status)) totalUnreadCount++;
                        if (!threads.containsKey(sender)) threads.put(sender, new ArrayList<>());
                        threads.get(sender).add(doc);
                    }

                    if (tvMessageBadge != null) {
                        if (totalUnreadCount > 0) {
                            tvMessageBadge.setText(String.valueOf(totalUnreadCount));
                            tvMessageBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvMessageBadge.setVisibility(View.GONE);
                        }
                    }

                    if (threads.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText(isAmharic ? "የመልዕክት ሳጥንዎ ባዶ ነው።" : "Your inbox is empty.");
                        empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                        empty.setPadding(0, 10, 0, 10);
                        solverInboxList.addView(empty);
                        return;
                    }

                    for (Map.Entry<String, List<DocumentSnapshot>> entry : threads.entrySet()) {
                        String sender = entry.getKey();
                        List<DocumentSnapshot> msgs = entry.getValue();

                        int threadUnreadCount = 0;
                        for(DocumentSnapshot m : msgs) if("Unread".equals(m.getString("status"))) threadUnreadCount++;

                        LinearLayout msgCard = new LinearLayout(this);
                        msgCard.setOrientation(LinearLayout.HORIZONTAL);
                        styleFloatingCard(msgCard);
                        msgCard.setPadding(40, 40, 40, 40);
                        msgCard.setGravity(Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                        params.setMargins(0, 0, 0, 20);
                        msgCard.setLayoutParams(params);

                        TextView tvAvatar = new TextView(this);
                        tvAvatar.setText(sender.substring(0, 1).toUpperCase());
                        tvAvatar.setTextColor(Color.WHITE);
                        tvAvatar.setTextSize(18);
                        tvAvatar.setGravity(Gravity.CENTER);
                        tvAvatar.setTypeface(null, android.graphics.Typeface.BOLD);

                        GradientDrawable circle = new GradientDrawable();
                        circle.setShape(GradientDrawable.OVAL);
                        circle.setColor(ContextCompat.getColor(this, R.color.unifix_blue));
                        tvAvatar.setBackground(circle);

                        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(120, 120);
                        avatarParams.setMarginEnd(30);
                        tvAvatar.setLayoutParams(avatarParams);
                        msgCard.addView(tvAvatar);

                        LinearLayout textContainer = new LinearLayout(this);
                        textContainer.setOrientation(LinearLayout.VERTICAL);
                        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

                        TextView tvSender = new TextView(this);
                        tvSender.setText("@" + sender);
                        tvSender.setTextSize(16);
                        tvSender.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                        tvSender.setTypeface(null, threadUnreadCount > 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                        textContainer.addView(tvSender);

                        DocumentSnapshot latestMsg = msgs.get(0);
                        TextView tvSnippet = new TextView(this);
                        tvSnippet.setText(latestMsg.getString("text"));
                        tvSnippet.setTextColor(threadUnreadCount > 0 ? ContextCompat.getColor(this, R.color.unifix_blue) : ContextCompat.getColor(this, R.color.text_secondary));
                        tvSnippet.setMaxLines(1);
                        tvSnippet.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        textContainer.addView(tvSnippet);

                        msgCard.addView(textContainer);

                        if (threadUnreadCount > 0) {
                            TextView tvThreadBadge = new TextView(this);
                            tvThreadBadge.setText(String.valueOf(threadUnreadCount));
                            tvThreadBadge.setTextColor(Color.WHITE);
                            tvThreadBadge.setTextSize(12);
                            tvThreadBadge.setGravity(Gravity.CENTER);

                            GradientDrawable badgeCircle = new GradientDrawable();
                            badgeCircle.setShape(GradientDrawable.OVAL);
                            badgeCircle.setColor(Color.parseColor("#dc3545"));
                            tvThreadBadge.setBackground(badgeCircle);

                            tvThreadBadge.setLayoutParams(new LinearLayout.LayoutParams(60, 60));
                            msgCard.addView(tvThreadBadge);
                        }

                        msgCard.setOnClickListener(v -> showPrivateChatThreadPopup(sender, msgs));
                        solverInboxList.addView(msgCard);
                    }
                });
    }

    private void showComposeMessageDialog(String prefilledText) {
        db.collection("users").whereIn("role", Arrays.asList("Admin", "Solver")).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<String> userNames = new ArrayList<>();
                        List<String> usernames = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String uName = doc.getString("username");
                            if (uName != null && !uName.equals(mySolverUsername)) {
                                String r = doc.getString("role");
                                String displayR = r;
                                if(isAmharic) {
                                    if(r.equals("Admin")) displayR = "አስተዳዳሪ";
                                    else if (r.equals("Solver")) displayR = "ባለሙያ";
                                }
                                userNames.add(uName + " (" + displayR + ")");
                                usernames.add(uName);
                            }
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(isAmharic ? "➕ አዲስ የግል መልዕክት" : "➕ New Private Message");

                        LinearLayout layout = new LinearLayout(this);
                        layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setPadding(40, 40, 40, 40);
                        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));

                        Spinner spinnerUsers = new Spinner(this);
                        spinnerUsers.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, userNames));
                        styleInputBox(spinnerUsers);
                        LinearLayout.LayoutParams spParams = new LinearLayout.LayoutParams(-1, 120);
                        spParams.setMargins(0, 0, 0, 30);
                        spinnerUsers.setLayoutParams(spParams);
                        layout.addView(spinnerUsers);

                        EditText etMessage = new EditText(this);
                        etMessage.setHint(isAmharic ? "መልዕክትዎን እዚህ ይጻፉ..." : "Type your message here...");
                        styleInputBox(etMessage);
                        etMessage.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                        etMessage.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
                        etMessage.setPadding(40, 40, 40, 40);
                        etMessage.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        etMessage.setMinLines(3);
                        etMessage.setMaxLines(8);
                        etMessage.setVerticalScrollBarEnabled(true);
                        etMessage.setGravity(Gravity.TOP | Gravity.START);
                        if (prefilledText != null && !prefilledText.isEmpty()) etMessage.setText(prefilledText);
                        layout.addView(etMessage);

                        builder.setView(layout);
                        builder.setPositiveButton(isAmharic ? "መልዕክት ላክ" : "Send Message", (dialog, which) -> {
                            String targetUser = usernames.get(spinnerUsers.getSelectedItemPosition());
                            String msg = etMessage.getText().toString().trim();

                            if (!msg.isEmpty()) {
                                Map<String, Object> dm = new HashMap<>();
                                dm.put("sender", mySolverUsername);
                                dm.put("recipient", targetUser);
                                dm.put("text", msg);
                                dm.put("timestamp", System.currentTimeMillis());
                                dm.put("status", "Unread");
                                db.collection("solver_inbox").add(dm).addOnSuccessListener(a -> Toast.makeText(this, isAmharic ? "ተልኳል!" : "Sent!", Toast.LENGTH_SHORT).show());
                            }
                        });
                        builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null);
                        builder.show();
                    }
                });
    }

    private void showPrivateChatThreadPopup(String sender, List<DocumentSnapshot> msgs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView titleView = new TextView(this);
        titleView.setText((isAmharic ? "ቻት: @" : "Chat: @") + sender);
        titleView.setPadding(40, 40, 40, 40);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.WHITE);
        titleView.setBackgroundColor(ContextCompat.getColor(this, R.color.unifix_blue));
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

        for (int i = msgs.size() - 1; i >= 0; i--) {
            DocumentSnapshot m = msgs.get(i);
            String msgSender = m.getString("sender");
            boolean isMe = mySolverUsername.equals(msgSender);

            LinearLayout bubbleWrapper = new LinearLayout(this);
            bubbleWrapper.setOrientation(LinearLayout.VERTICAL);
            bubbleWrapper.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            bubbleWrapper.setGravity(isMe ? Gravity.END : Gravity.START);

            TextView bubble = new TextView(this);
            bubble.setText(m.getString("text"));
            bubble.setPadding(40, 25, 40, 25);
            bubble.setTextSize(16);

            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(40f);
            if (isMe) {
                gd.setColor(ContextCompat.getColor(this, R.color.unifix_blue));
                bubble.setTextColor(Color.WHITE);
            } else {
                gd.setColor(ContextCompat.getColor(this, R.color.input_background));
                bubble.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                bubble.setElevation(2f);
            }
            bubble.setBackground(gd);

            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
            bp.setMargins(isMe ? 150 : 0, 10, isMe ? 0 : 150, 10);
            bubble.setLayoutParams(bp);

            bubbleWrapper.addView(bubble);
            chatLayout.addView(bubbleWrapper);

            if (!isMe && "Unread".equals(m.getString("status"))) db.collection("solver_inbox").document(m.getId()).update("status", "Read");
        }

        scrollView.addView(chatLayout);
        container.addView(scrollView);

        LinearLayout replyContainer = new LinearLayout(this);
        replyContainer.setOrientation(LinearLayout.HORIZONTAL);
        replyContainer.setPadding(10, 20, 10, 10);
        replyContainer.setGravity(Gravity.CENTER_VERTICAL);

        EditText etReply = new EditText(this);
        etReply.setHint(isAmharic ? "መልዕክት..." : "Message...");
        styleInputBox(etReply);
        etReply.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        etReply.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
        etReply.setPadding(40, 30, 40, 30);
        etReply.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etReply.setMaxLines(4);

        LinearLayout.LayoutParams repParams = new LinearLayout.LayoutParams(0, -2, 1);
        repParams.setMarginEnd(20);
        etReply.setLayoutParams(repParams);

        Button btnSendReply = new Button(this);
        btnSendReply.setText("➤");
        btnSendReply.setTextSize(18);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(ContextCompat.getColor(this, R.color.unifix_blue));
        btnBg.setShape(GradientDrawable.OVAL);
        btnSendReply.setBackground(btnBg);
        btnSendReply.setTextColor(Color.WHITE);
        btnSendReply.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        makeInteractive(btnSendReply);

        replyContainer.addView(etReply);
        replyContainer.addView(btnSendReply);
        container.addView(replyContainer);

        builder.setView(container);
        builder.setNegativeButton(isAmharic ? "ዝጋ" : "Close", (dialog, which) -> loadSolverMessages());
        builder.setNeutralButton(isAmharic ? "ውይይቱን ሰርዝ" : "Delete Thread", (dialog, which) -> {
            for (DocumentSnapshot m : msgs) db.collection("solver_inbox").document(m.getId()).delete();
            Toast.makeText(this, isAmharic ? "ውይይቱ ተሰርዟል።" : "Thread deleted.", Toast.LENGTH_SHORT).show();
            loadSolverMessages();
        });

        AlertDialog dialog = builder.create();
        btnSendReply.setOnClickListener(v -> {
            String replyText = etReply.getText().toString().trim();
            if (!replyText.isEmpty()) {
                Map<String, Object> dm = new HashMap<>();
                dm.put("sender", mySolverUsername);
                dm.put("recipient", sender);
                dm.put("text", replyText);
                dm.put("timestamp", System.currentTimeMillis());
                dm.put("status", "Unread");
                db.collection("solver_inbox").add(dm).addOnSuccessListener(a -> {
                    etReply.setText("");
                    dialog.dismiss();
                    loadSolverMessages();
                });
            }
        });

        dialog.show();
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
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