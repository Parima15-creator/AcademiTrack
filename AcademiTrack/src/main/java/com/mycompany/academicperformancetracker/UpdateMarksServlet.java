package com.mycompany.academicperformancetracker;

import java.io.*;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;

/**
 * UpdateMarksServlet handles incoming JSON data to update student marks.
 * It features an "Upsert" (Update or Insert) mechanism and triggers
 * an automatic recalculation of final internal scores.
 */
@WebServlet(name = "UpdateMarksServlet", urlPatterns = {"/UpdateMarksServlet"})
public class UpdateMarksServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        
        // --- SECTION 1: READING THE JSON PAYLOAD ---
        // Since we are sending JSON via POST, we can't use request.getParameter().
        // We must read the raw request body using a BufferedReader.
        
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        try {
            // Convert the raw string into a JSON Object
            JSONObject data = new JSONObject(sb.toString());
            String semester = data.getString("semester");
            

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // --- SECTION 2: PROCESSING MARKS ARRAY ---
            // The frontend sends an array of mark objects (e.g., edited cells in a table)
            JSONArray marks = data.getJSONArray("marks");
            for (int i = 0; i < marks.length(); i++) {
                JSONObject m = marks.getJSONObject(i);
                String roll = m.getString("roll");
                String sub = m.getString("sub");
                
                // Normalize the input (e.g., "ISA 1" becomes "isa1")
                String rawType = m.getString("type").toLowerCase().replace(" ", "");
                String dbColumn = "";

                // Whitelist validation: Ensures we only update allowed columns
                if (rawType.equals("isa1")) dbColumn = "isa1";
                else if (rawType.equals("isa2")) dbColumn = "isa2";
                else if (rawType.equals("isa3")) dbColumn = "isa3";
                else if (rawType.equals("assignment")) dbColumn = "assignment";

                if (!dbColumn.isEmpty()) {
                    /*
                     * A. UPSERT LOGIC (INSERT OR UPDATE):
                     * 'ON DUPLICATE KEY UPDATE' is a MySQL feature.
                     * If the roll_no/subject/sem combo exists, it updates the specific column.
                     * If it doesn't exist, it creates a new row.
                     */
                    String sql = "INSERT INTO isa_details (roll_no, subject_code, semester, " + dbColumn + ") " +
                                 "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + dbColumn + " = VALUES(" + dbColumn + ")";
                    
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setString(1, roll);
                        pst.setString(2, sub);
                        pst.setString(3, semester);
                        pst.setDouble(4, m.getDouble("val")); // The new mark value
                        pst.executeUpdate();
                    }

                    /*
                     * B. AUTOMATIC SYNC:
                     * Every time a single mark is updated, we trigger a recalculation
                     * to keep the Semester Dashboard up to date.
                     */
                    syncToSemesterDashboard(con, roll, sub, semester);
                }
            }
            
            response.getWriter().write("Success: ISA and Semester Marks Synced!");
            con.close();
            
        } catch (Exception e) {
            // Send a 500 error code so the frontend AJAX 'error' callback triggers
            response.setStatus(500);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }
    
    /**
     * Calculates the internal total based on university rules:
     * 1. Round up (ceil) each ISA mark.
     * 2. Take the Best 2 out of 3 ISAs and average them.
     * 3. Add the assignment marks.
     * 4. Update the 'semester_marks' table.
     */
    private void syncToSemesterDashboard(Connection con, String roll, String sub, String sem) throws SQLException {
        
        // Fetch all current marks for this student and subject
        String fetchSql = "SELECT isa1, isa2, isa3, assignment FROM isa_details WHERE roll_no = ? AND subject_code = ? AND semester = ?";
        
        try (PreparedStatement pstFetch = con.prepareStatement(fetchSql)) {
            pstFetch.setString(1, roll);
            pstFetch.setString(2, sub);
            pstFetch.setString(3, sem);
            ResultSet rs = pstFetch.executeQuery();

            if (rs.next()) {
                // Rule: Ceiling individual ISA scores
                double m1 = Math.ceil(rs.getDouble("isa1"));
                double m2 = Math.ceil(rs.getDouble("isa2"));
                double m3 = Math.ceil(rs.getDouble("isa3"));
                double assignment = rs.getDouble("assignment");
                
                // Logic: Best 2 of 3 calculation
                // Sum all three, subtract the minimum, then divide by 2
                double min = Math.min(m1, Math.min(m2, m3));
                double bestTwoAvg = Math.ceil((m1 + m2 + m3 - min) / 2.0);
                
                // Final internal total (Total = Average of Best 2 ISAs + Assignment)
                double total = bestTwoAvg + assignment;

                // Update the semester_marks table (isa_total column)
                String syncSql = "INSERT INTO semester_marks (roll_no, subject_code, semester, isa_total) " +
                                 "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE isa_total = VALUES(isa_total)";
                
                try (PreparedStatement pstSync = con.prepareStatement(syncSql)) {
                    pstSync.setString(1, roll);
                    pstSync.setString(2, sub);
                    pstSync.setString(3, sem);
                    pstSync.setDouble(4, total);
                    pstSync.executeUpdate();
                }
            }
        }
    }
}