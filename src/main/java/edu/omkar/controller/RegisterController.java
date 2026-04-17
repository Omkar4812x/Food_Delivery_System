package edu.omkar.controller;

import edu.omkar.model.UserModel;
import edu.omkar.services.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/RegisterController")
@MultipartConfig
public class RegisterController extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        UserModel user = new UserModel();
        user.setName(clean(request.getParameter("name")));
        user.setEmail(clean(request.getParameter("email")));
        user.setPhone(clean(request.getParameter("phone")));
        user.setAddress(clean(request.getParameter("address")));
        user.setPassword(clean(request.getParameter("password")));

        try {
            boolean created = userService.registerUser(user);
            if (created) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":true}");
            } else {
                response.sendError(HttpServletResponse.SC_CONFLICT, "Email already exists");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
