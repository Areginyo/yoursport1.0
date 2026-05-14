package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import areg.zakaryan.yoursport.api.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerStatsFragment extends Fragment {
    private static final String ARG = "player_id";
    private int playerId;

    public static PlayerStatsFragment newInstance(int id) {
        PlayerStatsFragment f = new PlayerStatsFragment();
        Bundle b = new Bundle(); b.putInt(ARG, id); f.setArguments(b); return f;
    }

    @Override public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) playerId = getArguments().getInt(ARG);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_player_stats, c, false);
        int yr = Calendar.getInstance().get(Calendar.YEAR);
        int mo = Calendar.getInstance().get(Calendar.MONTH);
        loadStats(v, mo < 6 ? yr - 1 : yr);
        return v;
    }

    @SuppressWarnings("unchecked")
    private void loadStats(View view, int season) {
        ProgressBar pb = view.findViewById(R.id.progressBar);
        TextView empty = view.findViewById(R.id.tvEmpty);
        ApiClient.getApiService().getPlayerProfile(playerId, season).enqueue(new Callback<Object>() {
            @Override public void onResponse(Call<Object> c, Response<Object> r) {
                if (!isAdded()) return;
                try {
                    Map<String,Object> body = (Map<String,Object>) r.body();
                    List<Map<String,Object>> resp = (List<Map<String,Object>>) body.get("response");
                    if (resp == null || resp.isEmpty()) { retry(view, pb, empty, season); return; }
                    List<Map<String,Object>> stats = (List<Map<String,Object>>) resp.get(0).get("statistics");
                    if (stats == null || stats.isEmpty()) { retry(view, pb, empty, season); return; }
                    pb.setVisibility(View.GONE);
                    bind(view, pickPrimary(stats), season);
                } catch (Exception e) { retry(view, pb, empty, season); }
            }
            @Override public void onFailure(Call<Object> c, Throwable t) {
                if (isAdded()) retry(view, pb, empty, season);
            }
        });
    }

    private void retry(View v, ProgressBar pb, TextView empty, int season) {
        if (season > 2018) loadStats(v, season - 1);
        else { pb.setVisibility(View.GONE); empty.setVisibility(View.VISIBLE); }
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> pickPrimary(List<Map<String,Object>> stats) {
        Map<String,Object> best = stats.get(0);
        int bestApps = 0;
        for (Map<String,Object> s : stats) {
            Map<String,Object> g = cm(s.get("games"));
            int apps = g != null ? ai(g.get("appearences")) : 0;
            if (apps > bestApps) { bestApps = apps; best = s; }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private void bind(View v, Map<String,Object> s, int season) {
        setVis(v, R.id.tvSeasonLabel, "SEASON " + season + "/" + (season+1));
        Map<String,Object> go = cm(s.get("goals")), sh = cm(s.get("shots")),
                pa = cm(s.get("passes")), ga = cm(s.get("games")),
                dr = cm(s.get("dribbles")), ta = cm(s.get("tackles")),
                du = cm(s.get("duels")), ca = cm(s.get("cards"));

        // Attack
        set(v, R.id.tvGoals, n(go,"total")); set(v, R.id.tvAssists, n(go,"assists"));
        set(v, R.id.tvShotsOn, n(sh,"on")); set(v, R.id.tvShotsTotal, n(sh,"total"));
        set(v, R.id.tvKeyPasses, n(pa,"key"));
        // Midfield
        set(v, R.id.tvAppearances, n(ga,"appearences")); set(v, R.id.tvMinutes, n(ga,"minutes"));
        String acc = pa != null ? sf(pa.get("accuracy")) : "";
        set(v, R.id.tvPassAccuracy, acc.isEmpty() ? "—" : acc+"%");
        int ds = dr!=null?ai(dr.get("success")):0, da = dr!=null?ai(dr.get("attempts")):0;
        set(v, R.id.tvDribbles, da>0 ? ds+"/"+da : ""+ds);
        // Defense
        set(v, R.id.tvTackles, n(ta,"total")); set(v, R.id.tvInterceptions, n(ta,"interceptions"));
        int dw=du!=null?ai(du.get("won")):0, dt=du!=null?ai(du.get("total")):0;
        set(v, R.id.tvDuelsWon, dt>0 ? dw+"/"+dt : ""+dw);
        set(v, R.id.tvYellow, n(ca,"yellow")); set(v, R.id.tvRed, n(ca,"red"));

        for (int id : new int[]{R.id.cardAttack, R.id.cardMidfield, R.id.cardDefense,
                R.id.lblAttack, R.id.lblMidfield, R.id.lblDefense, R.id.tvSeasonLabel}) {
            View w = v.findViewById(id); if (w!=null) w.setVisibility(View.VISIBLE);
        }
    }

    private String n(Map<String,Object> m, String k) {
        if (m==null) return "0"; Object o=m.get(k);
        if (o==null||"null".equals(String.valueOf(o))) return "0";
        if (o instanceof Number) return ""+((Number)o).intValue();
        try { return ""+(int)Double.parseDouble(""+o); } catch(Exception e) { return ""+o; }
    }
    private void set(View r, int id, String t) {
        TextView tv=r.findViewById(id); if(tv!=null) tv.setText(t==null||t.isEmpty()?"—":t);
    }
    private void setVis(View r, int id, String t) {
        TextView tv=r.findViewById(id); if(tv!=null){tv.setText(t);tv.setVisibility(View.VISIBLE);}
    }
    private String sf(Object v) { if(v==null) return ""; String s=""+v; return "null".equals(s)?"":s; }
    private int ai(Object v) {
        try { if(v instanceof Number) return ((Number)v).intValue();
            return Integer.parseInt((""+v).trim()); } catch(Exception e){return 0;}
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> cm(Object o) { return o instanceof Map?(Map<String,Object>)o:null; }
}
