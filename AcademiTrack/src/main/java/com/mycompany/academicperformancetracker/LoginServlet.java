/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

// When a form submits to this URL, this class handles the request.
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 1. Get data from the HTML form (name="facultyName" in index.html)
        String inputName = request.getParameter("Username");
        String inputemail = request.getParameter("College_Email_ID");     
        String inputdept = request.getParameter("Department");
        String inputPass = request.getParameter("Password");
        
        // 2. Initialize the session once
        HttpSession session = request.getSession();

        try {
            // 3. Connect to MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/academic_tracker", "root", "");
            
            String query = "SELECT * FROM faculty WHERE Username=? AND College_Email_ID=? AND Department=? AND Password=?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, inputName);
            pst.setString(2, inputemail);
            pst.setString(3, inputdept);
            pst.setString(4, inputPass);
            
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                // SUCCESS: Save the name into the session
                session.setAttribute("Username", inputName); 
                response.sendRedirect("dashboard.jsp");
            } else {
                // FAILURE: Wrong credentials, send back to login with error
                response.sendRedirect("index.html?error=1");
            }
            
            con.close();
            
        } 
        catch (Exception e) {
    e.printStackTrace();
    // Redirect to login with a specific db error flag
    response.sendRedirect("index.html?error=db"); 
}
    }
}