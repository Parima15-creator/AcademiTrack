package com.mycompany.academicperformancetracker;

import java.io.*;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;

@WebServlet(name = "UpdateMarksServlet", urlPatterns = {"/UpdateMarksServlet"})
public class UpdateMarksServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
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

            // 1. INSERT OR UPDATE MARKS (Removed 'New Subjects' logic)
            JSONArray marks = data.getJSONArray("marks");
            for (int i = 0; i < marks.length(); i++) {
                JSONObject m = marks.getJSONObject(i);
                String roll = m.getString("roll");
                String sub = m.getString("sub");
                String rawType = m.getString("type").toLowerCase().replace(" ", "");
                String dbColumn = "";

                // Whitelist valid columns for ISA
                if (rawType.equals("isa1")) dbColumn = "isa1";
                else if (rawType.equals("isa2")) dbColumn = "isa2";
                else if (rawType.equals("isa3")) dbColumn = "isa3";
                else if (rawType.equals("assignment")) dbColumn = "assignment";

                if (!dbColumn.isEmpty()) {
                    // A. Update the specific ISA/Assignment column in isa_details
                    String sql = "INSERT INTO isa_details (roll_no, subject_code, semester, " + dbColumn + ") " +
                                 "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + dbColumn + " = VALUES(" + dbColumn + ")";
                    
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setString(1, roll);
                        pst.setString(2, sub);
                        pst.setString(3, semester);
                        pst.setDouble(4, m.getDouble("val"));
                        pst.executeUpdate();
                    }

                    // B. Recalculate Rounded Total and Sync to semester_marks table automatically
                    syncToSemesterDashboard(con, roll, sub, semester);
                }
            }
            
            response.getWriter().write("Success: ISA and Semester Marks Synced!");
            con.close();
            
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }

    private void syncToSemesterDashboard(Connection con, String roll, String sub, String sem) throws SQLException {
        String fetchSql = "SELECT isa1, isa2, isa3, assignment FROM isa_details WHERE roll_no = ? AND subject_code = ? AND semester = ?";
        
        try (PreparedStatement pstFetch = con.prepareStatement(fetchSql)) {
            pstFetch.setString(1, roll);
            pstFetch.setString(2, sub);
            pstFetch.setString(3, sem);
            ResultSet rs = pstFetch.executeQuery();

            if (rs.next()) {
                double m1 = Math.ceil(rs.getDouble("isa1"));
                double m2 = Math.ceil(rs.getDouble("isa2"));
                double m3 = Math.ceil(rs.getDouble("isa3"));
                double assignment = rs.getDouble("assignment");

                double min = Math.min(m1, Math.min(m2, m3));
                double bestTwoAvg = Math.ceil((m1 + m2 + m3 - min) / 2.0);
                double total = bestTwoAvg + assignment;

                // Sync the calculated total into the semester_marks table
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