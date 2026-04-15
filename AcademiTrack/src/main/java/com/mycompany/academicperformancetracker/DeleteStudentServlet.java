package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet(name = "DeleteStudentServlet", urlPatterns = {"/DeleteStudentServlet"})
public class DeleteStudentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String rollNo = request.getParameter("rollNo");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            String sql = "DELETE FROM students WHERE roll_no = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, rollNo);
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    out.print("Student " + rollNo + " deleted successfully.");
                } else {
                    out.print("Error: Student not found.");
                }
            }
            con.close();
        } catch (Exception e) {
            out.print("Database Error: " + e.getMessage());
        }
    }
}