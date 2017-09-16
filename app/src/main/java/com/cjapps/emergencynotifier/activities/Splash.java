package com.cjapps.emergencynotifier.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.cjapps.emergencynotifier.R;

public class Splash extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splashScreenLogo = (ImageView) findViewById(R.id.splash_screen_logo);
        RelativeLayout splashScreenText = (RelativeLayout) findViewById(R.id.splash_screen_text);

        Animation slideInTop = AnimationUtils.loadAnimation(this, R.anim.abc_slide_in_top);
        Animation slideInBottom = AnimationUtils.loadAnimation(this, R.anim.abc_slide_in_bottom);

        // Set the slide in top animation to splash screen logo imageView
        splashScreenLogo.startAnimation(slideInTop);

        // Set the slide in bottom animation to splash screen text textView
        splashScreenText.startAnimation(slideInBottom);

        // Set AnimationListener on slideInBottom to trigger action on a particular event
        slideInBottom.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // After the animation ends start the @Main activity
                // Create a delay for the logo to be seen

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent mainActivity = new Intent(getApplicationContext(), Main.class);
                        startActivity(mainActivity);
                        Splash.this.finish();
                    }
                }, 2500);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }
}
