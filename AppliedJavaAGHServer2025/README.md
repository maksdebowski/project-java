# AppliedJavaAGHServer2025

## Cel projektu (grupa 4–5 osób)

Waszym zadaniem jest **rozbudowa serwera i klientów** gry labiryntowej.

* **Serwer rozwijacie jako grupa** – każdy członek zespołu musi umieć wyjaśnić: co zostało dodane/zmienione i jak działa.
* Serwer należy skonfigurować tak, aby **każdy członek grupy (jego klient)** wziął udział w końcowej rozgrywce.
* Komunikacja: **TCP + JSON (jedna wiadomość = jedna linia)**, zgodnie z `Request/Response`.

---

## Poziom 1 — protokół i podstawy klient–serwer (każdy uczestnik)

1. **Klient gry (konsolowy)**

    * Połączenie z serwerem uruchomionym na komputerze jednego z członków drużyny.
    * Wysłanie `Request.Authorize(key)` i obsługa `Response.Unauthorized` / `Response.Authorized`.
    * Przygotowanie **wspólnego pliku konfiguracyjnego** (tak, żeby każdy miał swoje `key`).

2. **Pobranie mapy**

    * Po autoryzacji klient odbiera `Response.StateCave(cave)` i buduje lokalną reprezentację mapy:

        * `rock(row,col)`, `rows()`, `columns()`
        * oraz **inne statyczne elementy**, jeśli je dodacie.

3. **Sterowanie (automatyczne)**

    * Klient ma działać automatycznie: strategia ruchu wybiera `Direction`, wysyła `Request.Command(Direction)` jako JSON-linię.
    * Strategia może być prosta (np. losowa z unikaniem ścian) albo sprytniejsza.

4. **Odbiór stanu w czasie rzeczywistym**

    * Klient czyta strumień `Response.StateLocations(...)` i aktualizuje:

        * `playerLocations`, `itemLocations`
        * własne `health` i `gold`.

5. **Renderowanie świata**

    * Widok oparty o:

        * skały z `cave.rock(r,c)`
        * graczy: `P` i `D`
        * itemy: `G` i `H`
        * inne elementy dodane przez grupę (jeśli będą).

---

## Poziom 2 — poprawki i bezpieczeństwo działania serwera (grupa)

6. **Naprawa ruchu poza mapę (bug krytyczny)**

    * W `Game.move()` można wyjść poza planszę, a `cave.rock()` nie ma sprawdzania granic → ryzyko wyjątku.
    * Wymaganie: dodać kontrolę granic (np. “jeśli poza mapą → ruch odrzucony”).

7. **Naprawa generatorów itemów**

    * W `generateHealth()` i `generateGold()` są użyte zamienione stałe (`NUM_GOLD` vs `NUM_HEALTH`).
    * Wymaganie: poprawić + dodać test, że liczby się zgadzają.

8. **Walidacja komend po stronie serwera**

    * Serwer ma być odporny na:

        * śmieciowy JSON / błędny typ requestu
        * `null`/braki pól
    * Wymaganie: logowanie błędu i dalsza praca serwera (bez wysypania wątku).

---

## Poziom 3 — mechanika gry (modyfikacje w `Game`)

9. **Koszt ruchu w HP**

    * Dodaj koszt za ruch, np. `-1 HP` za każdy zaakceptowany krok.
    * Dodatkowo: wprowadź “trudny teren” (nowy typ pola) *albo* zasadę “uderzenia w ścianę”:

        * np. jeśli gracz próbuje wejść w skałę / poza mapę → ruch odrzucony i `-5 HP`.

10. **Zasady walki — zmiana strategii**

* Aktualnie: gdy >1 gracz na polu, wszyscy tracą część HP.
* Wymaganie: zmienić strategię walki (i krótko opisać regułę w README).

11. **Zbieranie itemów — dzielenie proporcjonalne do HP**

