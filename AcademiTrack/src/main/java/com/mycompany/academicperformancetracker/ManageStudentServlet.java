package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "ManageStudentServlet", urlPatterns = {"/ManageStudentServlet"})
public class ManageStudentServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // 1. Get Action type (add or delete)
        String action = request.getParameter("action");
        String roll = request.getParameter("roll");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            if ("add".equals(action)) {
                // 2. Handle ADD logic
                String name = request.getParameter("name");
                String classId = request.getParameter("class"); // Comes from data-code (SE1, etc.)

                String sql = "INSERT INTO students (roll_no, name, class_id) VALUES (?, ?, ?)";
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, roll);
                pst.setString(2, name);
                pst.setString(3, classId);
                
                int row = pst.executeUpdate();
                if (row > 0) {
                    out.print("Student " + name + " added successfully!");
                } else {
                    out.print("Failed to add student.");
                }

            } else if ("delete".equals(action)) {
                // 3. Handle DELETE logic
                // Note: If you have Foreign Keys, delete marks from isa_details first!
                String deleteMarksSql = "DELETE FROM isa_details WHERE roll_no = ?";
                PreparedStatement pstMarks = con.prepareStatement(deleteMarksSql);
                pstMarks.setString(1, roll);
                pstMarks.executeUpdate();

                String sql = "DELETE FROM students WHERE roll_no = ?";
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, roll);
                
                int row = pst.executeUpdate();
                if (row > 0) {
                    out.print("Student with Roll No " + roll + " removed.");
                } else {
                    out.print("Error: Student not found.");
                }
            }

            con.close();
        } catch (Exception e) {
            out.print("Error: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}