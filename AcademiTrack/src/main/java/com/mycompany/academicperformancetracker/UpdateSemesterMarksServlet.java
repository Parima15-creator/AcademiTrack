package com.mycompany.academicperformancetracker;

import java.io.*;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;

/**
 * UpdateSemesterMarksServlet manages the final semester-end data.
 * It handles subject registration and bulk updates for SEA, Credits, and Grades.
 */
@WebServlet(name = "UpdateSemesterMarksServlet", urlPatterns = {"/UpdateSemesterMarksServlet"})
public class UpdateSemesterMarksServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1: READ RAW JSON STREAM
        // Reads the incoming JSON data from the request body.
        StringBuilder sb = new StringBuilder();
        String line;
        
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        try {
            JSONObject data = new JSONObject(sb.toString());
            String semester = data.getString("semester");
            
            // Database connection setup
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 2: REGISTER NEW SUBJECTS (Dependency Handling)
            /* 
             * 'semester_marks' has a Foreign Key relationship with 'subjects'.
             * If the frontend sends a subject that isn't in the DB yet, we must add it first.
             * 'INSERT IGNORE' prevents errors if the subject already exists.
             */
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

            // 3: BULK UPDATE MARKS
            JSONArray marks = data.getJSONArray("marks");
            for (int i = 0; i < marks.length(); i++) {
                JSONObject m = marks.getJSONObject(i);
                String typeFromUI = m.getString("type");
                String valStr = m.optString("val", "0").trim();
                String dbColumn = "";

                // Map the UI row labels to actual Database Column names
                if (typeFromUI.equalsIgnoreCase("ISA")) dbColumn = "isa_total";
                else if (typeFromUI.equalsIgnoreCase("SEA")) dbColumn = "sea_marks";
                else if (typeFromUI.equalsIgnoreCase("Credits Earned")) dbColumn = "credits";
                else if (typeFromUI.equalsIgnoreCase("Grade Point")) dbColumn = "grade_point";
                else if (typeFromUI.equalsIgnoreCase("Grade")) dbColumn = "grade";
                else if (typeFromUI.equalsIgnoreCase("Cleared In")) dbColumn = "cleared_in_sem";

                if (!dbColumn.isEmpty()) {
                    /*
                     * UPSERT (Insert or Update):
                     * Checks for existing roll_no + subject + semester combo.
                     * Updates ONLY the specific column (ISA, SEA, Grade, etc.) based on user edit.
                     */
                    String sql = "INSERT INTO semester_marks (roll_no, subject_code, semester, " + dbColumn + ") " +
                                 "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + dbColumn + " = VALUES(" + dbColumn + ")";
                    
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setString(1, m.getString("roll"));
                        pst.setString(2, m.getString("sub"));
                        pst.setString(3, semester);
                        
                        // 4: DATA TYPE SANITIZATION
                        if (dbColumn.equals("grade")) {
                            
                            // Text handling: Default to "-" if empty
                            pst.setString(4, valStr.isEmpty() ? "-" : valStr);
                        } else {
                            
                            // Numeric handling: Clean input strings (e.g., "Sem 3" -> "3")
                            // regex [^0-9.] removes everything except digits and decimal points
                            String cleanVal = valStr.replaceAll("[^0-9.]", ""); 
                            double numericVal = (cleanVal.isEmpty()) ? 0.0 : Double.parseDouble(cleanVal);
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