package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamSquadFragment extends Fragment {

    private static final String ARG_TEAM_ITEM = "team_item";

    private SearchItem teamItem;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView txtNoSquad;
    private TeamSquadAdapter adapter;

private final List<SquadItem> squadPlayers = new ArrayList<>();
    private final AtomicInteger pending = new AtomicInteger(1); 

    public static TeamSquadFragment newInstance(SearchItem teamItem) {
        TeamSquadFragment fragment = new TeamSquadFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TEAM_ITEM, teamItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) teamItem = getArguments().getParcelable(ARG_TEAM_ITEM);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_team_squad, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewSquad);
        progressBar  = view.findViewById(R.id.progressBar);
        txtNoSquad   = view.findViewById(R.id.txtNoSquad);

        adapter = new TeamSquadAdapter();
        adapter.setOnPlayerClickListener(item -> {
            android.content.Intent intent = new android.content.Intent(
                    requireContext(), PlayerDetailActivity.class);
            intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_ID, item.id);
            intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_NAME, item.name);
            intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_PHOTO, item.photo);
            intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_POSITION, item.position);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (teamItem == null) { showNoSquad(); return view; }

        progressBar.setVisibility(View.VISIBLE);
        loadSquad();
        return view;
    }

@SuppressWarnings("unchecked")
    private void loadSquad() {
        ApiClient.getApiService().getSquad(teamItem.id).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> body = (Map<String, Object>) response.body();
                        List<Map<String, Object>> resp = (List<Map<String, Object>>) body.get("response");
                        if (resp != null && !resp.isEmpty()) {
                            List<Map<String, Object>> players =
                                    (List<Map<String, Object>>) resp.get(0).get("players");
                            if (players != null) {
                                for (Map<String, Object> p : players) {
                                    String name = str(p.get("name"));
                                    if (name.isEmpty()) continue;
                                    squadPlayers.add(new SquadItem(
                                            numInt(p.get("id")), name,
                                            str(p.get("position")),
                                            numInt(p.get("number")) > 0 ? String.valueOf(numInt(p.get("number"))) : "",
                                            str(p.get("photo")), "", "", "", "",
                                            numInt(p.get("age"))
                                    ));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                checkDone();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) checkDone();
            }
        });
    }

@SuppressWarnings("unchecked")
    private void loadCoach() {
        ApiClient.getApiService().getCoach(teamItem.id).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> body = (Map<String, Object>) response.body();
                        List<Map<String, Object>> resp = (List<Map<String, Object>>) body.get("response");
                        if (resp != null) {
                            
                            for (Map<String, Object> c : resp) {
                                List<Map<String, Object>> career =
                                        (List<Map<String, Object>>) c.get("career");
                                if (career == null) continue;
                                boolean isCurrent = false;
                                for (Map<String, Object> entry : career) {
                                    Map<String, Object> ct = (Map<String, Object>) entry.get("team");
                                    if (ct == null) continue;
                                    int cid = numInt(ct.get("id"));
                                    Object end = entry.get("end");
                                    boolean endIsNull = (end == null || "null".equals(String.valueOf(end))
                                            || String.valueOf(end).trim().isEmpty());
                                    if (cid == teamItem.id && endIsNull) { isCurrent = true; break; }
                                }

                            }
                        }
                    }
                } catch (Exception ignored) {}
                checkDone();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) checkDone();
            }
        });
    }

private void checkDone() {
        if (!isAdded()) return;
        if (pending.decrementAndGet() > 0) return;

        progressBar.setVisibility(View.GONE);

        if (squadPlayers.isEmpty()) {
            showNoSquad();
        } else {
            adapter.setSquad(squadPlayers);
        }
    }

    private void showNoSquad() {
        progressBar.setVisibility(View.GONE);
        txtNoSquad.setVisibility(View.VISIBLE);
        txtNoSquad.setText("No squad information available for " +
                (teamItem != null ? teamItem.title : "this team"));
    }

    private int numInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); }
        catch (Exception e) { return 0; }
    }

    private String str(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return "null".equals(s) ? "" : s;
    }

public static class SquadItem {
        public int id;
        public String name, position, number, photo, nationality, birthDate, height, weight;
        public int age;

        public SquadItem(int id, String name, String position, String number,
                        String photo, String nationality, String birthDate,
                        String height, String weight, int age) {
            this.id = id; this.name = name; this.position = position;
            this.number = number; this.photo = photo; this.nationality = nationality;
            this.birthDate = birthDate; this.height = height;
            this.weight = weight; this.age = age;
        }

        public String getPositionCategory() {
            if (position == null) return "Other";
            String pos = position.toLowerCase();
            if (pos.equals("coach") || pos.contains("coach") || pos.contains("manager")) return "Coach";
            if (pos.contains("goalkeeper")) return "Goalkeepers";
            if (pos.contains("defender")) return "Defenders";
            if (pos.contains("midfielder")) return "Midfielders";
            if (pos.contains("attacker") || pos.contains("forward")) return "Forwards";
            return "Other";
        }
    }
}
