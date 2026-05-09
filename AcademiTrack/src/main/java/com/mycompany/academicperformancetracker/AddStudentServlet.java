// Ad student green button from dashboard
package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

// This servlet is triggered when a request is sent to "/AddStudentServlet".
@WebServlet(name = "AddStudentServlet", urlPatterns = {"/AddStudentServlet"})
public class AddStudentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Sets the response type to plain text so the calling AJAX function can display the message
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // 1. DATA RETRIEVAL: Extracting parameters sent from the HTML form/AJAX request
        String rollNo = request.getParameter("rollNo");
        String name = request.getParameter("name");
        String classId = request.getParameter("classId");

        try {
            // 2. DATABASE DRIVER: Loading the MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 3. CONNECTION: Establishing a link to the 'academic_tracker' database
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");
            
            // 4. SQL QUERY: Using '?' placeholders to prevent SQL Injection attacks
            String sql = "INSERT INTO students (roll_no, name, class_id) VALUES (?, ?, ?)";
            
            // 5. PREPARED STATEMENT: Safely mapping the variables to the SQL query
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, rollNo); // Maps to the first '?'
                pst.setString(2, name); // Maps to the second '?'
                pst.setString(3, classId); // Maps to the third '?'
                
                // 6. EXECUTION: Runs the INSERT command in the database
                pst.executeUpdate();
                
                // 7. FEEDBACK: Sending a success message back to the frontend
                out.print("Student " + name + " added successfully!");
            }
            // 8. CLEANUP: Closing the connection to free up database resources
            con.close();
        } catch (Exception e) {
            // Error handling: Catching driver issues or database constraints (e.g., duplicate Roll No)
            out.print("Error: " + e.getMessage());
        }
    }
}