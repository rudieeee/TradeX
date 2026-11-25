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
- [ ] Phase 4: market API, background price updates
- [ ] Phase 5: limit/stop-loss orders, leaderboard, analytics
