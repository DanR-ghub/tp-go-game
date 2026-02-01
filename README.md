# Go Game – Iteracja 1 (klient–serwer)

Projekt zaliczeniowy z laboratorium – uproszczona gra **Go** w architekturze **klient–serwer**.

* logika gry (plansza, bicie, zakaz samobójstwa) po stronie serwera,
* dwaj klienci łączą się do serwera i grają przeciwko sobie,
* interfejs tekstowy (terminal) – rysowanie planszy,
* projekt zrealizowany w Javie 17 z użyciem **Mavena**.

---

## 1. Funkcjonalność (Iteracja 1)

Zaimplementowane są zasady Go wymagane w iteracji 1:

* rozgrywka dwóch graczy: **BLACK** i **WHITE**,
* plansza kwadratowa (domyślnie 9×9),
* kolejność ruchów: **BLACK zaczyna**, potem naprzemiennie,
* legalny ruch:
  * kamień stawiany na puste pole,
  * bicie całych grup przeciwnika po utracie oddechów,
  * **zakaz samobójstwa** (chyba że ruch bije kamienie przeciwnika),
* możliwe akcje gracza:
  * `MOVE B2` lub `MOVE B 2` – ruch w notacji literowej (kolumna jako litera),
  * `PASS` – pas,
  * `RESIGN` – rezygnacja,
* koniec gry:
  * `RESIGN` → przeciwnik wygrywa,
  * dwa kolejne `PASS` → koniec gry z powodem `two passes` (bez liczenia punktów).

---

## 2. Wymagania

* Java 17+
* Maven 3.x
* dostęp do konsoli / terminala (Windows / Linux / WSL / macOS)

---

## 3. Budowanie projektu

W katalogu z `pom.xml`:

```bash
mvn clean compile
```

---

## 4. Uruchamianie – serwer i dwaj klienci

### 4.1. Uruchom serwer

```bash
java -cp target/classes pl.edu.go.server.GameServer
```

Serwer:

* nasłuchuje na porcie **5001**,
* tworzy planszę **9×9**,
* czeka na dwóch graczy.

Przykładowy log serwera:

```text
Server listening on port 5001
First player connected (BLACK)
Second player connected (WHITE)
Game started. Waiting for moves...
```

### 4.2. Uruchom dwóch klientów (w dwóch osobnych terminalach)

```bash
java -cp target/classes pl.edu.go.client.cli.CliClient
```

Po połączeniu klient wypisze m.in.:

```text
Connected to localhost:5001
INFO Connected as BLACK
Commands: MOVE B2 / MOVE B 2 | PASS | RESIGN  (or: exit)
WELCOME BLACK
INFO Game started. BLACK moves first.
...
TURN BLACK
```

Pierwszy podłączony klient gra czarnymi (**BLACK**), drugi – białymi (**WHITE**).

---

## 5. Sterowanie z konsoli (CLI)

### 5.1. Ruch (MOVE) – tylko notacja z literą

Dozwolone formaty:

```text
MOVE B2
MOVE B 2
```

Zasady:

* kolumny to litery `A..` (A=0, B=1, C=2, …),
* wiersze użytkownik wpisuje jako liczby **od 1 do size** (np. `2` oznacza drugi wiersz),
* klient konwertuje notację użytkownika na współrzędne 0-based.

Uwaga: **serwer nie przyjmuje notacji literowej**. Do serwera zawsze wysyłane jest:

```text
MOVE x y
```

gdzie `x` i `y` są liczbami 0-based.

Jeżeli użytkownik wpisze zły format (np. `MOVE 1 2`), klient wypisze błąd i **nie wyśle nic** do serwera.

### 5.2. PASS

```text
PASS
```

### 5.3. RESIGN

```text
RESIGN
```

### 5.4. Wyjście z klienta (lokalnie)

```text
exit
```

lub

```text
quit
```

Po każdym poprawnym ruchu serwer wysyła nowy stan planszy (`BOARD / ROW / END_BOARD`), a klient rysuje ją w terminalu jako grid z symbolami:

* `●` – kamień czarny,
* `○` – kamień biały,
* `.` – puste pole.

Po zakończeniu gry serwer wysyła np.:

```text
END WHITE resign
```

albo przy dwóch pasach:

