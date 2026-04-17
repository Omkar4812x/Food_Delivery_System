package edu.omkar.repository;

import edu.omkar.dbconfig.DBConfig;
import edu.omkar.model.UserModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepositoryImpl implements UserRepository {
    @Override
    public UserModel loginUser(String email, String password) throws SQLException {
        String sql = "SELECT id, name, email, phone, address, password, role FROM users WHERE LOWER(email) = LOWER(?) AND password = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            ps.setString(2, password == null ? "" : password.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserModel user = new UserModel();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setAddress(rs.getString("address"));
                    user.setPassword(rs.getString("password"));
                    user.setRole(rs.getString("role"));
                    return user;
                }
            }
        }
        return null;
    }

    @Override
    public boolean registerUser(UserModel user) throws SQLException {
        String checkSql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setString(1, user.getEmail());
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return false;
                }
            }

            String sql = "INSERT INTO users (name, email, phone, address, password, role) VALUES (?, ?, ?, ?, ?, 'customer')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPhone());
                ps.setString(4, user.getAddress());
                ps.setString(5, user.getPassword());
                return ps.executeUpdate() > 0;
            }
        }
    }
}
