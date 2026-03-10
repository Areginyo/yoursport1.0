package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText      edtSearch;
    private RecyclerView  recycler;
    private Button        btnContinue;
    private SearchAdapter adapter;

    private final Handler  searchHandler  = new Handler();
    private       Runnable searchRunnable;

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

        edtSearch   = findViewById(R.id.edtSearch);
        recycler    = findViewById(R.id.recyclerSearch);
        btnContinue = findViewById(R.id.btnContinue);

        adapter = new SearchAdapter(selected -> {
            btnContinue.setVisibility(selected.isEmpty() ? View.GONE : View.VISIBLE);
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
            }
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() >= 4) {
                    searchRunnable = () -> searchAll(query);
                    searchHandler.postDelayed(searchRunnable, 1000);
                } else {
                    adapter.setItems(new ArrayList<>());
                }
            }
        });

        btnContinue.setOnClickListener(v -> {
            // Navigate to home screen (coming soon)
        });
    }

    private void searchAll(String query) {
        List<SearchItem> results = new ArrayList<>();

        ApiClient.getApiService().getLeagues(query)
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                Map body = (Map) response.body();
                                List list = (List) body.get("response");
                                if (list != null && !list.isEmpty()) {
                                    results.add(new SearchItem("LEAGUES"));
                                    for (Object obj : list) {
                                        Map item   = (Map) obj;
                                        Map league = (Map) item.get("league");
                                        if (league != null) {
                                            int    id   = ((Double) league.get("id")).intValue();
                                            String name = (String) league.get("name");
                                            String logo = (String) league.get("logo");
                                            String type = (String) league.get("type");
                                            results.add(new SearchItem(
                                                    SearchItem.TYPE_ITEM, name, type, logo, id, "league"
                                            ));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("SEARCH", "Leagues error: " + e.getMessage());
                            }
                        }
                        searchTeams(query, results);
                    }
                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        Log.e("SEARCH", "Leagues failure: " + t.getMessage());
                        searchTeams(query, results);
                    }
                });
    }

    private void searchTeams(String query, List<SearchItem> results) {
        ApiClient.getApiService().getTeamsByName(query)
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                Map body = (Map) response.body();
                                List list = (List) body.get("response");
                                if (list != null && !list.isEmpty()) {
                                    results.add(new SearchItem("TEAMS"));
                                    for (Object obj : list) {
                                        Map item = (Map) obj;
                                        Map team = (Map) item.get("team");
                                        if (team != null) {
                                            int    id      = ((Double) team.get("id")).intValue();
                                            String name    = (String) team.get("name");
                                            String logo    = (String) team.get("logo");
                                            String country = (String) team.get("country");
                                            results.add(new SearchItem(
                                                    SearchItem.TYPE_ITEM, name, country, logo, id, "team"
                                            ));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("SEARCH", "Teams error: " + e.getMessage());
                            }
                        }
                        searchPlayers(query, results);
                    }
                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        Log.e("SEARCH", "Teams failure: " + t.getMessage());
                        searchPlayers(query, results);
                    }
                });
    }

    private void searchPlayers(String query, List<SearchItem> results) {
        ApiClient.getApiService().getPlayersByName(query, 2024)
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                Map body = (Map) response.body();
                                List list = (List) body.get("response");
                                if (list != null && !list.isEmpty()) {
                                    results.add(new SearchItem("PLAYERS"));
                                    for (Object obj : list) {
                                        Map item   = (Map) obj;
                                        Map player = (Map) item.get("player");
                                        if (player != null) {
                                            int    id          = ((Double) player.get("id")).intValue();
                                            String name        = (String) player.get("name");
                                            String photo       = (String) player.get("photo");
                                            String nationality = (String) player.get("nationality");
                                            results.add(new SearchItem(
                                                    SearchItem.TYPE_ITEM, name, nationality, photo, id, "player"
                                            ));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("SEARCH", "Players error: " + e.getMessage());
                            }
                        }
                        runOnUiThread(() -> adapter.setItems(results));
                    }
                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        Log.e("SEARCH", "Players failure: " + t.getMessage());
                        runOnUiThread(() -> adapter.setItems(results));
                    }
                });
    }
}