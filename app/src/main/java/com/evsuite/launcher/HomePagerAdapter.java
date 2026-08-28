package com.evsuite.launcher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Three-page horizontal carousel: the home (page 0), the system-info screen (page 1) and the
 * read-only vehicle page (page 2).
 *
 * <p>The home stays at position 0 whatever else is added: pressing Home must land on the
 * favourites grid, not on whichever page was last swiped to.
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    public static final int PAGE_COUNT = 3;

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new SystemInfoFragment();
        }
        if (position == 2) {
            return new VehicleInfoFragment();
        }
        return new HomeFragment();
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
