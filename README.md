# Go Game – Iteracja 2 (klient–serwer + GUI + scoring)

Projekt zaliczeniowy z laboratorium – uproszczona gra **Go** w architekturze **klient–serwer**.

* logika gry i walidacja ruchów po stronie serwera (`Board` + `Game`),
* dwaj klienci łączą się do serwera i grają przeciwko sobie,
* dostępne interfejsy:

  * **CLI** (terminal),
  * **GUI** (JavaFX),
* projekt zrealizowany w **Javie 17** z użyciem **Mavena**.

---

## 1. Funkcjonalność (Iteracja 2)

### 1.1. Rozgrywka

* dwóch graczy: **BLACK** i **WHITE**,
* plansza kwadratowa (domyślnie **9×9**),
* kolejność ruchów: **BLACK zaczyna**, potem naprzemiennie,
* legalny ruch:

  * kamień stawiany na puste pole,
  * bicie całych grup przeciwnika po utracie oddechów,
  * **zakaz samobójstwa** (chyba że ruch bije kamienie przeciwnika).

### 1.2. Komendy gracza

* `MOVE ...` – wykonanie ruchu:

  * CLI: `MOVE B2` lub `MOVE B 2` (notacja literowa po stronie klienta),
  * protokół do serwera: zawsze `MOVE x y` (0-based).
* `PASS` – pas.
* `RESIGN` – rezygnacja (**zadanie 10: w dowolnym momencie gry**).
* `AGREE` – zgoda na zakończenie i punktację (tylko w fazie `SCORING_REVIEW`).
* `RESUME` – wznowienie gry z `SCORING_REVIEW` do `PLAYING` (tylko w fazie `SCORING_REVIEW`).

### 1.3. Fazy gry (zadanie 8)

Po dwóch kolejnych `PASS` gra **nie kończy się od razu**, tylko przechodzi do fazy:

* `PLAYING` – normalna gra,
* `SCORING_REVIEW` – tryb przeglądu punktacji:

  * gracze mogą tylko: `AGREE` lub `RESUME`,
  * serwer wysyła wynik i mapy pomocnicze (SCORE/TERRITORY/DEADSTONES),
* `FINISHED` – gra zakończona.

**Kluczowy detal wznowienia (RESUME):**
jeżeli któryś gracz wykona `RESUME`, wracamy do `PLAYING`, resetujemy licznik kolejnych PASS, a **następny ruch wykonuje przeciwnik wznawiającego** (wznawiający „oddaje” ruch).

### 1.4. Punktacja (zadanie 9)

W `SCORING_REVIEW` serwer oblicza wynik jako:

**Score = Territory + Dead Stones**

* `TERRITORY` – mapa terytorium (BLACK/WHITE/NEUTRAL/SEKI),
* `DEADSTONES` – maska kamieni uznanych za martwe (punkty dla przeciwnika),
* `SCORE` – finalne liczby punktów BLACK/WHITE.

Gra kończy się dopiero po:

* `AGREE` od BLACK i `AGREE` od WHITE → `END ... territory`.

---

## 2. Wymagania

* Java 17+
* Maven 3.x
* (GUI) JavaFX – przez Maven dependency (`javafx-controls`)
* dostęp do konsoli / terminala (Windows / Linux / WSL / macOS)

---

## 3. Budowanie projektu

W katalogu z `pom.xml`:

```bash
mvn clean compile
```

Testy:

```bash
mvn test
```

---

## 4. Uruchamianie

### 4.1. Serwer

Najprościej przez Maven (spójne z konfiguracją projektu):

```bash
mvn -Dexec.mainClass=pl.edu.go.server.GameServer exec:java
```

Serwer:

* nasłuchuje na porcie **5001**,
* tworzy planszę **9×9**,
* czeka na dwóch graczy.

### 4.2. Klient CLI (dwa terminale)

W dwóch osobnych terminalach:

```bash
mvn -Dexec.mainClass=pl.edu.go.client.cli.CliClient exec:java
```

### 4.3. Klient GUI (JavaFX)

```bash
mvn javafx:run
```

Uruchom dwa razy (dla dwóch klientów) w dwóch procesach/oknach.

---

## 5. Sterowanie – CLI

### 5.1. MOVE (notacja literowa – tylko po stronie klienta)

Dozwolone formaty:

```text
MOVE B2
MOVE B 2
```

Zasady:

* kolumny: `A..` (A=0, B=1, C=2, …),
* wiersze: **od 1 do size**,
* klient konwertuje do współrzędnych 0-based i wysyła do serwera:

```text
MOVE x y
```

### 5.2. PASS / RESIGN

```text
PASS
RESIGN
```

### 5.3. SCORING_REVIEW: AGREE / RESUME

Po wejściu do trybu review:

```text
AGREE
RESUME
```

---

