# TradeX – Virtual Stock Trading Platform

TradeX is a desktop stock market simulator. Users trade NSE stocks with virtual money (₹1,00,000 to start),
manage a portfolio, watch live prices and compare their returns on a leaderboard, without risking real money.

**Stack:** Java 21+, JavaFX + CSS, MySQL (JDBC), Maven, Jackson (JSON), JUnit 5.

## Features
| Module | What it does |
|---|---|
| Authentication | Register / log in with username or email; PBKDF2-SHA256 salted password hashes |
| Wallet | ₹1,00,000 starting balance, updated atomically on every trade |
| Market | Search stocks, live price + day change, 60-day price chart |
| Trading | Market, **limit** and **stop-loss** orders; buy and sell |
| Portfolio | Holdings, average price, P&L and return %, allocation pie chart |
| Watchlist | Add / remove favourite stocks |
| Orders | Pending, executed, cancelled and rejected conditional orders; cancel pending ones |
| Transactions | Full trade history, **CSV export** |
| Analytics | Total / realised / unrealised P&L, win rate, portfolio value over time, P&L by holding |
| Leaderboard | Users ranked by total portfolio value and return |
| Admin panel | List users, reset or delete accounts, monitor recent trades (admins only) |

## Setup
```bash
brew install maven mysql && brew services start mysql
mysql -uroot -e "CREATE DATABASE tradex; CREATE USER 'tradex'@'localhost' IDENTIFIED BY 'tradex'; GRANT ALL ON tradex.* TO 'tradex'@'localhost';"
mysql -uroot tradex < src/main/resources/db/schema.sql     # tables + 15 seeded NSE stocks
mvn javafx:run                                              # run the app
```
Override DB settings with `TRADEX_DB_URL`, `TRADEX_DB_USER`, `TRADEX_DB_PASSWORD` (the default password `tradex`
is for local development only).

### Making an admin
There is deliberately no way to become admin from the UI. Promote a registered user in SQL, then log in again:
```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your_username';
```
Admins appear in the sidebar with an **Admin** tab, are excluded from the leaderboard, and are checked again in
`AdminService` on every call (hiding the tab is not the security boundary).

## How trading works
- **Market orders** execute immediately at the stock's current price inside one database transaction:
  wallet debit/credit, holding update, order and transaction records commit together or not at all.
- **Limit orders:** a buy fills when the price is at or below the limit; a sell when at or above it.
- **Stop-loss orders** (sell only) fill when the price falls to the trigger.
- Pending orders are checked after every price update (and right after placement). They fill at the *market* price
  at trigger time, which can be better than the limit.
- Pending orders **do not reserve** cash or shares. If you no longer have the money or shares when the order
  triggers, it is marked `REJECTED`.
- Average purchase price is a weighted average; realised P&L uses average-cost accounting.

## Market data
Prices come from Yahoo Finance's unofficial chart endpoint (no API key) and refresh every 60 seconds on a
background thread. If a symbol can't be fetched (network down, renamed ticker) that symbol follows a small simulated
random walk instead; the header shows whether prices are live or simulated. The endpoint is unofficial and may change
or rate-limit. To use another source, implement `MarketDataProvider`.

The 60-day chart history is **generated** (a random walk ending at the current price) the first time the app runs;
real daily points accumulate from then on, one per day.

## Architecture
```
 JavaFX views (FXML + CSS)
        │  controllers/        thin: read input, call services off the UI thread (Async), update the view
        ▼
   services/                   business rules: TradingService, PortfolioService, AnalyticsService,
        │                      LeaderboardService, AdminService, MarketService, PriceUpdater, ...
        ▼
   repositories/               JDBC + SQL only (one class per aggregate)
        ▼
   database/Database           connections ──► MySQL
                                   ▲
   PriceUpdater (daemon thread, every 60s) ── MarketDataProvider (Yahoo) / SimulatedPriceModel
        └─ hooks: process pending orders, record portfolio snapshots
```

```
src/main/java/com/tradex/
  Main.java            app start, background updater
  models/              User, Order (abstract) → BuyOrder / SellOrder, OrderRecord, Holding, Transaction, Quote, ...
  controllers/         one per screen
  services/            business logic
  repositories/        database access
  database/            connection factory
  exceptions/          TradeXException hierarchy
  utils/               PasswordHasher, Session, Async, Fmt, Tables, ReportExporter, PriceFeed
src/main/resources/    fxml/ (screens), css/style.css, db/schema.sql
```

## Java concepts in the code
| Concept | Where |
|---|---|
| Encapsulation | `User` (immutable, no password hash exposed); wallet changes only via `WalletRepository` |
| Inheritance | `Order` → `BuyOrder`, `SellOrder`; `TradeXException` hierarchy |
| Polymorphism | `order.execute(...)` does the right thing for buys and sells |
| Abstraction | `TradingService`, `MarketDataProvider` interfaces; `Order` abstract class |
| Collections | holdings lists, watchlist `Set`, `Map` of prices, `ConcurrentHashMap` of locks |
| Multithreading | `PriceUpdater` scheduler; `Async` runs DB work off the JavaFX thread |
| Synchronization | per-user lock in `TradingServiceImpl` plus `SELECT ... FOR UPDATE` row locks |
| Exception handling | `InsufficientBalanceException`, `InvalidOrderException`, `MarketDataException`, ... |
| JDBC | all of `repositories/` (prepared statements, transactions, batches) |
| JavaFX | FXML screens, charts, tables, CSS |
| Networking | `YahooFinanceProvider` (`java.net.http.HttpClient`) |
| File handling | `ReportExporter` (CSV, with spreadsheet-formula neutralisation) |

## Database
Tables: `users`, `wallets`, `stocks`, `holdings`, `orders`, `transactions`, `watchlists`, `price_history`, plus
`portfolio_snapshots` (portfolio value per user per 15 minutes, for the analytics chart).

## Testing
```bash
mvn test                              # fast unit tests (hashing, validation, triggers, P&L maths, CSV, parsing)
mvn test -Dtest='*IT,*Test'           # everything, including DB integration tests (needs MySQL)
```
Integration tests (`*IT`) use the local `tradex` database, create their own users and clean up after themselves.
They include a concurrency test (20 threads buying against one wallet) and `FxmlLoadIT`, which loads every screen on
the JavaFX thread and needs a graphical session.

## Known limitations
- Prices are delayed/unofficial; the simulated fallback is not real market data.
- Pending orders don't reserve funds or shares (see above).
- One currency (₹) and NSE stocks only; the stock list is seeded, not searched from an exchange.
- Single-machine app: the per-user lock is in-process, so multiple app instances rely on the database row locks.
