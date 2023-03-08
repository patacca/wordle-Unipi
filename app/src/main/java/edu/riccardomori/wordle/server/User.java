package edu.riccardomori.wordle.server;

public class User {
    private String username;
    private String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean passwordMatch(String password) {
        return this.password.equals(password);
    }
}
