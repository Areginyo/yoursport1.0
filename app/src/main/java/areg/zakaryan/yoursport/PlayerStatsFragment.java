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
                    bindAggregated(view, stats, season);
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
    private void bindAggregated(View v, List<Map<String,Object>> stats, int season) {
        // Sum all stats across every league
        int goals=0, assists=0, shotsOn=0, shotsTotal=0, keyPasses=0;
        int appearances=0, minutes=0;
        int dribSuccess=0, dribAttempts=0;
        int tackles=0, interceptions=0;
        int duelsWon=0, duelsTotal=0;
        int yellow=0, red=0;
        // For pass accuracy: weighted average by appearances
        double passAccWeighted=0; int passAccApps=0;

        for (Map<String,Object> s : stats) {
            Map<String,Object> go = cm(s.get("goals")), sh = cm(s.get("shots")),
                    pa = cm(s.get("passes")), ga = cm(s.get("games")),
                    dr = cm(s.get("dribbles")), ta = cm(s.get("tackles")),
                    du = cm(s.get("duels")), ca = cm(s.get("cards"));

            goals       += gn(go,"total");       assists     += gn(go,"assists");
            shotsOn     += gn(sh,"on");           shotsTotal  += gn(sh,"total");
            keyPasses   += gn(pa,"key");

            int app = gn(ga,"appearences");
            appearances += app;
            minutes     += gn(ga,"minutes");

            // Pass accuracy: weight by appearances per league
            if (pa != null) {
                int acc = gn(pa,"accuracy"); // 0 if null
                if (acc > 0 && app > 0) {
                    passAccWeighted += acc * app;
                    passAccApps += app;
                }
            }

            dribSuccess  += gn(dr,"success");    dribAttempts += gn(dr,"attempts");
            tackles      += gn(ta,"total");      interceptions += gn(ta,"interceptions");
            duelsWon     += gn(du,"won");         duelsTotal   += gn(du,"total");
            yellow       += gn(ca,"yellow");      red          += gn(ca,"red");
        }

        setVis(v, R.id.tvSeasonLabel, "SEASON " + season + "/" + (season+1) + "  (All competitions)");

        set(v, R.id.tvGoals, ""+goals);          set(v, R.id.tvAssists, ""+assists);
        set(v, R.id.tvShotsOn, ""+shotsOn);      set(v, R.id.tvShotsTotal, ""+shotsTotal);
        set(v, R.id.tvKeyPasses, ""+keyPasses);

        set(v, R.id.tvAppearances, ""+appearances); set(v, R.id.tvMinutes, ""+minutes);

        if (passAccApps > 0) {
            int avgAcc = (int) Math.round(passAccWeighted / passAccApps);
            set(v, R.id.tvPassAccuracy, avgAcc + "%");
        } else {
            set(v, R.id.tvPassAccuracy, "—");
        }

        set(v, R.id.tvDribbles, dribAttempts > 0 ? dribSuccess+"/"+dribAttempts : ""+dribSuccess);

        set(v, R.id.tvTackles, ""+tackles);      set(v, R.id.tvInterceptions, ""+interceptions);
        set(v, R.id.tvDuelsWon, duelsTotal > 0 ? duelsWon+"/"+duelsTotal : ""+duelsWon);
        set(v, R.id.tvYellow, ""+yellow);         set(v, R.id.tvRed, ""+red);

        for (int id : new int[]{R.id.cardAttack, R.id.cardMidfield, R.id.cardDefense,
                R.id.lblAttack, R.id.lblMidfield, R.id.lblDefense, R.id.tvSeasonLabel}) {
            View w = v.findViewById(id); if (w!=null) w.setVisibility(View.VISIBLE);
        }
    }

    /** Get numeric value from a map, returning 0 for null/missing */
    private int gn(Map<String,Object> m, String k) {
        if (m == null) return 0;
        Object o = m.get(k);
        if (o == null || "null".equals(String.valueOf(o))) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return (int) Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
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
