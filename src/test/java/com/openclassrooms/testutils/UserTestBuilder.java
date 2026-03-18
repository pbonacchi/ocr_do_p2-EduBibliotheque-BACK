package com.openclassrooms.testutils;

import com.openclassrooms.etudiant.entities.User;

public class UserTestBuilder {

    private Long id;
    private String firstName = "John";
    private String lastName = "Doe";
    private String login = "jdoe";
    private String password = "password";

    private UserTestBuilder() {
    }

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserTestBuilder withLogin(String login) {
        this.login = login;
        return this;
    }

    public UserTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserTestBuilder withNoPassword() {
        this.password = null;
        return this;
    }

    public User build() {
        User user = new User();
        if (id != null) {
            user.setId(id);
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setPassword(password);
        return user;
    }
    
}