## 6. Protokół tekstowy klient–serwer

Komunikacja to protokół tekstowy: jedna linia = jedna wiadomość.

### 6.1. Komendy klient → serwer

Serwer rozumie:

* `MOVE x y` – dwa argumenty liczbowe (0-based),
* `PASS`
* `RESIGN`
* `AGREE` (tylko `SCORING_REVIEW`)
* `RESUME` (tylko `SCORING_REVIEW`)

Walidacja formatu odbywa się w `TextCommandFactory`. Błędne formaty skutkują `ERROR ...`.

### 6.2. Odpowiedzi serwer → klient

* `WELCOME BLACK|WHITE`
* `TURN BLACK|WHITE`
* `PHASE PLAYING|SCORING_REVIEW|FINISHED`
* `ERROR <opis>`
* `END <WINNER> <reason>`

Opis planszy:

```text
BOARD <size>
ROW <wiersz0>
ROW <wiersz1>
...
ROW <wierszN-1>
END_BOARD
```

gdzie `<wiersz>` to ciąg znaków:

* `.` – puste pole
* `X` – kamień czarny
* `O` – kamień biały

Dane punktacji (wysyłane w `SCORING_REVIEW`, a także po zakończeniu przez terytorium):

```text
SCORE <black> <white>
TERRITORY <size>
TROW <string>
...
END_TERRITORY
DEADSTONES <size>
DROW <string>
...
END_DEADSTONES
```

Interpretacja:

* `TROW` – znaki określają terytorium (BLACK/WHITE/NEUTRAL/SEKI; dokładna reprezentacja zależna od implementacji GUI),
* `DROW` – `1` oznacza kamień uznany za martwy (punkt dla przeciwnika), `0` – brak oznaczenia.

---

## 7. Struktura pakietów

```text
pl.edu.go.board
    Board
    BoardFactory
    Territory

pl.edu.go.model
    Stone
    StoneGroup

pl.edu.go.move
    Move
    MoveAdapter
    MoveFactory

pl.edu.go.analysis
    PositionAnalyzer
    TerritoryAnalyzer
    ScoreCalculator

pl.edu.go.game
    Game
    ObservableGame
    GameObserver
    GamePhase
    GameResult
    PlayerColor

pl.edu.go.command
    GameCommand
    PlaceStoneCommand
    PassCommand
    ResignCommand
    AgreeCommand
    ResumeCommand
    TextCommandFactory

pl.edu.go.server
    GameServer
    GameSession
    ClientHandler

pl.edu.go.client.net
    NetworkClient

pl.edu.go.client.gui
    GuiClientApp
    GameModel
    GameController
    BoardView

pl.edu.go.client.cli
    CliClient

pl.edu.go
    MainTest
```

---

## 8. Wzorce projektowe i architektura

* **Client–Server**: `GameServer` + klienci (CLI/GUI)
* **Layered Architecture**:

  * transport: `ClientHandler`, `NetworkClient`
  * aplikacja: `GameSession`
  * domena: `Game`, `Board`, analiza (`ScoreCalculator`, `TerritoryAnalyzer`, `PositionAnalyzer`)
* **Composite**: `StoneGroup` zawiera `Stone`
* **Adapter**: `MoveAdapter` (notacja użytkownika ⇄ współrzędne)
* **Factory Method / Simple Factory**: `BoardFactory`, `MoveFactory`, `TextCommandFactory`
* **Command**: `GameCommand` + komendy (`PlaceStoneCommand`, `PassCommand`, `ResignCommand`, `AgreeCommand`, `ResumeCommand`)
* **Observer**:

  * Subject: `Game` / `ObservableGame`
  * Observer: `GameSession` (wysyła stan do klientów)
* **MVC (GUI)**:

  * Model: `GameModel`
  * View: `BoardView`
  * Controller: `GameController`

---

## 9. Dokumentacja i UML

### 9.1. Javadoc

Generowanie:

```bash
mvn javadoc:javadoc
```

Podgląd:

```bash
xdg-open target/site/apidocs/index.html
```

### 9.2. UML (PlantUML)

Plik:

* `src/main/java/pl/edu/go/all.puml`

Generowanie PNG:

```bash
plantuml -tpng src/main/java/pl/edu/go/all.puml
xdg-open src/main/java/pl/edu/go/all.png
```

---

## 10. Uruchamianie w skrócie

1. Kompilacja:

```bash
mvn clean compile
```

2. Serwer:

```bash
mvn -q exec:java@server
```

3. Klienci (CLI lub GUI):

```bash
mvn -Dexec.mainClass=pl.edu.go.client.cli.CliClient exec:java

lub

mvn javafx:run
```

4. Gra:
   `MOVE ...`, `PASS`, `RESIGN`, a w `SCORING_REVIEW`: `AGREE` / `RESUME`.