package areg.zakaryan.yoursport;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import areg.zakaryan.yoursport.model.SearchItem;

public class FavoritesManager {

    private static final String TAG = "FavoritesManager";
    private static final String COLLECTION_USERS = "users";
    private static final String FIELD_FAVORITES = "favorites";

public static void saveSelectedItems(ArrayList<SearchItem> items) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No user logged in, cannot save favorites");
            return;
        }

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (SearchItem item : items) {
            if (item.type != SearchItem.TYPE_ITEM) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("type", item.type);
            map.put("title", item.title != null ? item.title : "");
            map.put("subtitle", item.subtitle != null ? item.subtitle : "");
            map.put("logoUrl", item.logoUrl != null ? item.logoUrl : "");
            map.put("id", item.id);
            map.put("category", item.category != null ? item.category : "");
            itemsList.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_FAVORITES, itemsList);

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Favorites saved: " + itemsList.size() + " items"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save favorites", e));
    }

public static void loadSelectedItems(OnFavoritesLoadedListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No user logged in, returning empty list");
            listener.onLoaded(new ArrayList<>());
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    ArrayList<SearchItem> items = parseFromDocument(documentSnapshot);
                    Log.d(TAG, "Favorites loaded: " + items.size() + " items");
                    listener.onLoaded(items);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load favorites", e);
                    listener.onLoaded(new ArrayList<>());
                });
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<SearchItem> parseFromDocument(DocumentSnapshot doc) {
        ArrayList<SearchItem> items = new ArrayList<>();
        if (doc == null || !doc.exists()) return items;

        Object favObj = doc.get(FIELD_FAVORITES);
        if (!(favObj instanceof List)) return items;

        List<Map<String, Object>> list = (List<Map<String, Object>>) favObj;
        for (Map<String, Object> map : list) {
            try {
                int type = ((Number) map.get("type")).intValue();
                String title = (String) map.get("title");
                String subtitle = (String) map.get("subtitle");
                String logoUrl = (String) map.get("logoUrl");
                int id = ((Number) map.get("id")).intValue();
                String category = (String) map.get("category");

                items.add(new SearchItem(type, title, subtitle, logoUrl, id, category));
            } catch (Exception e) {
                Log.w(TAG, "Skipping malformed favorite item", e);
            }
        }
        return items;
    }

    public interface OnFavoritesLoadedListener {
        void onLoaded(ArrayList<SearchItem> items);
    }
}
