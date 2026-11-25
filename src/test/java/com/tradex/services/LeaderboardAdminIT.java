package com.tradex.services;

import com.tradex.TestSupport;
import com.tradex.exceptions.*;
import com.tradex.models.*;
import com.tradex.repositories.SnapshotRepository;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardAdminIT {
    private final TradingService trading = new TradingServiceImpl();
    private final AdminService admin = new AdminService();
    private User a, b, boss;

    @BeforeAll static void requireDb() { Assumptions.assumeTrue(TestSupport.dbAvailable(), "MySQL not available"); }

    @BeforeEach void setup() throws Exception {
        TestSupport.setPrice("ITC", "400.00");
        a = TestSupport.newUser("lb_a");
        b = TestSupport.newUser("lb_b");
        boss = TestSupport.newUser("lb_admin");
        TestSupport.promoteToAdmin(boss);
        boss = new User(boss.getId(), boss.getUsername(), boss.getEmail(), Role.ADMIN);
    }

    @AfterEach void cleanup() throws Exception {
        for (User u : new User[]{a, b, boss}) if (u != null) TestSupport.deleteUser(u);
    }

    private LeaderboardEntry entry(User u) throws Exception {
        return new LeaderboardService().getLeaderboard().stream().filter(e -> e.userId() == u.getId()).findFirst().orElseThrow();
    }

    @Test void ranksByTotalValueAndExcludesAdmins() throws Exception {
        trading.buy(a.getId(), "ITC", 100);         // a holds 100 ITC
        TestSupport.setPrice("ITC", "500.00");      // a gains 10,000; b is flat
        assertEquals(0, new BigDecimal("10.00").compareTo(entry(a).returnPct()));
        assertEquals(0, BigDecimal.ZERO.compareTo(entry(b).returnPct()));
        assertTrue(entry(a).rank() < entry(b).rank());
        assertTrue(new LeaderboardService().getLeaderboard().stream().noneMatch(e -> e.userId() == boss.getId()));
        TestSupport.setPrice("ITC", "400.00");
    }

    @Test void snapshotsAreRecordedAndIdempotentPerBucket() throws Exception {
        var repo = new SnapshotRepository();
        // recordAll writes a row for every user, so use a far-past bucket and remove it afterwards.
        LocalDateTime bucket = LocalDateTime.of(2000, 1, 1, 0, 0);
        try {
            repo.recordAll(bucket);
            repo.recordAll(bucket);
            var pts = repo.history(a.getId(), 10);
            assertEquals(1, pts.size());
            assertEquals(0, new BigDecimal("100000").compareTo(pts.get(0).price()));
        } finally {
            try (var c = com.tradex.database.Database.getConnection();
                 var ps = c.prepareStatement("DELETE FROM portfolio_snapshots WHERE bucket = ?")) {
                ps.setTimestamp(1, java.sql.Timestamp.valueOf(bucket));
                ps.executeUpdate();
            }
        }
    }

    @Test void nonAdminsAreRefused() {
        assertThrows(AuthenticationException.class, () -> admin.listUsers(a));
        assertThrows(AuthenticationException.class, () -> admin.deleteUser(a, b.getId()));
        assertThrows(AuthenticationException.class, () -> admin.resetAccount(a, b.getId()));
        assertThrows(AuthenticationException.class, () -> admin.recentActivity(null, 5));
    }

    @Test void adminListsUsersAndSeesActivity() throws Exception {
        trading.buy(a.getId(), "ITC", 1);
        var row = admin.listUsers(boss).stream().filter(r -> r.id() == a.getId()).findFirst().orElseThrow();
        assertEquals(1, row.trades());
        assertTrue(admin.recentActivity(boss, 50).stream().anyMatch(r -> r[1].equals(a.getUsername())));
    }

    @Test void adminCanResetAndDeleteUserWithTradeHistory() throws Exception {
        trading.buy(a.getId(), "ITC", 10);
        trading.placeConditionalOrder(a.getId(), "ITC", Side.BUY, OrderType.LIMIT, 1, new BigDecimal("100"));
        admin.resetAccount(boss, a.getId());
        assertTrue(trading.getHoldings(a.getId()).isEmpty());
        assertTrue(trading.getTransactions(a.getId()).isEmpty());
        assertTrue(trading.getOrders(a.getId()).isEmpty());
        assertEquals(0, new BigDecimal("100000").compareTo(trading.getBalance(a.getId())));

        trading.buy(b.getId(), "ITC", 1);
        admin.deleteUser(boss, b.getId());
        assertTrue(admin.listUsers(boss).stream().noneMatch(r -> r.id() == b.getId()));
    }

    @Test void adminCannotDeleteSelfOrOtherAdmins() {
        assertThrows(ValidationException.class, () -> admin.deleteUser(boss, boss.getId()));
        assertThrows(ValidationException.class, () -> admin.resetAccount(boss, boss.getId()));
    }
}
