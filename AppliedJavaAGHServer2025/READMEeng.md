# AppliedJavaAGHServer2025


## Project goal (group of 4–5 people)

Your task is to **extend the server and the clients** of a maze game.

* **You develop the server as a group** — every team member must be able to explain what was added/changed and how it works.
* The server must be configured so that **each group member (their client)** participates in the final match.
* Communication: **TCP + JSON (one message = one line)**, consistent with `Request/Response`.

---

## Level 1 — protocol and client–server basics (each participant)

1. **Game client (console)**

    * Connect to the server running on the computer of one team member.
    * Send `Request.Authorize(key)` and handle `Response.Unauthorized` / `Response.Authorized`.
    * Prepare a **shared configuration file** (so that everyone has their own `key`).

2. **Download the map**

    * After authorization, the client receives `Response.StateCave(cave)` and builds a local map representation:

        * `rock(row,col)`, `rows()`, `columns()`
        * plus **any other static game elements** you add.

3. **Control (automatic)**

    * The client must run automatically: a movement strategy chooses a `Direction` and sends `Request.Command(Direction)` as a JSON line.
    * The strategy can be simple (e.g., random with wall avoidance) or more advanced.

4. **Real-time state updates**

    * The client reads the stream of `Response.StateLocations(...)` and updates:

        * `playerLocations`, `itemLocations`
        * its own `health` and `gold`.

5. **World rendering**

    * The view is based on:

        * rocks from `cave.rock(r,c)`
        * players: `P` and `D`
        * items: `G` and `H`
        * any other elements added by the team (if present).

---

## Level 2 — server robustness and safety fixes (group)

6. **Fix moving outside the map (critical bug)**

    * In `Game.move()` it is possible to move outside the board, and `cave.rock()` does not check bounds → risk of exceptions.
    * Requirement: add boundary checks (e.g., “if outside → reject the move”).

7. **Fix item generators**

    * In `generateHealth()` and `generateGold()` the constants are swapped (`NUM_GOLD` vs `NUM_HEALTH`).
    * Requirement: fix it + add a test verifying the correct counts.

8. **Server-side command validation**

    * The server must be resilient to:

        * garbage JSON / wrong request type
        * `null` / missing fields
    * Requirement: log the error and keep the server running (do not crash the thread).

---

## Level 3 — gameplay mechanics (changes in `Game`)

9. **Movement cost in HP**

    * Add a movement cost, e.g. `-1 HP` for each accepted step.
    * Additionally: introduce “difficult terrain” (a new tile type) *or* a “hit the wall” rule:

        * e.g., if a player tries to enter a rock / go out of bounds → the move is rejected and `-5 HP`.

10. **Combat rules — change the strategy**

* Currently: if >1 player is on the same tile, everyone loses some HP.
* Requirement: change the combat strategy (and briefly describe the rule in the README).

11. **Item pickup — proportional split by HP**

* Currently: only the strongest player on the tile gets the item.
* New rule: split the item value proportionally to players’ HP on that tile, e.g.:

    * `share_i = floor(value * hp_i / sum_hp)`
    * distribute the remainder deterministically (e.g., highest HP first or by name order).
* Applies to both Gold and Health.

12. **Item respawn**

* Currently: if there is no Gold/Health on the map → generate it.
* Requirement: respawn by constant density (e.g., “keep at least X Gold and Y Health”) **or** respawn every N ticks.

13. **HP limit**

* Introduce `maxHealth` (e.g., 100). Healing must not exceed the limit.

14. **Score**

* Define the score, e.g.:

    * `score = gold + hpBonus - timePenalty(moves/ticks)`
* The score must be displayable by the client (e.g., at the end or live).

---

## 14a. Exit from the Maze

* **Modify maze generation** so that there is a dedicated **Exit tile** which is the objective of the game.
* The player’s goal is to **reach the exit with the highest possible score**.
* Generation must guarantee that:

    * the **exit is reachable** (from every player’s start position),
    * the map is not randomly “blocked”.

### Fair start requirement: similar distance to the exit

* When placing players, ensure their start positions are at a **as similar distance as possible (ideally the same distance)** from the exit.
* Practical method: after generating the map, compute distances from the `Exit` (e.g., BFS over non-rock tiles), then:

    * pick starts from the **same distance layer**, or
    * minimize the difference (e.g., make `maxDist - minDist` as small as possible).

### Additional suggestion

* **Round end condition**

    * The game ends for a player when they step onto the `Exit` (they are “finished”), but the server may:

        * end the entire match when everyone finishes **or** after a tick limit.

---

## Level 4 — multiplayer and NPC (group)

15. **Multiple players configuration**

* The server reads `configuration.json` (known players).
* Requirement:

    * every group member has an entry (unique name + key),
    * validation (e.g., unique names/keys, reasonable key length).

16. **Dragon as an NPC**

* The code contains `Player.Dragon`.
* Requirement: add the dragon as a server-controlled NPC (move each tick).
* Collision/combat with the dragon must be consistent with your new combat rule.

---

## Level 5 — solution quality

17. **Readability and maintainability**

* meaningful names, small methods, no “magic numbers”
* a short description of the rules in `README` (what you changed and how the game works)

18. **Tests**

* Minimum tests for:

    * movement and map boundaries,
    * item generation (counts),
    * combat and loot splitting,
    * HP limit.

---

## Minimum “pass” requirement (practical)

* The server runs stably and supports multiple configured players.
* Each group member runs their client, authorizes with their own key, and participates live (ticks + state updates).
* The implemented changes in `Game` actually work (movement HP cost, new combat, item splitting, respawn, maxHP, score, dragon NPC, exit).
