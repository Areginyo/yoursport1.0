package areg.zakaryan.yoursport.model;

import java.util.List;

public class HonoursResponse {
    public List<Honour> honours;

    public static class Honour {
        public String idHonour;
        public String strHonour;
        public String strPlayer;
        public String strTeam;
        public String strSport;
        public String strSeason;
    }
}