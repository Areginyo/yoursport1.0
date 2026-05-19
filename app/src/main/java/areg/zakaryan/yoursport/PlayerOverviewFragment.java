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

import com.bumptech.glide.Glide;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.api.ApiClientSports;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerOverviewFragment extends Fragment {

    private static final String ARG_PLAYER_ID = "player_id";
    private int playerId;

    public static PlayerOverviewFragment newInstance(int playerId) {
        PlayerOverviewFragment f = new PlayerOverviewFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_PLAYER_ID, playerId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) playerId = getArguments().getInt(ARG_PLAYER_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_overview, container, false);
        loadPlayer(view, currentSeason());
        return view;
    }

private int currentSeason() {
        int yr = Calendar.getInstance().get(Calendar.YEAR);
        int mo = Calendar.getInstance().get(Calendar.MONTH); 
        
        return mo < 6 ? yr - 1 : yr;
    }

    @SuppressWarnings("unchecked")
    private void loadPlayer(View view, int season) {
        ProgressBar pb  = view.findViewById(R.id.progressBar);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);

        ApiClient.getApiService().getPlayerProfile(playerId, season)
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        if (!isAdded()) return;
                        try {
                            Map<String, Object> body = (Map<String, Object>) response.body();
                            if (body == null) { tryPreviousSeason(view, pb, tvEmpty, season); return; }

                            List<Map<String, Object>> resp =
                                    (List<Map<String, Object>>) body.get("response");

                            if (resp == null || resp.isEmpty()) {
                                
                                tryPreviousSeason(view, pb, tvEmpty, season);
                                return;
                            }

                            pb.setVisibility(View.GONE);
                            Map<String, Object> entry  = resp.get(0);
                            Map<String, Object> player = castMap(entry.get("player"));
                            List<Map<String, Object>> stats =
                                    (List<Map<String, Object>>) entry.get("statistics");

                            if (player != null) bindPersonal(view, player, stats);
                            if (stats != null && !stats.isEmpty()) {
                                
                                Map<String, Object> primaryStat = pickPrimaryStat(stats);
                                bindClub(view, primaryStat);
                            }
                            
                            if (player != null) findNationalTeam(view, player, stats);

                        } catch (Exception e) {
                            tryPreviousSeason(view, pb, tvEmpty, season);
                        }
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        if (!isAdded()) return;
                        tryPreviousSeason(view, pb, tvEmpty, season);
                    }
                });
    }

    private void tryPreviousSeason(View view, ProgressBar pb, TextView tvEmpty, int season) {
        if (!isAdded()) return;
        if (season > 2018) {
            loadPlayer(view, season - 1);
        } else {
            pb.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("unchecked")
    private void bindPersonal(View view, Map<String, Object> player, List<Map<String, Object>> stats) {
        setText(view, R.id.tvAge, strOrDash(player.get("age")) + " y.o.");
        setText(view, R.id.tvNationality, strOrDash(player.get("nationality")));
        setText(view, R.id.tvHeight, strOrDash(player.get("height")));
        setText(view, R.id.tvWeight, strOrDash(player.get("weight")));

String position = "—";
        String number = "—";
        if (stats != null && !stats.isEmpty()) {
            Map<String, Object> primaryStat = pickPrimaryStat(stats);
            Map<String, Object> games = castMap(primaryStat.get("games"));
            if (games != null) {
                String pos = safe(games.get("position"));
                if (!pos.isEmpty()) position = pos;
                int num = asInt(games.get("number"));
                if (num > 0) number = String.valueOf(num);
            }
        }
        setText(view, R.id.tvPosition, position);
        setText(view, R.id.tvNumber, number);
    }

@SuppressWarnings("unchecked")
    private Map<String, Object> pickPrimaryStat(List<Map<String, Object>> stats) {
        Map<String, Object> best = stats.get(0);
        int bestApps = 0;
        for (Map<String, Object> s : stats) {
            Map<String, Object> games = castMap(s.get("games"));
            int apps = games != null ? asInt(games.get("appearences")) : 0;
            if (apps > bestApps) { bestApps = apps; best = s; }
        }
        return best;
    }

    private int asInt(Object v) {
        try {
            if (v == null) return 0;
            if (v instanceof Number) return ((Number) v).intValue();
            String s = String.valueOf(v).trim();
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s);
        } catch (Exception e) { return 0; }
    }

    @SuppressWarnings("unchecked")
    private void bindClub(View view, Map<String, Object> stat) {
        Map<String, Object> team   = castMap(stat.get("team"));
        Map<String, Object> league = castMap(stat.get("league"));
        if (team == null) return;

        String clubName   = safe(team.get("name"));
        String clubLogo   = safe(team.get("logo"));
        String leagueName = league != null ? safe(league.get("name")) : "";

        ((TextView) view.findViewById(R.id.tvClubName)).setText(clubName.isEmpty() ? "—" : clubName);
        ((TextView) view.findViewById(R.id.tvClubLeague)).setText(leagueName);

        ImageView ivLogo = view.findViewById(R.id.ivClubLogo);
        if (!clubLogo.isEmpty()) {
            Glide.with(ivLogo.getContext()).load(clubLogo)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder).into(ivLogo);
        }
        if (getActivity() instanceof PlayerDetailActivity) {
            ((PlayerDetailActivity) getActivity()).updateClubInfo(clubName, clubLogo);
        }
    }

    @SuppressWarnings("unchecked")
    private void findNationalTeam(View view, Map<String, Object> player,
                                   List<Map<String, Object>> stats) {

for (Map<String, Object> stat : stats) {
            Map<String, Object> league = castMap(stat.get("league"));
            Map<String, Object> team   = castMap(stat.get("team"));
            if (league == null || team == null) continue;

            String leagueName = safe(league.get("name")).toLowerCase();
            String leagueType = safe(league.get("type")).toLowerCase();
            String teamName   = safe(team.get("name"));

            boolean isInternationalLeague =
                    leagueName.contains("nations league")
                    || leagueName.contains("world cup")
                    || leagueName.contains("qualif")
                    || leagueName.contains("friendlies")
                    || leagueName.contains("euro ")
                    || leagueName.contains("copa america")
                    || leagueName.contains("africa cup")
                    || leagueName.contains("gold cup")
                    || leagueName.contains("asian cup")
                    || leagueName.contains("olympic");

            if (isInternationalLeague && !isClubName(teamName)) {
                String logo = safe(team.get("logo"));
                showNationalTeam(view, teamName, logo);
                showCards(view);
                return;
            }
        }

String nationality = safe(player.get("nationality"));
        if (!nationality.isEmpty()) {
            
            String countryName = nationalityToCountry(nationality);
            String isoCode = countryToIsoCode(countryName);
            
            String flagUrl = isoCode.isEmpty() ? ""
                    : "https://flagcdn.com/w160/" + isoCode + ".png";
            showNationalTeam(view, countryName, flagUrl);
            showCards(view);
            return;
        }

showCards(view);
    }

