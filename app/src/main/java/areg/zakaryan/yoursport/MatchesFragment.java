package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import areg.zakaryan.yoursport.model.SearchItem;
import areg.zakaryan.yoursport.model.MatchItem;

// public class MatchItem { String homeTeam, awayTeam, league, date, homeId, awayId, leagueId; ... }

public class MatchesFragment extends Fragment {

    private static final String ARG_SELECTED_ITEMS = "selected_items";

    private RecyclerView rvMatches;
    private TextView tvStatus;

    // private MatchesAdapter adapter;

    public static MatchesFragment newInstance(ArrayList<SearchItem> selectedItems) {
        MatchesFragment f = new MatchesFragment();
        Bundle b = new Bundle();
        b.putParcelableArrayList(ARG_SELECTED_ITEMS, selectedItems);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_matches, container, false);

        rvMatches = v.findViewById(R.id.rv_matches);
        tvStatus = v.findViewById(R.id.tv_status);

        rvMatches.setLayoutManager(new LinearLayoutManager(requireContext()));
        // rvMatches.setAdapter(adapter = new MatchesAdapter());

        loadMatches();

        return v;
    }

    private void loadMatches() {
        ArrayList<SearchItem> favorites = getArguments() != null ?
                getArguments().getParcelableArrayList(ARG_SELECTED_ITEMS) : new ArrayList<>();

        tvStatus.setText("Загрузка матчей...");
        tvStatus.setVisibility(View.VISIBLE);

        // 1. Получаем матчи (по API-Sports / fixtures?date=... или ?league=...)
        List<MatchItem> allMatches = fetchMatches();  // ← Retrofit call

        // 2. Разделяем на приоритетные и остальные
        List<MatchItem> priorityMatches = new ArrayList<>();
        List<MatchItem> otherMatches = new ArrayList<>();

        for (MatchItem match : allMatches) {
            boolean isPriority = false;

            for (SearchItem fav : favorites) {
                if ("league".equals(fav.category) && String.valueOf(fav.id).equals(match.leagueId)) {
                    isPriority = true;
                    break;
                }
                if ("team".equals(fav.category)) {
                    if (String.valueOf(fav.id).equals(match.homeTeamId) || String.valueOf(fav.id).equals(match.awayTeamId)) {
                        isPriority = true;
                        break;
                    }
                }
                // Для "player" — сложнее, нужно проверять составы (отдельный запрос players/statistics)
                // Для UFC/F1/Tennis — аналогично адаптировать id
            }

            if (isPriority) {
                priorityMatches.add(match);
            } else {
                otherMatches.add(match);
            }
        }

        // 3. Сортируем по дате (если нужно)
        // priorityMatches.sort(Comparator.comparing(MatchItem::getDate));
        // otherMatches.sort(...);

        List<MatchItem> finalList = new ArrayList<>();
        finalList.addAll(priorityMatches);
        finalList.addAll(otherMatches);

        // adapter.submitList(finalList);

        if (finalList.isEmpty()) {
            tvStatus.setText("No matches yet");
        } else {
            tvStatus.setVisibility(View.GONE);
        }
    }

    private List<MatchItem> fetchMatches() {
        // Реальный код: Retrofit -> https://v3.football.api-sports.io/fixtures?date=2026-03-14
        // Header: x-apisports-key = ТВОЙ_КЛЮЧ
        return new ArrayList<>();
    }
}