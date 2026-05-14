package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerOverviewFragment extends Fragment {

    private static final String ARG_PLAYER_ID = "player_id";
    private int playerId;

    public static PlayerOverviewFragment newInstance(int playerId) {
        PlayerOverviewFragment f = new PlayerOverviewFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_PLAYER_ID, playerId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) playerId = getArguments().getInt(ARG_PLAYER_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_overview, container, false);
        loadPlayer(view, currentSeason());
        return view;
    }

    /** API-Football season = start year of the season (e.g. 2025 = 2025/26) */
    private int currentSeason() {
        int yr = Calendar.getInstance().get(Calendar.YEAR);
        int mo = Calendar.getInstance().get(Calendar.MONTH); // 0-indexed
        // Seasons typically start in July-August; before July use previous year
        return mo < 6 ? yr - 1 : yr;
    }

    @SuppressWarnings("unchecked")
    private void loadPlayer(View view, int season) {
        ProgressBar pb  = view.findViewById(R.id.progressBar);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);

        ApiClient.getApiService().getPlayerProfile(playerId, season)
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        if (!isAdded()) return;
                        try {
                            Map<String, Object> body = (Map<String, Object>) response.body();
                            if (body == null) { tryPreviousSeason(view, pb, tvEmpty, season); return; }

                            List<Map<String, Object>> resp =
                                    (List<Map<String, Object>>) body.get("response");

                            if (resp == null || resp.isEmpty()) {
                                // Try one season back
                                tryPreviousSeason(view, pb, tvEmpty, season);
                                return;
                            }

                            pb.setVisibility(View.GONE);
                            Map<String, Object> entry  = resp.get(0);
                            Map<String, Object> player = castMap(entry.get("player"));
                            List<Map<String, Object>> stats =
                                    (List<Map<String, Object>>) entry.get("statistics");

                            if (player != null) bindPersonal(view, player, stats);
                            if (stats != null && !stats.isEmpty()) {
                                // Pick primary league stat (most appearances)
                                Map<String, Object> primaryStat = pickPrimaryStat(stats);
                                bindClub(view, primaryStat);
                                findNationalTeam(view, stats);
                            }
                            showCards(view);

                        } catch (Exception e) {
                            tryPreviousSeason(view, pb, tvEmpty, season);
                        }
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        if (!isAdded()) return;
                        tryPreviousSeason(view, pb, tvEmpty, season);
                    }
                });
    }

    private void tryPreviousSeason(View view, ProgressBar pb, TextView tvEmpty, int season) {
        if (!isAdded()) return;
        if (season > 2018) {
            loadPlayer(view, season - 1);
        } else {
            pb.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("unchecked")
    private void bindPersonal(View view, Map<String, Object> player, List<Map<String, Object>> stats) {
        setText(view, R.id.tvAge, strOrDash(player.get("age")) + " y.o.");
        setText(view, R.id.tvNationality, strOrDash(player.get("nationality")));
        setText(view, R.id.tvHeight, strOrDash(player.get("height")));
        setText(view, R.id.tvWeight, strOrDash(player.get("weight")));

        // Position and Number come from statistics[0].games in API-Football
        String position = "—";
        String number = "—";
        if (stats != null && !stats.isEmpty()) {
            Map<String, Object> primaryStat = pickPrimaryStat(stats);
            Map<String, Object> games = castMap(primaryStat.get("games"));
            if (games != null) {
                String pos = safe(games.get("position"));
                if (!pos.isEmpty()) position = pos;
                int num = asInt(games.get("number"));
                if (num > 0) number = String.valueOf(num);
            }
        }
        setText(view, R.id.tvPosition, position);
        setText(view, R.id.tvNumber, number);
    }

    /** Pick the stat entry with most appearances (= primary domestic league) */
    @SuppressWarnings("unchecked")
    private Map<String, Object> pickPrimaryStat(List<Map<String, Object>> stats) {
        Map<String, Object> best = stats.get(0);
        int bestApps = 0;
        for (Map<String, Object> s : stats) {
            Map<String, Object> games = castMap(s.get("games"));
            int apps = games != null ? asInt(games.get("appearences")) : 0;
            if (apps > bestApps) { bestApps = apps; best = s; }
        }
        return best;
    }

    private int asInt(Object v) {
        try {
            if (v == null) return 0;
            if (v instanceof Number) return ((Number) v).intValue();
            String s = String.valueOf(v).trim();
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s);
        } catch (Exception e) { return 0; }
    }

    @SuppressWarnings("unchecked")
    private void bindClub(View view, Map<String, Object> stat) {
        Map<String, Object> team   = castMap(stat.get("team"));
        Map<String, Object> league = castMap(stat.get("league"));
        if (team == null) return;

        String clubName   = safe(team.get("name"));
        String clubLogo   = safe(team.get("logo"));
        String leagueName = league != null ? safe(league.get("name")) : "";

        ((TextView) view.findViewById(R.id.tvClubName)).setText(clubName.isEmpty() ? "—" : clubName);
        ((TextView) view.findViewById(R.id.tvClubLeague)).setText(leagueName);

        ImageView ivLogo = view.findViewById(R.id.ivClubLogo);
        if (!clubLogo.isEmpty()) {
            Glide.with(ivLogo.getContext()).load(clubLogo)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder).into(ivLogo);
        }
        if (getActivity() instanceof PlayerDetailActivity) {
            ((PlayerDetailActivity) getActivity()).updateClubInfo(clubName, clubLogo);
        }
    }

    @SuppressWarnings("unchecked")
    private void findNationalTeam(View view, List<Map<String, Object>> stats) {
        for (Map<String, Object> stat : stats) {
            Map<String, Object> league = castMap(stat.get("league"));
            if (league == null) continue;
            // National leagues have country == team name typically, or type != "League"
            String leagueName = safe(league.get("name")).toLowerCase();
            if (leagueName.contains("nation") || leagueName.contains("world cup")
                    || leagueName.contains("euro") || leagueName.contains("copa")) {
                Map<String, Object> team = castMap(stat.get("team"));
                if (team == null) continue;
                String name = safe(team.get("name"));
                String logo = safe(team.get("logo"));
                if (!name.isEmpty()) {
                    ((TextView) view.findViewById(R.id.tvNationalTeam)).setText(name);
                    ImageView iv = view.findViewById(R.id.ivNationalLogo);
                    if (!logo.isEmpty()) {
                        Glide.with(iv.getContext()).load(logo)
                                .placeholder(R.drawable.ic_placeholder)
                                .error(R.drawable.ic_placeholder).into(iv);
                    }
                    view.findViewById(R.id.cardNational).setVisibility(View.VISIBLE);
                    return;
                }
            }
        }
    }

    private void showCards(View view) {
        view.findViewById(R.id.cardPersonal).setVisibility(View.VISIBLE);
        view.findViewById(R.id.cardClub).setVisibility(View.VISIBLE);
    }

    private void setText(View view, int id, String text) {
        TextView tv = view.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private String strOrDash(Object v) {
        String s = safe(v);
        return s.isEmpty() ? "—" : s;
    }

    private String safe(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return "null".equalsIgnoreCase(s) ? "" : s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }
}
