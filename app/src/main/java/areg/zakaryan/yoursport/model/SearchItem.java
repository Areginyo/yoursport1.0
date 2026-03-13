package areg.zakaryan.yoursport.model;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchItem implements Parcelable {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    public int type;
    public String title;
    public String subtitle;
    public String logoUrl;
    public int id;
    public String category; // "league", "team", "player"

    // Заголовок
    public SearchItem(String headerTitle) {
        this.type = TYPE_HEADER;
        this.title = headerTitle;
    }

    // Элемент
    public SearchItem(int type, String title, String subtitle, String logoUrl, int id, String category) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.logoUrl = logoUrl;
        this.id = id;
        this.category = category;
    }

    protected SearchItem(Parcel in) {
        type = in.readInt();
        title = in.readString();
        subtitle = in.readString();
        logoUrl = in.readString();
        id = in.readInt();
        category = in.readString();
    }

    public static final Creator<SearchItem> CREATOR = new Creator<SearchItem>() {
        @Override
        public SearchItem createFromParcel(Parcel in) {
            return new SearchItem(in);
        }

        @Override
        public SearchItem[] newArray(int size) {
            return new SearchItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(title);
        dest.writeString(subtitle);
        dest.writeString(logoUrl);
        dest.writeInt(id);
        dest.writeString(category);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchItem that = (SearchItem) o;
        return id == that.id &&
                java.util.Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, category);
    }
}