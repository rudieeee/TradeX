# TradeX
A virtual trading platform where users trade stocks using virtual money (₹1,00,000 starting balance).

**Stack:** Java 21+, JavaFX, MySQL (JDBC), Maven.

## Setup
```bash
brew install maven mysql && brew services start mysql
mysql -uroot -e "CREATE DATABASE tradex; CREATE USER 'tradex'@'localhost' IDENTIFIED BY 'tradex'; GRANT ALL ON tradex.* TO 'tradex'@'localhost';"
mysql -uroot tradex < src/main/resources/db/schema.sql
mvn javafx:run      # run the app
mvn test                  # unit tests
mvn test -Dtest='*IT'     # DB integration tests (need MySQL)
```
Override DB settings with `TRADEX_DB_URL`, `TRADEX_DB_USER`, `TRADEX_DB_PASSWORD`.

## Progress
- [x] Phase 1: project setup, MySQL schema, login/registration (PBKDF2 password hashing)
- [x] Phase 2: wallet, buy/sell engine (atomic, row-locked), transaction history
- [x] Phase 3: dashboard, market + price charts + trading, portfolio, watchlist, transactions, CSS theme
- [x] Phase 4: Yahoo Finance prices (simulated fallback), background updates every 60s, live-refreshing views
- [ ] Phase 5: limit/stop-loss orders, leaderboard, analytics

## Market data
Prices come from Yahoo Finance's unofficial chart endpoint (no API key) and refresh every 60 seconds in a background thread.
If a symbol can't be fetched (network down, renamed ticker) its price follows a small simulated random walk instead;
the header shows whether prices are live or simulated. The endpoint is unofficial and may change or rate-limit.
To use another source, implement `MarketDataProvider`.