private String countryToIsoCode(String country) {
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        map.put("France", "fr"); map.put("Brazil", "br"); map.put("Argentina", "ar");
        map.put("Spain", "es"); map.put("Germany", "de"); map.put("England", "gb-eng");
        map.put("Italy", "it"); map.put("Portugal", "pt"); map.put("Netherlands", "nl");
        map.put("Belgium", "be"); map.put("Croatia", "hr"); map.put("Poland", "pl");
        map.put("Morocco", "ma"); map.put("Senegal", "sn"); map.put("Cameroon", "cm");
        map.put("Egypt", "eg"); map.put("Ivory Coast", "ci"); map.put("Uruguay", "uy");
        map.put("Colombia", "co"); map.put("Chile", "cl"); map.put("Mexico", "mx");
        map.put("United States", "us"); map.put("Japan", "jp"); map.put("South Korea", "kr");
        map.put("Australia", "au"); map.put("Serbia", "rs"); map.put("Switzerland", "ch");
        map.put("Denmark", "dk"); map.put("Norway", "no"); map.put("Sweden", "se");
        map.put("Turkey", "tr"); map.put("Greece", "gr"); map.put("Austria", "at");
        map.put("Czech Republic", "cz"); map.put("Hungary", "hu"); map.put("Slovakia", "sk");
        map.put("Romania", "ro"); map.put("Ukraine", "ua"); map.put("Russia", "ru");
        map.put("Scotland", "gb-sct"); map.put("Wales", "gb-wls");
        map.put("Republic of Ireland", "ie"); map.put("Algeria", "dz");
        map.put("Tunisia", "tn"); map.put("Nigeria", "ng"); map.put("Ghana", "gh");
        map.put("Congo DR", "cd"); map.put("Guinea", "gn"); map.put("Mali", "ml");
        map.put("Ecuador", "ec"); map.put("Paraguay", "py"); map.put("Bolivia", "bo");
        map.put("Peru", "pe"); map.put("Venezuela", "ve"); map.put("Saudi Arabia", "sa");
        map.put("Iran", "ir"); map.put("Iraq", "iq"); map.put("Qatar", "qa");
        map.put("China", "cn"); map.put("India", "in"); map.put("Canada", "ca");
        map.put("Finland", "fi"); map.put("Albania", "al"); map.put("North Macedonia", "mk");
        map.put("Slovenia", "si"); map.put("Georgia", "ge"); map.put("Israel", "il");
        map.put("Iceland", "is"); map.put("Kosovo", "xk"); map.put("Montenegro", "me");
        map.put("Bosnia", "ba"); map.put("Bulgaria", "bg"); map.put("Armenia", "am");
        map.put("Azerbaijan", "az"); map.put("Kazakhstan", "kz"); map.put("New Zealand", "nz");
        map.put("South Africa", "za"); map.put("Zambia", "zm"); map.put("Zimbabwe", "zw");
        map.put("Angola", "ao"); map.put("Mozambique", "mz"); map.put("Tanzania", "tz");
        map.put("Kenya", "ke"); map.put("Uganda", "ug"); map.put("Ethiopia", "et");
        map.put("Burkina Faso", "bf"); map.put("Gabon", "ga"); map.put("Benin", "bj");
        map.put("Togo", "tg"); map.put("Rwanda", "rw"); map.put("Libya", "ly");
        map.put("Sudan", "sd"); map.put("Somalia", "so"); map.put("Congo", "cg");
        map.put("Namibia", "na"); map.put("Botswana", "bw"); map.put("Madagascar", "mg");
        map.put("Cape Verde", "cv"); map.put("Gambia", "gm"); map.put("Sierra Leone", "sl");
        map.put("Costa Rica", "cr"); map.put("Panama", "pa"); map.put("Honduras", "hn");
        map.put("Guatemala", "gt"); map.put("El Salvador", "sv"); map.put("Nicaragua", "ni");
        map.put("Jamaica", "jm"); map.put("Haiti", "ht"); map.put("Cuba", "cu");
        map.put("Dominican Republic", "do"); map.put("Trinidad and Tobago", "tt");
        map.put("Curacao", "cw"); map.put("Syria", "sy"); map.put("Jordan", "jo");
        map.put("Lebanon", "lb"); map.put("Palestine", "ps"); map.put("Bahrain", "bh");
        map.put("Kuwait", "kw"); map.put("Oman", "om"); map.put("UAE", "ae");
        map.put("Yemen", "ye"); map.put("Thailand", "th"); map.put("Vietnam", "vn");
        map.put("Indonesia", "id"); map.put("Philippines", "ph"); map.put("Malaysia", "my");
        map.put("Singapore", "sg"); map.put("Myanmar", "mm"); map.put("Cambodia", "kh");
        map.put("Pakistan", "pk"); map.put("Bangladesh", "bd"); map.put("Sri Lanka", "lk");
        map.put("Nepal", "np"); map.put("Afghanistan", "af"); map.put("Uzbekistan", "uz");
        String code = map.get(country);
        return code != null ? code : "";
    }

