import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MenuTest {

    @Test
    public void getItemsReturnsCopyIncludingSoldOut() {
        Menu menu = new Menu("Burger", "Pizza", "Salad");
        menu.markSoldOut("Pizza");
        List<String> items = menu.getItems();
        assertEquals(Arrays.asList("Burger", "Pizza", "Salad"), items);
        items.clear();
        assertEquals(3, menu.getItems().size());
    }

    @Test
    public void emptyMenuReturnsEmptyList() {
        assertTrue(new Menu().getItems().isEmpty());
    }

    @Test
    public void inStock() {
        Menu menu = new Menu("Pizza");
        assertTrue(menu.inStock("Pizza"));
        assertTrue(menu.inStock(" pizza "));
        assertFalse(menu.inStock(null));
        assertFalse(menu.inStock(""));
        assertFalse(menu.inStock("Tacos"));
        menu.markSoldOut("Pizza");
        assertFalse(menu.inStock("Pizza"));
        menu.markSoldOut("Pizza"); // repeat has no effect
        assertFalse(menu.inStock("Pizza"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void markUnknownSoldOutThrows() {
        new Menu("Pizza").markSoldOut("Tacos");
    }

    @Test(expected = IllegalArgumentException.class)
    public void markNullSoldOutThrows() {
        new Menu("Pizza").markSoldOut(null);
    }
}
