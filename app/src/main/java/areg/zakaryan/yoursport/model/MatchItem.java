package areg.zakaryan.yoursport.model;

import android.os.Parcel;
import android.os.Parcelable;

public class MatchItem implements Parcelable {

    public boolean isLeagueHeader = false;
    public int leagueId;
    public int leagueID;
    public String leagueName;
    public String leagueLogo;
    public int leagueSortOrder = -1;

    public String homeTeam;
    public String awayTeam;
    public String homeLogo;
    public String awayLogo;
    public String score;
    public String time;
    public String date;
    public String status;
    public boolean isLive = false;
    public String matchId;
    
    public int homeTeamId;
    public int awayTeamId;

    public MatchItem() {}

    protected MatchItem(Parcel in) {
        isLeagueHeader = in.readByte() != 0;
        leagueId = in.readInt();
        leagueID = leagueId;
        leagueName = in.readString();
        leagueLogo = in.readString();
        leagueSortOrder = in.readInt();
        homeTeam = in.readString();
        awayTeam = in.readString();
        homeLogo = in.readString();
        awayLogo = in.readString();
        score = in.readString();
        time = in.readString();
        date = in.readString();
        status = in.readString();
        isLive = in.readByte() != 0;
        matchId = in.readString();
        homeTeamId = in.readInt();
        awayTeamId = in.readInt();
    }

    public static final Creator<MatchItem> CREATOR = new Creator<MatchItem>() {
        @Override
        public MatchItem createFromParcel(Parcel in) {
            return new MatchItem(in);
        }

        @Override
        public MatchItem[] newArray(int size) {
            return new MatchItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isLeagueHeader ? 1 : 0));
        dest.writeInt(leagueId);
        dest.writeString(leagueName);
        dest.writeString(leagueLogo);
        dest.writeInt(leagueSortOrder);
        dest.writeString(homeTeam);
        dest.writeString(awayTeam);
        dest.writeString(homeLogo);
        dest.writeString(awayLogo);
        dest.writeString(score);
        dest.writeString(time);
        dest.writeString(date);
        dest.writeString(status);
        dest.writeByte((byte) (isLive ? 1 : 0));
        dest.writeString(matchId);
        dest.writeInt(homeTeamId);
        dest.writeInt(awayTeamId);
    }
}