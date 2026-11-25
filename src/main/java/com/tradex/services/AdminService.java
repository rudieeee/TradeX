package com.tradex.services;

import com.tradex.exceptions.*;
import com.tradex.models.*;
import com.tradex.repositories.AdminRepository;

import java.util.List;

/** Admin-only operations. Every call re-checks the actor's role, regardless of what the UI shows. */
public class AdminService {
    private final AdminRepository repo = new AdminRepository();

    public List<AdminUserRow> listUsers(User actor) throws TradeXException { requireAdmin(actor); return repo.listUsers(); }
    public List<String[]> recentActivity(User actor, int limit) throws TradeXException { requireAdmin(actor); return repo.recentActivity(limit); }

    public void deleteUser(User actor, long userId) throws TradeXException {
        requireAdmin(actor);
        if (actor.getId() == userId) throw new ValidationException("You cannot delete your own account");
        requireNotAdmin(userId, "Admin accounts cannot be deleted here");
        repo.deleteUser(userId);
    }

    public void resetAccount(User actor, long userId) throws TradeXException {
        requireAdmin(actor);
        requireNotAdmin(userId, "Admin accounts have no trading data to reset");
        repo.resetAccount(userId);
    }

    private void requireAdmin(User actor) throws AuthenticationException {
        if (actor == null || !actor.isAdmin()) throw new AuthenticationException("Admin access required");
    }

    private void requireNotAdmin(long userId, String msg) throws TradeXException {
        boolean isAdmin = repo.listUsers().stream().anyMatch(u -> u.id() == userId && u.role() == Role.ADMIN);
        if (isAdmin) throw new ValidationException(msg);
    }
}
