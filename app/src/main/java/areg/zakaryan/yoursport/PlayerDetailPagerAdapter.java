package areg.zakaryan.yoursport;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PlayerDetailPagerAdapter extends FragmentStateAdapter {

    private final int playerId;

    public PlayerDetailPagerAdapter(@NonNull FragmentActivity activity, int playerId) {
        super(activity);
        this.playerId = playerId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return PlayerOverviewFragment.newInstance(playerId);
            case 1: return PlayerStatsFragment.newInstance(playerId);
            case 2: return PlayerCareerFragment.newInstance(playerId);
            default: return PlayerOverviewFragment.newInstance(playerId);
        }
    }

    @Override
    public int getItemCount() { return 3; }
}
