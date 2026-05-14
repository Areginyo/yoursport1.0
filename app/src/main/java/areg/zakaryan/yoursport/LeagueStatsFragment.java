package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import areg.zakaryan.yoursport.api.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeagueStatsFragment extends Fragment {

    private static final String ARG_LEAGUE_ID = "league_id";

    private int leagueId;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private TextView lblTopScorers, lblTopAssists, lblTopGA, lblYellowCards, lblRedCards;
    private LinearLayout llTopScorers, llTopAssists, llTopGA, llYellowCards, llRedCards;

    // Store raw data for computing G+A
    private List<PlayerStat> scorersData;
    private List<PlayerStat> assistsData;

    public static LeagueStatsFragment newInstance(int leagueId) {
        LeagueStatsFragment f = new LeagueStatsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LEAGUE_ID, leagueId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) leagueId = getArguments().getInt(ARG_LEAGUE_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_league_stats, container, false);

        progressBar = v.findViewById(R.id.progressBar);
        tvEmpty     = v.findViewById(R.id.tvEmpty);

        lblTopScorers  = v.findViewById(R.id.lblTopScorers);
        lblTopAssists  = v.findViewById(R.id.lblTopAssists);
        lblTopGA       = v.findViewById(R.id.lblTopGA);
        lblYellowCards = v.findViewById(R.id.lblYellowCards);
        lblRedCards    = v.findViewById(R.id.lblRedCards);

        llTopScorers  = v.findViewById(R.id.llTopScorers);
        llTopAssists  = v.findViewById(R.id.llTopAssists);
        llTopGA       = v.findViewById(R.id.llTopGA);
        llYellowCards = v.findViewById(R.id.llYellowCards);
        llRedCards    = v.findViewById(R.id.llRedCards);

        loadAllStats();
        return v;
    }

    private int getSeason() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        return cal.get(Calendar.MONTH) < 6 ? year - 1 : year;
    }

    private void loadAllStats() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        int season = getSeason();
        AtomicInteger pending = new AtomicInteger(4); // scorers, assists, yellow, red

        // Top Scorers
        ApiClient.getApiService().getTopScorers(leagueId, season).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                scorersData = parsePlayerStats(response, "goals");
                populateSection(lblTopScorers, llTopScorers, scorersData, "goals");
                checkDone(pending);
            }
            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) checkDone(pending);
            }
        });

        // Top Assists
        ApiClient.getApiService().getTopAssists(leagueId, season).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                assistsData = parsePlayerStats(response, "assists");
                populateSection(lblTopAssists, llTopAssists, assistsData, "assists");
                checkDone(pending);
            }
            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) checkDone(pending);
            }
        });

        // Top Yellow Cards
        ApiClient.getApiService().getTopYellowCards(leagueId, season).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                List<PlayerStat> data = parseCardStats(response, "yellow");
                populateSection(lblYellowCards, llYellowCards, data, "yellow");
                checkDone(pending);
            }
            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) checkDone(pending);
            }
        });

        // Top Red Cards
        ApiClient.getApiService().getTopRedCards(leagueId, season).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                List<PlayerStat> data = parseCardStats(response, "red");
                populateSection(lblRedCards, llRedCards, data, "red");
                checkDone(pending);
            }
            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) checkDone(pending);
            }
        });
    }

    private void checkDone(AtomicInteger pending) {
        if (pending.decrementAndGet() == 0) {
            if (!isAdded()) return;
            progressBar.setVisibility(View.GONE);

            // Build Goals + Assists section from combined data
            buildGoalsAndAssists();

            // Check if everything is empty
            boolean allEmpty = llTopScorers.getVisibility() == View.GONE
                    && llTopAssists.getVisibility() == View.GONE
                    && llTopGA.getVisibility() == View.GONE
                    && llYellowCards.getVisibility() == View.GONE
                    && llRedCards.getVisibility() == View.GONE;

            if (allEmpty) {
                tvEmpty.setVisibility(View.VISIBLE);
            }
        }
    }

    private void buildGoalsAndAssists() {
        if (scorersData == null || scorersData.isEmpty()) return;

        // Build a combined G+A list from scorers data (they have goals+assists)
        List<PlayerStat> gaList = new ArrayList<>();
        for (PlayerStat ps : scorersData) {
            int ga = ps.goals + ps.assists;
            if (ga > 0) {
                PlayerStat combined = new PlayerStat();
                combined.name     = ps.name;
                combined.photo    = ps.photo;
                combined.teamName = ps.teamName;
                combined.goals    = ps.goals;
                combined.assists  = ps.assists;
                combined.value    = ga;
                gaList.add(combined);
            }
        }

        // Also merge assists data if they have goal info
        if (assistsData != null) {
            for (PlayerStat as : assistsData) {
                boolean found = false;
                for (PlayerStat existing : gaList) {
                    if (existing.name.equals(as.name) && existing.teamName.equals(as.teamName)) {
                        found = true;
                        // Update if assists data has better assist count
                        if (as.assists > existing.assists) {
                            existing.assists = as.assists;
                            existing.value = existing.goals + existing.assists;
                        }
                        break;
                    }
                }
                if (!found && (as.goals + as.assists) > 0) {
                    PlayerStat combined = new PlayerStat();
                    combined.name     = as.name;
                    combined.photo    = as.photo;
                    combined.teamName = as.teamName;
                    combined.goals    = as.goals;
                    combined.assists  = as.assists;
                    combined.value    = as.goals + as.assists;
                    gaList.add(combined);
                }
            }
        }

        // Sort by G+A descending
        gaList.sort((a, b) -> Integer.compare(b.value, a.value));

        // Take top 3
        List<PlayerStat> top3 = gaList.subList(0, Math.min(3, gaList.size()));
        populateSection(lblTopGA, llTopGA, top3, "ga");
    }

    private void populateSection(TextView label, LinearLayout container,
                                  List<PlayerStat> data, String type) {
        if (data == null || data.isEmpty()) return;

        label.setVisibility(View.VISIBLE);
        container.setVisibility(View.VISIBLE);
        container.removeAllViews();

        int limit = Math.min(3, data.size());
        for (int i = 0; i < limit; i++) {
            PlayerStat ps = data.get(i);
            View itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_top_player, container, false);

            ((TextView) itemView.findViewById(R.id.tvRank)).setText(String.valueOf(i + 1));
            ((TextView) itemView.findViewById(R.id.tvPlayerName)).setText(ps.name);
            ((TextView) itemView.findViewById(R.id.tvPlayerTeam)).setText(ps.teamName);

            // Stat value display
            TextView tvValue = itemView.findViewById(R.id.tvStatValue);
            if ("ga".equals(type)) {
                tvValue.setText(ps.goals + "G + " + ps.assists + "A");
                tvValue.setTextSize(14);
            } else {
                tvValue.setText(String.valueOf(ps.value));
            }

            // Color for card stats
            if ("yellow".equals(type)) {
                tvValue.setTextColor(0xFFFFEB3B);
            } else if ("red".equals(type)) {
                tvValue.setTextColor(0xFFF44336);
            }

            // Player photo
            ImageView ivPhoto = itemView.findViewById(R.id.ivPlayerPhoto);
            if (ps.photo != null && !ps.photo.isEmpty()) {
                Glide.with(requireContext())
                        .load(ps.photo)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(ivPhoto);
            }

            // Add divider between items
            if (i < limit - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(0xFF2A2A2A);
                container.addView(itemView);
                container.addView(divider);
            } else {
                container.addView(itemView);
            }
        }
    }

    // ── Parsing ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<PlayerStat> parsePlayerStats(Response<Object> response, String statType) {
        List<PlayerStat> result = new ArrayList<>();
        if (!response.isSuccessful() || response.body() == null) return result;

        try {
            Map<String, Object> body = (Map<String, Object>) response.body();
            List<?> resp = (List<?>) body.get("response");
            if (resp == null) return result;

            for (Object obj : resp) {
                if (!(obj instanceof Map)) continue;
                Map<String, Object> entry = (Map<String, Object>) obj;

                Map<String, Object> player = castMap(entry.get("player"));
                List<?> stats = (List<?>) entry.get("statistics");
                if (player == null || stats == null || stats.isEmpty()) continue;

                Map<String, Object> stat = (Map<String, Object>) stats.get(0);
                Map<String, Object> team = castMap(stat.get("team"));
                Map<String, Object> goalsMap = castMap(stat.get("goals"));

                PlayerStat ps = new PlayerStat();
                ps.name     = str(player.get("name"));
                ps.photo    = str(player.get("photo"));
                ps.teamName = team != null ? str(team.get("name")) : "";

                if (goalsMap != null) {
                    ps.goals   = numInt(goalsMap.get("total"));
                    ps.assists = numInt(goalsMap.get("assists"));
                }

                if ("goals".equals(statType)) {
                    ps.value = ps.goals;
                } else if ("assists".equals(statType)) {
                    ps.value = ps.assists;
                }

                result.add(ps);
            }
        } catch (Exception ignored) {}

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<PlayerStat> parseCardStats(Response<Object> response, String cardType) {
        List<PlayerStat> result = new ArrayList<>();
        if (!response.isSuccessful() || response.body() == null) return result;

        try {
            Map<String, Object> body = (Map<String, Object>) response.body();
            List<?> resp = (List<?>) body.get("response");
            if (resp == null) return result;

            for (Object obj : resp) {
                if (!(obj instanceof Map)) continue;
                Map<String, Object> entry = (Map<String, Object>) obj;

                Map<String, Object> player = castMap(entry.get("player"));
                List<?> stats = (List<?>) entry.get("statistics");
                if (player == null || stats == null || stats.isEmpty()) continue;

                Map<String, Object> stat = (Map<String, Object>) stats.get(0);
                Map<String, Object> team = castMap(stat.get("team"));
                Map<String, Object> cards = castMap(stat.get("cards"));

                PlayerStat ps = new PlayerStat();
                ps.name     = str(player.get("name"));
                ps.photo    = str(player.get("photo"));
                ps.teamName = team != null ? str(team.get("name")) : "";

                if (cards != null) {
                    if ("yellow".equals(cardType)) {
                        ps.value = numInt(cards.get("yellow"));
                    } else {
                        ps.value = numInt(cards.get("red"));
                    }
                }

                result.add(ps);
            }
        } catch (Exception ignored) {}

        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object v) {
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    private int numInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return (int) Math.round(Double.parseDouble(String.valueOf(v))); }
        catch (Exception e) { return 0; }
    }

    private String str(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return "null".equals(s) ? "" : s;
    }

    // ── Data class ───────────────────────────────────────────────────────────

    static class PlayerStat {
        String name;
        String photo;
        String teamName;
        int goals;
        int assists;
        int value; // the displayed stat count
    }
}
