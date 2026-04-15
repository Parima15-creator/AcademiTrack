package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet(name = "AddStudentServlet", urlPatterns = {"/AddStudentServlet"})
public class AddStudentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String rollNo = request.getParameter("rollNo");
        String name = request.getParameter("name");
        String classId = request.getParameter("classId");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            String sql = "INSERT INTO students (roll_no, name, class_id) VALUES (?, ?, ?)";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, rollNo);
                pst.setString(2, name);
                pst.setString(3, classId);
                pst.executeUpdate();
                out.print("Student " + name + " added successfully!");
            }
            con.close();
        } catch (Exception e) {
            out.print("Error: " + e.getMessage());
        }
    }
}