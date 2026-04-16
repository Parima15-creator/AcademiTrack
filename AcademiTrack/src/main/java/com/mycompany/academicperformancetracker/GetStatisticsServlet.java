package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet(name = "GetStatisticsServlet", urlPatterns = {"/GetStatisticsServlet"})
public class GetStatisticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String classId = request.getParameter("class");
        String sem = request.getParameter("sem");
        
        // Default to Semester 1 if no semester is provided
        if (sem == null || sem.isEmpty()) {
            sem = "1"; 
        }
        
        JSONObject result = new JSONObject();

        // Database credentials
        String url = "jdbc:mysql://localhost:3306/academic_tracker";
        String user = "root";
        String password = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Using try-with-resources for automatic closing of connection and statements
            try (Connection con = DriverManager.getConnection(url, user, password)) {
                
                // 1. TOP 3 TOPPERS
                JSONArray toppers = new JSONArray();
                String topperSql = "SELECT s.name, AVG(m.grade_point) as avg_gp " +
                                   "FROM students s JOIN semester_marks m ON s.roll_no = m.roll_no " +
                                   "WHERE s.class_id = ? AND m.semester = ? " +
                                   "GROUP BY s.roll_no, s.name ORDER BY avg_gp DESC LIMIT 3";
                
                try (PreparedStatement pst1 = con.prepareStatement(topperSql)) {
                    pst1.setString(1, classId);
                    pst1.setString(2, sem);
                    ResultSet rs1 = pst1.executeQuery();
                    while(rs1.next()) {
                        JSONObject t = new JSONObject();
                        t.put("name", rs1.getString("name"));
                        t.put("gpa", String.format("%.2f", rs1.getDouble("avg_gp")));
                        toppers.put(t);
                    }
                }
                result.put("toppers", toppers);

                // 2. PASS / FAIL COUNTS
                String statsSql = "SELECT " +
                    "COUNT(DISTINCT CASE WHEN grade = 'F' THEN roll_no END) as fail_count, " +
                    "COUNT(DISTINCT roll_no) as total_students, " +
                    "AVG(grade_point) as class_avg " +
                    "FROM semester_marks WHERE semester = ? AND roll_no IN (SELECT roll_no FROM students WHERE class_id = ?)";
                
                try (PreparedStatement pst2 = con.prepareStatement(statsSql)) {
                    pst2.setString(1, sem);
                    pst2.setString(2, classId);
                    ResultSet rs2 = pst2.executeQuery();
                    if(rs2.next()) {
                        int total = rs2.getInt("total_students");
                        int failed = rs2.getInt("fail_count");
                        result.put("total", total);
                        result.put("passCount", total - failed);
                        result.put("failCount", failed);
                        result.put("avgGpa", String.format("%.2f", rs2.getDouble("class_avg")));
                    }
                }
                
                // 3. GRADE DISTRIBUTION (Formatted for Chart.js)
                JSONArray labels = new JSONArray();
                JSONArray counts = new JSONArray();
                String gradeSql = "SELECT grade, COUNT(*) as count FROM semester_marks " +
                                  "WHERE semester = ? AND roll_no IN (SELECT roll_no FROM students WHERE class_id = ?) " +
                                  "GROUP BY grade ORDER BY grade";
                
                try (PreparedStatement pst3 = con.prepareStatement(gradeSql)) {
                    pst3.setString(1, sem);
                    pst3.setString(2, classId);
                    ResultSet rs3 = pst3.executeQuery();
                    while(rs3.next()) {
                        labels.put(rs3.getString("grade"));
                        counts.put(rs3.getInt("count"));
                    }
                }
                result.put("gradeLabels", labels);
                result.put("gradeData", counts);

                out.print(result.toString());
            }

        } catch (Exception e) {
            response.setStatus(500);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}