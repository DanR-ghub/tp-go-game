package pl.edu.go.client.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * {@code BoardView} renderuje planszę Go w JavaFX.
 *
 * <p><b>MVC:</b> pełni rolę <b>View</b>. Rysuje siatkę i kamienie, a w trybie punktacji wizualizuje:
 * <ul>
 *   <li>{@code TERRITORY} — terytorium na pustych polach,</li>
 *   <li>{@code DEADSTONES} — krzyżyki na kamieniach uznanych za martwe.</li>
 * </ul>
 *
 * <p>Komponent nie liczy nic biznesowo; tylko wizualizuje dane z {@link pl.edu.go.client.gui.GameModel}.
 */
public final class BoardView extends Canvas {

    public interface IntersectionClickHandler {
        void onClick(int x, int y);
    }

    private final GameModel model;
    private IntersectionClickHandler clickHandler = (x, y) -> {};

    public BoardView(GameModel model, double width, double height) {
        super(width, height);
        this.model = model;
        setOnMouseClicked(this::handleClick);
        redraw();
    }

    public void setClickHandler(IntersectionClickHandler clickHandler) {
        this.clickHandler = clickHandler != null ? clickHandler : (x, y) -> {};
    }

    public void redraw() {
        GraphicsContext g = getGraphicsContext2D();

        int size = model.getBoardSize();
        if (size <= 1) return;

        double w = getWidth();
        double h = getHeight();

        g.setFill(Color.BEIGE);
        g.fillRect(0, 0, w, h);

        double pad = 20.0;
        double gridW = w - 2 * pad;
        double gridH = h - 2 * pad;

        double stepX = gridW / (size - 1);
        double stepY = gridH / (size - 1);

        // grid — rysowanie siatki planszy (linie poziome i pionowe)
        g.setStroke(Color.BLACK);
        g.setLineWidth(1.0);
        for (int i = 0; i < size; i++) {
            double x = pad + i * stepX;
            double y = pad + i * stepY;
            g.strokeLine(pad, y, pad + gridW, y);
            g.strokeLine(x, pad, x, pad + gridH);
        }

        int[][] board = model.getBoard();

        // promień — dopasowany do kroku siatki, żeby kamienie mieściły się na przecięciach
        double stoneR = Math.min(stepX, stepY) * 0.21;

        // stones — rysowanie kamieni na przecięciach siatki
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int cell = board[x][y];
                if (cell == GameModel.EMPTY) continue;

                double cx = pad + x * stepX;
                double cy = pad + y * stepY;

                if (cell == GameModel.BLACK) {
                    g.setFill(Color.BLACK);
                    g.fillOval(cx - stoneR, cy - stoneR, 2 * stoneR, 2 * stoneR);
                    g.setStroke(Color.BLACK);
                    g.strokeOval(cx - stoneR, cy - stoneR, 2 * stoneR, 2 * stoneR);
                } else {
                    g.setFill(Color.WHITE);
                    g.fillOval(cx - stoneR, cy - stoneR, 2 * stoneR, 2 * stoneR);
                    g.setStroke(Color.BLACK);
                    g.strokeOval(cx - stoneR, cy - stoneR, 2 * stoneR, 2 * stoneR);
                }
            }
        }

        // overlays tylko w review albo po zakończeniu territory — nakładki punktacji (terytorium/martwe)
        if (model.showScoringOverlays()) {

            // TERRITORY overlay na pustych polach — znaczniki wg mapy terytorium (b/w/s)
            char[][] terr = model.getTerritoryMap();
            if (terr != null && terr.length == size && terr[0].length == size) {
                double r = stoneR * 0.38;
                double oldAlpha = g.getGlobalAlpha();

                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        if (board[x][y] != GameModel.EMPTY) continue;

                        char t = terr[x][y];

                        if (t == 'b') {
                            // 'b' — terytorium czarnych
                            g.setGlobalAlpha(0.25);
                            g.setFill(Color.BLACK);
                            double cx = pad + x * stepX;
                            double cy = pad + y * stepY;
                            g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                        } else if (t == 'w') {
                            // 'w' — terytorium białych
                            g.setGlobalAlpha(0.40); // bardziej widoczne białe punkty
                            g.setFill(Color.WHITE);
                            double cx = pad + x * stepX;
                            double cy = pad + y * stepY;
                            g.fillOval(cx - r, cy - r, 2 * r, 2 * r);

                            // kontur dla czytelności na jasnym tle
                            g.setGlobalAlpha(0.25);
                            g.setStroke(Color.BLACK);
                            g.setLineWidth(1.0);
                            g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                        } else if (t == 's') {
                            // 's' — seki
                            g.setGlobalAlpha(0.22);
                            g.setFill(Color.GRAY);
                            double cx = pad + x * stepX;
                            double cy = pad + y * stepY;
                            double rr = r * 0.95;
                            g.fillRect(cx - rr, cy - rr, 2 * rr, 2 * rr);
                        }
                    }
                }

                g.setGlobalAlpha(oldAlpha);
            }

            // DEAD stones overlay – krzyżyk na kamieniach uznanych za martwe
            boolean[][] dead = model.getDeadMask();
            if (dead != null && dead.length == size && dead[0].length == size) {
                double oldAlpha = g.getGlobalAlpha();

                g.setStroke(Color.RED);
                g.setLineWidth(2.0);
                g.setGlobalAlpha(0.75);

                double crossR = stoneR * 0.70;

                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        if (board[x][y] == GameModel.EMPTY) continue;
                        if (!dead[x][y]) continue;

                        double cx = pad + x * stepX;
                        double cy = pad + y * stepY;

                        g.strokeLine(cx - crossR, cy - crossR, cx + crossR, cy + crossR);
                        g.strokeLine(cx - crossR, cy + crossR, cx + crossR, cy - crossR);
                    }
                }

                g.setGlobalAlpha(oldAlpha);
            }
        }
    }

    private void handleClick(MouseEvent e) {
        int size = model.getBoardSize();
        if (size <= 1) return;

        double w = getWidth();
        double h = getHeight();

        double pad = 20.0;
        double gridW = w - 2 * pad;
        double gridH = h - 2 * pad;

        double stepX = gridW / (size - 1);
        double stepY = gridH / (size - 1);

        double mx = e.getX();
        double my = e.getY();

        // Mapowanie pikseli na indeksy planszy: najbliższe przecięcie siatki (round)
        int x = (int) Math.round((mx - pad) / stepX);
        int y = (int) Math.round((my - pad) / stepY);

        if (x < 0 || y < 0 || x >= size || y >= size) return;

        clickHandler.onClick(x, y);
    }
}
