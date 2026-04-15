package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet(name = "DeleteSubjectServlet", urlPatterns = {"/DeleteSubjectServlet"})
public class DeleteSubjectServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/plain;charset=UTF-8");
        String subCode = request.getParameter("code");
        PrintWriter out = response.getWriter();

        if (subCode == null || subCode.trim().isEmpty()) {
            out.print("Error: Subject code is missing.");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // Because of 'ON DELETE CASCADE', deleting the subject automatically deletes the marks!
            String sql = "DELETE FROM subjects WHERE subject_code = ?";
            
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, subCode);
                int rowsAffected = pst.executeUpdate();

                if (rowsAffected > 0) {
                    out.print("Subject " + subCode + " deleted successfully.");
                } else {
                    out.print("Error: Subject not found in database.");
                }
            }
            con.close();
        } catch (Exception e) {
            out.print("Database Error: " + e.getMessage());
        }
    }
}