package edu.omkar.controller;

import com.google.gson.Gson;
import edu.omkar.model.UserModel;
import edu.omkar.services.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/LoginController")
@MultipartConfig
public class LoginController extends HttpServlet {
    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = clean(request.getParameter("email"));
        String password = clean(request.getParameter("password"));
        boolean ajax = isAjax(request);

        try {
            UserModel user = userService.loginUser(email, password);
            if (user != null) {
                HttpSession session = request.getSession();
                user.setPassword(null);
                session.setAttribute("user", user);
                if (ajax) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(gson.toJson(user));
                } else if ("admin".equalsIgnoreCase(user.getRole())) {
                    response.sendRedirect("admindashboard.html");
                } else {
                    response.sendRedirect("userdashboard.html");
                }
            } else if (ajax) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("Invalid email or password");
            } else {
                response.sendRedirect("login.html?error=1");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (ajax) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                response.sendRedirect("login.html?error=1");
            }
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.toLowerCase().contains("application/json"));
    }
}
