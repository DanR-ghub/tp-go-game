package pl.edu.go.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pl.edu.go.client.net.NetworkClient;
import pl.edu.go.game.GamePhase;

/**
 * {@code GuiClientApp} jest główną aplikacją JavaFX klienta gry Go.
 *
 * <p><b>Wzorzec architektoniczny:</b> <b>MVC</b> (w praktyce odmiana MVP).
 * <ul>
 *   <li><b>Model</b>: {@link pl.edu.go.client.gui.GameModel} — stan gry po stronie klienta,</li>
 *   <li><b>View</b>: {@link pl.edu.go.client.gui.BoardView} + elementy UI,</li>
 *   <li><b>Controller</b>: {@link pl.edu.go.client.gui.GameController} — mapuje akcje UI na komendy protokołu.</li>
 * </ul>
 *
 * <p>GUI nie liczy reguł Go ani punktacji — jest prezentacją stanu otrzymanego z serwera.
 */
public final class GuiClientApp extends Application {

    /** Model klienta, aktualizowany wyłącznie komunikatami z serwera. */
    private final GameModel model = new GameModel();

    /** Warstwa sieciowa klienta (połączenie + odbiór linii protokołu). */
    private final NetworkClient net = new NetworkClient();

    private BoardView boardView;
    private GameController controller;

    private Label statusLabel;
    private Label turnLabel;
    private Label colorLabel;
    private Label phaseLabel;
    private Label scoreLabel;

    private Button passBtn;
    private Button resignBtn;
    private Button agreeBtn;
    private Button resumeBtn;

    /**
     * Inicjalizuje UI, wiąże MVC oraz podłącza obsługę sieci.
     *
     * <p>Najważniejsze powiązania:
     * <ul>
     *   <li>kliknięcia w {@link BoardView} → {@link GameController#onIntersectionClicked(int, int)}</li>
     *   <li>linie z serwera → {@link GameModel#acceptServerLine(String)}</li>
     *   <li>zmiana modelu → {@link #refreshUI()} na wątku JavaFX</li>
     * </ul>
     */
    @Override
    public void start(Stage stage) {
        controller = new GameController(net, model);

        TextField hostField = new TextField("localhost");
        hostField.setPrefColumnCount(12);

        TextField portField = new TextField("5001");
        portField.setPrefColumnCount(6);

        Button connectBtn = new Button("Connect");
        Button disconnectBtn = new Button("Disconnect");
        disconnectBtn.setDisable(true);

        HBox connectBar = new HBox(8,
                new Label("Host:"), hostField,
                new Label("Port:"), portField,
                connectBtn, disconnectBtn);
        connectBar.setPadding(new Insets(10));

        statusLabel = new Label("Disconnected");
        colorLabel = new Label("Color: -");
        turnLabel = new Label("Turn: -");
        phaseLabel = new Label("Phase: -");
        scoreLabel = new Label("Score: -");

        VBox infoBox = new VBox(6, statusLabel, colorLabel, turnLabel, phaseLabel, scoreLabel);
        infoBox.setPadding(new Insets(10));

        boardView = new BoardView(model, 560, 560);
        boardView.setClickHandler(controller::onIntersectionClicked);

        passBtn = new Button("PASS");
        resignBtn = new Button("RESIGN");
        agreeBtn = new Button("AGREE");
        resumeBtn = new Button("RESUME");

        passBtn.setMaxWidth(Double.MAX_VALUE);
        resignBtn.setMaxWidth(Double.MAX_VALUE);
        agreeBtn.setMaxWidth(Double.MAX_VALUE);
        resumeBtn.setMaxWidth(Double.MAX_VALUE);

        passBtn.setOnAction(e -> controller.sendPass());
        resignBtn.setOnAction(e -> controller.sendResign());
        agreeBtn.setOnAction(e -> controller.sendAgree());
        resumeBtn.setOnAction(e -> controller.sendResume());

        VBox actionsBox = new VBox(8,
                passBtn, resignBtn,
                new Separator(),
                agreeBtn, resumeBtn);
        actionsBox.setPadding(new Insets(10));
        actionsBox.setFillWidth(true);

        VBox rightPane = new VBox(10, infoBox, actionsBox);
        rightPane.setPadding(new Insets(10));
        rightPane.setPrefWidth(340);

        BorderPane root = new BorderPane();
        root.setTop(connectBar);
        root.setCenter(boardView);
        root.setRight(rightPane);

        Scene scene = new Scene(root, 950, 650);
        stage.setTitle("Go Game - GUI Client");
        stage.setScene(scene);
        stage.show();

        // Listener modelu może być wywołany z wątku sieciowego; UI odświeżamy przez Platform.runLater(...)
        model.addListener(() -> Platform.runLater(this::refreshUI));

        // Odbierane linie z serwera aktualizują model; model sam powiadamia GUI przez listener
        net.setOnLine(model::acceptServerLine);

        // Obsługa błędów sieciowych: przełączamy UI do stanu rozłączonego
        net.setOnError(ex -> Platform.runLater(() -> {
            System.err.println("[CLIENT] Network error: " + ex.getMessage());
            statusLabel.setText("Disconnected (error)");
            connectBtn.setDisable(false);
            disconnectBtn.setDisable(true);
            refreshUI();
        }));

        // Connect: nawiązanie połączenia i przełączenie UI w tryb "Connected"
        connectBtn.setOnAction(e -> {
            String host = hostField.getText().trim();
            int port;

            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException nfe) {
                statusLabel.setText("Invalid port");
                return;
            }

            try {
                net.connect(host, port);
                statusLabel.setText("Connected: " + host + ":" + port);
                connectBtn.setDisable(true);
                disconnectBtn.setDisable(false);
                refreshUI();
            } catch (Exception ex) {
                statusLabel.setText("Connect failed");
                System.err.println("[CLIENT] Connect failed: " + ex.getMessage());
                connectBtn.setDisable(false);
                disconnectBtn.setDisable(true);
            }
        });

