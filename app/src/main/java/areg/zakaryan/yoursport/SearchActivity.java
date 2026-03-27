package areg.zakaryan.yoursport;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.api.ApiClientSports;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText edtSearch;
    private RecyclerView recycler;
    private Button btnContinue;
    private ProgressBar progressBar;

    private SearchAdapter adapter;

    private final List<SearchItem> allLeagues = new ArrayList<>();
    private final List<SearchItem> allTeams = new ArrayList<>();

    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    private ArrayList<String> selectedSports = new ArrayList<>();
    private final ArrayList<SearchItem> selectedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtSearch = findViewById(R.id.edtSearch);
        recycler = findViewById(R.id.recyclerSearch);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progressBar);

        selectedSports = getIntent().getStringArrayListExtra("selected_sports");
        if (selectedSports == null || selectedSports.isEmpty()) {
            selectedSports = new ArrayList<>();
            selectedSports.add("Football");
        }

        adapter = new SearchAdapter(selectedItems, item -> {
            if (selectedItems.contains(item)) {
                selectedItems.remove(item);
            } else {
                selectedItems.add(item);
            }
            btnContinue.setVisibility(selectedItems.isEmpty() ? View.GONE : View.VISIBLE);
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnContinue.setVisibility(View.GONE);

        loadInitialData();

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() >= 3) {
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, 500);
                } else {
                    showInitialList();
                }
            }
        });

        btnContinue.setOnClickListener(v -> {
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Select at least one item", Toast.LENGTH_SHORT).show();
                return;
            }

            // Сохраняем флаг завершения онбординга
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("onboarding_completed", true)
                    .apply();

            Intent intent = new Intent(SearchActivity.this, HomeActivity.class);
            intent.putParcelableArrayListExtra("selected_items", selectedItems);

            // Очищаем весь стек: SportChoice и SearchActivity больше не будут открываться
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });
    }

    private void loadInitialData() {
        progressBar.setVisibility(View.VISIBLE);

        if (selectedSports.contains("Football")) {
            loadFootballLeagues();
        }

        if (selectedSports.contains("Basketball")) {
            loadSportsDbTeams("NBA");
        }

        if (selectedSports.contains("Formula 1")) {
            loadSportsDbTeams("Formula 1");
        }
    }

    private void loadFootballLeagues() {
        ApiClient.getApiService().getCompetitions().enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("Search", "Failed to load leagues");
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    return;
                }

                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<Map<String, Object>> competitions = (List<Map<String, Object>>) body.get("competitions");

                    List<String> leagueCodes = new ArrayList<>();

                    if (competitions != null) {
                        for (Map<String, Object> comp : competitions) {
                            Number idNum = (Number) comp.get("id");
                            int id = idNum != null ? idNum.intValue() : 0;
                            String name = (String) comp.get("name");
                            String code = (String) comp.get("code");
                            String emblem = (String) comp.get("emblem");

                            Map<String, Object> area = (Map<String, Object>) comp.get("area");
                            String country = area != null ? (String) area.get("name") : "";

                            allLeagues.add(new SearchItem(
                                    SearchItem.TYPE_ITEM,
                                    name,
                                    country,
                                    emblem,
                                    id,
                                    "league"
                            ));

                            if (code != null && !code.isEmpty()) {
                                leagueCodes.add(code);
                            }
                        }
                    }

                    loadTeamsForLeagues(leagueCodes, 0);

                } catch (Exception e) {
                    Log.e("Search", "Leagues parsing error", e);
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e("Search", "Leagues network error", t);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadTeamsForLeagues(List<String> codes, int index) {
        if (index >= codes.size()) {
            showInitialList();
            progressBar.setVisibility(View.GONE);
            return;
        }

        String code = codes.get(index);

        ApiClient.getApiService().getTeamsByLeague(code).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> body = (Map<String, Object>) response.body();
                        List<Map<String, Object>> teams = (List<Map<String, Object>>) body.get("teams");

                        if (teams != null) {
                            for (Map<String, Object> team : teams) {
                                Number idNum = (Number) team.get("id");
                                int id = idNum != null ? idNum.intValue() : 0;
                                String name = (String) team.get("name");
                                String crest = (String) team.get("crest");

                                Map<String, Object> area = (Map<String, Object>) team.get("area");
                                String country = area != null ? (String) area.get("name") : "";

                                boolean exists = allTeams.stream().anyMatch(t -> t.id == id);
                                if (!exists && id > 0) {
                                    allTeams.add(new SearchItem(
                                            SearchItem.TYPE_ITEM,
                                            name,
                                            country,
                                            crest,
                                            id,
                                            "team"
                                    ));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Search", "Teams parsing error", e);
                    }
                }
                loadTeamsForLeagues(codes, index + 1);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                loadTeamsForLeagues(codes, index + 1);
            }
        });
    }

    private void loadSportsDbTeams(String leagueName) {
        ApiClientSports.getApiService().getAllTeams(leagueName).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> body = (Map<String, Object>) response.body();
                        List<Map<String, Object>> teams = (List<Map<String, Object>>) body.get("teams");

                        if (teams != null) {
                            for (Map<String, Object> team : teams) {
                                String idStr = (String) team.get("idTeam");
                                int id = idStr != null ? Integer.parseInt(idStr) : 0;
                                if (id == 0) continue;

                                String name = (String) team.get("strTeam");
                                String badge = (String) team.get("strBadge");
                                String country = (String) team.get("strCountry");

                                if (allTeams.stream().noneMatch(t -> t.id == id)) {
                                    allTeams.add(new SearchItem(
                                            SearchItem.TYPE_ITEM,
                                            name,
                                            country,
                                            badge,
                                            id,
                                            "team"
                                    ));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Search", "SportsDB parsing error", e);
                    }
                }
                showInitialList();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                showInitialList();
            }
        });
    }

    private void performSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);

        String lowerQuery = query.toLowerCase();
        List<SearchItem> results = new ArrayList<>();

        // Leagues
        List<SearchItem> filteredLeagues = new ArrayList<>();
        for (SearchItem item : allLeagues) {
            if (item.title != null && item.title.toLowerCase().contains(lowerQuery)) {
                filteredLeagues.add(item);
            }
        }
        if (!filteredLeagues.isEmpty()) {
            results.add(new SearchItem("LEAGUES"));
            results.addAll(filteredLeagues);
        }

        // Teams
        List<SearchItem> filteredTeams = new ArrayList<>();
        for (SearchItem item : allTeams) {
            if (item.title != null && item.title.toLowerCase().contains(lowerQuery)) {
                filteredTeams.add(item);
            }
        }
        if (!filteredTeams.isEmpty()) {
            results.add(new SearchItem("TEAMS"));
            results.addAll(filteredTeams);
        }

        // Players via TheSportsDB
        ApiClientSports.getApiService().searchPlayers(query).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                List<SearchItem> players = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> body = (Map<String, Object>) response.body();
                        List<Map<String, Object>> playerList = (List<Map<String, Object>>) body.get("player");

                        if (playerList != null) {
                            for (Map<String, Object> player : playerList) {
                                String sport = (String) player.get("strSport");
                                String appSport = getAppSportName(sport);

                                if (selectedSports.contains(appSport)) {
                                    String idStr = (String) player.get("idPlayer");
                                    int id = idStr != null ? Integer.parseInt(idStr) : 0;
                                    if (id == 0) continue;

                                    String name = (String) player.get("strPlayer");
                                    String photo = (String) player.get("strThumb");
                                    String nationality = (String) player.get("strNationality");

                                    players.add(new SearchItem(
                                            SearchItem.TYPE_ITEM,
                                            name,
                                            nationality,
                                            photo,
                                            id,
                                            "player"
                                    ));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Search", "Players parsing error", e);
                    }
                }

                if (!players.isEmpty()) {
                    results.add(new SearchItem("PLAYERS"));
                    results.addAll(players);
                }

                runOnUiThread(() -> {
                    adapter.setItems(results);
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                runOnUiThread(() -> {
                    adapter.setItems(results);
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void showInitialList() {
        List<SearchItem> list = new ArrayList<>();

        if (!allLeagues.isEmpty()) {
            list.add(new SearchItem("LEAGUES"));
            list.addAll(allLeagues.subList(0, Math.min(10, allLeagues.size())));
        }

        if (!allTeams.isEmpty()) {
            list.add(new SearchItem("TEAMS"));
            list.addAll(allTeams.subList(0, Math.min(20, allTeams.size())));
        }

        adapter.setItems(list);
        progressBar.setVisibility(View.GONE);
    }

    private String getAppSportName(String sportsDbSport) {
        if (sportsDbSport == null) return "";

        switch (sportsDbSport) {
            case "Soccer":
                return "Football";
            case "MMA":
            case "Fighting":
                return "UFC";
            case "Motorsport":
                return "Formula 1";
            case "Basketball":
                return "Basketball";
            case "Tennis":
                return "Tennis";
            default:
                return "";
        }
    }
}