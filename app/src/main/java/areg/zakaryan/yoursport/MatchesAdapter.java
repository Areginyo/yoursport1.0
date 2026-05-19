package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import areg.zakaryan.yoursport.model.MatchItem;

public class MatchesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MATCH  = 1;

    private List<MatchItem> items = new ArrayList<>();

private OnMatchClickListener listener;
    private OnTeamClickListener teamClickListener;
    private OnLeagueClickListener leagueClickListener;

    public interface OnMatchClickListener {
        void onMatchClick(MatchItem match);
    }

    public interface OnTeamClickListener {
        void onTeamClick(int teamId, String teamName, String teamLogo);
    }

    public interface OnLeagueClickListener {
        void onLeagueClick(MatchItem header);
    }

    public void setOnMatchClickListener(OnMatchClickListener listener) {
        this.listener = listener;
    }

    public void setOnTeamClickListener(OnTeamClickListener listener) {
        this.teamClickListener = listener;
    }

    public void setOnLeagueClickListener(OnLeagueClickListener listener) {
        this.leagueClickListener = listener;
    }

    public void submitList(List<MatchItem> newItems) {
        this.items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isLeagueHeader ? TYPE_HEADER : TYPE_MATCH;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderVH(inf.inflate(R.layout.item_league_header, parent, false));
        } else {
            return new MatchVH(inf.inflate(R.layout.item_match, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MatchItem item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind(item, leagueClickListener);
        } else {
            ((MatchVH) holder).bind(item, listener, teamClickListener);   
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvLeague;
        ImageView ivLogo;

        HeaderVH(View v) {
            super(v);
            tvLeague = v.findViewById(R.id.tv_league_name);
            ivLogo   = v.findViewById(R.id.iv_league_logo);
        }

        void bind(MatchItem item, OnLeagueClickListener leagueClickListener) {
            tvLeague.setText(item.leagueName != null ? item.leagueName : "");
            loadImg(ivLogo, item.leagueLogo);

            itemView.setOnClickListener(v -> {
                if (leagueClickListener != null && item.leagueId > 0) {
                    leagueClickListener.onLeagueClick(item);
                }
            });
        }

        private void loadImg(ImageView iv, String url) {
            if (url != null && !url.isEmpty()) {
                Glide.with(iv.getContext()).load(url)
                        .apply(new RequestOptions().fitCenter())
                        .into(iv);
            }
        }
    }

static class MatchVH extends RecyclerView.ViewHolder {
        ImageView ivHome, ivAway;
        TextView tvHome, tvAway, tvHomeScore, tvAwayScore, tvStatus, tvTime;

        MatchVH(View v) {
            super(v);
            tvTime      = v.findViewById(R.id.tv_time);
            tvHome      = v.findViewById(R.id.tv_home_team);
            tvAway      = v.findViewById(R.id.tv_away_team);
            tvHomeScore = v.findViewById(R.id.tv_home_score);
            tvAwayScore = v.findViewById(R.id.tv_away_score);
            tvStatus    = v.findViewById(R.id.tv_status);
            ivHome      = v.findViewById(R.id.iv_home_logo);
            ivAway      = v.findViewById(R.id.iv_away_logo);
        }

        void bind(MatchItem m, OnMatchClickListener listener, OnTeamClickListener teamClickListener) {
            tvHome.setText(m.homeTeam != null ? m.homeTeam : "");
            tvAway.setText(m.awayTeam != null ? m.awayTeam : "");

            tvTime.setText(formatTime(m.time));

            if (m.score != null && m.score.contains(" - ")) {
                String[] p = m.score.split(" - ");
                String homeScore = formatScore(p.length > 0 ? p[0] : "0");
                String awayScore = formatScore(p.length > 1 ? p[1] : "0");
                tvHomeScore.setText(homeScore);
                tvAwayScore.setText(awayScore);
                tvHomeScore.setVisibility(View.VISIBLE);
                tvAwayScore.setVisibility(View.VISIBLE);
                highlightWinner(homeScore, awayScore);
            } else {
                tvHomeScore.setVisibility(View.INVISIBLE);
                tvAwayScore.setVisibility(View.INVISIBLE);
                tvHome.setTextColor(0xFFFFFFFF);
                tvAway.setTextColor(0xFFCCCCCC);
            }

            bindStatus(m.status, m.isLive);
            loadImg(ivHome, m.homeLogo);
            loadImg(ivAway, m.awayLogo);

itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMatchClick(m);
                }
            });

ivHome.setOnClickListener(v -> {
                if (teamClickListener != null && m.homeTeamId > 0) {
                    teamClickListener.onTeamClick(m.homeTeamId, m.homeTeam, m.homeLogo);
                }
            });

            ivAway.setOnClickListener(v -> {
                if (teamClickListener != null && m.awayTeamId > 0) {
                    teamClickListener.onTeamClick(m.awayTeamId, m.awayTeam, m.awayLogo);
                }
            });
        }

