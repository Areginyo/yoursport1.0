package areg.zakaryan.yoursport;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import areg.zakaryan.yoursport.model.SearchItem;

public class TeamDetailPagerAdapter extends FragmentStateAdapter {

    private final SearchItem teamItem;

    public TeamDetailPagerAdapter(@NonNull FragmentActivity fragmentActivity, SearchItem teamItem) {
        super(fragmentActivity);
        this.teamItem = teamItem;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return TeamNewsFragment.newInstance(teamItem);
            case 1: return TeamMatchesFragment.newInstance(teamItem);
            case 2: return TeamSeasonFragment.newInstance(teamItem);
            case 3: return TeamSquadFragment.newInstance(teamItem);
            case 4: return TeamTransfersFragment.newInstance(teamItem);
            default: return TeamNewsFragment.newInstance(teamItem);
        }
    }

    @Override
    public int getItemCount() {
        return 5; 
    }
}
