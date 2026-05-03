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
        String subject = request.getParameter("subject");
        
        JSONObject result = new JSONObject();

        // Database credentials
        String url = "jdbc:mysql://localhost:3306/academic_tracker";
        String user = "root";
        String password = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(url, user, password)) {

                // 1. DYNAMIC SUBJECT LIST
                // Added a check to exclude 'undefined' subjects if marks aren't entered yet
                JSONArray subjects = new JSONArray();
                String subSql = "SELECT DISTINCT subject_code FROM semester_marks WHERE semester = ? " +
                                "AND grade != 'undefined' " +
                                "AND roll_no IN (SELECT roll_no FROM students WHERE class_id = ?)";
                try (PreparedStatement psSub = con.prepareStatement(subSql)) {
                    psSub.setString(1, sem);
                    psSub.setString(2, classId);
                    ResultSet rsSub = psSub.executeQuery();
                    while(rsSub.next()) subjects.put(rsSub.getString("subject_code"));
                }
                result.put("subjects", subjects);

                // 2. PASS/FAIL & REATTEMPT COUNTS
                // Refined: Counts 'F' as fail and standard grades as pass. Ignores 'undefined'.
                String statsSql = "SELECT " +
                    "COUNT(DISTINCT CASE WHEN grade IN ('O','A+','A','B+','B','C','P') THEN roll_no END) as pass_count, " +
                    "COUNT(DISTINCT CASE WHEN grade = 'F' THEN roll_no END) as fail_count, " +
                    "COUNT(DISTINCT CASE WHEN attempt_no > 1 AND grade != 'F' AND grade != 'undefined' THEN roll_no END) as recovery " +
                    "FROM semester_marks WHERE semester = ? AND roll_no IN (SELECT roll_no FROM students WHERE class_id = ?)";
                
                try (PreparedStatement psStats = con.prepareStatement(statsSql)) {
                    psStats.setString(1, sem);
                    psStats.setString(2, classId);
                    ResultSet rsStats = psStats.executeQuery();
                    if(rsStats.next()){
                        result.put("passCount", rsStats.getInt("pass_count"));
                        result.put("failCount", rsStats.getInt("fail_count"));
                        result.put("reattemptSuccess", rsStats.getInt("recovery"));
                    }
                }

                // 3. CLASS COMPARISON (e.g., SE1 vs SE2)
                // Added filter to ignore 0.0 GPA rows (undefined results) for a true average
                if (classId != null && classId.length() >= 2) {
                    String prefix = classId.substring(0, 2); 
                    String compSql = "SELECT s.class_id, AVG(m.grade_point) as avgGpa " +
                                     "FROM students s JOIN semester_marks m ON s.roll_no = m.roll_no " +
                                     "WHERE s.class_id LIKE ? AND m.semester = ? AND m.grade != 'undefined' " +
                                     "GROUP BY s.class_id";
                    try (PreparedStatement psComp = con.prepareStatement(compSql)) {
                        psComp.setString(1, prefix + "%");
                        psComp.setString(2, sem);
                        ResultSet rsComp = psComp.executeQuery();
                        JSONArray comparison = new JSONArray();
                        while(rsComp.next()){
                            JSONObject c = new JSONObject();
                            c.put("class", rsComp.getString("class_id"));
                            c.put("avgGpa", Math.round(rsComp.getDouble("avgGpa") * 100.0) / 100.0); // Round to 2 decimals
                            comparison.put(c);
                        }
                        result.put("comparison", comparison);
                    }
                }

                // 4. SUBJECT TOPPERS
                if(subject != null && !subject.isEmpty()){
                    JSONArray toppers = new JSONArray();
                    String topperSql = "SELECT s.name, (m.isa_total + m.sea_marks) as total_marks " +
                                       "FROM students s JOIN semester_marks m ON s.roll_no = m.roll_no " +
                                       "WHERE m.subject_code = ? AND s.class_id = ? AND m.semester = ? " +
                                       "AND m.grade != 'undefined' " +
                                       "ORDER BY total_marks DESC LIMIT 5";
                    try (PreparedStatement psTop = con.prepareStatement(topperSql)) {
                        psTop.setString(1, subject);
                        psTop.setString(2, classId);
                        psTop.setString(3, sem);
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
                
                out.print(result.toString());
            }
        } catch (Exception e) {
            response.setStatus(500);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}