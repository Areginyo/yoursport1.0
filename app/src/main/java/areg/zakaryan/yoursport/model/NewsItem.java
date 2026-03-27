package areg.zakaryan.yoursport.model;

public class NewsItem {
    public String id;
    public String title;
    public String description;
    public String date;
    public String source;
    public String imageUrl;
    public String url;

    public NewsItem() {}

    public NewsItem(String id, String title, String description, String date, String source, String imageUrl, String url) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.source = source;
        this.imageUrl = imageUrl;
        this.url = url;
    }
}