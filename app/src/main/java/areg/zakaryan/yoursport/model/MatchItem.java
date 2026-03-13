package areg.zakaryan.yoursport.model;

public class MatchItem {

    // Обязательные поля для фильтрации по выбранным командам/лигам
    public String matchId;          // id матча
    public String homeTeamId;       // ← вместо homeId (лучше с Team, чтобы было понятно)
    public String awayTeamId;       // ← вместо awayId
    public String leagueId;         // id лиги

    // Для отображения
    public String homeTeamName;
    public String awayTeamName;
    public String leagueName;
    public String date;             // дата/время в строке, например "2025-03-15T18:00"
    public String status;           // "NS" (not started), "1H", "FT" и т.д.
    public String homeScore;        // "2" или null
    public String awayScore;

    // Опционально для TV-фрагмента
    public boolean hasBroadcast;    // есть ли трансляция

    // Пустой конструктор (для Gson/Retrofit)
    public MatchItem() {}


}