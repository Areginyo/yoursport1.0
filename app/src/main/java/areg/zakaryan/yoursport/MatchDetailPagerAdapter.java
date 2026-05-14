package areg.zakaryan.yoursport;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import areg.zakaryan.yoursport.model.MatchItem;

public class MatchDetailPagerAdapter extends FragmentStateAdapter {

    private final MatchItem match;

    public MatchDetailPagerAdapter(@NonNull FragmentActivity fragmentActivity, MatchItem match) {
        super(fragmentActivity);
        this.match = match;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return MatchOverviewFragment.newInstance(match);
            case 1:
                return MatchLineupsFragment.newInstance(match);
            case 2:
                return MatchStatsFragment.newInstance(match);
            case 3:
                return MatchH2HFragment.newInstance(match);
            default:
                return MatchOverviewFragment.newInstance(match);
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}