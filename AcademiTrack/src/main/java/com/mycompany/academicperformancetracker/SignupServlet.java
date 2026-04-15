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
        String name = request.getParameter("fullName");
        String dept = request.getParameter("department");
        String pass = request.getParameter("password");

        try {
            // 2. Load the MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 3. Connect to XAMPP (Database: academic_tracker, User: root, Pass: empty)
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 4. Create the SQL Query to insert data into your 'faculty' table
            String query = "INSERT INTO faculty (full_name, department, password) VALUES (?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, name);
            pst.setString(2, dept);
            pst.setString(3, pass);

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