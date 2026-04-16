package com.mycompany.academicperformancetracker;

import java.io.*;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;

@WebServlet(name = "UpdateSemesterMarksServlet", urlPatterns = {"/UpdateSemesterMarksServlet"})
public class UpdateSemesterMarksServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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

            // 1. Register Subjects first to avoid Foreign Key errors
            if (data.has("newSubjects")) {
                JSONArray newSubs = data.getJSONArray("newSubjects");
                String sqlSub = "INSERT IGNORE INTO subjects (subject_code, subject_name, semester) VALUES (?, ?, ?)";
                try (PreparedStatement pstSub = con.prepareStatement(sqlSub)) {
                    for (int j = 0; j < newSubs.length(); j++) {
                        JSONObject ns = newSubs.getJSONObject(j);
                        pstSub.setString(1, ns.getString("code"));
                        pstSub.setString(2, ns.getString("name"));
                        pstSub.setString(3, semester);
                        pstSub.executeUpdate();
                    }
                }
            }

            // 2. Insert or Update Marks
            JSONArray marks = data.getJSONArray("marks");
            for (int i = 0; i < marks.length(); i++) {
                JSONObject m = marks.getJSONObject(i);
                String typeFromUI = m.getString("type");
                String valStr = m.optString("val", "0").trim();
                String dbColumn = "";

                // Exact match with rowTypes in JSP
                if (typeFromUI.equalsIgnoreCase("ISA")) dbColumn = "isa_total";
                else if (typeFromUI.equalsIgnoreCase("SEA")) dbColumn = "sea_marks";
                else if (typeFromUI.equalsIgnoreCase("Credits Earned")) dbColumn = "credits";
                else if (typeFromUI.equalsIgnoreCase("Grade Point")) dbColumn = "grade_point";
                else if (typeFromUI.equalsIgnoreCase("Grade")) dbColumn = "grade";

                if (!dbColumn.isEmpty()) {
                    String sql = "INSERT INTO semester_marks (roll_no, subject_code, semester, " + dbColumn + ") " +
                                 "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + dbColumn + " = VALUES(" + dbColumn + ")";
                    
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setString(1, m.getString("roll"));
                        pst.setString(2, m.getString("sub"));
                        pst.setString(3, semester);
                        
                        if (dbColumn.equals("grade")) {
                            pst.setString(4, valStr.isEmpty() ? "-" : valStr);
                        } else {
                            // Convert to double to avoid SQL string-to-number crashes
                            double numericVal = (valStr.isEmpty() || valStr.equals("-")) ? 0.0 : Double.parseDouble(valStr);
                            pst.setDouble(4, numericVal);
                        }
                        pst.executeUpdate();
                    }
                }
            }
            response.getWriter().write("Successfully updated database!");
            con.close();
        } catch (Exception e) {
            e.printStackTrace(); // This prints the REAL error in your NetBeans/IDE console
            response.setStatus(500);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }
}