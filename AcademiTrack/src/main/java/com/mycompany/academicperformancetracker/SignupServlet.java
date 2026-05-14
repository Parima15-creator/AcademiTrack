/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
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

@WebServlet(name = "SignupServlet", urlPatterns = {"/SignupServlet"})
public class SignupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // 1. Get data from your HTML form fields
        String name = request.getParameter("Username");
        String email = request.getParameter("College_Email_ID");
        String dept = request.getParameter("Department");
        String pass = request.getParameter("Password");

        try {
            // 2. Load the MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 3. Connect to XAMPP (Database: academic_tracker, User: root, Pass: empty)
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 4. Create the SQL Query to insert data into your 'faculty' table
            String query = "INSERT INTO faculty (Username, College_Email_ID, Department, Password) VALUES (?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(query);

            // Map all 4 parameters to the query placeholders
            pst.setString(1, name);  // Maps to Username
            pst.setString(2, email); // Maps to College_Email_ID
            pst.setString(3, dept);  // Maps to Department
            pst.setString(4, pass);  // Maps to Password

            // 5. Execute the update
            int rowCount = pst.executeUpdate();
            
            if (rowCount > 0) {
                // If successful, go back to the Sign In page
                response.sendRedirect("index.html");
            } else {
                response.getWriter().print("Registration failed. Please try again.");
            }
            
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("Error: " + e.getMessage());
        }
    }
}