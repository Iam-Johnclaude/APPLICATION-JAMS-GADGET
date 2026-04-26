package com.jamsgadget.inventory;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.jamsgadget.inventory.util.ThemeHelper;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etPassword;
    private TextInputLayout tilFullName, tilEmail, tilPassword;
    private TextView tvHeaderName, tvHeaderEmail, tvInitial;
    private MaterialButton btnUpdateProfile, btnUpdatePassword;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        initViews();
        loadUserData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderEmail = findViewById(R.id.tvHeaderEmail);
        tvInitial = findViewById(R.id.tvInitial);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadUserData() {
        String email = currentUser.getEmail();
        etEmail.setText(email);
        tvHeaderEmail.setText(email);

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        etFullName.setText(fullName);
                        tvHeaderName.setText(fullName);
                        if (fullName != null && !fullName.isEmpty()) {
                            tvInitial.setText(fullName.substring(0, 1).toUpperCase());
                        }
                    } else {
                        // If document doesn't exist, create it from Auth data
                        createInitialUserDocument();
                    }
                });
    }

    private void createInitialUserDocument() {
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
        user.put("email", currentUser.getEmail());
        user.put("role", "admin");
        
        db.collection("users").document(currentUser.getUid()).set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> loadUserData());
    }

    private void updateProfile() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Name is required");
            return;
        }

        setLoading(true);
        
        // Update Firebase Auth Display Name
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        currentUser.updateProfile(profileUpdates).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                // Update Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("fullName", fullName);
                updates.put("email", email);

                db.collection("users").document(currentUser.getUid()).set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            // Update Email in Auth if changed
                            if (!email.equalsIgnoreCase(currentUser.getEmail())) {
                                currentUser.updateEmail(email).addOnCompleteListener(emailTask -> {
                                    setLoading(false);
                                    if (emailTask.isSuccessful()) {
                                        Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        loadUserData();
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Email update failed: " + emailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                setLoading(false);
                                Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                loadUserData();
                            }
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(ProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, "Profile update failed in Auth", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePassword() {
        String newPassword = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        currentUser.updatePassword(newPassword).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                etPassword.setText("");
                Toast.makeText(ProfileActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ProfileActivity.this, "Password update failed. You may need to re-login.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnUpdateProfile.setEnabled(!isLoading);
        btnUpdatePassword.setEnabled(!isLoading);
    }
}
