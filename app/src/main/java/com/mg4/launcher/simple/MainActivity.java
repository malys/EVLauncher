package com.mg4.launcher.simple;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.mg4.launcher.simple.update.UpdateHook;

/**
 * Hosts the two-page home carousel ({@link HomePagerAdapter}) and the bottom pagination
 * bars. Page 1 is the launcher home, page 2 the useful-info screen.
 */
public class MainActivity extends AppCompatActivity {

    private View[] pageBars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 pager = findViewById(R.id.home_pager);
        pager.setAdapter(new HomePagerAdapter(this));

        pageBars = new View[]{
                findViewById(R.id.page_bar_0),
                findViewById(R.id.page_bar_1)};
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
            }
        });
        // Post so the indicator reads the page ViewPager2 restores after a layout pass
        // (state restore doesn't reliably fire onPageSelected for the initial position).
        pager.post(() -> updateIndicator(pager.getCurrentItem()));

        // Unstable builds check the pre-release channel on launch and download in the
        // background. On stable this call is a no-op stub: the updater is not in the APK.
        UpdateHook.checkInBackground(this);
    }

    /** Highlights the bar of the current page and shrinks the others (SAIC-style pagination). */
    private void updateIndicator(int position) {
        for (int i = 0; i < pageBars.length; i++) {
            boolean active = i == position;
            View bar = pageBars[i];
            bar.getLayoutParams().width = getResources().getDimensionPixelSize(active
                    ? R.dimen.page_indicator_active_width
                    : R.dimen.page_indicator_inactive_width);
            bar.setBackgroundResource(active
                    ? R.drawable.page_bar_active
                    : R.drawable.page_bar_inactive);
            bar.requestLayout();
        }
    }
}
