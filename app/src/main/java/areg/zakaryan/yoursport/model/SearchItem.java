package areg.zakaryan.yoursport.model;

public class SearchItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM   = 1;

    public int    type;
    public String title;
    public String subtitle;
    public String logoUrl;
    public int    id;
    public String category; // "league", "team", "player"

    // Конструктор для заголовка
    public SearchItem(String headerTitle) {
        this.type  = TYPE_HEADER;
        this.title = headerTitle;
    }

    // Конструктор для элемента
    public SearchItem(int type, String title, String subtitle, String logoUrl, int id, String category) {
        this.type     = TYPE_ITEM;
        this.title    = title;
        this.subtitle = subtitle;
        this.logoUrl  = logoUrl;
        this.id       = id;
        this.category = category;
    }
}