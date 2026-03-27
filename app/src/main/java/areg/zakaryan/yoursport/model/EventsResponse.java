package areg.zakaryan.yoursport.model;

import java.util.List;

public class EventsResponse {
    public List<Event> event;

    public static class Event {
        public String idEvent;
        public String strEvent;
        public String strEventAlternate;
        public String strFilename;
        public String dateEvent;
        public String strTime;
        public String strDescriptionEN;
        public String strPoster;
        public String strSquare;
        public String strFanart;
        public String strThumb;
        public String strBanner;
        public String strMap;
        public String strTweet1;
        public String strTweet2;
        public String strTweet3;
        public String strVideo;
        public String strStatus;
        public String strLeague;
        public String strHomeTeam;
        public String strAwayTeam;
        public String intHomeScore;
        public String intAwayScore;
    }
}