* Aktualnie: item dostaje tylko najsilniejszy gracz na polu.
* Nowa reguła: wartość itemu dzielona proporcjonalnie do HP graczy na polu, np.:

    * `share_i = floor(value * hp_i / sum_hp)`
    * resztę (z zaokrągleń) przydzielcie deterministycznie (np. najwyższe HP albo kolejność po nazwie).
* Dotyczy zarówno Gold jak i Health.

12. **Respawn itemów**

* Aktualnie: gdy nie ma ani jednego Gold/Health → generuj.
* Wymaganie: respawn wg stałej gęstości (np. “utrzymuj min. X Gold i Y Health”) **albo** respawn co N ticków.

13. **Limit HP**

* Wprowadź `maxHealth` (np. 100). Leczenie nie może przekraczać limitu.

14. **Wynik**

* Zdefiniuj wynik, np.:

    * `score = gold + bonusZaHP - karaZaCzas(ruchy/ticki)`
* Wynik ma być możliwy do pokazania w kliencie (np. na końcu lub na bieżąco).

---
Jasne — dopisuję wymaganie **po punkcie 14** i dorzucam kilka sensownych propozycji, które dobrze pasują do “wyjścia z labiryntu + fair start”.

**Zadanie z * Wyjście z Labiryntu**

* **Zmodyfikuj generowanie labiryntu**, aby istniało specjalne pole **Wyjścia (Exit)** będące celem rozgrywki.
* Celem gracza jest **dotarcie do wyjścia z jak najwyższym wynikiem**.
* Generowanie ma gwarantować, że:

    * **wyjście jest osiągalne** (z każdego startu gracza),
    * mapa nie jest “zablokowana” losowo.

* Podczas rozmieszczania graczy zapewnij, że ich pozycje startowe są w **możliwie podobnej odległości (najlepiej: takiej samej)** od wyjścia.
* Praktyczna metoda: po wygenerowaniu mapy policz odległości od `Exit` (np. BFS po polach niebędących skałami), a następnie:
    * wybierz starty z **tej samej warstwy odległości**, albo
    * minimalizuj różnicę (np. `maxDist - minDist` jak najmniejsze).



O 1. **Warunek zakończenia rundy**
* Gra kończy się dla gracza, gdy wejdzie na `Exit` (jest “finished”), ale serwer może:        
  * kończyć całą rozgrywkę, gdy wszyscy skończą **lub** po limicie ticków.

## Poziom 4 — multiplayer i NPC (grupa)

15. **Konfiguracja wielu graczy**

* Serwer czyta `configuration.json` (known players).
* Wymaganie:

    * każdy członek grupy ma wpis (unikalna nazwa + klucz),
    * walidacja (np. unikalne nazwy/klucze, sensowna długość klucza).

16. **Dragon jako NPC**

* W kodzie jest `Player.Dragon`.
* Wymaganie: dodać dragona jako NPC sterowanego przez serwer (ruch co tick).
* Kolizja/walka z dragonem powinna działać spójnie z Waszą nową regułą walki.

---

## Poziom 5 — jakość rozwiązania

17. **Czytelność i utrzymanie**

* sensowne nazwy, małe metody, brak “magicznych liczb”
* krótki opis reguł w `README` (co zmieniliście i jak gra działa)

18. **Testy**

* Minimum: testy na

    * ruch i granice mapy,
    * generowanie itemów (liczby),
    * walkę i dzielenie loot’u,
    * limit HP.

---

### Minimalne wymaganie “na zaliczenie” (praktyczne)

* Serwer uruchamia się stabilnie, obsługuje wielu graczy z konfiguracji.
* Każdy z członków grupy uruchamia swojego klienta, autoryzuje się własnym kluczem i gra “na żywo” (ticki + stany).
* Wprowadzone zmiany w `Game` faktycznie działają (HP koszt, nowa walka, dzielenie itemów, respawn, maxHP, wynik, dragon NPC).

Jeśli chcesz, mogę Ci to jeszcze przerobić na **krótką kartę wymagań (checklistę do oceny)** + propozycję podziału pracy w grupie (kto co robi), tak żeby było łatwo rozliczyć wkład każdej osoby.