private String nationalityToCountry(String nationality) {
        if (nationality == null) return "";
        
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        map.put("French", "France");
        map.put("Brazilian", "Brazil");
        map.put("Argentine", "Argentina");
        map.put("Spanish", "Spain");
        map.put("German", "Germany");
        map.put("English", "England");
        map.put("Italian", "Italy");
        map.put("Portuguese", "Portugal");
        map.put("Dutch", "Netherlands");
        map.put("Belgian", "Belgium");
        map.put("Croatian", "Croatia");
        map.put("Polish", "Poland");
        map.put("Moroccan", "Morocco");
        map.put("Senegalese", "Senegal");
        map.put("Cameroonian", "Cameroon");
        map.put("Egyptian", "Egypt");
        map.put("Ivorian", "Ivory Coast");
        map.put("Uruguayan", "Uruguay");
        map.put("Colombian", "Colombia");
        map.put("Chilean", "Chile");
        map.put("Mexican", "Mexico");
        map.put("American", "United States");
        map.put("Japanese", "Japan");
        map.put("South Korean", "South Korea");
        map.put("Australian", "Australia");
        map.put("Serbian", "Serbia");
        map.put("Swiss", "Switzerland");
        map.put("Danish", "Denmark");
        map.put("Norwegian", "Norway");
        map.put("Swedish", "Sweden");
        map.put("Turkish", "Turkey");
        map.put("Greek", "Greece");
        map.put("Austrian", "Austria");
        map.put("Czech", "Czech Republic");
        map.put("Hungarian", "Hungary");
        map.put("Slovak", "Slovakia");
        map.put("Romanian", "Romania");
        map.put("Ukrainian", "Ukraine");
        map.put("Russian", "Russia");
        map.put("Scottish", "Scotland");
        map.put("Welsh", "Wales");
        map.put("Irish", "Republic of Ireland");
        map.put("Algerian", "Algeria");
        map.put("Tunisian", "Tunisia");
        map.put("Nigerian", "Nigeria");
        map.put("Ghanaian", "Ghana");
        map.put("Congolese", "Congo DR");
        map.put("Guinean", "Guinea");
        map.put("Malian", "Mali");
        map.put("Ecuadorian", "Ecuador");
        map.put("Paraguayan", "Paraguay");
        map.put("Bolivian", "Bolivia");
        map.put("Peruvian", "Peru");
        map.put("Venezuelan", "Venezuela");
        map.put("Saudi Arabian", "Saudi Arabia");
        map.put("Iranian", "Iran");
        map.put("Iraqi", "Iraq");
        map.put("Qatari", "Qatar");
        map.put("Chinese", "China");
        map.put("Indian", "India");
        String mapped = map.get(nationality);
        return mapped != null ? mapped : nationality;
    }

    private void showNationalTeam(View view, String name, String logoUrl) {
        ((TextView) view.findViewById(R.id.tvNationalTeam)).setText(name);
        ImageView iv = view.findViewById(R.id.ivNationalLogo);
        if (!logoUrl.isEmpty()) {
            Glide.with(iv.getContext()).load(logoUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder).into(iv);
        }
        view.findViewById(R.id.cardNational).setVisibility(View.VISIBLE);
    }

private boolean isClubName(String name) {
        if (name == null || name.isEmpty()) return false;
        String lower = name.toLowerCase();
        String[] clubKeywords = {"fc", " cf", " sc", "united", " city", "athletic",
                "atletico", "olympique", "sporting", "dynamo", "club ",
                "racing", "real ", "inter ", " ac ", " as ", "vfb", "vfl",
                "borussia", "bayer ", "rovers", "wanderers", "hotspur",
                "arsenal", "chelsea", "liverpool", "barcelona", "juventus",
                "milan", "paris", "munich", "madrid"};
        for (String kw : clubKeywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private void showCards(View view) {
        view.findViewById(R.id.cardPersonal).setVisibility(View.VISIBLE);
        view.findViewById(R.id.cardClub).setVisibility(View.VISIBLE);
    }

    private void setText(View view, int id, String text) {
        TextView tv = view.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private String strOrDash(Object v) {
        String s = safe(v);
        return s.isEmpty() ? "—" : s;
    }

    private String safe(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return "null".equalsIgnoreCase(s) ? "" : s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }
}
