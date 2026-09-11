# Product model and economy

SkyRush is a short, virtual-bonus game about choosing when to stop a flight. It demonstrates a coherent loop, not a validated business model or a real-money product. No retention, fairness, or revenue claims have been measured.

## Choices and their consequences

| Element | Player purpose | What is actually implemented |
| --- | --- | --- |
| GREEN | A calm turquoise presentation and a shorter sequence of milestones | 9 configured levels |
| RED | A warmer presentation and more milestones to watch | 12 configured levels |
| Cashout | Secure the current payout rather than risk the stake for a larger multiplier | Server fixes payout after level 1, strictly before crash |
| Four stakes | Choose the amount of bonus balance exposed and the paired booster | Default 10/x1, 20/x2, 30/x3, 40/x4; four configurable options |
| Booster | An observable reason to wait for a specific milestone | Server chooses a visible level; effect and extra points apply only if reached before cashout |
| Continued flight | Compare the fixed decision with the eventual crash | Fixed payout remains unchanged; later levels still earn points |

RED is **not mathematically a higher crash-risk mode**. Both themes share the same crash distribution and growth rate. Threshold schedules, level counts, and available booster positions differ, so points and booster timing differ. Avoid claiming a theme changes crash odds. At the same base multiplier, the immediate cashout survival condition is identical in both themes.

Stake alone does not change the crash distribution. The default larger stakes are coupled to stronger boosters, so these are combined stake/booster choices, not four independently calibrated difficulty levels. If a player cashes out before the booster, only the larger stake changes the amount won or lost.

Waiting for a booster can multiply the payout and grant extra points, but exposes the stake until that level is reached. Cashout before the marker permanently misses its effect. This produces a visible tradeoff without asking the browser to predict a hidden crash. The linear/uniform model is intentionally simple; [game-math.md](game-math.md) quantifies its generous economy and limitations.

## Three distinct quantities

- **Bonus balance (Б):** spendable virtual units. Stakes debit it; cashout payouts and fragment conversions credit it. The initial demo wallet has 1000.00. There are no real purchases or withdrawals; optional simulated tickets spend virtual bonus balance.
- **Game points:** a record of levels, booster activation, and cashout in a round. They remain visible in results and history, even after a loss. They do not buy bets. They now determine the player position in the live round ranking and daily demo tournament; opponents are explicitly simulated.
- **Sky Fragments (✦):** persistent reward progress independent of points and wallet. Default wins grant 2 and losses grant 1; each 5 converts automatically to 5.00 bonus units at completion, preserving the remainder. This is an implemented benefit, not an inventory icon.

A win followed by a loss leaves 3 fragments. A further win reaches 5, gives a 5.00 balance bonus, and leaves zero. This provides a small reason to continue without removing the stake loss. Five lost BASIC bets cost 50.00 and return only 5.00 through fragments. Fragments do not guarantee that an exhausted wallet can play again: there is no free top-up endpoint. A fresh disposable demo database resets the demonstration wallet.

History closes the loop: players can inspect actual stake, fixed multiplier, crash, payout, points and fragments, then choose the next bet. It is a factual record, not a prediction of the next crash. Play Again returns to betting with the same theme; the ten-second idle timeout returns to the separate theme screen.

## Intentional limits

The economy is not balanced for long-term scarcity. Early cashouts and strong boosters can have positive expected net bonus returns; fragment conversion adds more units. This is acceptable as an explicitly documented demonstration model, but it is not evidence of a sustainable real-money economy. Before any economy expansion, measure bankroll changes over repeated play and decide a target experience. Do not change formulas immediately before submission merely to claim sophistication.

Optional features now include active discovery/recovery, rankings, daily demo tournament, simulated ticket offers, evaluator admin, and instant repeat. Competition adds a visible use for points; it does not award monetary prizes. Tickets demonstrate an optional bonus-spending flow and persist as a collection, but have no real drawing or redemption. Play Again still allows review; instant repeat uses current rules and normal server balance validation.
