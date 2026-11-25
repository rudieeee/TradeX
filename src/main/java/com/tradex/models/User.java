package com.tradex.models;

/** Immutable user account. The password hash is never exposed outside the repository/service layer. */
public final class User {
    private final long id;
    private final String username;
    private final String email;
    private final Role role;

    public User(long id, String username, String email, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public boolean isAdmin() { return role == Role.ADMIN; }
}
