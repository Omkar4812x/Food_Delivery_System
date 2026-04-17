package edu.omkar.repository;

import edu.omkar.model.UserModel;
import java.sql.SQLException;

public interface UserRepository {
    UserModel loginUser(String email, String password) throws SQLException;
    boolean registerUser(UserModel user) throws SQLException;
}
