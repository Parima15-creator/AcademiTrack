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
        
        // Set response type to plain text so the frontend (likely AJAX) can easily read the message
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // 1. EXTRACT PARAMETERS
        // 'action' determines if we are adding or deleting; 'roll' is the unique identifier for the student
        String action = request.getParameter("action");
        String roll = request.getParameter("roll");

        try {
            // 2. DATABASE CONNECTION
            // Load the MySQL JDBC Driver (Connector/J)
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");
            
            // 3. LOGIC BRANCHING: ADD STUDENT
            if ("add".equals(action)) {
                // Collect additional data sent from the form/AJAX
                String name = request.getParameter("name");
                String classId = request.getParameter("class"); // Comes from data-code (SE1, etc.)
                
                // SQL query using placeholders (?) to prevent SQL Injection attacks
                String sql = "INSERT INTO students (roll_no, name, class_id) VALUES (?, ?, ?)";
                PreparedStatement pst = con.prepareStatement(sql);
                // Bind the parameters to the query
                pst.setString(1, roll);
                pst.setString(2, name);
                pst.setString(3, classId);
                
                // executeUpdate() returns the number of rows affected (should be 1 for success)
                int row = pst.executeUpdate();
                if (row > 0) {
                    out.print("Student " + name + " added successfully!");
                } else {
                    out.print("Failed to add student.");
                }
                
                // 4. LOGIC BRANCHING: DELETE STUDENT
            } else if ("delete".equals(action)) {
                // 3. Handle DELETE logic
                /* 
                 * Because 'roll_no' is likely a Foreign Key in the 'isa_details' (marks) table, 
                 * we must delete the student's marks first to avoid a "Foreign Key Constraint" error.
                 */
                String deleteMarksSql = "DELETE FROM isa_details WHERE roll_no = ?";
                PreparedStatement pstMarks = con.prepareStatement(deleteMarksSql);
                pstMarks.setString(1, roll);
                pstMarks.executeUpdate();
                
                // Now that related records are gone, we can safely delete the student from the main table
                String sql = "DELETE FROM students WHERE roll_no = ?";
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, roll);
                
                int row = pst.executeUpdate();
                if (row > 0) {
                    out.print("Student with Roll No " + roll + " removed.");
                } else {
                    // This triggers if the query ran but no student matched that Roll No
                    out.print("Error: Student not found.");
                }
            }
            
            // 5. CLEAN UP
            con.close();
        } catch (Exception e) {
            // Error handling: Sends the exception message back to the client
            out.print("Error: " + e.getMessage());
        }
    }
    
    /**
     * Standard Override: Forwards HTTP GET requests to the processRequest method.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /**
     * Standard Override: Forwards HTTP POST requests to the processRequest method.
     * POST is generally preferred for "add" or "delete" actions for better security and data handling.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}