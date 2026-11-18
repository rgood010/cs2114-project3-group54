import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 * VIEW helper (Swing). A dropdown model that reads the selected hall's menu
 * from the model every time Swing asks for it, so the view never keeps its
 * own copy of the menu. The only thing it stores is which item the user
 * picked, which is view state, not model state.
 */
public class MenuComboModel extends AbstractListModel<String>
    implements ComboBoxModel<String> {

    private final OrderController controller;
    private final boolean inStockOnly;
    private Object selected;

    /**
     * @param controller  the session's controller
     * @param inStockOnly true to list only items that are in stock
     */
    public MenuComboModel(OrderController controller, boolean inStockOnly) {
        this.controller = controller;
        this.inStockOnly = inStockOnly;
    }

    /** Tells the dropdown the menu may have changed, so it reads it again. */
    public void refresh() {
        List<String> items = items();
        if (selected != null && !items.contains(selected)) {
            selected = null;
        }
        if (selected == null && !items.isEmpty()) {
            selected = items.get(0);
        }
        fireContentsChanged(this, -1, -1);
    }

    @Override
    public int getSize() {
        return items().size();
    }

    @Override
    public String getElementAt(int index) {
        List<String> items = items();
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    @Override
    public void setSelectedItem(Object item) {
        selected = item;
        fireContentsChanged(this, -1, -1);
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }

    /** Reads the current items straight from the selected hall. */
    private List<String> items() {
        List<String> out = new ArrayList<>();
        DiningHallModel hall = controller.getSelectedHall();
        if (hall == null) {
            return out;
        }
        for (String item : hall.getMenuItems()) {
            if (!inStockOnly || hall.isInStock(item)) {
                out.add(item);
            }
        }
        return out;
    }
}
