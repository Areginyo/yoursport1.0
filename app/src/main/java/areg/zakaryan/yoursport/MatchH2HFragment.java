package areg.zakaryan.yoursport;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.MatchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchH2HFragment extends Fragment {

    private static final String ARG_MATCH = "match";
    private static final int H2H_LAST = 10;

    private TextView tvHeadline;
    private TextView tvFallback;
    private LinearLayout llCompare;
    private LinearLayout llMatches;

    public static MatchH2HFragment newInstance(MatchItem match) {
        MatchH2HFragment fragment = new MatchH2HFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MATCH, match);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_match_h2h, container, false);
        tvHeadline = view.findViewById(R.id.tv_h2h_headline);
        tvFallback = view.findViewById(R.id.tv_h2h_fallback);
        llCompare = view.findViewById(R.id.ll_h2h_comparison);
        llMatches = view.findViewById(R.id.ll_h2h_matches_list);

        MatchItem match = getArguments() != null ? getArguments().getParcelable(ARG_MATCH) : null;
        if (match == null || match.matchId == null || match.matchId.trim().isEmpty()) {
            showFallback("No data H2H.");
            return view;
        }

        tvHeadline.setText(nz(match.homeTeam) + "  vs  " + nz(match.awayTeam));

if (match.homeTeamId > 0 && match.awayTeamId > 0) {
            fetchH2H(match.homeTeamId, match.awayTeamId, match);
        } else {
            
            ApiClient.getApiService().getMatchById(match.matchId).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (!isAdded()) return;
                    PairIds ids = extractTeamIds(response.body());
                    if (ids == null) {
                        showFallback("Unable to obtain pair composition for H2H.");
                        return;
                    }
                    fetchH2H(ids.teamAId, ids.teamBId, match);
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    if (!isAdded()) return;
                    showFallback("Network error.");
                }
            });
        }

        return view;
    }

    private void fetchH2H(int teamAId, int teamBId, MatchItem match) {
        String h2h = teamAId + "-" + teamBId;
        ApiClient.getApiService().getHeadToHead(h2h, H2H_LAST).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> c2, Response<Object> r2) {
                if (!isAdded()) return;
                if (!r2.isSuccessful() || r2.body() == null) {
                    showFallback("No response from the H2H server.");
                    return;
                }
                List<Map<String, Object>> list = unwrapList(r2.body());
                if (list.isEmpty()) {
                    showFallback("No head-to-head matches found.");
                    return;
                }
                tvFallback.setVisibility(View.GONE);
                H2hStats s = aggregate(list, teamAId, teamBId, nz(match.homeTeam), nz(match.awayTeam));
                try { renderComparison(s); } catch (Exception ex) {  }
                try { renderFixtureList(list, teamAId, teamBId, nz(match.homeTeam), nz(match.awayTeam)); } catch (Exception ex) { showFallback("Error displaying H2H data."); }
            }

            @Override
            public void onFailure(Call<Object> c2, Throwable t) {
                if (!isAdded()) return;
                showFallback("H2H loading error.");
            }
        });
    }

    private void showFallback(String msg) {
        tvFallback.setText(msg);
        tvFallback.setVisibility(View.VISIBLE);
        llCompare.removeAllViews();
        llMatches.removeAllViews();
    }

    private static final class PairIds {
        
        final int teamAId;
        final int teamBId;

        PairIds(int a, int b) {
            teamAId = a;
            teamBId = b;
        }
    }

    private static final class H2hStats {
        String labelHome;
        String labelAway;
        int winsHome;
        int winsAway;
        int draws;
        int n;
        double avgGoalsHome;
        double avgGoalsAway;
        int btts;
    }

    @SuppressWarnings("unchecked")
    private PairIds extractTeamIds(Object raw) {
        if (!(raw instanceof Map)) return null;
        Object resp = ((Map<String, Object>) raw).get("response");
        if (!(resp instanceof List) || ((List<?>) resp).isEmpty()) return null;
        Object first = ((List<?>) resp).get(0);
        if (!(first instanceof Map)) return null;
        Map<String, Object> m = (Map<String, Object>) first;
        Map<String, Object> teams = castMap(m.get("teams"));
        if (teams == null) return null;
        Map<String, Object> home = castMap(teams.get("home"));
        Map<String, Object> away = castMap(teams.get("away"));
        if (home == null || away == null) return null;
        int hid = asInt(home.get("id"));
        int aid = asInt(away.get("id"));
        if (hid <= 0 || aid <= 0) return null;
        return new PairIds(hid, aid);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> unwrapList(Object raw) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(raw instanceof Map)) return out;
        Object resp = ((Map<String, Object>) raw).get("response");
        if (!(resp instanceof List)) return out;
        for (Object o : (List<?>) resp) {
            if (o instanceof Map) out.add((Map<String, Object>) o);
        }
        return out;
    }

    private H2hStats aggregate(List<Map<String, Object>> fixtures, int tidHome, int tidAway,
                              String labelHome, String labelAway) {
        H2hStats s = new H2hStats();
        s.labelHome = labelHome.isEmpty() ? "Home" : labelHome;
        s.labelAway = labelAway.isEmpty() ? "Away" : labelAway;
        s.n = fixtures.size();

        double sumHome = 0;
        double sumAway = 0;

        for (Map<String, Object> m : fixtures) {
            Map<String, Object> teams = castMap(m.get("teams"));
            Map<String, Object> goals = castMap(m.get("goals"));
            if (teams == null || goals == null) continue;
            Map<String, Object> th = castMap(teams.get("home"));
            Map<String, Object> ta = castMap(teams.get("away"));
            int fh = th != null ? asInt(th.get("id")) : 0;
            int fa = ta != null ? asInt(ta.get("id")) : 0;
            int gh = goalInt(goals.get("home"));
            int ga = goalInt(goals.get("away"));

            int ghHome = goalsForTeam(fh, fa, gh, ga, tidHome);
            int ghAway = goalsForTeam(fh, fa, gh, ga, tidAway);
            sumHome += ghHome;
            sumAway += ghAway;

            if (gh > 0 && ga > 0) s.btts++;

            if (gh == ga) {
                s.draws++;
                continue;
            }
            int win = gh > ga ? fh : fa;
            if (win == tidHome) s.winsHome++;
            else if (win == tidAway) s.winsAway++;
        }

        if (s.n > 0) {
            s.avgGoalsHome = sumHome / s.n;
            s.avgGoalsAway = sumAway / s.n;
        }
        return s;
    }

    private int goalsForTeam(int fh, int fa, int gh, int ga, int teamId) {
        if (fh == teamId) return gh;
        if (fa == teamId) return ga;
        return 0;
    }

    private void renderComparison(H2hStats s) {
        llCompare.removeAllViews();
        if (s.n <= 0) return;

        addCompareCaption("Victories in the last " + s.n + " head-to-head matches");
        int denom = Math.max(1, s.n);
        addDualBars(s.labelHome, s.labelAway, s.winsHome, s.winsAway, s.draws, denom);

        addCompareCaption("Average goals per match");
        double maxG = Math.max(0.75, Math.max(s.avgGoalsHome, s.avgGoalsAway));
        int lh = Math.round((float) ((s.avgGoalsHome / maxG) * 100));
        int la = Math.round((float) ((s.avgGoalsAway / maxG) * 100));
        addTripleLine(String.format(Locale.getDefault(), "%.2f", s.avgGoalsHome),
                String.format(Locale.getDefault(), "%.2f", s.avgGoalsAway));
        LinearLayout row = dualProgressRow(lh, la);
        llCompare.addView(row);

        int bttsPct = Math.round((100f * s.btts) / Math.max(1, s.n));
        addCompareCaption("Both scored");
        TextView tb = sectionText(String.format(Locale.getDefault(),
                "%d of %d matches (%d%%)", s.btts, s.n, bttsPct), 13);
        tb.setPadding(0, dp(6), 0, dp(10));
        llCompare.addView(tb);

        TextView insight = sectionText(insightSentence(s), 13);
        insight.setTextColor(0xFFBBBBBB);
        insight.setPadding(0, dp(4), 0, 0);
        llCompare.addView(insight);
    }

    private String insightSentence(H2hStats s) {
        if (s.avgGoalsHome + s.avgGoalsAway >= 4.0 && s.btts * 2 >= s.n) {
            return "The series of meetings is often productive: many goals are scored and goals are exchanged.";
        }
        if (s.avgGoalsHome + s.avgGoalsAway <= 2.2) {
            return "There have been relatively few goals in recent head-to-head matches.";
        }
        if (Math.abs(s.winsHome - s.winsAway) >= 3 && s.draws <= 2) {
            return "A clear advantage of one of the sides in victories.";
        }
        return "The difference in victories is small, so close matches are possible.";
    }

    private void addCompareCaption(String text) {
        TextView t = sectionText(text, 14);
        t.setPadding(0, dp(12), 0, dp(6));
        t.setTextColor(0xFFAAFF00);
        llCompare.addView(t);
    }

    private TextView sectionText(String text, int sp) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return tv;
    }

    private void addTripleLine(String homeVal, String awayVal) {
        LinearLayout ll = new LinearLayout(requireContext());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        TextView lh = sectionText(homeVal, 15);
        TextView la = sectionText(awayVal, 15);
        lh.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        la.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        lh.setGravity(android.view.Gravity.START);
        la.setGravity(android.view.Gravity.END);
        ll.addView(lh);
        ll.addView(la);
        llCompare.addView(ll);
    }

    private void addDualBars(String lh, String ra, int winsHome, int winsAway, int draws, int denom) {
        addTripleLine(lh + ": " + winsHome, ra + ": " + winsAway);
        int ph = Math.round(100f * winsHome / denom);
        int pa = Math.round(100f * winsAway / denom);
        llCompare.addView(dualProgressRow(ph, pa));
        TextView td = sectionText("Draw: " + draws, 12);
        td.setTextColor(0xFF888888);
        td.setPadding(0, dp(4), 0, dp(12));
        llCompare.addView(td);
    }

    private LinearLayout dualProgressRow(int percentHome, int percentAway) {
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, dp(8));
        try {
            LinearProgressIndicator barH = new LinearProgressIndicator(requireContext());
            barH.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(10)));
            barH.setIndicatorColor(0xFFAAFF00);
            barH.setTrackColor(0xFF303030);
            barH.setTrackThickness(dp(10));
            barH.setProgressCompat(Math.max(1, percentHome), false);
            col.addView(barH);

            LinearProgressIndicator barA = new LinearProgressIndicator(requireContext());
            LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(10));
            lpA.topMargin = dp(6);
            barA.setLayoutParams(lpA);
            barA.setIndicatorColor(0xFFFFB74D);
            barA.setTrackColor(0xFF303030);
            barA.setTrackThickness(dp(10));
            barA.setProgressCompat(Math.max(1, percentAway), false);
            col.addView(barA);
        } catch (Exception e) {
            
            View vh = new View(requireContext());
            vh.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(10)));
            vh.setBackgroundColor(0xFFAAFF00);
            col.addView(vh);
            View va = new View(requireContext());
            LinearLayout.LayoutParams lpa = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(10));
            lpa.topMargin = dp(6);
            va.setLayoutParams(lpa);
            va.setBackgroundColor(0xFFFFB74D);
            col.addView(va);
        }
        return col;
    }

    private void renderFixtureList(List<Map<String, Object>> fixtures, int tidHome, int tidAway,
                                   String labelHome, String labelAway) {
        llMatches.removeAllViews();
        List<Map<String, Object>> sorted = new ArrayList<>(fixtures);
        Collections.sort(sorted, Comparator.comparingLong(this::fixtureMillis));
        Collections.reverse(sorted);
        int limit = Math.min(H2H_LAST, sorted.size());
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Context ctx = getContext();
        if (ctx == null) return;
        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (int i = 0; i < limit; i++) {
            Map<String, Object> m = sorted.get(i);
            Map<String, Object> teams = castMap(m.get("teams"));
            Map<String, Object> goals = castMap(m.get("goals"));
            Map<String, Object> fixture = castMap(m.get("fixture"));
            Map<String, Object> league = castMap(m.get("league"));
            if (teams == null || goals == null) continue;
            Map<String, Object> th = castMap(teams.get("home"));
            Map<String, Object> ta = castMap(teams.get("away"));
            int fh = th != null ? asInt(th.get("id")) : 0;
            int fa = ta != null ? asInt(ta.get("id")) : 0;
            int gh = goalInt(goals.get("home"));
            int ga = goalInt(goals.get("away"));
            String nHome = th != null ? safe(th.get("name")) : "?";
            String nAway = ta != null ? safe(ta.get("name")) : "?";
            String leagueName = league != null ? safe(league.get("name")) : "";
            String round = league != null ? safe(league.get("round")) : "";
            String dt = "";
            if (fixture != null) {
                try {
                    SimpleDateFormat api = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
                    Date d = api.parse(safe(fixture.get("date")).replace("Z", "+00:00"));
                    if (d != null) dt = df.format(d);
                } catch (Exception ignored) {
                    dt = safe(fixture.get("date"));
                }
            }

            int gForTabHome = goalsForTeam(fh, fa, gh, ga, tidHome);
            int gForTabAway = goalsForTeam(fh, fa, gh, ga, tidAway);
            String outcome;
            if (gForTabHome > gForTabAway) outcome = "Win: " + labelHome;
            else if (gForTabAway > gForTabHome) outcome = "Win: " + labelAway;
            else outcome = "Draw";

            View itemView = inflater.inflate(R.layout.item_h2h_match, llMatches, false);

            TextView tvHeader = itemView.findViewById(R.id.tv_h2h_header);
            TextView tvHomeName = itemView.findViewById(R.id.tv_h2h_home_name);
            TextView tvScore = itemView.findViewById(R.id.tv_h2h_score);
            TextView tvAwayName = itemView.findViewById(R.id.tv_h2h_away_name);
            TextView tvOutcome = itemView.findViewById(R.id.tv_h2h_outcome);
            ImageView ivHomeLogo = itemView.findViewById(R.id.iv_h2h_home_logo);
            ImageView ivAwayLogo = itemView.findViewById(R.id.iv_h2h_away_logo);

            String header = dt + (leagueName.isEmpty() ? "" : " · " + leagueName);
            tvHeader.setText(header);

            tvHomeName.setText(nHome);
            tvScore.setText(gh + " - " + ga);
            tvAwayName.setText(nAway);

            String outcomeText = labelHome + " (" + gForTabHome + " - " + gForTabAway + ") " + labelAway + " | " + outcome + (round.isEmpty() ? "" : " | " + round);
            tvOutcome.setText(outcomeText);

            com.bumptech.glide.Glide.with(ctx)
                    .load(th != null ? safe(th.get("logo")) : "")
                    .into(ivHomeLogo);

            com.bumptech.glide.Glide.with(ctx)
                    .load(ta != null ? safe(ta.get("logo")) : "")
                    .into(ivAwayLogo);

            llMatches.addView(itemView);

            if (i < limit - 1) {
                View div = new View(ctx);
                div.setBackgroundColor(0x33272727);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                dlp.leftMargin = dp(8);
                dlp.rightMargin = dp(8);
                div.setLayoutParams(dlp);
                llMatches.addView(div);
            }
        }
    }

    private long fixtureMillis(Map<String, Object> m) {
        Map<String, Object> fx = castMap(m.get("fixture"));
        if (fx == null) return 0L;
        try {
            SimpleDateFormat api = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
            Date d = api.parse(safe(fx.get("date")).replace("Z", "+00:00"));
            return d != null ? d.getTime() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private int dp(int x) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(x * d);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    private int asInt(Object v) {
        try {
            if (v == null) return 0;
            if (v instanceof Number) return ((Number) v).intValue();
            String s = String.valueOf(v).trim();
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private int goalInt(Object v) {
        if (v == null) return 0;
        try {
            String s = String.valueOf(v).trim();
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s.replaceAll("[^0-9+-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String safe(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        return "null".equalsIgnoreCase(s) ? "" : s;
    }

    private String nz(String s) {
        return s == null ? "" : s.trim();
    }
}