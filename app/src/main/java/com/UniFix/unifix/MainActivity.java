package com.UniFix.unifix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin, btnRegStudent, btnRegTeacher, btnToggleTheme, btnToggleLanguage;

    // --- UI ELEMENTS ---
    TextView tvForgotPassword, tvLoginTitle, tvLoginSubtitle, tvNoAccount;
    ImageView ivShowPassword;
    boolean isPasswordVisible = false;

    FirebaseFirestore db;
    SharedPreferences securityPrefs;

    // Language state
    boolean isAmharic = false;

    // Security Variables
    int failedAttempts = 0;
    long lockoutTime = 0;

    // Translated strings for Toast messages
    String msgEmptyFields = "Please enter Username and Password";
    String msgBanned = "ACCOUNT BANNED. Please contact the Admin.";
    String msgPending = "Wait, you are not authorized to login right now wait until the admins manually review your acount";
    String msgWrongPass = "Invalid credentials. Attempts left: ";
    String msgLockedOut = "Too many failed attempts. Locked out for 20 minutes.";
    String msgErrorDb = "Error connecting to database. Check your internet.";
    String msgWelcome = "Welcome ";

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
        setContentView(R.layout.activity_main);

        // =======================================================
        // CALL THE SEEDERS (DELETE THESE AFTER RUNNING ONCE!)
        // =======================================================
        generateTestSolvers();
        seedAdmins();

        db = FirebaseFirestore.getInstance();

        // Initialize Security Prefs for Lockout
        securityPrefs = getSharedPreferences("LoginSecurity", MODE_PRIVATE);
        failedAttempts = securityPrefs.getInt("failed_attempts", 0);
        lockoutTime = securityPrefs.getLong("lockout_time", 0);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegStudent = findViewById(R.id.btnRegStudent);
        btnRegTeacher = findViewById(R.id.btnRegTeacher);
        btnToggleTheme = findViewById(R.id.btnToggleTheme);
        btnToggleLanguage = findViewById(R.id.btnToggleLanguage);

        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ivShowPassword = findViewById(R.id.ivShowPassword);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        tvLoginSubtitle = findViewById(R.id.tvLoginSubtitle);
        tvNoAccount = findViewById(R.id.tvNoAccount);

        // Apply Premium Styling
        styleInputBox(etUsername);
        styleInputBox(etPassword);
        makeInteractive(btnLogin);
        makeInteractive(btnRegStudent);
        makeInteractive(btnRegTeacher);
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


        SharedPreferences prefs = getSharedPreferences("UniFixSettings", MODE_PRIVATE);
        isAmharic = prefs.getBoolean("isAmharic", false);

        if (btnToggleLanguage != null) {
            btnToggleLanguage.setText(isAmharic ? "🌐 ENG" : "🌐 አማ");
            makeInteractive(btnToggleLanguage);
            btnToggleLanguage.setOnClickListener(v -> {
                isAmharic = !isAmharic;
                prefs.edit().putBoolean("isAmharic", isAmharic).apply();
                recreate();
            });
        }

        applyTranslation();

        // ---------------------------------------------------
        // SHOW / HIDE PASSWORD LOGIC
        // ---------------------------------------------------
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

        // ---------------------------------------------------
        // NAVIGATION LOGIC
        // ---------------------------------------------------
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ForgotPasswordActivity.class)));
        btnRegStudent.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterStudentActivity.class)));
        btnRegTeacher.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterTeacherActivity.class)));

        // ---------------------------------------------------
        // LIGHTNING-FAST & INTERACTIVE FIREBASE LOGIN LOGIC
        // ---------------------------------------------------
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 1. Check Lockout Timer First
                long currentTime = System.currentTimeMillis();
                if (currentTime < lockoutTime) {
                    long remainingMillis = lockoutTime - currentTime;
                    long remainingMinutes = (remainingMillis / 1000) / 60;
                    Toast.makeText(MainActivity.this, (isAmharic ? "መለያው ተቆልፏል። ከ " : "Locked out. Try again in ") + remainingMinutes + (isAmharic ? " ደቂቃዎች በኋላ ይሞክሩ" : " mins."), Toast.LENGTH_LONG).show();
                    return;
                }

                String inputUsername = etUsername.getText().toString().trim().toLowerCase();
                String inputPassword = etPassword.getText().toString().trim();

                if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
                    Toast.makeText(MainActivity.this, msgEmptyFields, Toast.LENGTH_SHORT).show();
                    return;
                }

                btnLogin.setText(isAmharic ? "በማረጋገጥ ላይ..." : "Verifying...");
                btnLogin.setEnabled(false);

                db.collection("users").document(inputUsername).get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                String dbPassword = document.getString("password");
                                String role = document.getString("role");

                                Boolean isBanned = document.getBoolean("isBanned");
                                Boolean isPending = document.getBoolean("isPendingReview");
                                String actualFullName = document.getString("fullName");

                                // Security Check: Banned
                                if (isBanned != null && isBanned) {
                                    Toast.makeText(MainActivity.this, msgBanned, Toast.LENGTH_LONG).show();
                                    resetLoginButton();
                                    return;
                                }

                                // Security Check: Pending Manual Review Block
                                if (isPending != null && isPending) {
                                    Toast.makeText(MainActivity.this, msgPending, Toast.LENGTH_LONG).show();
                                    resetLoginButton();
                                    return;
                                }

                                // Password Validation
                                if (dbPassword != null && dbPassword.equals(inputPassword)) {
                                    // SUCCESS! Clear failed attempts
                                    securityPrefs.edit().putInt("failed_attempts", 0).putLong("lockout_time", 0).apply();
                                    failedAttempts = 0;

                                    btnLogin.setText(isAmharic ? "በማዞር ላይ..." : "Redirecting...");

                                    String translatedRole = role;
                                    if(isAmharic) {
                                        if("Student".equals(role)) translatedRole = "ተማሪ";
                                        else if("Teacher".equals(role)) translatedRole = "መምህር";
                                        else if("Solver".equals(role)) translatedRole = "ባለሙያ";
                                        else if("Admin".equals(role)) translatedRole = "አስተዳዳሪ";
                                    }
                                    Toast.makeText(MainActivity.this, msgWelcome + translatedRole + "!", Toast.LENGTH_SHORT).show();

                                    Intent intent;
                                    if ("Admin".equals(role)) {
                                        intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                                    } else if ("Student".equals(role)) {
                                        intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
                                    } else if ("Teacher".equals(role)) {
                                        intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
                                    } else if ("Solver".equals(role)) {
                                        intent = new Intent(MainActivity.this, SolverDashboardActivity.class);
                                    } else {
                                        Toast.makeText(MainActivity.this, "Unknown Role", Toast.LENGTH_SHORT).show();
                                        resetLoginButton();
                                        return;
                                    }

                                    intent.putExtra("USERNAME", inputUsername);

                                    String firstName = inputUsername;
                                    if (actualFullName != null && !actualFullName.trim().isEmpty()) {
                                        firstName = actualFullName.trim().split("\\s+")[0];
                                    }
                                    intent.putExtra("FULL_NAME", firstName);

                                    startActivity(intent);
                                    btnLogin.postDelayed(this::resetLoginButton, 1000);

                                } else {
                                    handleFailedAttempt();
                                }
                            } else {
                                handleFailedAttempt();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseLogin", "Error getting documents: ", e);
                            Toast.makeText(MainActivity.this, msgErrorDb, Toast.LENGTH_LONG).show();
                            resetLoginButton();
                        });
            }

            private void handleFailedAttempt() {
                failedAttempts++;
                if (failedAttempts >= 3) {
                    lockoutTime = System.currentTimeMillis() + (20 * 60 * 1000); // 20 minutes
                    securityPrefs.edit().putInt("failed_attempts", failedAttempts).putLong("lockout_time", lockoutTime).apply();
                    Toast.makeText(MainActivity.this, msgLockedOut, Toast.LENGTH_LONG).show();
                } else {
                    securityPrefs.edit().putInt("failed_attempts", failedAttempts).apply();
                    int remaining = 3 - failedAttempts;
                    Toast.makeText(MainActivity.this, msgWrongPass + remaining, Toast.LENGTH_SHORT).show();
                }
                resetLoginButton();
            }

            private void resetLoginButton() {
                btnLogin.setText(isAmharic ? "ግባ" : "LOGIN");
                btnLogin.setEnabled(true);
            }
        });
    }

    // --- TRANSLATION ENGINE ---
    private void applyTranslation() {
        if (isAmharic) {
            tvLoginTitle.setText("UniFix መግቢያ");
            tvLoginSubtitle.setText("የዩኒቨርሲቲ ችግር ሪፖርት ማቅረቢያ ስርዓት");
            etUsername.setHint("የተጠቃሚ ስም (Username)");
            etPassword.setHint("የይለፍ ቃል (Password)");
            tvForgotPassword.setText("የይለፍ ቃል ረሱ?");
            btnLogin.setText("ግባ");
            tvNoAccount.setText("መለያ የለዎትም?");
            btnRegStudent.setText("ተማሪ");
            btnRegTeacher.setText("መምህር");

            // Translated System Messages
            msgEmptyFields = "እባክዎ የተጠቃሚ ስም እና የይለፍ ቃል ያስገቡ";
            msgBanned = "መለያዎ ታግዷል። እባክዎ አስተዳዳሪን ያነጋግሩ።";
            msgPending = "ቆይ፣ አሁን መግባት አትችልም የአስተዳዳሪዎች ማረጋገጫ እስኪደርስ ጠብቅ";
            msgWrongPass = "የተሳሳተ መረጃ። የቀረዎት ሙከራ: ";
            msgLockedOut = "በጣም ብዙ የተሳሳቱ ሙከራዎች። ለ20 ደቂቃ ተቆልፏል።";
            msgErrorDb = "ከዳታቤዝ ጋር መገናኘት አልተቻለም።";
            msgWelcome = "እንኳን ደህና መጡ ";
        } else {
            tvLoginTitle.setText("UniFix Login");
            tvLoginSubtitle.setText("University Problem Reporting System");
            etUsername.setHint("Username");
            etPassword.setHint("Password");
            tvForgotPassword.setText("Forgot Password?");
            btnLogin.setText("LOGIN");
            tvNoAccount.setText("Don't have an account?");
            btnRegStudent.setText("STUDENT");
            btnRegTeacher.setText("TEACHER");

            // English System Messages
            msgEmptyFields = "Please enter Username and Password";
            msgBanned = "ACCOUNT BANNED. Please contact the Admin.";
            msgPending = "Wait, you are not authorized to login right now wait until the admins manually review your acount";
            msgWrongPass = "Invalid credentials. Attempts left: ";
            msgLockedOut = "Too many failed attempts. Locked out for 20 minutes.";
            msgErrorDb = "Error connecting to database. Check your internet.";
            msgWelcome = "Welcome ";
        }
    }

    // =========================================================
    // TEMPORARY DATABASE SEEDERS (RUN ONLY ONCE!)
    // =========================================================
    private void generateTestSolvers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String defaultPassword = "#123abcEF";


        String[][] solvers = {
                {"dbu_ict1", "ICT Expert One", "Staff ICT Manager"},
                {"dbu_ict2", "ICT Expert Two", "Staff ICT Manager"},
                {"dbu_dorm1", "Dorm Expert One", "Staff Dormitory Manager"},
                {"dbu_dorm2", "Dorm Expert Two", "Staff Dormitory Manager"},
                {"dbu_acad1", "Academic Expert One", "Staff Academic Resources Manager"},
                {"dbu_cafe1", "Cafe Expert One", "Staff Cafeteria Manager"},
                {"dbu_hr1", "HR Expert One", "Staff Human Resource Manager"},
                {"dbu_health1", "Health Expert One", "Staff Health Center Manager"},
                {"dbu_sec1", "Security Expert One", "Staff Campus Security Manager"},
                {"dbu_fin1", "Finance Expert One", "Staff Finance Manager"},
                {"dbu_adm1", "Admin Expert One", "Staff University Administration Manager"},
                {"dbu_gen1", "General Tech A", "Staff General Technician"},
                {"dbu_dean1", "Dean One", "Staff Dean"},
                {"dbu_head1", "Dept Head One", "Staff Department Head"}
        };

        for (String[] solver : solvers) {
            java.util.Map<String, Object> user = new java.util.HashMap<>();
            user.put("username", solver[0]);
            user.put("fullName", solver[1]);
            user.put("dept", solver[2]);
            user.put("password", defaultPassword);
            user.put("role", "Solver");
            user.put("isBanned", false);
            user.put("warnings", 0);

            db.collection("users").document(solver[0]).set(user)
                    .addOnSuccessListener(aVoid -> Log.d("SEEDER", "Successfully added: " + solver[0]));
        }
    }

    private void seedAdmins() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String[] adminUsernames = {"dbu_admin1", "dbu_admin2", "dbu_admin3", "dbu_admin4"};
        String defaultPassword = "#123abcEF";

        for (String username : adminUsernames) {
            java.util.Map<String, Object> admin = new java.util.HashMap<>();
            admin.put("username", username);
            admin.put("password", defaultPassword);
            admin.put("role", "Admin");
            admin.put("fullName", "Admin " + username.replace("dbu_admin", ""));
            admin.put("createdAt", System.currentTimeMillis());

            db.collection("users").document(username).set(admin)
                    .addOnSuccessListener(aVoid -> Log.d("SEED", "Seeded: " + username));
        }
    }
}