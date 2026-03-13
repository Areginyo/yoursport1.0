package areg.zakaryan.yoursport.model;

public class NewsItem {

    // Основные поля для новости (можно расширить по мере необходимости)
    public String title;          // заголовок новости
    public String description;    // краткое описание / текст
    public String url;            // ссылка на полную статью
    public String imageUrl;       // URL картинки (для Glide)
    public String source;         // источник (BBC, ESPN, Goal.com и т.д.)
    public String date;           // дата публикации (строка или timestamp)
    public String category;       // опционально: "football", "ufc" и т.д.

    // Пустой конструктор (нужен для Gson/Retrofit, если будешь парсить JSON)
    public NewsItem() {}

    // Можно добавить конструктор для удобства
    public NewsItem(String title, String description, String url, String imageUrl, String source, String date) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.imageUrl = imageUrl;
        this.source = source;
        this.date = date;
    }
}