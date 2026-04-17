package edu.omkar.services;

import edu.omkar.model.UserModel;
import edu.omkar.repository.UserRepository;
import edu.omkar.repository.UserRepositoryImpl;
import java.sql.SQLException;

public class UserService {
    private final UserRepository userRepository = new UserRepositoryImpl();

    public UserModel loginUser(String email, String password) throws SQLException {
        return userRepository.loginUser(email, password);
    }

    public boolean registerUser(UserModel user) throws SQLException {
        return userRepository.registerUser(user);
    }
}
