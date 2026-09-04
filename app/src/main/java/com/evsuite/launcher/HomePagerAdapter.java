package com.evsuite.launcher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Two-page horizontal carousel: the home (page 0) and the customisable metrics page (page 1),
 * which merges what used to be a fixed system page and a fixed vehicle page.
 *
 * <p>The home stays at position 0 whatever else is added: pressing Home must land on the
 * favourites grid, not on whichever page was last swiped to.
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    public static final int PAGE_COUNT = 2;

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new MetricsFragment();
        }
        return new HomeFragment();
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
