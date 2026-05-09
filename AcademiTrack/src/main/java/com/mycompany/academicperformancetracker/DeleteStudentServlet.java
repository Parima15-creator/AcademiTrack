package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

// This is called from a "Delete" button in the dashboard.
@WebServlet(name = "DeleteStudentServlet", urlPatterns = {"/DeleteStudentServlet"})
public class DeleteStudentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // 1. DATA RETRIEVAL: Getting the unique identifier for the student
        String rollNo = request.getParameter("rollNo");

        try {
            // 2. DATABASE SETUP: Standard JDBC boilerplate to connect
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");
            
            // 3. SQL QUERY: Deleting a specific row where the roll_no matches
            String sql = "DELETE FROM students WHERE roll_no = ?";
            
            
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, rollNo);
                
                // 4. EXECUTION & LOGIC: 
                // executeUpdate() returns an 'int' representing how many rows were affected.
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    // Success: At least one row was removed
                    out.print("Student " + rollNo + " deleted successfully.");
                } else {
                    // Failure: No row matched that Roll Number
                    out.print("Error: Student not found.");
                }
            }
            con.close();
        } catch (Exception e) {
            // Database Error handling (e.g., Foreign Key constraints if student has marks assigned)
            out.print("Database Error: " + e.getMessage());
        }
    }
}