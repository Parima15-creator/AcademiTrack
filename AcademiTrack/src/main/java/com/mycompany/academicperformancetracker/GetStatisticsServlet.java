package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * GetStatisticsServlet: Powers the AcademiTrack Analytics Dashboard.
 * Handles Dynamic Subject Lists, Pass/Fail Ratios, Division Comparisons, 
 * and Subject Toppers.
 */
@WebServlet(name = "GetStatisticsServlet", urlPatterns = {"/GetStatisticsServlet"})
public class GetStatisticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // Retrieve parameters from frontend fetch request
        String classId = request.getParameter("class");
        String sem = request.getParameter("sem");
        String subject = request.getParameter("subject");
        
        JSONObject result = new JSONObject();

        // Database Configuration
        String url = "jdbc:mysql://localhost:3306/academic_tracker";
        String dbUser = "root";
        String dbPass = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(url, dbUser, dbPass)) {

                // --- 1. DYNAMIC SUBJECT LIST ---
                // Populates the dropdown based on class and semester
                // --- 1. DYNAMIC SUBJECT LIST (With Names) ---
                JSONArray subjects = new JSONArray();
                // Join with subjects table to get names
                String subSql = "SELECT DISTINCT TRIM(m.subject_code) as code, TRIM(s.subject_name) as name " +
                                "FROM semester_marks m " +
                                "JOIN subjects s ON TRIM(m.subject_code) = TRIM(s.subject_code) " +
                                "WHERE TRIM(m.semester) = ? " +
                                "AND TRIM(m.roll_no) IN (SELECT TRIM(roll_no) FROM students WHERE TRIM(class_id) = ?)";

                try (PreparedStatement psSub = con.prepareStatement(subSql)) {
                    psSub.setString(1, sem != null ? sem.trim() : "");
                    psSub.setString(2, classId != null ? classId.trim() : "");
                    ResultSet rsSub = psSub.executeQuery();
                    while(rsSub.next()) {
                        JSONObject subObj = new JSONObject();
                        subObj.put("code", rsSub.getString("code"));
                        subObj.put("name", rsSub.getString("name"));
                        subjects.put(subObj);
                    }
                }
                result.put("subjects", subjects);

                // --- 2. PASS/FAIL & REATTEMPT COUNTS ---
                // Recovery: Students who passed (attempt > 1) after an initial fail
                // 2. PASS/FAIL & REATTEMPT COUNTS (Excluding Honours)
                // Logic: A student is 'Failed' if they have at least one 'F' in a non-honours subject.
                // A student is 'Passed' if they have zero 'F' grades in non-honours subjects.
                // Enhanced query to handle whitespace and database formatting
                String statsSql = "SELECT " +
                    "COUNT(DISTINCT CASE WHEN TRIM(m.grade) = 'F' THEN m.roll_no END) as failed_students, " +
                    "COUNT(DISTINCT m.roll_no) - COUNT(DISTINCT CASE WHEN TRIM(m.grade) = 'F' THEN m.roll_no END) as passed_students, " +
                    "COUNT(DISTINCT CASE WHEN m.attempt_no > 1 AND TRIM(m.grade) NOT IN ('F', '-', 'undefined') THEN m.roll_no END) as recovery " +
                    "FROM semester_marks m " +
                    "JOIN subjects sub ON TRIM(m.subject_code) = TRIM(sub.subject_code) " +
                    "WHERE TRIM(m.semester) = ? " +
                    "AND TRIM(m.roll_no) IN (SELECT TRIM(roll_no) FROM students WHERE TRIM(class_id) = ?) " +
                    "AND sub.is_honors = 0 AND TRIM(m.grade) NOT IN ('-', 'undefined')";
                try (PreparedStatement psStats = con.prepareStatement(statsSql)) {
                    psStats.setString(1, sem.trim());
                    psStats.setString(2, classId.trim());
                    ResultSet rsStats = psStats.executeQuery();
                    if(rsStats.next()){
                        result.put("passCount", rsStats.getInt("passed_students"));
                        result.put("failCount", rsStats.getInt("failed_students"));
                        result.put("reattemptSuccess", rsStats.getInt("recovery"));
                    }
                }
                // --- 3. CLASS COMPARISON (GPA Analysis) ---
                // Compares divisions (e.g., SE1 vs SE2) within the same academic year
                if (classId != null && classId.length() >= 2) {
                    String prefix = classId.substring(0, 2); 
                    String compSql = "SELECT TRIM(s.class_id) as cls, AVG(m.grade_point) as gpa " +
                                     "FROM students s JOIN semester_marks m ON TRIM(s.roll_no) = TRIM(m.roll_no) " +
                                     "WHERE TRIM(s.class_id) LIKE ? AND TRIM(m.semester) = ? " +
                                     "AND TRIM(m.grade) NOT IN ('-', 'undefined') GROUP BY TRIM(s.class_id)";
                    
                    try (PreparedStatement psComp = con.prepareStatement(compSql)) {
                        psComp.setString(1, prefix + "%");
                        psComp.setString(2, sem != null ? sem.trim() : "");
                        ResultSet rsComp = psComp.executeQuery();
                        JSONArray comparison = new JSONArray();
                        while(rsComp.next()){
                            JSONObject c = new JSONObject();
                            c.put("class", rsComp.getString("cls"));
                            c.put("avgGpa", Math.round(rsComp.getDouble("gpa") * 100.0) / 100.0);
                            comparison.put(c);
                        }
                        result.put("comparison", comparison);
                    }
                }

                // --- 4. SUBJECT TOPPERS (Top 5 by Total Marks) ---
                // Only executes if the teacher has selected a specific subject from the dropdown
                if(subject != null && !subject.trim().isEmpty()){
                    JSONArray toppers = new JSONArray();
                    String topperSql = "SELECT s.name, (m.isa_total + m.sea_marks) as total_marks " +
                                       "FROM students s JOIN semester_marks m ON TRIM(s.roll_no) = TRIM(m.roll_no) " +
                                       "WHERE TRIM(m.subject_code) = ? AND TRIM(s.class_id) = ? AND TRIM(m.semester) = ? " +
                                       "AND TRIM(m.grade) != '-' " +
                                       "ORDER BY total_marks DESC LIMIT 5";
                    
                    try (PreparedStatement psTop = con.prepareStatement(topperSql)) {
                        psTop.setString(1, subject.trim());
                        psTop.setString(2, classId != null ? classId.trim() : "");
                        psTop.setString(3, sem != null ? sem.trim() : "");
                        ResultSet rsTop = psTop.executeQuery();
                        while(rsTop.next()){
                            JSONObject t = new JSONObject();
                            t.put("name", rsTop.getString("name"));
                            t.put("score", rsTop.getDouble("total_marks"));
                            toppers.put(t);
                        }
                    }
                    result.put("subjectToppers", toppers);
                }

                // Output final JSON response
                out.print(result.toString());
                
            }
        } catch (Exception e) {
            // Handle database errors (Driver not found, SQL Syntax, etc.)
            response.setStatus(500);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}