package pl.edu.go.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Wzorzec: Composite (KOMPOZYT)
 * ---------------------------------
 * StoneGroup reprezentuje grupę kamieni połączonych liniowo.
 * Grupa zachowuje się jak jeden obiekt (ma wspólne oddechy).
 * Umożliwia liczenie oddechów i wykrywanie bicia grupy.
 */
public class StoneGroup {
    private final int color;
    private final Set<Stone> stones = new HashSet<>();

    public StoneGroup(int color) {
        this.color = color;
    }

    public void addStone(Stone s) {
        stones.add(s);
    }

    public int getColor() {
        return color;
    }

    public Set<Stone> getStones() {
        return stones;
    }

    @Override
    public String toString() {
        return "Group(color=" + color + ", stones=" + stones + ")";
    }
}
