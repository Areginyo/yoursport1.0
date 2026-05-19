package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import areg.zakaryan.yoursport.api.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerCareerFragment extends Fragment {
    private static final String ARG = "player_id";
    private int playerId;

    public static PlayerCareerFragment newInstance(int id) {
        PlayerCareerFragment f = new PlayerCareerFragment();
        Bundle b = new Bundle();
        b.putInt(ARG, id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) playerId = getArguments().getInt(ARG);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_player_career, c, false);
        RecyclerView rv = v.findViewById(R.id.rvCareer);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        load(v, rv);
        return v;
    }

    @SuppressWarnings("unchecked")
    private void load(View view, RecyclerView rv) {
        ProgressBar pb = view.findViewById(R.id.progressBar);
        TextView empty = view.findViewById(R.id.tvEmpty);

        ApiClient.getApiService().getPlayerTransfers(playerId)
                .enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> c, Response<Object> r) {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                try {
                    Map<String, Object> body = (Map<String, Object>) r.body();
                    List<Map<String, Object>> resp =
                            (List<Map<String, Object>>) body.get("response");

List<Map<String, Object>> transfers = null;
                    if (resp != null && !resp.isEmpty()) {
                        transfers = (List<Map<String, Object>>) resp.get(0).get("transfers");
                    }

                    if (transfers == null || transfers.isEmpty()) {
                        empty.setVisibility(View.VISIBLE);
                        return;
                    }

List<Map<String, Object>> sorted = new ArrayList<>(transfers);
                    Collections.sort(sorted, (a, b2) -> {
                        long da = parseDate(sf(a.get("date")));
                        long db = parseDate(sf(b2.get("date")));
                        return Long.compare(da, db);
                    });

List<CE> entries = new ArrayList<>();
                    String lastClubId = "";
                    for (Map<String, Object> t : sorted) {
                        Map<String, Object> teams = cm(t.get("teams"));
                        Map<String, Object> toTeam = teams != null ? cm(teams.get("in")) : null;
                        if (toTeam == null) continue;

                        String clubId = sf(toTeam.get("id"));
                        String name   = sf(toTeam.get("name"));
                        String logo   = sf(toTeam.get("logo"));
                        String date   = formatDate(sf(t.get("date")));

                        if (name.isEmpty()) continue;
                        
                        if (clubId.equals(lastClubId)) continue;
                        lastClubId = clubId;

                        entries.add(new CE(name, logo, date));
                    }

                    if (entries.isEmpty()) {
                        empty.setVisibility(View.VISIBLE);
                    } else {
                        
                        Collections.reverse(entries);
                        rv.setAdapter(new CA(entries));
                    }

                } catch (Exception e) {
                    empty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<Object> c, Throwable t) {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                view.findViewById(R.id.tvEmpty).setVisibility(View.VISIBLE);
            }
        });
    }

private long parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = sdf.parse(dateStr);
            return d != null ? d.getTime() : 0;
        } catch (Exception e) { return 0; }
    }

private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "—";
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);
            Date d = in.parse(dateStr);
            return d != null ? out.format(d) : dateStr;
        } catch (Exception e) { return dateStr; }
    }

static class CE {
        String name, logo, date;
        CE(String n, String l, String d) { name = n; logo = l; date = d; }
    }

static class CA extends RecyclerView.Adapter<CA.VH> {
        private final List<CE> items;
        CA(List<CE> i) { items = i; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_career_entry, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            CE e = items.get(pos);
            h.tvClub.setText(e.name);
            h.tvDate.setText(e.date);
            if (e.logo != null && !e.logo.isEmpty())
                Glide.with(h.ivLogo.getContext()).load(e.logo)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(h.ivLogo);
            else
                h.ivLogo.setImageResource(R.drawable.ic_placeholder);
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivLogo;
            TextView tvClub, tvDate;
            VH(View v) {
                super(v);
                ivLogo = v.findViewById(R.id.ivClubLogo);
                tvClub = v.findViewById(R.id.tvClubName);
                tvDate = v.findViewById(R.id.tvYears);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cm(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }
    private String sf(Object v) {
        if (v == null) return "";
        String s = "" + v;
        return "null".equals(s) ? "" : s;
    }
}
