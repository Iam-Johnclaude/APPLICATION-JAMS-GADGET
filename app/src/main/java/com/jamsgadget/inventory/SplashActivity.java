package com.jamsgadget.inventory;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private List<Animator> activeAnimators = new ArrayList<>();
    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Immersive full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        startEnhancedAnimations();
        checkAuthAndNavigate();
    }

    private void startEnhancedAnimations() {
        View ringOuter = findViewById(R.id.ringOuter);
        View ringMiddle = findViewById(R.id.ringMiddle);
        View logoRing = findViewById(R.id.logoRing);
        View identityGroup = findViewById(R.id.identityGroup);
        View appName = findViewById(R.id.appName);
        View systemLabel = findViewById(R.id.systemLabel);
        View tagline = findViewById(R.id.tagline);
        View divider = findViewById(R.id.divider);
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        // 1. Initial State
        identityGroup.setAlpha(0f);
        identityGroup.setTranslationY(30f);

        // 2. Pulse Animation for Logo
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(logoRing, "scaleX", 1.0f, 1.15f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(logoRing, "scaleY", 1.0f, 1.15f);
        pulseX.setRepeatCount(ObjectAnimator.INFINITE);
        pulseX.setRepeatMode(ObjectAnimator.REVERSE);
        pulseY.setRepeatCount(ObjectAnimator.INFINITE);
        pulseY.setRepeatMode(ObjectAnimator.REVERSE);
        
        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(pulseX, pulseY);
        pulseSet.setDuration(1500);
        pulseSet.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseSet.start();
        activeAnimators.add(pulseSet);

        // 3. Constant Rotation for Rings
        ObjectAnimator rotateOuter = ObjectAnimator.ofFloat(ringOuter, "rotation", 0f, 360f);
        rotateOuter.setDuration(25000);
        rotateOuter.setRepeatCount(ObjectAnimator.INFINITE);
        rotateOuter.setInterpolator(null);
        rotateOuter.start();
        activeAnimators.add(rotateOuter);

        ObjectAnimator rotateMiddle = ObjectAnimator.ofFloat(ringMiddle, "rotation", 0f, -360f);
        rotateMiddle.setDuration(15000);
        rotateMiddle.setRepeatCount(ObjectAnimator.INFINITE);
        rotateMiddle.setInterpolator(null);
        rotateMiddle.start();
        activeAnimators.add(rotateMiddle);

        // 4. Identity Entrance Sequence
        identityGroup.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        appName.animate().alpha(1f).setDuration(600).setStartDelay(600).start();
        systemLabel.animate().alpha(1f).setDuration(600).setStartDelay(800).start();
        
        ObjectAnimator divScale = ObjectAnimator.ofFloat(divider, "scaleX", 0f, 1f);
        ObjectAnimator divAlpha = ObjectAnimator.ofFloat(divider, "alpha", 0f, 1f);
        AnimatorSet divSet = new AnimatorSet();
        divSet.playTogether(divScale, divAlpha);
        divSet.setDuration(600);
        divSet.setStartDelay(1000);
        divSet.start();
        activeAnimators.add(divSet);

        tagline.animate().alpha(1f).setDuration(600).setStartDelay(1200).start();

        // 5. Loading Dots Sequence
        startDotBounce(dot1, 1400);
        startDotBounce(dot2, 1550);
        startDotBounce(dot3, 1700);
    }

    private void startDotBounce(View dot, long delay) {
        if (dot == null) return;
        dot.setAlpha(0f);
        dot.animate().alpha(1f).setDuration(200).setStartDelay(delay).withEndAction(() -> {
            ObjectAnimator bounce = ObjectAnimator.ofFloat(dot, "translationY", 0f, -20f);
            bounce.setDuration(500);
            bounce.setRepeatCount(ObjectAnimator.INFINITE);
            bounce.setRepeatMode(ObjectAnimator.REVERSE);
            bounce.setInterpolator(new AccelerateDecelerateInterpolator());
            bounce.start();
            activeAnimators.add(bounce);
        }).start();
    }

    private void checkAuthAndNavigate() {
        splashHandler.postDelayed(() -> {
            FirebaseUser user = mAuth.getCurrentUser();
            Intent intent = new Intent(this, user != null ? DashboardActivity.class : LoginActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 3500);
    }

    @Override
    protected void onDestroy() {
        splashHandler.removeCallbacksAndMessages(null);
        for (Animator anim : activeAnimators) {
            if (anim != null) anim.cancel();
        }
        super.onDestroy();
    }
}
