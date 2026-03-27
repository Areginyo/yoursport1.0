package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import areg.zakaryan.yoursport.model.SearchItem;

public class TvFragment extends Fragment {

    private static final String ARG_SELECTED_ITEMS = "selected_items";
    private static final String ARG_CURRENT_SPORT = "current_sport";

    private ArrayList<SearchItem> selectedItems;
    private String currentSport;

    public TvFragment() {

    }

    public static TvFragment newInstance(ArrayList<SearchItem> selectedItems, String currentSport) {
        TvFragment fragment = new TvFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_SELECTED_ITEMS, selectedItems);
        args.putString(ARG_CURRENT_SPORT, currentSport);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedItems = getArguments().getParcelableArrayList(ARG_SELECTED_ITEMS);
            currentSport = getArguments().getString(ARG_CURRENT_SPORT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tv, container, false);



        return view;
    }
}