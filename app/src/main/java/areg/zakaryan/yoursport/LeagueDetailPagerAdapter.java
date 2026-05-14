package areg.zakaryan.yoursport;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class LeagueDetailPagerAdapter extends FragmentStateAdapter {

    private final int leagueId;
    private final String leagueName;
    private final String leagueLogo;

    public LeagueDetailPagerAdapter(@NonNull FragmentActivity activity,
                                     int leagueId, String leagueName, String leagueLogo) {
        super(activity);
        this.leagueId   = leagueId;
        this.leagueName = leagueName;
        this.leagueLogo = leagueLogo;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:  return LeagueCalendarFragment.newInstance(leagueId, leagueName, leagueLogo);
            case 1:  return LeagueTableFragment.newInstance(leagueId);
            case 2:  return LeagueStatsFragment.newInstance(leagueId);
            default: return LeagueCalendarFragment.newInstance(leagueId, leagueName, leagueLogo);
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Calendar, Table, Statistics
    }
}