private String formatScore(String raw) {
            if (raw == null || raw.trim().isEmpty()) return "0";
            try {
                double d = Double.parseDouble(raw.trim());
                return String.valueOf((int) d);
            } catch (NumberFormatException e) {
                return raw.trim();
            }
        }

        private String formatTime(String t) {
            if (t == null || t.isEmpty()) return "--:--";
            if (t.contains("T")) {
                try {
                    String timePart = t.split("T")[1];
                    return timePart.substring(0, 5);
                } catch (Exception e) { return "--:--"; }
            }
            return t.length() >= 5 ? t.substring(0, 5) : t;
        }

        private void bindStatus(String status, boolean isLive) {
            if (isLive) {
                tvStatus.setText("LIVE");
                tvStatus.setTextColor(0xFFFF4444);
                return;
            }
            if (status == null) { tvStatus.setText(""); return; }
            switch (status) {
                case "FINISHED":
                case "FT":
                case "AET":
                case "PEN":
                    tvStatus.setText("FT");
                    tvStatus.setTextColor(0xFF888888);
                    break;
                case "IN_PLAY":
                case "PAUSED":
                case "1H":
                case "2H":
                case "HT":
                case "ET":
                case "P":
                    tvStatus.setText("LIVE");
                    tvStatus.setTextColor(0xFFFF4444);
                    break;
                case "TIMED":
                case "SCHEDULED":
                case "Upcoming":
                case "NS":
                case "TBD":
                    tvStatus.setText("");
                    break;
                case "POSTPONED":
                case "PST":
                    tvStatus.setText("PST");
                    tvStatus.setTextColor(0xFFFF8800);
                    break;
                case "CANCELLED":
                case "CANC":
                case "ABD":
                case "AWD":
                case "WO":
                    tvStatus.setText("CAN");
                    tvStatus.setTextColor(0xFFFF4444);
                    break;
                default:
                    tvStatus.setText(status);
                    tvStatus.setTextColor(0xFF888888);
            }
        }

        private void highlightWinner(String homeStr, String awayStr) {
            try {
                int h = Integer.parseInt(homeStr);
                int a = Integer.parseInt(awayStr);
                if (h > a) {
                    setColors(0xFFFFFFFF, 0xFF888888, 0xFFFFFFFF, 0xFF888888);
                } else if (a > h) {
                    setColors(0xFF888888, 0xFFFFFFFF, 0xFF888888, 0xFFFFFFFF);
                } else {
                    setColors(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
                }
            } catch (NumberFormatException e) {
                tvHome.setTextColor(0xFFFFFFFF);
                tvAway.setTextColor(0xFFCCCCCC);
            }
        }

        private void setColors(int hTeam, int aTeam, int hScore, int aScore) {
            tvHome.setTextColor(hTeam);
            tvAway.setTextColor(aTeam);
            tvHomeScore.setTextColor(hScore);
            tvAwayScore.setTextColor(aScore);
        }

        private void loadImg(ImageView iv, String url) {
            if (url != null && !url.isEmpty()) {
                Glide.with(iv.getContext()).load(url)
                        .apply(new RequestOptions()
                                .fitCenter()
                                .placeholder(R.drawable.ic_placeholder)
                                .error(R.drawable.ic_placeholder))
                        .into(iv);
            } else {
                iv.setImageResource(R.drawable.ic_placeholder);
            }
        }
    }
}