        // Disconnect: zamknięcie połączenia i powrót UI do stanu początkowego
        disconnectBtn.setOnAction(e -> {
            net.disconnect();
            statusLabel.setText("Disconnected");
            connectBtn.setDisable(false);
            disconnectBtn.setDisable(true);
            refreshUI();
        });

        refreshUI();
    }

    /**
     * Odświeża widok na podstawie aktualnego stanu {@link GameModel} i połączenia sieciowego.
     *
     * <p>Odpowiada za:
     * <ul>
     *   <li>przerysowanie planszy,</li>
     *   <li>aktualizację etykiet (color/turn/phase/score),</li>
     *   <li>aktywację/dezaktywację przycisków zależnie od stanu gry.</li>
     * </ul>
     */
    private void refreshUI() {
        boardView.redraw();

        if (model.getMyColor() != null) colorLabel.setText("Color: " + model.getMyColor());
        else colorLabel.setText("Color: -");

        if (model.getPhase() == GamePhase.PLAYING && model.getCurrentTurn() != null) {
            turnLabel.setText("Turn: " + model.getCurrentTurn());
        } else {
            turnLabel.setText("Turn: -");
        }

        phaseLabel.setText("Phase: " + model.getPhase());

        boolean showScore = model.showScoringOverlays();
        if (showScore && model.getScoreBlack() != null && model.getScoreWhite() != null) {
            scoreLabel.setText("Score: BLACK " + model.getScoreBlack() + " / WHITE " + model.getScoreWhite());
        } else {
            scoreLabel.setText("Score: -");
        }

        boolean connected = net.isConnected();
        boolean finished = model.isFinished();

        passBtn.setDisable(!connected || finished || !model.canPlayNow());
        resignBtn.setDisable(!connected || finished);

        boolean review = model.inReview();
        agreeBtn.setDisable(!connected || finished || !review);
        resumeBtn.setDisable(!connected || finished || !review);

        if (connected && !finished) {
            // statusLabel zostawiamy jako stan połączenia
        } else if (finished) {
            statusLabel.setText("Finished: " + model.getEndMessage());
        }
    }

    /**
     * Sprząta zasoby po zamknięciu aplikacji (rozłącza klienta).
     */
    @Override
    public void stop() {
        net.disconnect();
    }
}
