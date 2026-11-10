import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MODEL. A dining hall's menu: each item name maps to whether it is in stock.
 * Item lookups ignore case and surrounding spaces.
 */
public class Menu {

    private final Map<String, Boolean> availability;

    /**
     * Creates a menu where every listed item starts in stock.
     *
     * @param items the item names
     */
    public Menu(String... items) {
        availability = new LinkedHashMap<>();
        for (String item : items) {
            if (item == null || item.trim().isEmpty()) {
                throw new IllegalArgumentException("Blank menu item");
            }
            availability.put(item.trim(), Boolean.TRUE);
        }
    }

    /**
     * @return a copy of all item names, including sold-out items
     */
    public List<String> getItems() {
        return new ArrayList<>(availability.keySet());
    }

    /**
     * @param item an item name
     * @return true only for a known item that is in stock
     */
    public boolean inStock(String item) {
        String name = find(item);
        return name != null && availability.get(name);
    }

    /**
     * Marks a known item unavailable. Repeating the call has no effect.
     *
     * @param item the item name
     * @throws IllegalArgumentException if item is null, blank or unknown
     */
    public void markSoldOut(String item) {
        String name = find(item);
        if (name == null) {
            throw new IllegalArgumentException("Unknown menu item: " + item);
        }
        availability.put(name, Boolean.FALSE);
    }

    /**
     * Looks up the menu's own spelling of an item name.
     *
     * @param item the name to look for (case-insensitive)
     * @return the canonical item name, or null if null, blank or unknown
     */
    public String find(String item) {
        if (item == null || item.trim().isEmpty()) {
            return null;
        }
        String wanted = item.trim();
        for (String name : availability.keySet()) {
            if (name.equalsIgnoreCase(wanted)) {
                return name;
            }
        }
        return null;
    }
}
