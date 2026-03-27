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
    private static final String ARG_SPORT = "sport";

    private ArrayList<SearchItem> selectedItems;
    private String currentSport;

    public static MatchesFragment newInstance(ArrayList<SearchItem> selectedItems, String sport) {
        MatchesFragment fragment = new MatchesFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_SELECTED_ITEMS, selectedItems);
        args.putString(ARG_SPORT, sport);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedItems = getArguments().getParcelableArrayList(ARG_SELECTED_ITEMS);
            currentSport = getArguments().getString(ARG_SPORT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_matches, container, false);



        return view;
    }
}