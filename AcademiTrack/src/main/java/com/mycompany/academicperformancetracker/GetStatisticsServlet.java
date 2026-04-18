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

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 1. Fetch Available Subjects for this semester
            JSONArray subjects = new JSONArray();
            String subSql = "SELECT DISTINCT subject_code FROM semester_marks WHERE semester = ?";
            PreparedStatement psSub = con.prepareStatement(subSql);
            psSub.setString(1, sem);
            ResultSet rsSub = psSub.executeQuery();
            while(rsSub.next()) subjects.put(rsSub.getString("subject_code"));
            result.put("subjects", subjects);

            // 2. Pass/Fail & Reattempt Counts
            String statsSql = "SELECT " +
                "COUNT(DISTINCT roll_no) as total, " +
                "COUNT(DISTINCT CASE WHEN grade = 'F' THEN roll_no END) as fail_count, " +
                "COUNT(DISTINCT CASE WHEN attempt_no > 1 AND grade != 'F' THEN roll_no END) as recovery " +
                "FROM semester_marks WHERE semester = ? AND roll_no IN (SELECT roll_no FROM students WHERE class_id = ?)";
            
            PreparedStatement psStats = con.prepareStatement(statsSql);
            psStats.setString(1, sem);
            psStats.setString(2, classId);
            ResultSet rsStats = psStats.executeQuery();
            if(rsStats.next()){
                int total = rsStats.getInt("total");
                int failed = rsStats.getInt("fail_count");
                result.put("passCount", total - failed);
                result.put("failCount", failed);
                result.put("reattemptSuccess", rsStats.getInt("recovery"));
            }

            // 3. Class Comparison (e.g., SE1 vs SE2)
            String prefix = classId.substring(0, 2); // Get "FE", "SE", etc.
            String compSql = "SELECT s.class_id, AVG(m.grade_point) as gpa " +
                             "FROM students s JOIN semester_marks m ON s.roll_no = m.roll_no " +
                             "WHERE s.class_id LIKE ? AND m.semester = ? GROUP BY s.class_id";
            PreparedStatement psComp = con.prepareStatement(compSql);
            psComp.setString(1, prefix + "%");
            psComp.setString(2, sem);
            ResultSet rsComp = psComp.executeQuery();
            JSONArray comparison = new JSONArray();
            while(rsComp.next()){
                JSONObject c = new JSONObject();
                c.put("class", rsComp.getString("class_id"));
                c.put("gpa", rsComp.getDouble("gpa"));
                comparison.put(c);
            }
            result.put("comparison", comparison);

            // 4. Subject Toppers (Top 5)
            if(subject != null && !subject.isEmpty()){
                String topperSql = "SELECT s.name, (m.isa_total + m.sea_marks) as total " +
                                   "FROM students s JOIN semester_marks m ON s.roll_no = m.roll_no " +
                                   "WHERE m.subject_code = ? AND s.class_id = ? " +
                                   "ORDER BY total DESC LIMIT 5";
                PreparedStatement psTop = con.prepareStatement(topperSql);
                psTop.setString(1, subject);
                psTop.setString(2, classId);
                ResultSet rsTop = psTop.executeQuery();
                JSONArray toppers = new JSONArray();
                while(rsTop.next()){
                    JSONObject t = new JSONObject();
                    t.put("name", rsTop.getString("name"));
                    t.put("score", rsTop.getDouble("total"));
                    toppers.put(t);
                }
                result.put("subjectToppers", toppers);
            }

            out.print(result.toString());
            con.close();
        } catch (Exception e) {
            response.setStatus(500);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}