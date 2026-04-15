package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.BufferedReader;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet(name = "UpdateSemesterMarksServlet", urlPatterns = {"/UpdateSemesterMarksServlet"})
public class UpdateSemesterMarksServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        
        try {
            JSONObject data = new JSONObject(sb.toString());
            String semester = data.getString("semester");

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 1. INSERT OR UPDATE MARKS (Removed 'Register New Subjects' logic)
            JSONArray marks = data.getJSONArray("marks");
            for (int i = 0; i < marks.length(); i++) {
                JSONObject m = marks.getJSONObject(i);
                String roll = m.getString("roll"); 
                String sub = m.getString("sub");
                String type = m.getString("type");
                String val = m.getString("val");

                String dbColumn = "";
                if(type.equals("ISA")) dbColumn = "isa_total";
                else if(type.equals("SEA")) dbColumn = "sea_marks";
                else if(type.equals("Credits Earned")) dbColumn = "credits"; 
                else if(type.equals("Grade Point")) dbColumn = "grade_point";
                else if(type.equals("Grade")) dbColumn = "grade";

                if (!dbColumn.isEmpty()) {
                    // Update only existing semester results
                    String sql = "INSERT INTO semester_marks (roll_no, subject_code, semester, " + dbColumn + ") " +
                                 "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + dbColumn + " = VALUES(" + dbColumn + ")";
                    
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setString(1, roll);
                        pst.setString(2, sub);
                        pst.setString(3, semester);
                        pst.setString(4, val);
                        pst.executeUpdate();
                    }
                }
            }
            response.getWriter().print("Semester marks updated successfully!");
            con.close();
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().print("Database Error: " + e.getMessage());
        }
    }
}