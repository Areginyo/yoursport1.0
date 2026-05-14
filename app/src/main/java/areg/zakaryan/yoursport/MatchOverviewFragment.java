package areg.zakaryan.yoursport;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.concurrent.atomic.AtomicInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.MatchItem;
import areg.zakaryan.yoursport.model.NewsItem;
import areg.zakaryan.yoursport.model.NewsResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchOverviewFragment extends Fragment {

    private static final String ARG_MATCH = "match";
    private static final int FORM_LAST = 10;
    private static final int NEWS_MAX = 8;
    private static final String NEWS_API_KEY = "e2c5ad86dd95403c8a9f7e535d1f3d56";

    private LinearLayout llFormHomeScroll;
    private LinearLayout llFormAwayScroll;
    private LinearLayout llDetailsContainer;
    private LinearLayout llNewsContainer;
    private ProgressBar progressForm;
    private ProgressBar progressDetails;
    private ProgressBar progressNews;
    private TextView tvFormHomeTeam;
    private TextView tvFormAwayTeam;
    private TextView tvNewsEmpty;
    private TextView tvDetailsFallback;
    private TextView tvLeagueSmall;

    // Live events views
    private LinearLayout llLiveEventsSection;
    private TextView tvLiveBadge;
    private TextView tvFinishedBadge;
    private TextView tvLiveHomeTeam;
    private TextView tvLiveScore;
    private TextView tvLiveAwayTeam;
    private TextView tvLiveMinute;
    private ProgressBar progressEvents;
    private LinearLayout llEventsContainer;
    private TextView tvEventsEmpty;

    public static MatchOverviewFragment newInstance(MatchItem match) {
        MatchOverviewFragment fragment = new MatchOverviewFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MATCH, match);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_match_overview, container, false);
        llFormHomeScroll = view.findViewById(R.id.ll_form_home_scroll);
        llFormAwayScroll = view.findViewById(R.id.ll_form_away_scroll);
        llDetailsContainer = view.findViewById(R.id.ll_details_container);
        llNewsContainer = view.findViewById(R.id.ll_news_container);
        progressForm = view.findViewById(R.id.progress_form);
        progressDetails = view.findViewById(R.id.progress_details);
        progressNews = view.findViewById(R.id.progress_news);
        tvFormHomeTeam = view.findViewById(R.id.tv_form_home_team);
        tvFormAwayTeam = view.findViewById(R.id.tv_form_away_team);
        tvNewsEmpty = view.findViewById(R.id.tv_news_empty);
        tvDetailsFallback = view.findViewById(R.id.tv_overview_details_fallback);
        tvLeagueSmall = view.findViewById(R.id.tv_league_name_small);

        llLiveEventsSection = view.findViewById(R.id.ll_live_events_section);
        tvLiveBadge = view.findViewById(R.id.tv_live_badge);
        tvFinishedBadge = view.findViewById(R.id.tv_finished_badge);
        tvLiveHomeTeam = view.findViewById(R.id.tv_live_home_team);
        tvLiveScore = view.findViewById(R.id.tv_live_score);
        tvLiveAwayTeam = view.findViewById(R.id.tv_live_away_team);
        tvLiveMinute = view.findViewById(R.id.tv_live_minute);
        progressEvents = view.findViewById(R.id.progress_events);
        llEventsContainer = view.findViewById(R.id.ll_events_container);
        tvEventsEmpty = view.findViewById(R.id.tv_events_empty);

        MatchItem match = getArguments() != null ? getArguments().getParcelable(ARG_MATCH) : null;
        if (match == null) {
            showEmpty("No match data.");
            return view;
        }

        tvFormHomeTeam.setText(match.homeTeam != null ? match.homeTeam : "Home");
        tvFormAwayTeam.setText(match.awayTeam != null ? match.awayTeam : "Away");
        progressForm.setVisibility(View.VISIBLE);
        progressDetails.setVisibility(View.VISIBLE);
        tvDetailsFallback.setVisibility(View.GONE);

        loadMatchBundle(match);
        return view;
    }

    private void showEmpty(String msg) {
        if (progressForm != null) progressForm.setVisibility(View.GONE);
        if (progressDetails != null) progressDetails.setVisibility(View.GONE);
        if (tvDetailsFallback != null) {
            tvDetailsFallback.setText(msg);
            tvDetailsFallback.setVisibility(View.VISIBLE);
        }
    }

    private void loadMatchBundle(MatchItem match) {
        if (match.matchId == null || match.matchId.trim().isEmpty()) {
            progressForm.setVisibility(View.GONE);
            progressDetails.setVisibility(View.GONE);
            renderDetailsFromMatchItem(match);
            setupLiveEventsFromMatchItem(match);
            loadNews(match);
            return;
        }

        ApiClient.getApiService().getMatchById(match.matchId).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                Object body = response.isSuccessful() ? response.body() : null;
                Map<String, Object> fixtureMap = unwrapFixture(body);
                if (fixtureMap == null) {
                    progressDetails.setVisibility(View.GONE);
                    renderDetailsFromMatchItem(match);
                    setupLiveEventsFromMatchItem(match);
                    loadTeamForm(match.homeTeamId, match.awayTeamId, llFormHomeScroll, llFormAwayScroll, match.matchId);
                    loadNews(match);
                    return;
                }

                enrichMatchFromFixture(fixtureMap, match);
                progressDetails.setVisibility(View.GONE);
                populateDetails(fixtureMap, match);
                setupLiveEvents(fixtureMap, match);
                int hid = resolveTeamIdFromFixture(fixtureMap, true, match.homeTeamId);
                int aid = resolveTeamIdFromFixture(fixtureMap, false, match.awayTeamId);
                loadTeamForm(hid, aid, llFormHomeScroll, llFormAwayScroll, match.matchId);
                loadNews(match);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (!isAdded()) return;
                progressDetails.setVisibility(View.GONE);
                renderDetailsFromMatchItem(match);
                setupLiveEventsFromMatchItem(match);
                loadTeamForm(match.homeTeamId, match.awayTeamId, llFormHomeScroll, llFormAwayScroll, match.matchId);
                loadNews(match);
            }
        });
    }

    /** Fill match.teamIds when API returns fuller data than list adapter. */
    @SuppressWarnings("unchecked")
    private void enrichMatchFromFixture(Map<String, Object> m, MatchItem match) {
        Map<String, Object> teams = castMap(m.get("teams"));
        if (teams == null) return;
        Map<String, Object> home = castMap(teams.get("home"));
        Map<String, Object> away = castMap(teams.get("away"));
        if (home != null && match.homeTeamId <= 0) match.homeTeamId = asInt(home.get("id"));
        if (away != null && match.awayTeamId <= 0) match.awayTeamId = asInt(away.get("id"));
    }

    private int resolveTeamIdFromFixture(Map<String, Object> m, boolean wantHome, int fallbackId) {
        Map<String, Object> teams = castMap(m.get("teams"));
        if (teams != null) {
            Map<String, Object> t = castMap(teams.get(wantHome ? "home" : "away"));
            if (t != null) {
                int id = asInt(t.get("id"));
                if (id > 0) return id;
            }
        }
        return fallbackId;
    }

    private void populateDetails(Map<String, Object> m, MatchItem match) {
        llDetailsContainer.removeAllViews();

        Map<String, Object> fixture = castMap(m.get("fixture"));
        Map<String, Object> league = castMap(m.get("league"));
        Map<String, Object> venue = fixture != null ? castMap(fixture.get("venue")) : null;

        String dateRaw = fixture != null ? safe(fixture.get("date")) : "";
        String venueLine = joinVenue(venue);
        String referee = fixture != null ? safe(fixture.get("referee")) : "";
        String leagueName = league != null ? safe(league.get("name")) : safe(match.leagueName);
        String round = league != null ? safe(league.get("round")) : "";

        tvLeagueSmall.setText(leagueName);
        tvLeagueSmall.setVisibility(leagueName.isEmpty() ? View.GONE : View.VISIBLE);

        addDetailRow(llDetailsContainer, "🗓️", resolveDateCaption(dateRaw), null);
        addDetailRow(llDetailsContainer, "🏆",
                leagueName.isEmpty() ? "—" : leagueName,
                round.isEmpty() ? null : round);
        addDetailRow(llDetailsContainer, "🏟️", venueLine.isEmpty() ? "—" : venueLine, null);
        addDetailRow(llDetailsContainer, "👤", referee.isEmpty() ? "—" : referee, null);
    }

    private String joinVenue(Map<String, Object> venue) {
        if (venue == null) return "";
        String name = safe(venue.get("name"));
        String city = safe(venue.get("city"));
        if (!name.isEmpty() && !city.isEmpty()) return name + ", " + city;
        return name.isEmpty() ? city : name;
    }

    private void renderDetailsFromMatchItem(MatchItem match) {
        llDetailsContainer.removeAllViews();
        addDetailRow(llDetailsContainer, "🗓️",
                formatDateHuman(match.date, match.time),
                safe(match.score).isEmpty() ? null : "Match score: " + match.score);
        addDetailRow(llDetailsContainer, "🏆", emptyDash(match.leagueName), null);
        String statusStr = emptyDash(match.status);
        addDetailRow(llDetailsContainer, "ℹ️", "Status", statusStr);
    }

    private void addDetailRow(LinearLayout parent, String emoji, String main, String subtitle) {
        int padH = dp(16);
        int padV = dp(14);

        LinearLayout outer = new LinearLayout(requireContext());
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setPadding(padH, padV, padH, padV);

        TextView emojiTv = new TextView(requireContext());
        emojiTv.setText(emoji);
        emojiTv.setTextSize(18);
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(dp(34), LinearLayout.LayoutParams.WRAP_CONTENT);
        emojiTv.setLayoutParams(eLp);

        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(tLp);

        TextView mainTv = new TextView(requireContext());
        mainTv.setTextColor(0xFFFFFFFF);
        mainTv.setTextSize(14);
        mainTv.setText(main);
        textCol.addView(mainTv);

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView subTv = new TextView(requireContext());
            subTv.setTextColor(0xFF9C9C9C);
            subTv.setTextSize(12);
            subTv.setText(subtitle.trim());
            subTv.setPadding(0, dp(4), 0, 0);
            textCol.addView(subTv);
        }

        outer.addView(emojiTv);
        outer.addView(textCol);
        parent.addView(outer);

        View divider = new View(requireContext());
        divider.setBackgroundColor(0x332A2A2A);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        dLp.leftMargin = padH + dp(34);
        divider.setLayoutParams(dLp);
        parent.addView(divider);
    }

    private int dp(int dps) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dps * d);
    }

    private String resolveDateCaption(String utcIso) {
        if (utcIso == null || utcIso.isEmpty()) return "—";
        try {
            SimpleDateFormat api = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
            Date dt = api.parse(utcIso.replace("Z", "+00:00"));
            if (dt == null) return utcIso.replace("T", " ");
            Calendar cal = Calendar.getInstance();
            Calendar today = Calendar.getInstance();
            cal.setTime(dt);
            boolean sameDay = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            Calendar y = Calendar.getInstance();
            y.add(Calendar.DAY_OF_YEAR, -1);
            boolean yesterday = cal.get(Calendar.YEAR) == y.get(Calendar.YEAR)
                    && cal.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR);
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            if (sameDay) return "Today in " + tf.format(dt);
            if (yesterday) return "Yesterday in " + tf.format(dt);
            SimpleDateFormat full = new SimpleDateFormat("d MMMM · HH:mm", new Locale("ru"));
            full.setTimeZone(TimeZone.getDefault());
            tf.setTimeZone(TimeZone.getDefault());
            SimpleDateFormat dayOnly = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
            dayOnly.setTimeZone(TimeZone.getDefault());
            return dayOnly.format(dt) + " · " + tf.format(dt);
        } catch (Exception e) {
            return utcIso.replace("T", " ").replace("Z", "");
        }
    }

    private String formatDateHuman(String date, String time) {
        if (time != null && time.contains("T")) return resolveDateCaption(time);
        if (date != null && !date.trim().isEmpty()) return date;
        return "—";
    }

    private interface FixturesCallback {
        void onLoaded(List<Map<String, Object>> rows);
    }

    private void loadTeamForm(int homeTeamId, int awayTeamId, LinearLayout llHome,
                              LinearLayout llAway, String currentFixtureId) {
        if (homeTeamId <= 0 && awayTeamId <= 0) {
            progressForm.setVisibility(View.GONE);
            llHome.removeAllViews();
            llAway.removeAllViews();
            addChipPlaceholder(llHome, "No team ID");
            addChipPlaceholder(llAway, "No team ID");
            return;
        }

        AtomicInteger pending = new AtomicInteger(0);
        if (homeTeamId > 0) pending.incrementAndGet();
        if (awayTeamId > 0) pending.incrementAndGet();

        Runnable onOneDone = () -> {
            if (!isAdded()) return;
            if (pending.decrementAndGet() <= 0) {
                progressForm.setVisibility(View.GONE);
            }
        };

        if (homeTeamId > 0) {
            fetchLastFixtures(homeTeamId, currentFixtureId, rows -> {
                fillForm(llHome, rows, homeTeamId);
                onOneDone.run();
            });
        } else {
            llHome.removeAllViews();
            addChipPlaceholder(llHome, "—");
        }

        if (awayTeamId > 0) {
            fetchLastFixtures(awayTeamId, currentFixtureId, rows -> {
                fillForm(llAway, rows, awayTeamId);
                onOneDone.run();
            });
        } else {
            llAway.removeAllViews();
            addChipPlaceholder(llAway, "—");
        }

        int total = pending.get();
        if (total == 0) {
            progressForm.setVisibility(View.GONE);
        }
    }

    private void fetchLastFixtures(int teamId, String excludeFixtureId, FixturesCallback onDone) {
        ApiClient.getApiService().getLastFixturesByTeam(teamId, FORM_LAST).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                List<Map<String, Object>> rows = extractFixtureRows(response.body());
                rows.removeIf(m -> {
                    if (excludeFixtureId == null) return false;
                    Map<String, Object> fx = castMap(m.get("fixture"));
                    return fx != null && excludeFixtureId.equals(String.valueOf(asInt(fx.get("id"))));
                });
                onDone.onLoaded(rows);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (!isAdded()) return;
                onDone.onLoaded(Collections.emptyList());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFixtureRows(Object raw) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(raw instanceof Map)) return out;
        Object resp = ((Map<?, ?>) raw).get("response");
        if (!(resp instanceof List)) return out;
        for (Object o : (List<?>) resp) {
            if (o instanceof Map) out.add((Map<String, Object>) o);
        }
        return out;
    }

    private void fillForm(LinearLayout container, List<Map<String, Object>> fixtures, int teamFocusedId) {
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            container.removeAllViews();
            if (fixtures.isEmpty()) {
                addChipPlaceholder(container, "No data");
                return;
            }
            List<Map<String, Object>> sorted = new ArrayList<>(fixtures);
            Collections.sort(sorted, Comparator.comparingInt(m -> {
                Map<String, Object> fx = castMap(m.get("fixture"));
                try {
                    SimpleDateFormat api = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
                    Date d = api.parse(safe(fx != null ? fx.get("date") : "").replace("Z", "+00:00"));
                    return d != null ? (int) (d.getTime() / 1000L) : 0;
                } catch (Exception e) {
                    return 0;
                }
            }));
            Collections.reverse(sorted);
            for (Map<String, Object> m : sorted) {
                View chip = buildFormChip(m, teamFocusedId);
                if (chip != null) container.addView(chip);
            }
        });
    }

    private void addChipPlaceholder(LinearLayout container, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xFF888888);
        tv.setPadding(dp(8), dp(8), dp(8), dp(8));
        container.addView(tv);
    }

    @SuppressWarnings("unchecked")
    private View buildFormChip(Map<String, Object> m, int teamFocusedId) {
        Map<String, Object> teams = castMap(m.get("teams"));
        Map<String, Object> goals = castMap(m.get("goals"));
        Map<String, Object> fixture = castMap(m.get("fixture"));
        if (teams == null || goals == null) return null;

        Map<String, Object> th = castMap(teams.get("home"));
        Map<String, Object> ta = castMap(teams.get("away"));
        int homeId = th != null ? asInt(th.get("id")) : 0;
        int awayId = ta != null ? asInt(ta.get("id")) : 0;
        int gh = goalInt(goals.get("home"));
        int ga = goalInt(goals.get("away"));

        String opponent;
        boolean weHome = (homeId == teamFocusedId);
        if (weHome) {
            opponent = ta != null ? safe(ta.get("name")) : "Away";
        } else {
            opponent = th != null ? safe(th.get("name")) : "Home";
        }

        int forUs = weHome ? gh : ga;
        int vsUs = weHome ? ga : gh;

        Character resultRus;
        int colorBadge;
        if (forUs > vsUs) {
            resultRus = 'W';
            colorBadge = 0xFF2E7D32;
        } else if (forUs < vsUs) {
            resultRus = 'L';
            colorBadge = 0xFFC62828;
        } else {
            resultRus = 'D';
            colorBadge = 0xFF757575;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View chip = inflater.inflate(R.layout.item_overview_form_chip, null, false);
        TextView tvOpp = chip.findViewById(R.id.tv_opponent);
        TextView tvSc = chip.findViewById(R.id.tv_score);
        TextView tvR = chip.findViewById(R.id.tv_result);
        tvOpp.setText(shortName(opponent, 26));
        tvSc.setText(forUs + " : " + vsUs);
        tvR.setText(String.valueOf(resultRus));
        tvR.setTextColor(colorBadge);
        return chip;
    }

    private String shortName(String name, int maxLen) {
        if (name == null) return "";
        if (name.length() <= maxLen) return name;
        return name.substring(0, maxLen - 1) + "…";
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

    private void loadNews(MatchItem match) {
        progressNews.setVisibility(View.VISIBLE);
        llNewsContainer.removeAllViews();
        tvNewsEmpty.setVisibility(View.GONE);

        String hq = "\"" + nz(match.homeTeam) + "\" OR \"" + nz(match.awayTeam) + "\"";
        String q = "(" + nz(match.homeTeam) + ") AND (" + nz(match.awayTeam) + ") AND football";
        ApiClient.getNewsApiService().getEverything(q + " OR " + hq, "en", "publishedAt", NEWS_API_KEY)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        if (!isAdded()) return;
                        progressNews.setVisibility(View.GONE);
                        List<NewsItem> items = toNewsItems(response.body());
                        if (items.isEmpty()) {
                            tvNewsEmpty.setText("There is no news on this match..");
                            tvNewsEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        int n = Math.min(NEWS_MAX, items.size());
                        for (int i = 0; i < n; i++) {
                            NewsItem it = items.get(i);
                            View row = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.item_overview_news, llNewsContainer, false);
                            TextView t = row.findViewById(R.id.tv_news_title);
                            TextView m = row.findViewById(R.id.tv_news_meta);
                            t.setText(it.title);
                            m.setText((it.source != null ? it.source : "") + (it.date != null ? " · " + it.date : ""));
                            row.setOnClickListener(v -> {
                                if (it.url != null && !it.url.isEmpty()) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(it.url)));
                                }
                            });
                            llNewsContainer.addView(row);
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        progressNews.setVisibility(View.GONE);
                        tvNewsEmpty.setText("Failed to load news.");
                        tvNewsEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private List<NewsItem> toNewsItems(NewsResponse response) {
        List<NewsItem> list = new ArrayList<>();
        if (response == null || response.articles == null) return list;
        for (NewsResponse.Article a : response.articles) {
            if (a == null) continue;
            NewsItem it = new NewsItem();
            it.title = a.title != null ? a.title : "";
            it.description = a.description != null ? a.description : "";
            it.date = a.publishedAt;
            it.source = a.source != null ? a.source.name : "";
            it.url = a.url;
            if (!it.title.isEmpty()) list.add(it);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapFixture(Object raw) {
        if (!(raw instanceof Map)) return null;
        Object responseObj = ((Map<String, Object>) raw).get("response");
        if (!(responseObj instanceof List) || ((List<?>) responseObj).isEmpty()) return null;
        Object first = ((List<?>) responseObj).get(0);
        return first instanceof Map ? (Map<String, Object>) first : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private int asInt(Object val) {
        try {
            if (val == null) return 0;
            if (val instanceof Number) return ((Number) val).intValue();
            String s = String.valueOf(val).trim();
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String safe(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        return "null".equalsIgnoreCase(s) ? "" : s;
    }

    private String emptyDash(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    // ==================== LIVE EVENTS ====================

    private boolean isLiveStatus(String s) {
        if (s == null) return false;
        s = s.toUpperCase().trim();
        return s.equals("1H") || s.equals("2H") || s.equals("HT") || s.equals("ET")
                || s.equals("BT") || s.equals("P") || s.equals("SUSP") || s.equals("INT") || s.equals("LIVE");
    }

    private boolean isFinishedStatus(String s) {
        if (s == null) return false;
        s = s.toUpperCase().trim();
        return s.equals("FT") || s.equals("AET") || s.equals("PEN");
    }

    @SuppressWarnings("unchecked")
    private void setupLiveEvents(Map<String, Object> fixtureMap, MatchItem match) {
        Map<String, Object> fixture = castMap(fixtureMap.get("fixture"));
        Map<String, Object> statusMap = fixture != null ? castMap(fixture.get("status")) : null;
        String shortStatus = statusMap != null ? safe(statusMap.get("short")) : "";
        int elapsed = statusMap != null ? asInt(statusMap.get("elapsed")) : 0;

        boolean live = isLiveStatus(shortStatus) || match.isLive;
        boolean finished = isFinishedStatus(shortStatus)
                || (!match.isLive && match.status != null && isFinishedStatus(match.status));

        if (!live && !finished) {
            llLiveEventsSection.setVisibility(View.GONE);
            return;
        }

        llLiveEventsSection.setVisibility(View.VISIBLE);

        Map<String, Object> teams = castMap(fixtureMap.get("teams"));
        Map<String, Object> goals = castMap(fixtureMap.get("goals"));
        if (teams != null) {
            Map<String, Object> h = castMap(teams.get("home"));
            Map<String, Object> a = castMap(teams.get("away"));
            tvLiveHomeTeam.setText(h != null ? safe(h.get("name")) : safe(match.homeTeam));
            tvLiveAwayTeam.setText(a != null ? safe(a.get("name")) : safe(match.awayTeam));
        } else {
            tvLiveHomeTeam.setText(safe(match.homeTeam));
            tvLiveAwayTeam.setText(safe(match.awayTeam));
        }
        if (goals != null) {
            tvLiveScore.setText(asInt(goals.get("home")) + " : " + asInt(goals.get("away")));
        } else if (match.score != null) {
            tvLiveScore.setText(match.score);
        }

        if (live) {
            tvLiveBadge.setVisibility(View.VISIBLE);
            tvFinishedBadge.setVisibility(View.GONE);
            if (elapsed > 0) {
                tvLiveMinute.setText(elapsed + "'");
                tvLiveMinute.setVisibility(View.VISIBLE);
            }
        } else {
            tvLiveBadge.setVisibility(View.GONE);
            tvFinishedBadge.setText(shortStatus.isEmpty() ? "FT" : shortStatus.toUpperCase());
            tvFinishedBadge.setVisibility(View.VISIBLE);
            tvLiveMinute.setVisibility(View.GONE);
        }

        loadFixtureEvents(match);
    }

    private void setupLiveEventsFromMatchItem(MatchItem match) {
        boolean live = match.isLive;
        boolean finished = match.status != null && isFinishedStatus(match.status);
        if (!live && !finished) {
            llLiveEventsSection.setVisibility(View.GONE);
            return;
        }
        llLiveEventsSection.setVisibility(View.VISIBLE);
        tvLiveHomeTeam.setText(safe(match.homeTeam));
        tvLiveAwayTeam.setText(safe(match.awayTeam));
        if (match.score != null && !match.score.isEmpty()) tvLiveScore.setText(match.score);
        if (live) {
            tvLiveBadge.setVisibility(View.VISIBLE);
            tvFinishedBadge.setVisibility(View.GONE);
        } else {
            tvLiveBadge.setVisibility(View.GONE);
            tvFinishedBadge.setText("FT");
            tvFinishedBadge.setVisibility(View.VISIBLE);
        }
        loadFixtureEvents(match);
    }

    private void loadFixtureEvents(MatchItem match) {
        if (match.matchId == null || match.matchId.trim().isEmpty()) {
            tvEventsEmpty.setText("No match ID");
            tvEventsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        progressEvents.setVisibility(View.VISIBLE);
        tvEventsEmpty.setVisibility(View.GONE);
        llEventsContainer.removeAllViews();

        ApiClient.getApiService().getFixtureEvents(match.matchId).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                progressEvents.setVisibility(View.GONE);
                List<Map<String, Object>> events = extractEventsList(response.body());
                if (events.isEmpty()) {
                    tvEventsEmpty.setText("No events yet");
                    tvEventsEmpty.setVisibility(View.VISIBLE);
                    return;
                }
                renderEvents(events);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (!isAdded()) return;
                progressEvents.setVisibility(View.GONE);
                tvEventsEmpty.setText("Failed to load events");
                tvEventsEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractEventsList(Object raw) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(raw instanceof Map)) return out;
        Object resp = ((Map<?, ?>) raw).get("response");
        if (!(resp instanceof List)) return out;
        for (Object o : (List<?>) resp) {
            if (o instanceof Map) out.add((Map<String, Object>) o);
        }
        return out;
    }

    private void renderEvents(List<Map<String, Object>> events) {
        llEventsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Map<String, Object> ev : events) {
            View row = inflater.inflate(R.layout.item_live_event, llEventsContainer, false);
            TextView tvMin = row.findViewById(R.id.tv_event_minute);
            TextView tvIco = row.findViewById(R.id.tv_event_icon);
            TextView tvPl  = row.findViewById(R.id.tv_event_player);
            TextView tvDet = row.findViewById(R.id.tv_event_detail);
            TextView tvTm  = row.findViewById(R.id.tv_event_team);

            Map<String, Object> time = castMap(ev.get("time"));
            int elapsed = time != null ? asInt(time.get("elapsed")) : 0;
            int extra   = time != null ? asInt(time.get("extra")) : 0;
            tvMin.setText(extra > 0 ? elapsed + "+" + extra + "'" : elapsed + "'");

            String type   = safe(ev.get("type"));
            String detail = safe(ev.get("detail"));
            tvIco.setText(eventEmoji(type, detail));

            Map<String, Object> player = castMap(ev.get("player"));
            Map<String, Object> assist = castMap(ev.get("assist"));
            String pName = player != null ? safe(player.get("name")) : "";
            String aName = assist != null ? safe(assist.get("name")) : "";
            tvPl.setText(pName.isEmpty() ? "—" : pName);

            String detailTxt = eventDetail(type, detail, aName);
            if (!detailTxt.isEmpty()) {
                tvDet.setText(detailTxt);
                tvDet.setVisibility(View.VISIBLE);
            } else {
                tvDet.setVisibility(View.GONE);
            }

            Map<String, Object> team = castMap(ev.get("team"));
            tvTm.setText(shortName(team != null ? safe(team.get("name")) : "", 12));

            llEventsContainer.addView(row);
        }
    }

    private String eventEmoji(String type, String detail) {
        if (type == null) return "•";
        switch (type.toLowerCase()) {
            case "goal":
                if (detail != null && detail.toLowerCase().contains("own"))    return "⚽\uD83D\uDFE5";
                if (detail != null && detail.toLowerCase().contains("penalty") && detail.toLowerCase().contains("missed")) return "❌";
                if (detail != null && detail.toLowerCase().contains("penalty")) return "⚽(P)";
                return "⚽";
            case "card":
                if (detail != null && detail.toLowerCase().contains("red"))    return "\uD83D\uDFE5";
                if (detail != null && detail.toLowerCase().contains("second")) return "\uD83D\uDFE8\uD83D\uDFE5";
                return "\uD83D\uDFE8";
            case "subst": return "\uD83D\uDD04";
            case "var":   return "\uD83D\uDCFA";
            default:      return "•";
        }
    }

    private String eventDetail(String type, String detail, String assistName) {
        if (type == null) return "";
        switch (type.toLowerCase()) {
            case "goal":
                if (detail != null && detail.toLowerCase().contains("own"))    return "Own Goal";
                if (detail != null && detail.toLowerCase().contains("missed")) return "Missed Penalty";
                if (detail != null && detail.toLowerCase().contains("penalty")) {
                    return !assistName.isEmpty() ? "Penalty · Assist: " + assistName : "Penalty";
                }
                return !assistName.isEmpty() ? "Assist: " + assistName : "";
            case "card":
                return detail != null ? detail : "";
            case "subst":
                return !assistName.isEmpty() ? "↔ " + assistName : "";
            case "var":
                return detail != null ? detail : "";
            default:
                return "";
        }
    }
}