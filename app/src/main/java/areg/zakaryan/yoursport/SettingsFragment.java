package areg.zakaryan.yoursport;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import areg.zakaryan.yoursport.model.SearchItem;

public class SettingsFragment extends Fragment {

    private static final String ARG_SELECTED_ITEMS = "selected_items";
    private static final String ARG_CURRENT_SPORT = "current_sport";

    private ArrayList<SearchItem> selectedItems;
    private String currentSport;

    private TextView tvSelectedCount;
    private LinearLayout btnChangeSelection;
    private LinearLayout btnLogout;
    private LinearLayout btnResetApp;

    public static SettingsFragment newInstance(ArrayList<SearchItem> selectedItems, String currentSport) {
        SettingsFragment fragment = new SettingsFragment();
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
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        tvSelectedCount = view.findViewById(R.id.tv_selected_count);
        btnChangeSelection = view.findViewById(R.id.btn_change_selection);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnResetApp = view.findViewById(R.id.btn_reset_app);

        setupUI();
        setupListeners();

        return view;
    }

    private void setupUI() {
        int count = selectedItems != null ? selectedItems.size() : 0;
        tvSelectedCount.setText(count + " items selected");
    }

    private void setupListeners() {

        // Change Selection — load latest from Firestore, then open SportChoice
        btnChangeSelection.setOnClickListener(v -> {
            FavoritesManager.loadSelectedItems(items -> {
                if (!isAdded()) return;
                Intent intent = new Intent(requireActivity(), SportChoice.class);
                intent.putExtra("from_settings", true);
                intent.putParcelableArrayListExtra("selected_items", items);
                startActivity(intent);
            });
        });

        // ================== LOG OUT ==================
        btnLogout.setOnClickListener(v -> {
            // Выход из Firebase аккаунта
            FirebaseAuth.getInstance().signOut();

            // Сбрасываем флаги
            SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", 0);
            prefs.edit()
                    .putBoolean("is_logged_in", false)
                    .putBoolean("is_onboarding_completed", false)
                    .apply();

            Toast.makeText(requireContext(), "You have been logged out", Toast.LENGTH_SHORT).show();

            // Открываем MainActivity (экран логина)
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            requireActivity().finishAffinity();
        });

        // ================== RESET APP ==================
        btnResetApp.setOnClickListener(v -> {
            // Выход из аккаунта + полный сброс
            FirebaseAuth.getInstance().signOut();

            SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", 0);
            prefs.edit().clear().apply();

            Toast.makeText(requireContext(), "App has been completely reset", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            requireActivity().finishAffinity();
        });
    }
}