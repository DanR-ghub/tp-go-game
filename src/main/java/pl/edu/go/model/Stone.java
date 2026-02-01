package pl.edu.go.model;

/**
 * Wzorzec: Composite (LIŚĆ)
 * --------------------------------
 * Stone reprezentuje pojedynczy kamień na planszy Go.
 * Jest najmniejszym elementem struktury Composite.
 * Kamienie są później łączone w grupy (StoneGroup).
 */
public class Stone {
    private final int x;
    private final int y;
    private final int color;

    public Stone(int x, int y, int color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "Stone(" + x + "," + y + ", color=" + color + ")";
    }
}
