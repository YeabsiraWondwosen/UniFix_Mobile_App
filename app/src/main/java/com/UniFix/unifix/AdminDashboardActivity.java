package com.UniFix.unifix;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
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
import android.widget.GridLayout;
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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class AdminDashboardActivity extends AppCompatActivity {

    boolean isAmharic = false;

    LinearLayout tabLayoutContainer;
    Button btnTabReports, btnTabUsers, btnTabMessages, btnTabAnalytics, btnSettings;
    TextView tvMessageBadge, tvWelcomeName;
    LinearLayout containerReports, containerUsers, containerMessages, containerAnalytics;

    TextView tvGlobalAlertBadge;
    List<DocumentSnapshot> activeSystemAlerts = new ArrayList<>();

    TextView tvTechCount, tvDormCount, tvAcademicCount, tvCafeteriaCount, tvOtherCount;
    TextView tvHRCount, tvHealthCount, tvSecurityCount, tvFinanceCount, tvAdminCount;

    EditText etSearchUsername;
    Spinner spinSearchCategory;
    Button btnSearchHistory;
    LinearLayout listSearchResults, listUsers;

    LinearLayout sectionManualApprovals, listManualApprovals;
    LinearLayout adminInboxList;
    Button btnToggleAddStaff;
    LinearLayout sectionHeadAdmin;
    EditText etNewStaffUser, etNewStaffName, etNewStaffPass;
    Spinner spinNewStaffRole;
    Button btnCreateStaff;

    Button btnSubTabInbox, btnSubTabSpam;
    boolean showingSpam = false;

    PieChart pieChartCategory;
    BarChart barChartStatus, barChartSolver;
    TextView tvAnalyticsTotalReports, tvAnalyticsPending, tvAnalyticsResolved, tvAnalyticsTotalUsers;
    Button btnDownloadCSV, btnDownloadPDF;

    FirebaseFirestore db;
    String loggedInUserName = "Unknown Admin";

    List<DocumentSnapshot> allUsersCache = new ArrayList<>();
    LinearLayout dynamicUserSearchLayout;
    LinearLayout roleFoldersContainer;
    LinearLayout listUsersData;

    static boolean hasShownWelcomePopup = false;

    Stack<String> tabHistory = new Stack<>();
    String currentTab = "reports";
    long backPressedTime = 0;

    private void styleFloatingCard(View v) {
        if (v != null) {
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(40f);
            shape.setColor(ContextCompat.getColor(this, R.color.input_background));
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
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        String userId = getIntent().getStringExtra("USERNAME");
        if (userId != null) loggedInUserName = userId;

        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);

        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        tabLayoutContainer = findViewById(R.id.tabLayoutContainer);
        btnTabReports = findViewById(R.id.btnTabReports);
        btnTabUsers = findViewById(R.id.btnTabUsers);
        btnTabMessages = findViewById(R.id.btnTabMessages);
        btnTabAnalytics = findViewById(R.id.btnTabAnalytics);
        tvMessageBadge = findViewById(R.id.tvMessageBadge);
        btnSettings = findViewById(R.id.btnSettings);

        tvWelcomeName.setText((isAmharic ? "መለያ: @" : "Account: @") + loggedInUserName);

        makeInteractive(btnTabReports);
        makeInteractive(btnTabUsers);
        makeInteractive(btnTabMessages);
        makeInteractive(btnTabAnalytics);
        makeInteractive(btnSettings);

        setupGlobalAlertBell();

        containerReports = findViewById(R.id.containerReports);
        containerUsers = findViewById(R.id.containerUsers);
        containerMessages = findViewById(R.id.containerMessages);
        containerAnalytics = findViewById(R.id.containerAnalytics);

        listUsers = findViewById(R.id.listUsers);

        pieChartCategory = findViewById(R.id.pieChartCategory);
        barChartStatus = findViewById(R.id.barChartStatus);
        barChartSolver = findViewById(R.id.barChartSolver);
        tvAnalyticsTotalReports = findViewById(R.id.tvAnalyticsTotalReports);
        tvAnalyticsPending = findViewById(R.id.tvAnalyticsPending);
        tvAnalyticsResolved = findViewById(R.id.tvAnalyticsResolved);
        tvAnalyticsTotalUsers = findViewById(R.id.tvAnalyticsTotalUsers);

        btnDownloadCSV = findViewById(R.id.btnDownloadCSV);
        btnDownloadPDF = findViewById(R.id.btnDownloadPDF);
        makeInteractive(btnDownloadCSV);
        makeInteractive(btnDownloadPDF);

        tvTechCount = findViewById(R.id.tvTechCount);
        tvDormCount = findViewById(R.id.tvDormCount);
        tvAcademicCount = findViewById(R.id.tvAcademicCount);
        tvCafeteriaCount = findViewById(R.id.tvCafeteriaCount);
        tvHRCount = findViewById(R.id.tvHRCount);
        tvHealthCount = findViewById(R.id.tvHealthCount);
        tvSecurityCount = findViewById(R.id.tvSecurityCount);
        tvFinanceCount = findViewById(R.id.tvFinanceCount);
        tvAdminCount = findViewById(R.id.tvAdminCount);
        tvOtherCount = findViewById(R.id.tvOtherCount);

        etSearchUsername = findViewById(R.id.etSearchUsername);
        spinSearchCategory = findViewById(R.id.spinSearchCategory);
        btnSearchHistory = findViewById(R.id.btnSearchHistory);

        styleInputBox(etSearchUsername);
        styleInputBox(spinSearchCategory);
        makeInteractive(btnSearchHistory);

        listSearchResults = findViewById(R.id.listSearchResults);
        adminInboxList = findViewById(R.id.adminInboxList);

        btnSubTabInbox = findViewById(R.id.btnSubTabInbox);
        btnSubTabSpam = findViewById(R.id.btnSubTabSpam);
        makeInteractive(btnSubTabInbox);
        makeInteractive(btnSubTabSpam);

        btnSubTabInbox.setOnClickListener(v -> switchInboxTab(false));
        btnSubTabSpam.setOnClickListener(v -> switchInboxTab(true));

        sectionManualApprovals = findViewById(R.id.sectionManualApprovals);
        listManualApprovals = findViewById(R.id.listManualApprovals);

        btnToggleAddStaff = findViewById(R.id.btnToggleAddStaff);
        makeInteractive(btnToggleAddStaff);
        sectionHeadAdmin = findViewById(R.id.sectionHeadAdmin);

        etNewStaffUser = findViewById(R.id.etNewStaffUser);
        etNewStaffName = findViewById(R.id.etNewStaffName);
        etNewStaffPass = findViewById(R.id.etNewStaffPass);
        spinNewStaffRole = findViewById(R.id.spinNewStaffRole);
        btnCreateStaff = findViewById(R.id.btnCreateStaff);

        styleInputBox(etNewStaffUser);
        styleInputBox(etNewStaffName);
        styleInputBox(etNewStaffPass);
        styleInputBox(spinNewStaffRole);
        makeInteractive(btnCreateStaff);

        btnToggleAddStaff.setOnClickListener(v -> {
            if (sectionHeadAdmin.getVisibility() == View.GONE) {
                sectionHeadAdmin.setVisibility(View.VISIBLE);
                btnToggleAddStaff.setText(isAmharic ? "➖ የሰራተኛ ፎርም ዝጋ" : "➖ Close Staff Form");
                btnToggleAddStaff.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6c757d")));
            } else {
                sectionHeadAdmin.setVisibility(View.GONE);
                btnToggleAddStaff.setText(isAmharic ? "➕ አዲስ ሰራተኛ አክል" : "➕ Add New Staff Member");
                btnToggleAddStaff.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
            }
        });

        btnTabReports.setOnClickListener(v -> switchToTab("reports", true));
        btnTabUsers.setOnClickListener(v -> switchToTab("users", true));
        btnTabMessages.setOnClickListener(v -> switchToTab("messages", true));
        btnTabAnalytics.setOnClickListener(v -> switchToTab("analytics", true));

        btnSettings.setOnClickListener(v -> showSettingsMenu());
        btnDownloadCSV.setOnClickListener(v -> generateCSV());
        btnDownloadPDF.setOnClickListener(v -> generatePDF());

        if(findViewById(R.id.cardTechnology) != null) findViewById(R.id.cardTechnology).setOnClickListener(v -> openCategoryReports("Staff ICT Manager"));
        if(findViewById(R.id.cardDormitory) != null) findViewById(R.id.cardDormitory).setOnClickListener(v -> openCategoryReports("Staff Dormitory Manager"));
        if(findViewById(R.id.cardAcademic) != null) findViewById(R.id.cardAcademic).setOnClickListener(v -> openCategoryReports("Staff Academic Resources Manager"));
        if(findViewById(R.id.cardCafeteria) != null) findViewById(R.id.cardCafeteria).setOnClickListener(v -> openCategoryReports("Staff Cafeteria Manager"));
        if(findViewById(R.id.cardHR) != null) findViewById(R.id.cardHR).setOnClickListener(v -> openCategoryReports("Staff Human Resource Manager"));
        if(findViewById(R.id.cardHealth) != null) findViewById(R.id.cardHealth).setOnClickListener(v -> openCategoryReports("Staff Health Center Manager"));
        if(findViewById(R.id.cardSecurity) != null) findViewById(R.id.cardSecurity).setOnClickListener(v -> openCategoryReports("Staff Campus Security Manager"));
        if(findViewById(R.id.cardFinance) != null) findViewById(R.id.cardFinance).setOnClickListener(v -> openCategoryReports("Staff Finance Manager"));
        if(findViewById(R.id.cardAdmin) != null) findViewById(R.id.cardAdmin).setOnClickListener(v -> openCategoryReports("Staff University Administration Manager"));
        if(findViewById(R.id.cardOther) != null) findViewById(R.id.cardOther).setOnClickListener(v -> openCategoryReports("Other"));

        makeInteractive(findViewById(R.id.cardTechnology));
        makeInteractive(findViewById(R.id.cardDormitory));
        makeInteractive(findViewById(R.id.cardAcademic));
        makeInteractive(findViewById(R.id.cardCafeteria));
        makeInteractive(findViewById(R.id.cardHR));
        makeInteractive(findViewById(R.id.cardHealth));
        makeInteractive(findViewById(R.id.cardSecurity));
        makeInteractive(findViewById(R.id.cardFinance));
        makeInteractive(findViewById(R.id.cardAdmin));
        makeInteractive(findViewById(R.id.cardOther));

        String[] searchCategories;
        if (isAmharic) {
            searchCategories = new String[]{"ሁሉም ምድቦች", "የአይሲቲ (ICT) ኃላፊ", "የመኝታ ክፍል ኃላፊ", "የአካዳሚክ ግብዓቶች ኃላፊ", "የካፌ ኃላፊ", "የሰው ኃይል ኃላፊ", "የጤና ጣቢያ ኃላፊ", "የካምፓስ ደህንነት ኃላፊ", "የፋይናንስ ኃላፊ", "የዩኒቨርሲቲ አስተዳደር", "አጠቃላይ ቴክኒሻን", "ዲን", "የትምህርት ክፍል ኃላፊ", "ሌላ"};
        } else {
            searchCategories = new String[]{"All Categories", "Staff ICT Manager", "Staff Dormitory Manager", "Staff Academic Resources Manager", "Staff Cafeteria Manager", "Staff Human Resource Manager", "Staff Health Center Manager", "Staff Campus Security Manager", "Staff Finance Manager", "Staff University Administration Manager", "Staff General Technician", "Staff Dean", "Staff Department Head", "Other"};
        }

        ArrayAdapter<String> searchCatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, searchCategories);
        spinSearchCategory.setAdapter(searchCatAdapter);

        btnSearchHistory.setOnClickListener(v -> {
            String searchQuery = etSearchUsername.getText().toString().trim().toLowerCase();
            String searchCat = spinSearchCategory.getSelectedItem().toString();
            String dbSearchCat = getEnglishCategory(searchCat);

            if (searchQuery.isEmpty() && dbSearchCat.equals("All Categories")) {
                Toast.makeText(this, isAmharic ? "እባክዎ የመፈለጊያ ቃል ወይም ምድብ ይምረጡ።" : "Enter a search term or select a category.", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSearchHistory.setText(isAmharic ? "በመፈለግ ላይ..." : "Searching...");
            btnSearchHistory.setEnabled(false);
            performGlobalSearch(searchQuery, dbSearchCat);
        });

        applyStaticTranslations();
        setupUserSearchUI();
        setupHeadAdminCreateStaffUI();

        loadDashboardStats();
        fetchAllUsersOnce();
        loadAdminMessages();
        loadPendingManualReviews();

        switchToTab("reports", false);
    }

    private void setupGlobalAlertBell() {
        Button btnBell = findViewById(R.id.btnBell);
        tvGlobalAlertBadge = findViewById(R.id.tvGlobalAlertBadge);
        View badgeContainer = findViewById(R.id.badgeContainer);

        if (btnBell != null && tvGlobalAlertBadge != null && badgeContainer != null) {
            makeInteractive(btnBell);
            btnBell.setOnClickListener(v -> showSystemAlertsDialog());

            db.collection("admin_messages")
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

                db.collection("admin_messages").document(doc.getId()).update("status", "Read");
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

        TextView tvNavTitle = findViewById(R.id.navTitle);
        if (tvNavTitle != null) tvNavTitle.setText("UniFix አስተዳዳሪ");

        btnSettings.setText("⚙️");

        btnTabReports.setText("አጠቃላይ እይታ");
        btnTabUsers.setText("ተጠቃሚዎች");
        btnTabMessages.setText("መልዕክቶች");
        btnTabAnalytics.setText("ትንታኔዎች");

        TextView tvManualApprovalsTitle = findViewById(R.id.tvManualApprovalsTitle);
        if (tvManualApprovalsTitle != null) tvManualApprovalsTitle.setText("⚠️ ማረጋገጫ የሚጠብቁ መለያዎች");

        TextView tvDeepSearchTitle = findViewById(R.id.tvDeepSearchTitle);
        if (tvDeepSearchTitle != null) tvDeepSearchTitle.setText("ጥልቅ የፍለጋ ታሪክ");

        etSearchUsername.setHint("ተጠቃሚ፣ ምድብ ወይም ችግር ይፈልጉ...");
        btnSearchHistory.setText("ፈልግ");

        TextView tvActiveReportsTitle = findViewById(R.id.tvActiveReportsTitle);
        if (tvActiveReportsTitle != null) tvActiveReportsTitle.setText("የነቁ ሪፖርቶች አጠቃላይ እይታ");

        TextView t1 = findViewById(R.id.tvCardIct); if (t1 != null) t1.setText("አይሲቲ (ICT)");
        TextView t2 = findViewById(R.id.tvCardDorm); if (t2 != null) t2.setText("መኝታ ክፍል");
        TextView t3 = findViewById(R.id.tvCardAcad); if (t3 != null) t3.setText("አካዳሚክ");
        TextView t4 = findViewById(R.id.tvCardCafe); if (t4 != null) t4.setText("ካፌ");
        TextView t5 = findViewById(R.id.tvCardHr); if (t5 != null) t5.setText("የሰው ኃይል");
        TextView t6 = findViewById(R.id.tvCardHealth); if (t6 != null) t6.setText("ጤና ጣቢያ");
        TextView t7 = findViewById(R.id.tvCardSec); if (t7 != null) t7.setText("ደህንነት");
        TextView t8 = findViewById(R.id.tvCardFin); if (t8 != null) t8.setText("ፋይናንስ");
        TextView t9 = findViewById(R.id.tvCardAdmin); if (t9 != null) t9.setText("አስተዳደር");
        TextView t10 = findViewById(R.id.tvCardOther); if (t10 != null) t10.setText("ሌላ");

        btnToggleAddStaff.setText("➕ አዲስ ሰራተኛ አክል");

        TextView tvCreateStaffTitle = findViewById(R.id.tvCreateStaffTitle);
        if (tvCreateStaffTitle != null) tvCreateStaffTitle.setText("👑 የሰራተኛ መለያ ፍጠር");

        etNewStaffUser.setHint("የተጠቃሚ ስም (ለምሳሌ admin5)");
        etNewStaffName.setHint("ሙሉ ስም");
        etNewStaffPass.setHint("የይለፍ ቃል");
        btnCreateStaff.setText("ደህንነቱ የተጠበቀ መለያ ፍጠር");

        TextView tvCommunicationTitle = findViewById(R.id.tvCommunicationTitle);
        if (tvCommunicationTitle != null) tvCommunicationTitle.setText("የመገናኛ ማዕከል");

        btnSubTabInbox.setText("ዋና መልዕክት ሳጥን");
        btnSubTabSpam.setText("አይፈለጌ / ውድቅ የተደረጉ");

        TextView tvAnalyticsTitle = findViewById(R.id.tvAnalyticsTitle);
        if (tvAnalyticsTitle != null) tvAnalyticsTitle.setText("አጠቃላይ የካምፓስ ትንታኔዎች");

        TextView tl1 = findViewById(R.id.tvLabelTotal); if (tl1 != null) tl1.setText("ጠቅላላ ሪፖርቶች");
        TextView tl2 = findViewById(R.id.tvLabelPending); if (tl2 != null) tl2.setText("በመጠባበቅ ላይ");
        TextView tl3 = findViewById(R.id.tvLabelResolved); if (tl3 != null) tl3.setText("የተፈቱ");
        TextView tl4 = findViewById(R.id.tvLabelUsers); if (tl4 != null) tl4.setText("ጠቅላላ ተጠቃሚዎች");

        TextView tc1 = findViewById(R.id.tvChart1Title); if (tc1 != null) tc1.setText("ሪፖርቶች በምድብ");
        TextView tc2 = findViewById(R.id.tvChart2Title); if (tc2 != null) tc2.setText("የሪፖርት ሁኔታ");
        TextView tc3 = findViewById(R.id.tvChart3Title); if (tc3 != null) tc3.setText("🏆 የባለሙያ አፈጻጸም (የተጠናቀቁ)");

        btnDownloadCSV.setText("📥 CSV አውርድ");
        btnDownloadPDF.setText("📥 PDF አውርድ");
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
            case "reports": return 0;
            case "users": return 1;
            case "messages": return 2;
            case "analytics": return 3;
            default: return 0;
        }
    }

    private void switchToTab(String tab, boolean addToHistory) {
        if (currentTab.equals(tab) && (containerReports.getVisibility() == View.VISIBLE || containerUsers.getVisibility() == View.VISIBLE)) {
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

        containerReports.setVisibility(View.GONE);
        containerUsers.setVisibility(View.GONE);
        containerMessages.setVisibility(View.GONE);
        containerAnalytics.setVisibility(View.GONE);

        int inactiveBg = ContextCompat.getColor(this, R.color.input_background);
        int inactiveText = ContextCompat.getColor(this, R.color.text_primary);
        btnTabReports.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
        btnTabReports.setTextColor(inactiveText);
        btnTabUsers.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
        btnTabUsers.setTextColor(inactiveText);
        btnTabMessages.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
        btnTabMessages.setTextColor(inactiveText);
        if(btnTabAnalytics.getVisibility() == View.VISIBLE) {
            btnTabAnalytics.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
            btnTabAnalytics.setTextColor(inactiveText);
        }

        int activeBg = ContextCompat.getColor(this, R.color.unifix_blue);
        int activeText = Color.WHITE;

        if (tab.equals("reports")) {
            containerReports.setVisibility(View.VISIBLE);
            btnTabReports.setBackgroundTintList(ColorStateList.valueOf(activeBg));
            btnTabReports.setTextColor(activeText);
        } else if (tab.equals("users")) {
            containerUsers.setVisibility(View.VISIBLE);
            btnTabUsers.setBackgroundTintList(ColorStateList.valueOf(activeBg));
            btnTabUsers.setTextColor(activeText);
        } else if (tab.equals("messages")) {
            containerMessages.setVisibility(View.VISIBLE);
            btnTabMessages.setBackgroundTintList(ColorStateList.valueOf(activeBg));
            btnTabMessages.setTextColor(activeText);
            loadAdminMessages();
        } else if (tab.equals("analytics")) {
            containerAnalytics.setVisibility(View.VISIBLE);
            btnTabAnalytics.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
            btnTabAnalytics.setTextColor(activeText);
            loadGlobalAnalytics();
        }
    }

    private void showSettingsMenu() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        String themeText = isNight ? (isAmharic ? "☀️ ብሩህ ገጽታ" : "☀️ Light Theme") : (isAmharic ? "🌙 ጨለማ ገጽታ" : "🌙 Dark Theme");
        String langText = isAmharic ? "🌐 English" : "🌐 አማርኛ";

        String[] options = {
                isAmharic ? "መገለጫ ያስተካክሉ" : "Edit Profile",
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

    private void showWelcomePopup(int totalTasks) {
        String title = isAmharic ? "🔔 እርምጃ ያስፈልጋል" : "🔔 Action Required";
        String msg = isAmharic ? "እንኳን ደህና መጡ, @" + loggedInUserName + "!\n\nከመመደባቸው በፊት የእርስዎን ግምገማ የሚፈልጉ " + totalTasks + " የሚጠብቁ ወይም ይግባኝ የተባሉ ሪፖርቶች አሉ።" : "Welcome back, @" + loggedInUserName + "!\n\nYou have " + totalTasks + " pending or appealed reports that REQUIRE YOUR REVIEW before assignment.";
        String btn = isAmharic ? "እንጀምር" : "Let's Go";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(btn, null)
                .setCancelable(false)
                .show();
    }

    private void showEditProfileDialog() {
        db.collection("users").document(loggedInUserName).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(isAmharic ? "የመገለጫ ቅንብሮች ⚙️" : "Profile Settings ⚙️");

                ScrollView scroll = new ScrollView(this);
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 40, 50, 40);
                scroll.addView(layout);

                TextView tvLockedUser = new TextView(this);
                tvLockedUser.setText((isAmharic ? "የተጠቃሚ ስም: @" : "Username: @") + loggedInUserName + (isAmharic ? " (የተቆለፈ)" : " (Locked)"));
                tvLockedUser.setTextColor(Color.GRAY);
                tvLockedUser.setPadding(0, 0, 0, 20);
                layout.addView(tvLockedUser);

                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
                p.setMargins(0, 0, 0, 20);

                final EditText etName = new EditText(this);
                etName.setHint(isAmharic ? "ሙሉ ስም" : "Full Name");
                etName.setText(doc.getString("fullName"));
                styleInputBox(etName);
                etName.setPadding(40, 40, 40, 40);
                etName.setLayoutParams(p);
                layout.addView(etName);

                final EditText etPhoneEdit = new EditText(this);
                etPhoneEdit.setHint(isAmharic ? "ስልክ ቁጥር" : "Phone Number");
                if (doc.getString("phone") != null) etPhoneEdit.setText(doc.getString("phone"));
                etPhoneEdit.setInputType(InputType.TYPE_CLASS_PHONE);
                styleInputBox(etPhoneEdit);
                etPhoneEdit.setPadding(40, 40, 40, 40);
                etPhoneEdit.setLayoutParams(p);
                layout.addView(etPhoneEdit);

                final EditText etNewPass = new EditText(this);
                etNewPass.setHint(isAmharic ? "አዲስ የይለፍ ቃል (የአሁኑን ለማቆየት ባዶ ይተዉት)" : "New Password (Leave blank to keep current)");
                styleInputBox(etNewPass);
                etNewPass.setPadding(40, 40, 40, 40);
                etNewPass.setLayoutParams(p);
                layout.addView(etNewPass);

                builder.setView(scroll);
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
                                Toast.makeText(this, isAmharic ? "የመገለጫ ቅንብሮች ዘምነዋል! ✅" : "Profile Settings Updated! ✅", Toast.LENGTH_LONG).show();
                                tvWelcomeName.setText((isAmharic ? "መለያ: @" : "Account: @") + loggedInUserName);
                            });
                });
                builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null);
                builder.show();
            }
        });
    }

    private void loadGlobalAnalytics() {
        db.collection("users").get().addOnSuccessListener(userSnaps -> {
            int totalUsers = userSnaps.size();
            tvAnalyticsTotalUsers.setText(String.valueOf(totalUsers));
        });

        db.collection("reports").get().addOnSuccessListener(snaps -> {
            int pending = 0, assigned = 0, finished = 0, declined = 0;
            Map<String, Integer> categoryCounts = new HashMap<>();
            Map<String, Integer> solverPerformance = new HashMap<>();

            for(DocumentSnapshot d : snaps.getDocuments()) {
                String s = d.getString("status");
                String cat = d.getString("category");
                String solver = d.getString("assignedTo");

                if("Pending".equals(s)) pending++;
                else if("Finished".equals(s) || "Completed".equals(s)) {
                    finished++;
                    if (solver != null && !solver.isEmpty()) {
                        solverPerformance.put(solver, solverPerformance.getOrDefault(solver, 0) + 1);
                    }
                }
                else if("Declined".equals(s)) declined++;
                else assigned++;

                if (cat != null) {
                    String displayCat = getTranslatedCategory(cat);
                    categoryCounts.put(displayCat, categoryCounts.getOrDefault(displayCat, 0) + 1);
                }
            }

            tvAnalyticsTotalReports.setText(String.valueOf(snaps.size()));
            tvAnalyticsPending.setText(String.valueOf(pending));
            tvAnalyticsResolved.setText(String.valueOf(finished));

            List<PieEntry> pieEntries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }

            PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
            pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            pieDataSet.setValueTextColor(Color.WHITE);
            pieDataSet.setValueTextSize(14f);

            PieData pieData = new PieData(pieDataSet);
            pieChartCategory.setData(pieData);
            pieChartCategory.getDescription().setEnabled(false);
            pieChartCategory.getLegend().setTextColor(Color.WHITE);
            pieChartCategory.setHoleColor(Color.parseColor("#111111"));
            pieChartCategory.animateY(1000);
            pieChartCategory.invalidate();

            List<BarEntry> barEntries = new ArrayList<>();
            barEntries.add(new BarEntry(0f, pending));
            barEntries.add(new BarEntry(1f, assigned));
            barEntries.add(new BarEntry(2f, finished));
            barEntries.add(new BarEntry(3f, declined));

            BarDataSet barDataSet = new BarDataSet(barEntries, "Count");
            barDataSet.setColors(new int[] {Color.parseColor("#ffc107"), Color.parseColor("#0d6efd"), Color.parseColor("#198754"), Color.parseColor("#dc3545")});
            barDataSet.setValueTextColor(Color.WHITE);
            barDataSet.setValueTextSize(14f);

            BarData barData = new BarData(barDataSet);
            barChartStatus.setData(barData);

            String[] statusLabels = isAmharic ? new String[]{"በመጠባበቅ ላይ", "የተመደበ", "የተጠናቀቀ", "ውድቅ"} : new String[]{"Pending", "Assigned", "Finished", "Declined"};
            XAxis xAxis = barChartStatus.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(statusLabels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextColor(Color.WHITE);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);

            barChartStatus.getAxisLeft().setTextColor(Color.WHITE);
            barChartStatus.getAxisRight().setEnabled(false);
            barChartStatus.getDescription().setEnabled(false);
            barChartStatus.getLegend().setTextColor(Color.WHITE);
            barChartStatus.animateY(1000);
            barChartStatus.invalidate();

            List<BarEntry> solverEntries = new ArrayList<>();
            List<String> solverNames = new ArrayList<>();
            int index = 0;
            for (Map.Entry<String, Integer> entry : solverPerformance.entrySet()) {
                solverEntries.add(new BarEntry(index, entry.getValue()));
                solverNames.add(entry.getKey());
                index++;
            }

            BarDataSet solverDataSet = new BarDataSet(solverEntries, isAmharic ? "የተጠናቀቁ ተግባራት" : "Tasks Completed");
            solverDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
            solverDataSet.setValueTextColor(Color.WHITE);
            solverDataSet.setValueTextSize(14f);

            BarData sData = new BarData(solverDataSet);
            barChartSolver.setData(sData);

            XAxis sxAxis = barChartSolver.getXAxis();
            sxAxis.setValueFormatter(new IndexAxisValueFormatter(solverNames));
            sxAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            sxAxis.setTextColor(Color.WHITE);
            sxAxis.setGranularity(1f);
            sxAxis.setDrawGridLines(false);

            barChartSolver.getAxisLeft().setTextColor(Color.WHITE);
            barChartSolver.getAxisRight().setEnabled(false);
            barChartSolver.getDescription().setEnabled(false);
            barChartSolver.getLegend().setTextColor(Color.WHITE);
            barChartSolver.animateY(1000);
            barChartSolver.invalidate();
        });
    }

    private void generateCSV() {
        db.collection("reports").get().addOnSuccessListener(snaps -> {
            StringBuilder csv = new StringBuilder("ID,Date,Category,Status,Reporter,AssignedSolver\n");
            for(DocumentSnapshot d : snaps.getDocuments()) {
                csv.append(d.getId()).append(",")
                        .append(d.getString("date")).append(",")
                        .append(d.getString("category")).append(",")
                        .append(d.getString("status")).append(",")
                        .append(d.getString("reporterUsername")).append(",")
                        .append(d.getString("assignedTo") == null ? "None" : d.getString("assignedTo")).append("\n");
            }

            try {
                File file = new File(getExternalFilesDir(null), "UniFix_Analytics.csv");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(csv.toString().getBytes());
                fos.close();
                Toast.makeText(this, isAmharic ? "CSV ፋይል ተቀምጧል: " + file.getAbsolutePath() : "CSV Saved to App Files: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, isAmharic ? "CSV ፋይል በማመንጨት ላይ ስህተት ተፈጥሯል" : "Error generating CSV", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generatePDF() {
        db.collection("reports").get().addOnSuccessListener(snaps -> {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();

            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(16);
            canvas.drawText("UniFix Performance Report", 10, 25, paint);

            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setTextSize(10);
            int y = 50;
            canvas.drawText("Total Reports: " + snaps.size(), 10, y, paint);
            y += 20;

            for (int i=0; i<Math.min(snaps.size(), 20); i++) {
                DocumentSnapshot d = snaps.getDocuments().get(i);
                canvas.drawText(d.getString("date") + " | " + d.getString("category") + " | " + d.getString("status"), 10, y, paint);
                y += 15;
            }

            document.finishPage(page);

            try {
                File file = new File(getExternalFilesDir(null), "UniFix_Report.pdf");
                document.writeTo(new FileOutputStream(file));
                Toast.makeText(this, isAmharic ? "PDF ፋይል ተቀምጧል: " + file.getAbsolutePath() : "PDF Saved to App Files: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, isAmharic ? "PDF በማመንጨት ላይ ስህተት ተፈጥሯል" : "Error generating PDF", Toast.LENGTH_SHORT).show();
            }
            document.close();
        });
    }

    private void setupUserSearchUI() {
        dynamicUserSearchLayout = new LinearLayout(this);
        dynamicUserSearchLayout.setOrientation(LinearLayout.VERTICAL);
        dynamicUserSearchLayout.setPadding(0, 0, 0, 10);

        TextView tvHeader = new TextView(this);
        tvHeader.setText(isAmharic ? "የተጠቃሚዎች ማውጫ" : "User Directory");
        tvHeader.setTextColor(ContextCompat.getColor(this, R.color.unifix_blue));
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setPadding(0,0,0,10);

        EditText etSearch = new EditText(this);
        etSearch.setHint(isAmharic ? "ስም፣ መታወቂያ ወይም የሥራ ድርሻ ይፈልጉ..." : "Search name, ID, or job title...");
        styleInputBox(etSearch);
        etSearch.setPadding(40, 30, 40, 30);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(-1, -2);
        etParams.setMargins(0,0,0,20);
        etSearch.setLayoutParams(etParams);

        Spinner spinRole = new Spinner(this);
        String[] roles = isAmharic ? new String[]{"ሁሉም", "ተማሪ", "መምህር", "ባለሙያ", "አስተዳዳሪ"} : new String[]{"All Roles", "Student", "Teacher", "Solver", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinRole.setAdapter(adapter);
        spinRole.setPadding(20, 20, 20, 20);
        styleInputBox(spinRole);

        dynamicUserSearchLayout.addView(tvHeader);
        dynamicUserSearchLayout.addView(etSearch);
        dynamicUserSearchLayout.addView(spinRole);
        listUsers.addView(dynamicUserSearchLayout);

        roleFoldersContainer = new LinearLayout(this);
        roleFoldersContainer.setOrientation(LinearLayout.VERTICAL);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(true);
        grid.setAlignmentMode(GridLayout.ALIGN_MARGINS);

        grid.addView(createFolderCard(isAmharic ? "ተማሪዎች 🎓" : "Students 🎓", 1, spinRole));
        grid.addView(createFolderCard(isAmharic ? "መምህራን 📚" : "Teachers 📚", 2, spinRole));
        grid.addView(createFolderCard(isAmharic ? "ባለሙያዎች 🔧" : "Staff / Solvers 🔧", 3, spinRole));
        grid.addView(createFolderCard(isAmharic ? "አስተዳዳሪዎች 👑" : "Admins 👑", 4, spinRole));

        roleFoldersContainer.addView(grid);
        listUsers.addView(roleFoldersContainer);

        listUsersData = new LinearLayout(this);
        listUsersData.setOrientation(LinearLayout.VERTICAL);
        listUsers.addView(listUsersData);

        TextWatcher filterWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(etSearch.getText().toString(), spinRole.getSelectedItem().toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etSearch.addTextChangedListener(filterWatcher);

        spinRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterUsers(etSearch.getText().toString(), roles[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private LinearLayout createFolderCard(String title, int spinnerIndex, Spinner spinRole) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30, 40, 30, 40);
        styleFloatingCard(card);
        makeInteractive(card);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        params.setMargins(10, 10, 10, 10);
        card.setLayoutParams(params);

        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor(ContextCompat.getColor(this, R.color.unifix_blue));
        text.setTextSize(14f);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(text);
        card.setOnClickListener(v -> spinRole.setSelection(spinnerIndex));
        return card;
    }

    private void fetchAllUsersOnce() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allUsersCache = task.getResult().getDocuments();
                filterUsers("", isAmharic ? "ሁሉም" : "All Roles");
            }
        });
    }

    private void filterUsers(String query, String roleFilter) {
        listUsersData.removeAllViews();
        String lowerQuery = query.toLowerCase().trim();

        if (lowerQuery.isEmpty() && (roleFilter.equals("All Roles") || roleFilter.equals("ሁሉም"))) {
            roleFoldersContainer.setVisibility(View.VISIBLE);
            return;
        }

        roleFoldersContainer.setVisibility(View.GONE);
        int count = 0;

        String dbRoleFilter = roleFilter;
        if(isAmharic) {
            if(roleFilter.equals("ተማሪ")) dbRoleFilter = "Student";
            else if(roleFilter.equals("መምህር")) dbRoleFilter = "Teacher";
            else if(roleFilter.equals("ባለሙያ")) dbRoleFilter = "Solver";
            else if(roleFilter.equals("አስተዳዳሪ")) dbRoleFilter = "Admin";
        }

        for (DocumentSnapshot doc : allUsersCache) {
            String role = doc.getString("role");
            String username = doc.getString("username");
            String actualId = doc.getString("id");
            String name = doc.getString("fullName");
            String dept = doc.getString("dept");

            if (role == null) role = "Unknown";
            if (username == null) username = "";
            if (actualId == null) actualId = "";
            if (name == null) name = "";
            if (dept == null) dept = "";

            if (!dbRoleFilter.equals("All Roles") && !dbRoleFilter.equals("ሁሉም") && !role.equals(dbRoleFilter)) continue;

            if (lowerQuery.isEmpty() || username.toLowerCase().contains(lowerQuery) || name.toLowerCase().contains(lowerQuery) || dept.toLowerCase().contains(lowerQuery) || actualId.toLowerCase().contains(lowerQuery)) {
                addUserCardToUI(doc);
                count++;
            }
        }
        if (count == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(isAmharic ? "ምንም ተጠቃሚ አልተገኘም።" : "No users found.");
            tvEmpty.setPadding(0, 30, 0, 0);
            listUsersData.addView(tvEmpty);
        }
    }


    private void addUserCardToUI(DocumentSnapshot doc) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 40, 40, 40);
        styleFloatingCard(card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);

        String user = doc.getString("username");
        String role = doc.getString("role");
        String jobTitle = doc.getString("dept");
        Long warningsRaw = doc.getLong("warnings");
        int warnings = (warningsRaw != null) ? warningsRaw.intValue() : 0;
        Boolean isBanned = doc.getBoolean("isBanned");

        String translatedRole = role;
        if(isAmharic) {
            if("Student".equals(role)) translatedRole = "ተማሪ";
            else if("Teacher".equals(role)) translatedRole = "መምህር";
            else if("Solver".equals(role)) translatedRole = "ባለሙያ";
            else if("Admin".equals(role)) translatedRole = "አስተዳዳሪ";
        }

        String displayTitle = user + " (" + translatedRole + ")";
        if (jobTitle != null && !jobTitle.isEmpty()) {
            displayTitle += "\n" + (isAmharic ? "ኃላፊነት: " : "Title: ") + getTranslatedCategory(jobTitle);
        }

        if (isBanned != null && isBanned) {
            displayTitle += "\n" + (isAmharic ? "[ታግዷል]" : "[BANNED]");
            GradientDrawable redShape = new GradientDrawable();
            redShape.setCornerRadius(40f);
            redShape.setColor(Color.parseColor("#FFE8E8"));
            card.setBackground(redShape);
        }

        TextView tvTitle = new TextView(this);
        tvTitle.setText(displayTitle);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvTitle.setTypeface(null, isBanned != null && isBanned ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        card.addView(tvTitle);

        boolean isHeadAdmin = "dbu_admin1".equals(loggedInUserName) || "dbu_admin2".equals(loggedInUserName);
        boolean isTargetHeadAdmin = "dbu_admin1".equals(doc.getId()) || "dbu_admin2".equals(doc.getId());

        if (isBanned != null && isBanned && isHeadAdmin) {
            String reason = doc.getString("lastWarningReason");
            TextView tvBanReason = new TextView(this);
            tvBanReason.setText((isAmharic ? "የታገደበት ምክንያት: " : "Ban Reason: ") + (reason != null ? reason : "Unknown"));
            tvBanReason.setTextColor(Color.parseColor("#dc3545"));
            tvBanReason.setPadding(0, 0, 0, 10);
            card.addView(tvBanReason);

            LinearLayout reviewLayout = new LinearLayout(this);
            reviewLayout.setOrientation(LinearLayout.HORIZONTAL);

            Button btnRestore = new Button(this);
            btnRestore.setText(isAmharic ? "እገዳ አንሳ" : "RESTORE");
            btnRestore.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
            btnRestore.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams pR = new LinearLayout.LayoutParams(0, -2, 1);
            pR.setMargins(0, 0, 8, 0);
            btnRestore.setLayoutParams(pR);
            makeInteractive(btnRestore);
            btnRestore.setOnClickListener(v -> {
                db.collection("users").document(doc.getId())
                        .update("isBanned", false, "warnings", 0, "lastWarningReason", isAmharic ? "በዋና አስተዳዳሪ ተመልሷል" : "Restored by Head Admin.")
                        .addOnSuccessListener(a -> fetchAllUsersOnce());
            });

            Button btnDelete = new Button(this);
            btnDelete.setText(isAmharic ? "በቋሚነት ሰርዝ" : "PERMANENT DELETE");
            btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
            btnDelete.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams pD = new LinearLayout.LayoutParams(0, -2, 1);
            btnDelete.setLayoutParams(pD);
            makeInteractive(btnDelete);
            btnDelete.setOnClickListener(v -> confirmDeleteUser(doc.getId(), role));

            reviewLayout.addView(btnRestore);
            reviewLayout.addView(btnDelete);
            card.addView(reviewLayout);

        } else if ("Admin".equals(role) && !isHeadAdmin) {
            TextView tvProtected = new TextView(this);
            tvProtected.setText(isAmharic ? "🛡️ የተጠበቀ የስርዓት አስተዳዳሪ" : "🛡️ Protected System Admin");
            tvProtected.setTextColor(Color.parseColor("#198754"));
            tvProtected.setTypeface(null, android.graphics.Typeface.ITALIC);
            tvProtected.setPadding(0, 10, 0, 0);
            card.addView(tvProtected);
        } else if ("Admin".equals(role) && isTargetHeadAdmin) {
            TextView tvProtected = new TextView(this);
            tvProtected.setText(isAmharic ? "👑 ዋና አስተዳዳሪ" : "👑 Head Admin");
            tvProtected.setTextColor(Color.parseColor("#0d6efd"));
            tvProtected.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);
            tvProtected.setPadding(0, 10, 0, 0);
            card.addView(tvProtected);
        } else {
            TextView tvWarn = new TextView(this);
            tvWarn.setText((isAmharic ? "ማስጠንቀቂያዎች: " : "Warnings: ") + warnings + "/3");
            tvWarn.setPadding(0, 8, 0, 8);
            if (warnings > 0) tvWarn.setTextColor(Color.parseColor("#dc3545"));
            card.addView(tvWarn);

            Button btnManage = new Button(this);
            btnManage.setText(isAmharic ? "⚙️ ተጠቃሚን አስተዳድር" : "⚙️ Manage User");
            btnManage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6c757d")));
            btnManage.setTextColor(Color.WHITE);
            makeInteractive(btnManage);
            btnManage.setOnClickListener(v -> showManageUserDialog(doc, warnings));
            card.addView(btnManage);
        }
        listUsersData.addView(card);
    }

    private void showManageUserDialog(DocumentSnapshot doc, int currentWarnings) {
        String uid = doc.getId();
        String currentName = doc.getString("fullName");
        String currentPhone = doc.getString("phone");
        String currentRole = doc.getString("role");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isAmharic ? "ተጠቃሚን ያስተዳድሩ: @" + uid : "Manage User: @" + uid);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));

        final EditText etName = new EditText(this);
        etName.setHint(isAmharic ? "ሙሉ ስም" : "Full Name");
        if (currentName != null) etName.setText(currentName);
        styleInputBox(etName);
        etName.setPadding(40, 40, 40, 40);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 20);
        etName.setLayoutParams(lp);
        layout.addView(etName);

        final EditText etPhoneEdit = new EditText(this);
        etPhoneEdit.setHint(isAmharic ? "ስልክ ቁጥር" : "Phone Number");
        if (currentPhone != null) etPhoneEdit.setText(currentPhone);
        etPhoneEdit.setInputType(InputType.TYPE_CLASS_PHONE);
        styleInputBox(etPhoneEdit);
        etPhoneEdit.setPadding(40, 40, 40, 40);
        etPhoneEdit.setLayoutParams(lp);
        layout.addView(etPhoneEdit);

        Button btnSave = new Button(this);
        btnSave.setText(isAmharic ? "💾 መረጃ አስቀምጥ" : "💾 Save Info");
        btnSave.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0d6efd")));
        btnSave.setTextColor(Color.WHITE);
        btnSave.setLayoutParams(lp);
        makeInteractive(btnSave);
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhoneEdit.getText().toString().trim();

            if (!phone.isEmpty() && !phone.matches("^((09|07)\\d{8})|(\\+251(9|7)\\d{8})$")) {
                Toast.makeText(this, isAmharic ? "ስልክ ቁጥሩ ትክክል አይደለም። (09.., 07.., +2519.., ወይንም +2517.. ይጠቀሙ)" : "Invalid phone. Use 09.., 07.., +2519.., or +2517.. (8 digits)", Toast.LENGTH_LONG).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            if (!name.isEmpty()) updates.put("fullName", name);
            if (!phone.isEmpty()) updates.put("phone", phone);

            db.collection("users").document(uid).update(updates).addOnSuccessListener(a -> {
                Toast.makeText(this, isAmharic ? "መረጃ ተዘምኗል!" : "Info Updated!", Toast.LENGTH_SHORT).show();
                fetchAllUsersOnce();
            });
        });
        layout.addView(btnSave);


        if (currentWarnings > 0) {
            Button btnRetract = new Button(this);
            btnRetract.setText(isAmharic ? "➖ ማስጠንቀቂያ አንሳ (-1)" : "➖ Retract Warning (-1)");
            btnRetract.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
            btnRetract.setTextColor(Color.WHITE);
            btnRetract.setLayoutParams(lp);
            makeInteractive(btnRetract);
            btnRetract.setOnClickListener(v -> {
                db.collection("users").document(uid).update("warnings", currentWarnings - 1).addOnSuccessListener(a -> {
                    Toast.makeText(this, isAmharic ? "ማስጠንቀቂያ ተነስቷል" : "Warning Retracted", Toast.LENGTH_SHORT).show();
                    fetchAllUsersOnce();
                });
            });
            layout.addView(btnRetract);
        }

        Button btnWarn = new Button(this);
        btnWarn.setText(isAmharic ? "⚠️ ማስጠንቀቂያ ስጥ (+1)" : "⚠️ Issue Warning (+1)");
        btnWarn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffc107")));
        btnWarn.setTextColor(Color.BLACK);
        btnWarn.setLayoutParams(lp);
        makeInteractive(btnWarn);
        btnWarn.setOnClickListener(v -> issueWarning(uid, currentWarnings, uid));
        layout.addView(btnWarn);

        Button btnDelete = new Button(this);
        btnDelete.setText(isAmharic ? "🗑️ ተጠቃሚውን ሰርዝ" : "🗑️ Delete User");
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setLayoutParams(lp);
        makeInteractive(btnDelete);
        btnDelete.setOnClickListener(v -> confirmDeleteUser(uid, currentRole));
        layout.addView(btnDelete);

        scroll.addView(layout);
        builder.setView(scroll);
        builder.setNegativeButton(isAmharic ? "ዝጋ" : "Close", null);
        builder.show();
    }

    private void issueWarning(String documentId, int currentWarnings, String username) {
        int newWarnings = currentWarnings + 1;
        boolean shouldBan = newWarnings >= 3;
        db.collection("users").document(documentId)
                .update("warnings", newWarnings, "isBanned", shouldBan, "lastWarningReason", "Admin issued a manual warning.")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, (isAmharic ? "ማስጠንቀቂያ ተሰጥቷል። ጠቅላላ: " : "Warning Issued. Total: ") + newWarnings, Toast.LENGTH_SHORT).show();

                    if (shouldBan) {
                        notifyHeadAdminsOfBan(username, "Manual warning hit 3 strikes.");
                    }
                    fetchAllUsersOnce();
                });
    }

    private void notifyHeadAdminsOfBan(String bannedUser, String reason) {
        String alertText = "🚨 ACCOUNT BANNED: @" + bannedUser + " has been banned. Reason: " + reason + ". Please review their account in the Users tab to Restore or Delete permanently.";

        Map<String, Object> msgToAdmin1 = new HashMap<>();
        msgToAdmin1.put("sender", "System Alerts");
        msgToAdmin1.put("recipient", "dbu_admin1");
        msgToAdmin1.put("text", alertText);
        msgToAdmin1.put("timestamp", System.currentTimeMillis());
        msgToAdmin1.put("status", "Unread");
        db.collection("admin_messages").add(msgToAdmin1);

        Map<String, Object> msgToAdmin2 = new HashMap<>();
        msgToAdmin2.put("sender", "System Alerts");
        msgToAdmin2.put("recipient", "dbu_admin2");
        msgToAdmin2.put("text", alertText);
        msgToAdmin2.put("timestamp", System.currentTimeMillis());
        msgToAdmin2.put("status", "Unread");
        db.collection("admin_messages").add(msgToAdmin2);
    }

    private void confirmDeleteUser(String documentId, String role) {
        new AlertDialog.Builder(this)
                .setTitle(isAmharic ? role + "ን ይሰርዙ?" : "Delete " + role + "?")
                .setMessage(isAmharic ? "እርግጠኛ ነዎት ይህን መለያ በቋሚነት መሰረዝ ይፈልጋሉ? ይህ እርምጃ ሊቀለበስ አይችልም።" : "Are you sure you want to permanently delete this account? This action cannot be undone.")
                .setPositiveButton(isAmharic ? "ሰርዝ" : "DELETE", (dialog, which) -> {
                    db.collection("users").document(documentId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, isAmharic ? "መለያው በቋሚነት ተሰርዟል።" : "Account permanently deleted.", Toast.LENGTH_SHORT).show();
                                fetchAllUsersOnce();
                            });
                })
                .setNegativeButton(isAmharic ? "አቋርጥ" : "Cancel", null)
                .show();
    }

    private void setupHeadAdminCreateStaffUI() {
        String[] specificJobTitles = {
                "Staff Human Resource Manager", "Staff General Technician", "Staff Dean",
                "Staff ICT Manager", "Staff Cafeteria Manager", "Staff Finance Manager",
                "Staff Department Head", "Staff Health Center Manager",
                "Staff University Administration Manager", "Staff Dormitory Manager",
                "Staff Campus Security Manager", "Staff Academic Resources Manager", "Sub-Admin"
        };

        String[] displayTitles = new String[specificJobTitles.length];
        for(int i=0; i<specificJobTitles.length; i++) {
            displayTitles[i] = getTranslatedCategory(specificJobTitles[i]);
            if (specificJobTitles[i].equals("Sub-Admin")) displayTitles[i] = isAmharic ? "ምክትል አስተዳዳሪ" : "Sub-Admin";
        }

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayTitles);
        spinNewStaffRole.setAdapter(roleAdapter);

        btnCreateStaff.setOnClickListener(v -> {
            String u = etNewStaffUser.getText().toString().trim().toLowerCase();
            String n = etNewStaffName.getText().toString().trim();
            String p = etNewStaffPass.getText().toString().trim();

            int selectedIndex = spinNewStaffRole.getSelectedItemPosition();
            String dbSelectedTitle = specificJobTitles[selectedIndex];

            if (u.isEmpty() || n.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, isAmharic ? "እባክዎ ሁሉንም መስኮች ይሙሉ" : "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnCreateStaff.setText(isAmharic ? "በመፍጠር ላይ..." : "Creating...");
            btnCreateStaff.setEnabled(false);

            Map<String, Object> newUser = new HashMap<>();
            newUser.put("username", u);
            newUser.put("fullName", n);
            newUser.put("password", p);

            newUser.put("role", dbSelectedTitle.equals("Sub-Admin") ? "Admin" : "Solver");
            newUser.put("dept", dbSelectedTitle);

            newUser.put("isBanned", false);
            newUser.put("warnings", 0);
            newUser.put("status", "Active");
            newUser.put("isPendingReview", false);

            db.collection("users").document(u).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Toast.makeText(this, isAmharic ? "ስህተት፡ የተጠቃሚ ስም አስቀድሞ በስራ ላይ ነው!" : "Error: Username already exists!", Toast.LENGTH_SHORT).show();
                    btnCreateStaff.setText(isAmharic ? "ደህንነቱ የተጠበቀ መለያ ፍጠር" : "Create Secure Account");
                    btnCreateStaff.setEnabled(true);
                } else {
                    db.collection("users").document(u).set(newUser).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, isAmharic ? "የሰራተኛ መለያ ተፈጥሯል!" : "Staff Account Created!", Toast.LENGTH_SHORT).show();
                        etNewStaffUser.setText("");
                        etNewStaffName.setText("");
                        etNewStaffPass.setText("");
                        btnCreateStaff.setText(isAmharic ? "ደህንነቱ የተጠበቀ መለያ ፍጠር" : "Create Secure Account");
                        btnCreateStaff.setEnabled(true);
                        sectionHeadAdmin.setVisibility(View.GONE);
                        btnToggleAddStaff.setText(isAmharic ? "➕ አዲስ ሰራተኛ አክል" : "➕ Add New Staff Member");
                        btnToggleAddStaff.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
                        fetchAllUsersOnce();
                    });
                }
            });
        });
    }

    private void loadDashboardStats() {
        db.collection("reports").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int tech = 0, dorm = 0, academic = 0, cafe = 0;
                        int hr = 0, health = 0, security = 0, finance = 0, admin = 0, other = 0;
                        int totalActive = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("status");
                            String assignedBy = document.getString("assignedByAdmin");
                            boolean canManage = "Pending".equals(status) || loggedInUserName.equals(assignedBy);

                            if (canManage && ("Pending".equals(status) || "Assigned".equals(status) || "Appealed".equals(status))) {
                                totalActive++;
                                String category = document.getString("category");
                                if (category != null) {
                                    switch (category) {
                                        case "Staff ICT Manager": tech++; break;
                                        case "Staff Dormitory Manager": dorm++; break;
                                        case "Staff Academic Resources Manager": academic++; break;
                                        case "Staff Cafeteria Manager": cafe++; break;
                                        case "Staff Human Resource Manager": hr++; break;
                                        case "Staff Health Center Manager": health++; break;
                                        case "Staff Campus Security Manager": security++; break;
                                        case "Staff Finance Manager": finance++; break;
                                        case "Staff University Administration Manager": admin++; break;
                                        default: other++; break;
                                    }
                                } else {
                                    other++;
                                }
                            }
                        }

                        if(tvTechCount != null) tvTechCount.setText(String.valueOf(tech));
                        if(tvDormCount != null) tvDormCount.setText(String.valueOf(dorm));
                        if(tvAcademicCount != null) tvAcademicCount.setText(String.valueOf(academic));
                        if(tvCafeteriaCount != null) tvCafeteriaCount.setText(String.valueOf(cafe));
                        if(tvHRCount != null) tvHRCount.setText(String.valueOf(hr));
                        if(tvHealthCount != null) tvHealthCount.setText(String.valueOf(health));
                        if(tvSecurityCount != null) tvSecurityCount.setText(String.valueOf(security));
                        if(tvFinanceCount != null) tvFinanceCount.setText(String.valueOf(finance));
                        if(tvAdminCount != null) tvAdminCount.setText(String.valueOf(admin));
                        if(tvOtherCount != null) tvOtherCount.setText(String.valueOf(other));

                        if (!hasShownWelcomePopup && totalActive > 0) {
                            showWelcomePopup(totalActive);
                            hasShownWelcomePopup = true;
                        } else if (!hasShownWelcomePopup) {
                            hasShownWelcomePopup = true;
                        }
                    }
                });
    }

    private void loadPendingManualReviews() {
        db.collection("users").whereEqualTo("isPendingReview", true).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                listManualApprovals.removeAllViews();
                if (task.getResult().isEmpty()) {
                    sectionManualApprovals.setVisibility(View.GONE);
                    return;
                }
                sectionManualApprovals.setVisibility(View.VISIBLE);

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String username = doc.getString("username");
                    String role = doc.getString("role");
                    String actualId = doc.getString("id");

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(40, 40, 40, 40);
                    styleFloatingCard(card);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                    params.setMargins(0, 0, 0, 16);
                    card.setLayoutParams(params);

                    TextView tvInfo = new TextView(this);
                    tvInfo.setText((isAmharic ? "ሚና: " : "Role: ") + role + "\n" + (isAmharic ? "የተጠየቀው የተጠቃሚ ስም: " : "Requested Username: ") + username + "\n" + (isAmharic ? "የቀረበው መታወቂያ: " : "Claimed ID: ") + actualId);
                    tvInfo.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    tvInfo.setPadding(0, 0, 0, 16);
                    card.addView(tvInfo);

                    LinearLayout btnLayout = new LinearLayout(this);
                    btnLayout.setOrientation(LinearLayout.HORIZONTAL);

                    Button btnApprove = new Button(this);
                    btnApprove.setText(isAmharic ? "አጽድቅ" : "Approve");
                    btnApprove.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
                    btnApprove.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams btnParams1 = new LinearLayout.LayoutParams(0, -2, 1);
                    btnParams1.setMargins(0, 0, 8, 0);
                    btnApprove.setLayoutParams(btnParams1);
                    makeInteractive(btnApprove);
                    btnApprove.setOnClickListener(v -> resolveManualReview(doc.getId(), true));

                    Button btnReject = new Button(this);
                    btnReject.setText(isAmharic ? "ውድቅ አድርግ" : "Reject");
                    btnReject.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
                    btnReject.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(0, -2, 1);
                    btnParams2.setMargins(8, 0, 0, 0);
                    btnReject.setLayoutParams(btnParams2);
                    makeInteractive(btnReject);
                    btnReject.setOnClickListener(v -> resolveManualReview(doc.getId(), false));

                    btnLayout.addView(btnApprove);
                    btnLayout.addView(btnReject);
                    card.addView(btnLayout);
                    listManualApprovals.addView(card);
                }
            }
        });
    }

    private void resolveManualReview(String userId, boolean approve) {
        if (approve) {
            db.collection("users").document(userId)
                    .update("isPendingReview", false, "status", "Active")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, isAmharic ? "መለያው ጸድቋል ✅" : "Account Approved ✅", Toast.LENGTH_SHORT).show();
                        sendInAppNotification(userId, isAmharic ? "መለያዎ ጸድቋል" : "Account Approved",
                                isAmharic ? "እንኳን ደስ አለዎት! መለያዎ በዋና አስተዳዳሪ ተረጋግጦ ጸድቋል። አሁን መግባት ይችላሉ።"
                                        : "Congratulations! Your account has been manually verified and approved by the Head Admin. You can now log in.");
                        loadPendingManualReviews();
                        fetchAllUsersOnce();
                    });
        } else {
            db.collection("users").document(userId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, isAmharic ? "መለያው ውድቅ ተደርጓል ❌" : "Account Rejected ❌", Toast.LENGTH_SHORT).show();
                        loadPendingManualReviews();
                    });
        }
    }

    private void openCategoryReports(String targetCategory) {
        db.collection("reports").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listSearchResults.removeAllViews();

                        TextView header = new TextView(this);
                        String displayTarget = getTranslatedCategory(targetCategory);
                        header.setText((isAmharic ? "የነቁ ሪፖርቶች (" : "Active ") + displayTarget + (isAmharic ? "):" : " Reports:"));
                        header.setTextSize(18);
                        header.setTextColor(ContextCompat.getColor(this, R.color.unifix_blue));
                        header.setPadding(0, 20, 0, 10);
                        header.setTypeface(null, android.graphics.Typeface.BOLD);
                        listSearchResults.addView(header);

                        long currentTime = System.currentTimeMillis();
                        long fortyEightHours = 48L * 60L * 60L * 1000L;
                        int activeCount = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            final String reportId = document.getId();
                            String status = document.getString("status");
                            String assignedBy = document.getString("assignedByAdmin");
                            boolean canManage = "Pending".equals(status) || loggedInUserName.equals(assignedBy);

                            if (!canManage) continue;

                            String rawCat = document.getString("category");
                            final String docCat = (rawCat == null) ? "Other" : rawCat;

                            boolean isMatch = false;
                            if ("Other".equals(targetCategory)) {
                                if (!"Staff ICT Manager".equals(docCat) && !"Staff Dormitory Manager".equals(docCat) &&
                                        !"Staff Academic Resources Manager".equals(docCat) && !"Staff Cafeteria Manager".equals(docCat) &&
                                        !"Staff Human Resource Manager".equals(docCat) && !"Staff Health Center Manager".equals(docCat) &&
                                        !"Staff Campus Security Manager".equals(docCat) && !"Staff Finance Manager".equals(docCat) &&
                                        !"Staff University Administration Manager".equals(docCat)) {
                                    isMatch = true;
                                }
                            } else {
                                isMatch = targetCategory.equals(docCat);
                            }

                            if (!isMatch) continue;

                            if ("Assigned".equals(status) || "Pending".equals(status) || "Appealed".equals(status)) {
                                activeCount++;
                                String desc = document.getString("description");
                                final String currentSolver = document.getString("assignedTo");
                                Long assignedTime = document.getLong("assignedTimestamp");
                                String repName = document.getString("reporterFullName");
                                final String repUser = document.getString("reporterUsername");

                                String repPhone = document.getString("reporterPhone");
                                if(repPhone == null || repPhone.isEmpty()) repPhone = isAmharic ? "አልቀረበም" : "Not provided";

                                if (repName == null) repName = "Unknown User";
                                if (assignedTime == null) assignedTime = currentTime;
                                boolean isOverdue = (currentTime - assignedTime) > fortyEightHours;

                                LinearLayout card = new LinearLayout(this);
                                card.setOrientation(LinearLayout.VERTICAL);
                                card.setPadding(40, 40, 40, 40);
                                styleFloatingCard(card);

                                if ("Appealed".equals(status) || isOverdue) {
                                    GradientDrawable redShape = new GradientDrawable();
                                    redShape.setCornerRadius(40f);
                                    redShape.setColor(Color.parseColor("#FFE8E8"));
                                    card.setBackground(redShape);
                                }

                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                                params.setMargins(0, 0, 0, 16);
                                card.setLayoutParams(params);

                                TextView tvReporter = new TextView(this);
                                tvReporter.setText((isAmharic ? "ሪፖርት አድራጊ: " : "Reported by: ") + repName + " (@" + repUser + ")");
                                tvReporter.setTextColor(Color.parseColor("#198754"));
                                tvReporter.setTypeface(null, android.graphics.Typeface.BOLD);
                                card.addView(tvReporter);

                                StringBuilder extraDetails = new StringBuilder();
                                Map<String, Object> specifics = (Map<String, Object>) document.get("specificDetails");
                                if (specifics != null && !specifics.isEmpty()) {
                                    extraDetails.append(isAmharic ? "\n\n📍 የተወሰኑ ዝርዝሮች:\n" : "\n\n📍 Specific Details:\n");
                                    for (Map.Entry<String, Object> entry : specifics.entrySet()) {
                                        String key = entry.getKey();
                                        key = key.substring(0, 1).toUpperCase() + key.substring(1);
                                        key = key.replaceAll("([A-Z])", " $1").trim();
                                        extraDetails.append("• ").append(key).append(": ").append(entry.getValue()).append("\n");
                                    }
                                }

                                TextView tvInfo = new TextView(this);
                                tvInfo.setText((isAmharic ? "የአሁኑ ባለሙያ: " : "Current Solver: ") + (currentSolver != null ? currentSolver : (isAmharic ? "የለም" : "None"))
                                        + "\n\n" + (isAmharic ? "የችግሩ ዝርዝር:" : "Issue Details:") + "\n" + desc
                                        + extraDetails.toString()
                                        + "\n\n📞 " + (isAmharic ? "ስልክ: " : "Phone: ") + repPhone);
                                tvInfo.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                                tvInfo.setPadding(0, 8, 0, 0);
                                tvInfo.setVisibility(View.GONE);

                                ImageView ivAttachment = new ImageView(this);
                                String imageUrl = document.getString("imageUrl");
                                boolean hasImage = imageUrl != null && !imageUrl.isEmpty();

                                if (hasImage) {
                                    LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT, 600);
                                    ivParams.setMargins(0, 20, 0, 0);
                                    ivAttachment.setLayoutParams(ivParams);
                                    ivAttachment.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    ivAttachment.setVisibility(View.GONE);

                                    ivAttachment.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                                    ivAttachment.setClipToOutline(true);
                                    GradientDrawable imgShape = new GradientDrawable();
                                    imgShape.setCornerRadius(20f);
                                    ivAttachment.setBackground(imgShape);

                                    Glide.with(this).load(imageUrl).into(ivAttachment);
                                }

                                TextView tvTapToExpand = new TextView(this);
                                tvTapToExpand.setText(isAmharic ? "ዝርዝሮችን ለማየት ይጫኑ ▼" : "Tap to view details ▼");
                                tvTapToExpand.setTextColor(Color.parseColor("#888888"));
                                tvTapToExpand.setTextSize(12);
                                tvTapToExpand.setPadding(0, 10, 0, 0);

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

                                Button btnGroupChat = new Button(this);
                                btnGroupChat.setText(isAmharic ? "💬 የትኬት ግሩፕ ቻት ክፈት" : "💬 Open Ticket Group Chat");
                                btnGroupChat.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6f42c1")));
                                btnGroupChat.setTextColor(Color.WHITE);
                                LinearLayout.LayoutParams gcParams = new LinearLayout.LayoutParams(-1, -2);
                                gcParams.setMargins(0, 10, 0, 10);
                                btnGroupChat.setLayoutParams(gcParams);
                                makeInteractive(btnGroupChat);
                                btnGroupChat.setOnClickListener(v -> showTicketGroupChat(reportId, getTranslatedCategory(docCat)));
                                card.addView(btnGroupChat);

                                if ("Appealed".equals(status)) {
                                    TextView tvWarn = new TextView(this);
                                    String rawReason = document.getString("appealReason");
                                    final String appealReason = (rawReason != null) ? rawReason : (isAmharic ? "ምንም ምክንያት አልቀረበም።" : "No reason provided.");

                                    tvWarn.setText((isAmharic ? "⚠️ ባለሙያ ይግባኝ ጠይቋል\nምክንያት: " : "⚠️ SOLVER APPEALED\nReason: ") + appealReason);
                                    tvWarn.setTextColor(Color.parseColor("#dc3545"));
                                    tvWarn.setTypeface(null, android.graphics.Typeface.BOLD);
                                    tvWarn.setPadding(0, 8, 0, 8);
                                    card.addView(tvWarn);

                                    LinearLayout actionLayout = new LinearLayout(this);
                                    actionLayout.setOrientation(LinearLayout.HORIZONTAL);

                                    Button btnReassign = new Button(this);
                                    btnReassign.setText(isAmharic ? "በድጋሚ መድብ" : "REASSIGN");
                                    btnReassign.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
                                    btnReassign.setTextColor(Color.WHITE);
                                    LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
                                    p1.setMargins(0, 0, 10, 0);
                                    btnReassign.setLayoutParams(p1);
                                    makeInteractive(btnReassign);
                                    btnReassign.setOnClickListener(v -> showReassignDialog(reportId, docCat, currentSolver));

                                    Button btnRejectWarn = new Button(this);
                                    btnRejectWarn.setText(isAmharic ? "ውድቅ አድርግና አስጠንቅቅ" : "REJECT & WARN");
                                    btnRejectWarn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
                                    btnRejectWarn.setTextColor(Color.WHITE);
                                    LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1);
                                    btnRejectWarn.setLayoutParams(p2);
                                    makeInteractive(btnRejectWarn);
                                    btnRejectWarn.setOnClickListener(v -> rejectAppealAndWarnSolver(reportId, currentSolver, docCat, appealReason));

                                    actionLayout.addView(btnReassign);
                                    actionLayout.addView(btnRejectWarn);
                                    card.addView(actionLayout);

                                } else if ("Pending".equals(status)) {
                                    LinearLayout reviewLayout = new LinearLayout(this);
                                    reviewLayout.setOrientation(LinearLayout.HORIZONTAL);
                                    reviewLayout.setPadding(0, 10, 0, 0);

                                    Button btnApprove = new Button(this);
                                    btnApprove.setText(isAmharic ? "ባለሙያ መድብ" : "ASSIGN EXPERT");
                                    btnApprove.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
                                    btnApprove.setTextColor(Color.WHITE);
                                    LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
                                    p1.setMargins(0, 0, 10, 0);
                                    btnApprove.setLayoutParams(p1);
                                    makeInteractive(btnApprove);
                                    btnApprove.setOnClickListener(v -> runRoundRobinAlgorithm(reportId, docCat));

                                    Button btnDecline = new Button(this);
                                    btnDecline.setText(isAmharic ? "አትቀበልና አስጠንቅቅ" : "DECLINE & WARN");
                                    btnDecline.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
                                    btnDecline.setTextColor(Color.WHITE);
                                    LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1);
                                    btnDecline.setLayoutParams(p2);
                                    makeInteractive(btnDecline);
                                    btnDecline.setOnClickListener(v -> declineAndWarnReporter(reportId, repUser, docCat));

                                    reviewLayout.addView(btnApprove);
                                    reviewLayout.addView(btnDecline);
                                    card.addView(reviewLayout);

                                } else if (isOverdue) {
                                    Button btnReassign = new Button(this);
                                    btnReassign.setText(isAmharic ? "በአስገዳጅ ሁኔታ ተግባሩን በድጋሚ መድብ" : "FORCE REASSIGN TASK");
                                    btnReassign.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
                                    btnReassign.setTextColor(Color.WHITE);
                                    makeInteractive(btnReassign);
                                    btnReassign.setOnClickListener(v -> showReassignDialog(reportId, docCat, currentSolver));
                                    card.addView(btnReassign);
                                }

                                listSearchResults.addView(card);
                            }
                        }

                        if (activeCount == 0) {
                            TextView empty = new TextView(this);
                            empty.setText(isAmharic ? "ሁሉም ነገር ንጹህ ነው! እዚህ ምንም ንቁ ችግሮች የሉም። 🎉" : "All clear! No active issues here. 🎉");
                            listSearchResults.addView(empty);
                        }
                    }
                });
    }

    private void declineAndWarnReporter(String reportId, String reporterUsername, String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isAmharic ? "ሪፖርቱን ውድቅ አድርገው ተጠቃሚውን ያስጠንቅቁ" : "Decline Report & Warn User");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(isAmharic ? "ምክንያት ያቅርቡ። ተጠቃሚው የ1 ጊዜ ማስጠንቀቂያ ይደርሰዋል እና ይህ ሪፖርት ወደ አይፈለጌ መልዕክት ሳጥን ይወሰዳል።" : "Provide a reason. The user will receive 1 strike warning and this report will be moved to the Spam Inbox.");
        tvLabel.setPadding(0, 0, 0, 20);
        layout.addView(tvLabel);

        final EditText inputReason = new EditText(this);
        inputReason.setHint(isAmharic ? "ውድቅ የተደረገበት ምክንያት..." : "Reason for declining...");
        styleInputBox(inputReason);
        inputReason.setPadding(40, 40, 40, 40);
        inputReason.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputReason.setMinLines(3);
        inputReason.setMaxLines(8);
        inputReason.setVerticalScrollBarEnabled(true);
        inputReason.setGravity(Gravity.TOP | Gravity.START);
        layout.addView(inputReason);

        builder.setView(layout);

        builder.setPositiveButton(isAmharic ? "ውድቅ አድርግና አስጠንቅቅ" : "Decline & Warn", (dialog, which) -> {
            String reason = inputReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, isAmharic ? "ምክንያት ማቅረብ አለብዎት።" : "You must provide a reason.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "Declined");
            updates.put("declineReason", reason);
            updates.put("resolvedTimestamp", System.currentTimeMillis());

            db.collection("reports").document(reportId).update(updates).addOnSuccessListener(aVoid -> {
                db.collection("users").document(reporterUsername).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long warningsRaw = doc.getLong("warnings");
                        int currentWarnings = (warningsRaw != null) ? warningsRaw.intValue() : 0;
                        int newWarnings = currentWarnings + 1;
                        boolean shouldBan = newWarnings >= 3;

                        db.collection("users").document(reporterUsername)
                                .update("warnings", newWarnings, "isBanned", shouldBan, "lastWarningReason", "False/Spam Report: " + reason)
                                .addOnSuccessListener(v -> {
                                    sendInAppNotification(reporterUsername, isAmharic ? "ሪፖርት ውድቅ ተደርጓል - ማስጠንቀቂያ ተሰጥቷል" : "Report Declined - WARNING ISSUED",
                                            isAmharic ? "የእርስዎ " + getTranslatedCategory(category) + " ሪፖርት በአስተዳዳሪ ውድቅ ተደርጓል። ምክንያት: " + reason + ". አሁን " + newWarnings + " ማስጠንቀቂያዎች አሉዎት። 3 ማስጠንቀቂያዎች = እገዳ።"
                                                    : "Your " + category + " report was declined by Admin. Reason: " + reason + ". You now have " + newWarnings + " strike(s). 3 strikes = Ban.");
                                    Toast.makeText(this, isAmharic ? "ሪፖርት ውድቅ ተደርጓል። ወደ አይፈለጌ ሳጥን ተወስዷል።" : "Report Declined. Moved to Spam Inbox.", Toast.LENGTH_LONG).show();

                                    openCategoryReports(category);
                                    loadDashboardStats();
                                    fetchAllUsersOnce();
                                });
                    }
                });
            });
        });
        builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null);
        builder.show();
    }

    private void rejectAppealAndWarnSolver(String reportId, String solverUsername, String category, String reason) {
        new AlertDialog.Builder(this)
                .setTitle(isAmharic ? "ይግባኙን ውድቅ አድርገው ባለሙያውን ያስጠንቅቁ" : "Reject Appeal & Warn Solver")
                .setMessage(isAmharic ? "ይህ የ @" + solverUsername + "ን ይግባኝ ውድቅ በማድረግ ስራውን እንዲሰሩ ያስገድዳቸዋል፣ እንዲሁም መደበኛ ማስጠንቀቂያ ይሰጣቸዋል። (3 ማስጠንቀቂያዎች = አውቶማቲክ እገዳ)። ይቀጥሉ?" : "This will reject @" + solverUsername + "'s appeal, force them to do the task, and issue a formal warning. (3 Warnings = Automatic Ban). Proceed?")
                .setPositiveButton(isAmharic ? "ማስጠንቀቂያ ስጥ" : "Issue Warning", (dialog, which) -> {

                    db.collection("users").document(solverUsername).get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Long warningsRaw = doc.getLong("warnings");
                            int currentWarnings = (warningsRaw != null) ? warningsRaw.intValue() : 0;
                            int newWarnings = currentWarnings + 1;
                            boolean shouldBan = newWarnings >= 3;

                            db.collection("users").document(solverUsername)
                                    .update("warnings", newWarnings, "isBanned", shouldBan, "lastWarningReason", "Admin rejected appeal: " + reason)
                                    .addOnSuccessListener(aVoid -> {
                                        String msg = (isAmharic ? "ማስጠንቀቂያ ለ " : "Warning issued to ") + solverUsername + (isAmharic ? " ተሰጥቷል። ጠቅላላ: " : ". Total: ") + newWarnings;
                                        if (shouldBan) {
                                            msg += isAmharic ? " [ታግዷል]" : " [BANNED]";
                                            notifyHeadAdminsOfBan(solverUsername, "Rejected appeal + 3 strikes reached.");
                                        }
                                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                                        fetchAllUsersOnce();
                                    });
                        }
                    });

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "Assigned");
                    updates.put("appealReason", null);

                    db.collection("reports").document(reportId).update(updates)
                            .addOnSuccessListener(aVoid -> {
                                openCategoryReports(category);
                                loadDashboardStats();
                            });
                })
                .setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null)
                .show();
    }

    private void performGlobalSearch(String query, String filterCategory) {
        db.collection("reports").get()
                .addOnCompleteListener(task -> {
                    btnSearchHistory.setText(isAmharic ? "ፈልግ" : "Search");
                    btnSearchHistory.setEnabled(true);

                    if (task.isSuccessful()) {
                        listSearchResults.removeAllViews();
                        int count = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String assignedBy = document.getString("assignedByAdmin");
                            String status = document.getString("status");
                            boolean canManage = "Pending".equals(status) || loggedInUserName.equals(assignedBy);
                            if (!canManage) continue;

                            String cat = document.getString("category");
                            String desc = document.getString("description");
                            String date = document.getString("date");
                            String repUser = document.getString("reporterUsername");
                            String repName = document.getString("reporterFullName");
                            String urgency = document.getString("urgency");
                            Long assignedTime = document.getLong("assignedTimestamp");

                            if(cat == null) cat = "";
                            if(desc == null) desc = "";
                            if(repUser == null) repUser = "";
                            if(repName == null) repName = "";

                            if (!filterCategory.equals("All Categories") && !cat.equals(filterCategory)) {
                                continue;
                            }

                            if (query.isEmpty() || repUser.toLowerCase().contains(query) || cat.toLowerCase().contains(query) || desc.toLowerCase().contains(query) || repName.toLowerCase().contains(query)) {
                                count++;
                                LinearLayout card = new LinearLayout(this);
                                card.setOrientation(LinearLayout.VERTICAL);
                                card.setPadding(40, 40, 40, 40);
                                styleFloatingCard(card);

                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                                params.setMargins(0, 0, 0, 16);
                                card.setLayoutParams(params);

                                TextView tvTitle = new TextView(this);
                                tvTitle.setText(getTranslatedCategory(cat) + (isAmharic ? " ችግር | " : " Issue | ") + date);
                                tvTitle.setTextSize(16);
                                tvTitle.setTextColor(Color.parseColor("#0d6efd"));
                                tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

                                TextView tvReporter = new TextView(this);
                                tvReporter.setText((isAmharic ? "ሪፖርት አድራጊ: " : "Reported by: ") + (repName.isEmpty() ? (isAmharic ? "ያልታወቀ ተጠቃሚ" : "Unknown User") : repName));
                                tvReporter.setTextColor(Color.parseColor("#198754"));

                                TextView tvStatus = new TextView(this);
                                tvStatus.setText((isAmharic ? "ሁኔታ: " : "Status: ") + status);
                                tvStatus.setTextColor("Completed".equals(status) ? Color.parseColor("#28a745") : Color.parseColor("#FFB300"));

                                String repPhone = document.getString("reporterPhone");
                                if(repPhone == null || repPhone.isEmpty()) repPhone = isAmharic ? "አልቀረበም" : "Not provided";

                                StringBuilder extraDetails = new StringBuilder();
                                Map<String, Object> specifics = (Map<String, Object>) document.get("specificDetails");
                                if (specifics != null && !specifics.isEmpty()) {
                                    extraDetails.append(isAmharic ? "\n\n📍 የተወሰኑ ዝርዝሮች:\n" : "\n\n📍 Specific Details:\n");
                                    for (Map.Entry<String, Object> entry : specifics.entrySet()) {
                                        String key = entry.getKey();
                                        key = key.substring(0, 1).toUpperCase() + key.substring(1);
                                        key = key.replaceAll("([A-Z])", " $1").trim();
                                        extraDetails.append("• ").append(key).append(": ").append(entry.getValue()).append("\n");
                                    }
                                }

                                TextView tvDesc = new TextView(this);
                                tvDesc.setText("\n" + (isAmharic ? "መግለጫ:\n" : "Description:\n") + desc
                                        + extraDetails.toString()
                                        + "\n\n📞 " + (isAmharic ? "ስልክ: " : "Phone: ") + repPhone);
                                tvDesc.setTextColor(Color.parseColor("#333333"));
                                tvDesc.setVisibility(View.GONE);

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

                                    ivAttachment.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                                    ivAttachment.setClipToOutline(true);
                                    GradientDrawable imgShape = new GradientDrawable();
                                    imgShape.setCornerRadius(20f);
                                    ivAttachment.setBackground(imgShape);

                                    Glide.with(this).load(imageUrl).into(ivAttachment);
                                }

                                TextView tvTapToExpand = new TextView(this);
                                tvTapToExpand.setText(isAmharic ? "ዝርዝሮችን ለማየት ይጫኑ ▼" : "Tap to view details ▼");
                                tvTapToExpand.setTextColor(Color.parseColor("#888888"));
                                tvTapToExpand.setTextSize(12);

                                card.setOnClickListener(v -> {
                                    if (tvDesc.getVisibility() == View.GONE) {
                                        tvDesc.setVisibility(View.VISIBLE);
                                        if (hasImage) ivAttachment.setVisibility(View.VISIBLE);
                                        tvTapToExpand.setText(isAmharic ? "ለማጠፍ ይጫኑ ▲" : "Tap to collapse ▲");
                                    } else {
                                        tvDesc.setVisibility(View.GONE);
                                        if (hasImage) ivAttachment.setVisibility(View.GONE);
                                        tvTapToExpand.setText(isAmharic ? "ዝርዝሮችን ለማየት ይጫኑ ▼" : "Tap to view details ▼");
                                    }
                                });

                                card.addView(tvTitle);
                                card.addView(tvReporter);
                                card.addView(tvStatus);
                                card.addView(tvDesc);

                                if (assignedTime != null && ("Assigned".equals(status) || "In Progress".equals(status))) {
                                    long duration = 48L * 60 * 60 * 1000;
                                    if ("Urgent".equals(urgency)) duration = 1L * 60 * 60 * 1000;
                                    else if ("High".equals(urgency)) duration = 6L * 60 * 60 * 1000;
                                    else if ("Medium".equals(urgency)) duration = 12L * 60 * 60 * 1000;
                                    else if ("Low".equals(urgency)) duration = 24L * 60 * 60 * 1000;

                                    long timeRemaining = (assignedTime + duration) - System.currentTimeMillis();
                                    TextView tvDeadline = new TextView(this);
                                    tvDeadline.setPadding(0, 10, 0, 10);

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

                                if (hasImage) card.addView(ivAttachment);
                                card.addView(tvTapToExpand);

                                listSearchResults.addView(card);
                            }
                        }

                        if (count == 0) {
                            TextView empty = new TextView(this);
                            if (query.isEmpty()) empty.setText(isAmharic ? "በዚህ ምድብ ምንም ውጤት አልተገኘም: " + getTranslatedCategory(filterCategory) : "No active results found in category: " + filterCategory);
                            else empty.setText(isAmharic ? "ለ '" + query + "' ምንም ውጤት አልተገኘም።" : "No results found for '" + query + "'.");
                            empty.setTextColor(Color.parseColor("#dc3545"));
                            listSearchResults.addView(empty);
                        } else {
                            Toast.makeText(this, (isAmharic ? count + " ተዛማጅ ውጤቶች ተገኝተዋል" : "Found " + count + " matches."), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, isAmharic ? "ፍለጋው አልተሳካም። የኢንተርኔት ግንኙነትዎን ያረጋግጡ።" : "Search failed. Check connection.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void runRoundRobinAlgorithm(String documentId, String category) {
        db.collection("users")
                .whereEqualTo("role", "Solver")
                .whereEqualTo("dept", category)
                .whereEqualTo("isBanned", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        calculateWorkloadAndAssign(documentId, category, task.getResult().getDocuments());
                    } else {
                        db.collection("users")
                                .whereEqualTo("role", "Solver")
                                .whereEqualTo("dept", "Staff General Technician")
                                .whereEqualTo("isBanned", false)
                                .get()
                                .addOnCompleteListener(fallbackTask -> {
                                    if (fallbackTask.isSuccessful() && !fallbackTask.getResult().isEmpty()) {
                                        calculateWorkloadAndAssign(documentId, category, fallbackTask.getResult().getDocuments());
                                    } else {
                                        Toast.makeText(this, "CRITICAL: No Solvers found for " + category, Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }

    private void calculateWorkloadAndAssign(String documentId, String category, List<DocumentSnapshot> availableSolvers) {
        db.collection("reports")
                .whereIn("status", Arrays.asList("Assigned", "In Progress"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Integer> solverWorkload = new HashMap<>();
                        for (DocumentSnapshot solver : availableSolvers) solverWorkload.put(solver.getString("username"), 0);

                        for (QueryDocumentSnapshot report : task.getResult()) {
                            String assignedTo = report.getString("assignedTo");
                            if (assignedTo != null && solverWorkload.containsKey(assignedTo)) {
                                solverWorkload.put(assignedTo, solverWorkload.get(assignedTo) + 1);
                            }
                        }

                        String selectedSolver = null;
                        int minimumTasks = Integer.MAX_VALUE;
                        for (Map.Entry<String, Integer> entry : solverWorkload.entrySet()) {
                            if (entry.getValue() < minimumTasks) {
                                minimumTasks = entry.getValue();
                                selectedSolver = entry.getKey();
                            }
                        }
                        if (selectedSolver != null) executeAssignment(documentId, selectedSolver, category);
                    }
                });
    }

    private void executeAssignment(String documentId, String solverUsername, String category) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Assigned");
        updates.put("assignedTo", solverUsername);
        updates.put("assignedByAdmin", loggedInUserName);
        updates.put("assignedTimestamp", System.currentTimeMillis());
        updates.put("appealReason", null);

        db.collection("reports").document(documentId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, (isAmharic ? "✅ ተግባር በራስ-ሰር ለ " : "✅ Task Auto-Assigned to ") + solverUsername + (isAmharic ? " ተመድቧል" : ""), Toast.LENGTH_SHORT).show();

                    sendInAppNotification(solverUsername, isAmharic ? "አዲስ የUniFix ተግባር ተመድቧል" : "New UniFix Task Assigned",
                            isAmharic ? "በ @" + loggedInUserName + " አዲስ የ " + getTranslatedCategory(category) + " ችግር ተመድቦልዎታል። እባክዎ ዳሽቦርድዎን ያረጋግጡ።"
                                    : "You have been assigned a new " + category + " issue by @" + loggedInUserName + ". Please check your active dashboard.");

                    db.collection("reports").document(documentId).get().addOnSuccessListener(doc -> {
                        String reporter = doc.getString("reporterUsername");
                        if (reporter != null) {
                            sendInAppNotification(reporter, isAmharic ? "የችግር ማሻሻያ፡ ተመድቧል" : "Issue Update: Assigned",
                                    isAmharic ? "መልካም ዜና! የእርስዎ የ " + getTranslatedCategory(category) + " ችግር ለባለሙያ @" + solverUsername + " ተመድቧል።"
                                            : "Good news! Your " + category + " issue has been assigned to expert @" + solverUsername + ".");
                        }
                    });

                    String refreshCategory = category;
                    if (!"Technology".equals(category) && !"Dormitory".equals(category) && !"Academic".equals(category) && !"Cafeteria".equals(category) && !"Teaching Materials".equals(category) && !"Classroom Mgmt".equals(category) && !"Work Environment".equals(category)) refreshCategory = "Other";
                    openCategoryReports(refreshCategory);
                    loadDashboardStats();
                });
    }

    private void showReassignDialog(String documentId, String category, String oldSolverUsername) {
        db.collection("users")
                .whereEqualTo("role", "Solver")
                .whereEqualTo("isBanned", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<String> solverNames = new ArrayList<>();
                        List<String> solverUsernames = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String sUser = doc.getString("username");
                            String label = doc.getString("fullName") + " (" + getTranslatedCategory(doc.getString("dept")) + ")";
                            if (sUser != null && sUser.equals(oldSolverUsername)) label += isAmharic ? " ⚠️ (ይግባኝ ብሏል - አይምረጡ)" : " ⚠️ (APPEALED - DO NOT SELECT)";
                            solverNames.add(label);
                            solverUsernames.add(sUser);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, solverNames);
                        Spinner spinner = new Spinner(this);
                        spinner.setAdapter(adapter);
                        spinner.setPadding(40, 40, 40, 40);

                        new AlertDialog.Builder(this)
                                .setTitle(isAmharic ? "ተግባር በድጋሚ መድብ 🚨" : "Reassign Task 🚨")
                                .setMessage(isAmharic ? "ይህን ችግር የሚረከብ አዲስ ባለሙያ ይምረጡ:" : "Select a NEW Solver to take over this issue:")
                                .setView(spinner)
                                .setPositiveButton(isAmharic ? "አሁኑኑ መድብ" : "Reassign Now", (dialog, which) -> {
                                    int selectedPosition = spinner.getSelectedItemPosition();
                                    forceReassignToSolver(documentId, solverUsernames.get(selectedPosition), category);
                                })
                                .setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null)
                                .show();
                    }
                });
    }

    private void forceReassignToSolver(String documentId, String solverUsername, String category) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Assigned");
        updates.put("assignedTo", solverUsername);
        updates.put("assignedByAdmin", loggedInUserName);
        updates.put("assignedTimestamp", System.currentTimeMillis());
        updates.put("appealReason", null);

        db.collection("reports").document(documentId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, (isAmharic ? "ተግባሩ ለ " : "Task reassigned to ") + solverUsername + (isAmharic ? " በድጋሚ ተመድቧል" : ""), Toast.LENGTH_SHORT).show();

                    sendInAppNotification(solverUsername, isAmharic ? "አስቸኳይ፡ በድጋሚ የተመደበ ተግባር" : "URGENT: Reassigned Task",
                            isAmharic ? "አንድ አስቸኳይ የ " + getTranslatedCategory(category) + " ችግር በአስገዳጅ ሁኔታ ለእርስዎ ተመድቧል። እባክዎ ወዲያውኑ መፍትሄ ይስጡት።"
                                    : "An urgent " + category + " issue has been forcefully reassigned to you. Please address it immediately.");

                    String refreshCategory = category;
                    if (!"Technology".equals(category) && !"Dormitory".equals(category) && !"Academic".equals(category) && !"Cafeteria".equals(category) && !"Teaching Materials".equals(category) && !"Classroom Mgmt".equals(category) && !"Work Environment".equals(category)) refreshCategory = "Other";
                    openCategoryReports(refreshCategory);
                    loadDashboardStats();
                });
    }

    private void switchInboxTab(boolean showSpamTab) {
        showingSpam = showSpamTab;
        if (showSpamTab) {
            btnSubTabSpam.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#dc3545")));
            btnSubTabSpam.setTextColor(Color.WHITE);
            btnSubTabInbox.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F3F5")));
            btnSubTabInbox.setTextColor(Color.BLACK);
            loadSpamInbox();
        } else {
            btnSubTabInbox.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0d6efd")));
            btnSubTabInbox.setTextColor(Color.WHITE);
            btnSubTabSpam.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F3F5")));
            btnSubTabSpam.setTextColor(Color.BLACK);
            loadAdminMessages();
        }
    }

    private void loadAdminMessages() {
        if (showingSpam) return;

        db.collection("admin_messages").whereEqualTo("recipient", loggedInUserName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    adminInboxList.removeAllViews();

                    Button btnCompose = new Button(this);
                    btnCompose.setText(isAmharic ? "➕ አዲስ መልዕክት ጻፍ" : "➕ Compose New Message");
                    btnCompose.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#198754")));
                    btnCompose.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams composeParams = new LinearLayout.LayoutParams(-1, -2);
                    composeParams.setMargins(0, 0, 0, 30);
                    btnCompose.setLayoutParams(composeParams);
                    btnCompose.setPadding(0, 30, 0, 30);
                    makeInteractive(btnCompose);
                    btnCompose.setOnClickListener(v -> showComposeMessageDialog());
                    adminInboxList.addView(btnCompose);

                    int totalUnreadCount = 0;
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

                    if (totalUnreadCount > 0) {
                        tvMessageBadge.setText(String.valueOf(totalUnreadCount));
                        tvMessageBadge.setVisibility(View.VISIBLE);
                    } else tvMessageBadge.setVisibility(View.GONE);

                    if (threads.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText(isAmharic ? "የመልዕክት ሳጥንዎ ባዶ ነው።" : "Your inbox is empty.");
                        empty.setTextColor(Color.parseColor("#888888"));
                        empty.setPadding(0, 10, 0, 10);
                        adminInboxList.addView(empty);
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
                        circle.setColor(Color.parseColor("#0d6efd"));
                        tvAvatar.setBackground(circle);

                        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(120, 120);
                        avatarParams.setMarginEnd(30);
                        tvAvatar.setLayoutParams(avatarParams);
                        msgCard.addView(tvAvatar);

                        LinearLayout textContainer = new LinearLayout(this);
                        textContainer.setOrientation(LinearLayout.VERTICAL);
                        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

                        TextView tvSender = new TextView(this);
                        tvSender.setText(sender);
                        tvSender.setTextSize(16);
                        tvSender.setTextColor(Color.BLACK);
                        tvSender.setTypeface(null, threadUnreadCount > 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                        textContainer.addView(tvSender);

                        DocumentSnapshot latestMsg = msgs.get(0);
                        TextView tvSnippet = new TextView(this);
                        tvSnippet.setText(latestMsg.getString("text"));
                        tvSnippet.setTextColor(threadUnreadCount > 0 ? Color.parseColor("#0d6efd") : Color.parseColor("#6c757d"));
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

                        msgCard.setOnClickListener(v -> showChatThreadPopup(sender, msgs));
                        adminInboxList.addView(msgCard);
                    }
                });
    }

    private void loadSpamInbox() {
        if (!showingSpam) return;

        db.collection("reports")
                .whereEqualTo("status", "Declined")
                .whereEqualTo("assignedByAdmin", loggedInUserName)
                .get()
                .addOnSuccessListener(snaps -> {
                    adminInboxList.removeAllViews();

                    if (snaps.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText(isAmharic ? "ምንም አይፈለጌ ወይም ውድቅ የተደረጉ ሪፖርቶች የሉም።" : "No Spam or Declined reports.");
                        empty.setTextColor(Color.parseColor("#888888"));
                        empty.setPadding(0, 10, 0, 10);
                        adminInboxList.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        String reporter = doc.getString("reporterUsername");
                        String cat = doc.getString("category");
                        String reason = doc.getString("declineReason");

                        LinearLayout card = new LinearLayout(this);
                        card.setOrientation(LinearLayout.VERTICAL);
                        styleFloatingCard(card);
                        card.setPadding(40, 40, 40, 40);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                        params.setMargins(0, 0, 0, 20);
                        card.setLayoutParams(params);

                        TextView tvTitle = new TextView(this);
                        tvTitle.setText((isAmharic ? "ውድቅ የተደረገ ሪፖርት - " : "Declined Report - ") + getTranslatedCategory(cat));
                        tvTitle.setTextColor(Color.parseColor("#dc3545"));
                        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                        card.addView(tvTitle);

                        TextView tvDetails = new TextView(this);
                        tvDetails.setText((isAmharic ? "ሪፖርት አድራጊ: @" : "Reported by: @") + reporter + "\n" + (isAmharic ? "ምክንያትዎ: " : "Your Reason: ") + reason);
                        tvDetails.setTextColor(Color.BLACK);
                        tvDetails.setPadding(0, 10, 0, 10);
                        card.addView(tvDetails);

                        adminInboxList.addView(card);
                    }
                });
    }

    private void showComposeMessageDialog() {
        db.collection("users")
                .whereIn("role", Arrays.asList("Solver", "Admin"))
                .whereEqualTo("isBanned", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<String> targetNames = new ArrayList<>();
                        List<String> targetUsernames = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String uName = doc.getString("username");
                            if (uName != null && !uName.equals(loggedInUserName)) {
                                String r = doc.getString("role");
                                String displayR = r;
                                if(isAmharic) {
                                    if(r.equals("Admin")) displayR = "አስተዳዳሪ";
                                    else if(r.equals("Solver")) displayR = "ባለሙያ";
                                }
                                String fName = doc.getString("fullName") != null ? doc.getString("fullName") : displayR;
                                targetNames.add(fName + " (@" + uName + ") - " + displayR);
                                targetUsernames.add(uName);
                            }
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(isAmharic ? "➕ አዲስ መልዕክት ለሰራተኛ" : "➕ New Message to Staff");
                        LinearLayout layout = new LinearLayout(this);
                        layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setPadding(40, 40, 40, 40);

                        TextView tvLabel = new TextView(this);
                        tvLabel.setText(isAmharic ? "የሰራተኛ አባል ይምረጡ:" : "Select Staff Member:");
                        tvLabel.setTextColor(Color.parseColor("#333333"));
                        tvLabel.setPadding(0, 0, 0, 10);
                        layout.addView(tvLabel);

                        Spinner spinnerSolvers = new Spinner(this);
                        spinnerSolvers.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, targetNames));
                        styleInputBox(spinnerSolvers);
                        LinearLayout.LayoutParams spParams = new LinearLayout.LayoutParams(-1, 120);
                        spParams.setMargins(0, 0, 0, 30);
                        spinnerSolvers.setLayoutParams(spParams);
                        layout.addView(spinnerSolvers);

                        EditText etMessage = new EditText(this);
                        etMessage.setHint(isAmharic ? "መልዕክትዎን እዚህ ይጻፉ..." : "Type your message here...");
                        styleInputBox(etMessage);
                        etMessage.setPadding(40, 40, 40, 40);
                        etMessage.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        etMessage.setMinLines(3);
                        etMessage.setMaxLines(8);
                        etMessage.setVerticalScrollBarEnabled(true);
                        etMessage.setGravity(Gravity.TOP | Gravity.START);
                        layout.addView(etMessage);

                        builder.setView(layout);
                        builder.setPositiveButton(isAmharic ? "መልዕክት ላክ" : "Send Message", (dialog, which) -> {
                            String targetUsername = targetUsernames.get(spinnerSolvers.getSelectedItemPosition());
                            String msg = etMessage.getText().toString().trim();
                            if (!msg.isEmpty()) {
                                Map<String, Object> reply = new HashMap<>();
                                reply.put("sender", loggedInUserName);
                                reply.put("recipient", targetUsername);
                                reply.put("text", msg);
                                reply.put("timestamp", System.currentTimeMillis());
                                reply.put("status", "Unread");

                                db.collection("users").document(targetUsername).get().addOnSuccessListener(doc -> {
                                    if ("Admin".equals(doc.getString("role"))) db.collection("admin_messages").add(reply);
                                    else db.collection("solver_inbox").add(reply);
                                    Toast.makeText(this, isAmharic ? "መልዕክቱ ተልኳል!" : "Message Sent!", Toast.LENGTH_SHORT).show();
                                });
                            }
                            else Toast.makeText(this, isAmharic ? "መልዕክት ባዶ መሆን አይችልም።" : "Message cannot be empty.", Toast.LENGTH_SHORT).show();
                        });
                        builder.setNegativeButton(isAmharic ? "ሰርዝ" : "Cancel", null);
                        builder.show();
                    } else Toast.makeText(this, isAmharic ? "ምንም ንቁ የሰራተኛ አባል አልተገኘም።" : "No active staff found.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showChatThreadPopup(String sender, List<DocumentSnapshot> msgs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView titleView = new TextView(this);
        titleView.setText((isAmharic ? "ውይይት: @" : "Chat: @") + sender);
        titleView.setPadding(40, 40, 40, 40);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.WHITE);
        titleView.setBackgroundColor(Color.parseColor("#0d6efd"));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        builder.setCustomTitle(titleView);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.parseColor("#F0F2F5"));
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
            boolean isMe = loggedInUserName.equals(msgSender);

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
            if (isMe) { gd.setColor(Color.parseColor("#0d6efd")); bubble.setTextColor(Color.WHITE); }
            else { gd.setColor(Color.parseColor("#FFFFFF")); bubble.setTextColor(Color.BLACK); bubble.setElevation(2f); }
            bubble.setBackground(gd);

            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
            bp.setMargins(isMe ? 150 : 0, 10, isMe ? 0 : 150, 10);
            bubble.setLayoutParams(bp);

            bubbleWrapper.addView(bubble);
            chatLayout.addView(bubbleWrapper);

            if (!isMe && "Unread".equals(m.getString("status"))) db.collection("admin_messages").document(m.getId()).update("status", "Read");
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
        btnBg.setColor(Color.parseColor("#0d6efd"));
        btnBg.setShape(GradientDrawable.OVAL);
        btnSendReply.setBackground(btnBg);
        btnSendReply.setTextColor(Color.WHITE);
        btnSendReply.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        makeInteractive(btnSendReply);

        replyContainer.addView(etReply);
        replyContainer.addView(btnSendReply);
        container.addView(replyContainer);

        builder.setView(container);
        builder.setNegativeButton(isAmharic ? "ዝጋ" : "Close", (dialog, which) -> loadAdminMessages());
        builder.setNeutralButton(isAmharic ? "ውይይቱን ሰርዝ" : "Delete Thread", (dialog, which) -> {
            for (DocumentSnapshot m : msgs) db.collection("admin_messages").document(m.getId()).delete();
            Toast.makeText(this, isAmharic ? "ውይይቱ ተሰርዟል።" : "Thread deleted.", Toast.LENGTH_SHORT).show();
            loadAdminMessages();
        });

        AlertDialog dialog = builder.create();
        btnSendReply.setOnClickListener(v -> {
            String replyText = etReply.getText().toString().trim();
            if (!replyText.isEmpty()) {
                Map<String, Object> reply = new HashMap<>();
                reply.put("sender", loggedInUserName);
                reply.put("recipient", sender);
                reply.put("text", replyText);
                reply.put("timestamp", System.currentTimeMillis());
                reply.put("status", "Unread");

                db.collection("users").document(sender).get().addOnSuccessListener(doc -> {
                    if ("Admin".equals(doc.getString("role"))) db.collection("admin_messages").add(reply);
                    else db.collection("solver_inbox").add(reply);

                    etReply.setText("");
                    dialog.dismiss();
                    loadAdminMessages();
                });
            } else Toast.makeText(this, isAmharic ? "መልዕክት ባዶ መሆን አይችልም።" : "Reply cannot be empty.", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
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
        container.setBackgroundColor(Color.parseColor("#F0F2F5"));
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
                                tvName.setTextColor(Color.GRAY);
                                tvName.setPadding(10,0,0,0);
                                bubbleWrapper.addView(tvName);
                            }

                            TextView bubble = new TextView(this);
                            bubble.setText(m.getString("text"));
                            bubble.setPadding(40, 25, 40, 25);
                            bubble.setTextSize(16);

                            GradientDrawable gd = new GradientDrawable();
                            gd.setCornerRadius(40f);
                            if (isMe) { gd.setColor(Color.parseColor("#6f42c1")); bubble.setTextColor(Color.WHITE); }
                            else { gd.setColor(ContextCompat.getColor(this, R.color.input_background)); bubble.setTextColor(ContextCompat.getColor(this, R.color.text_primary)); bubble.setElevation(2f); }
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