```text
END NONE two passes
```

Klient wyświetla komunikat `END ...` i **automatycznie kończy działanie**.

---

## 6. Protokół tekstowy klient–serwer

Komunikacja to protokół tekstowy: jedna linia = jedna wiadomość.

### 6.1. Komendy klient → serwer (protokół)

Serwer rozumie wyłącznie:

* `MOVE x y` – dokładnie dwa argumenty, obie wartości muszą być liczbami całkowitymi,
* `PASS` – bez argumentów,
* `RESIGN` – bez argumentów.

Walidacja formatu odbywa się w `TextCommandFactory`. Błędne formaty skutkują komunikatem `ERROR ...` (serwer nie crashuje).

### 6.2. Odpowiedzi serwer → klient

* `INFO <tekst>` – komunikaty informacyjne,
* `WELCOME BLACK|WHITE` – przypisanie koloru klientowi,
* `TURN BLACK|WHITE` – informacja o turze,
* `ERROR <opis>` – błąd (np. zły format komendy lub nielegalny ruch),
* `END <WINNER> <reason>` – koniec gry,
* opis planszy:

```text
BOARD <size>
ROW <wiersz0>
ROW <wiersz1>
...
ROW <wierszN-1>
END_BOARD
```

gdzie `<wiersz>` to ciąg znaków `'.'`, `'X'`, `'O'`:

* `.` – puste pole,
* `X` – kamień czarny,
* `O` – kamień biały.

Klient parsuje `BOARD / ROW / END_BOARD` i rysuje planszę w metodzie `displayBoard`.

---

## 7. Struktura pakietów

```text
pl.edu.go.board
    Board           // logika planszy: oddechy, grupy, bicie, zakaz samobójstwa
    BoardFactory    // fabryka tworząca plansze o zadanym rozmiarze

pl.edu.go.model
    Stone           // pojedynczy kamień
    StoneGroup      // grupa kamieni (łańcuch)

pl.edu.go.move
    Move            // obiekt ruchu (kolor + x,y)
    MoveAdapter     // adapter notacji użytkownika (B2/B 2) -> współrzędne
    MoveFactory     // fabryka obiektów Move

pl.edu.go.game
    Game            // logika wyższego poziomu: tura, pass, resign, koniec gry
    GameObserver    // interfejs obserwatora gry (Observer)
    GameResult      // wynik gry
    PlayerColor     // enum BLACK/WHITE + mapowanie na Board.BLACK/WHITE

pl.edu.go.command
    GameCommand         // interfejs wzorca Command
    PlaceStoneCommand   // komenda: postaw kamień (bazuje na Move)
    PassCommand         // komenda: pas
    ResignCommand       // komenda: rezygnacja
    TextCommandFactory  // fabryka: tekst protokołu -> GameCommand

pl.edu.go.server
    GameServer      // start serwera, akceptuje dwóch klientów
    GameSession     // sesja gry: łączy Game, Command i klienta
    ClientHandler   // obsługa klienta (wątek, socket)

pl.edu.go.client.cli
    CliClient       // klient konsolowy: wejście z klawiatury + render planszy
```

---

## 8. Wzorce projektowe

* **Composite**: `Stone` + `StoneGroup`
* **Adapter**: `MoveAdapter`
* **Factory Method**: `BoardFactory`, `MoveFactory`, `TextCommandFactory`
* **Command**: `GameCommand` + `PlaceStoneCommand`/`PassCommand`/`ResignCommand`
* **Observer**: `Game` (subject) + `GameSession` (observer)

---

## 9. Ograniczenia i uproszczenia

* Serwer obsługuje jedną grę naraz i dokładnie dwóch klientów.
* Brak liczenia punktów – przy dwóch `PASS` kończymy grę z `WINNER = NONE`.
* Brak reguł ko/superko (zgodnie z Iteracją 1).

---

## 10. Uruchamianie w skrócie

1. Skompiluj:

```bash
mvn clean compile
```

2. Serwer:

```bash
java -cp target/classes pl.edu.go.server.GameServer
```

3. Klient 1 i 2:

```bash
java -cp target/classes pl.edu.go.client.cli.CliClient
```

4. Graj komendami: 
```bash
`MOVE B2` / `MOVE B 2`, `PASS`, `RESIGN`.
```
