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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String classId = request.getParameter("class"), sem = request.getParameter("sem"), subject = request.getParameter("subject");
        JSONObject result = new JSONObject();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "")) {
                
                // 1. DYNAMIC SUBJECTS
                JSONArray subjects = new JSONArray();
                String subSql = "SELECT DISTINCT TRIM(m.subject_code) as code, TRIM(s.subject_name) as name " +
                                "FROM semester_marks m JOIN subjects s ON TRIM(m.subject_code) = TRIM(s.subject_code) " +
                                "WHERE TRIM(m.semester) = ? AND TRIM(m.roll_no) IN (SELECT roll_no FROM students WHERE class_id = ?)";
                try (PreparedStatement ps = con.prepareStatement(subSql)) {
                    ps.setString(1, sem); ps.setString(2, classId);
                    ResultSet rs = ps.executeQuery();
                    while(rs.next()) {
                        JSONObject s = new JSONObject(); s.put("code", rs.getString("code")); s.put("name", rs.getString("name"));
                        subjects.put(s);
                    }
                }
                result.put("subjects", subjects);

                // 2. SEMESTER PERFORMANCE (Fixed: 47 vs 53 Logic)
                // Filtered by semester inside the count
                String statsSql = "SELECT COUNT(DISTINCT roll_no) as total, " +
                  "COUNT(DISTINCT CASE WHEN TRIM(grade)='F' THEN roll_no END) as failed " +
                  "FROM semester_marks WHERE TRIM(semester)=? " +
                  "AND TRIM(roll_no) IN (SELECT TRIM(roll_no) FROM students WHERE TRIM(class_id)=?)";
                try (PreparedStatement ps = con.prepareStatement(statsSql)) {
                    ps.setString(1, sem); ps.setString(2, classId);
                    ResultSet rs = ps.executeQuery();
                    if(rs.next()) {
                        result.put("totalAppeared", rs.getInt("total"));
                        result.put("failCount", rs.getInt("failed"));
                        result.put("passCount", rs.getInt("total") - rs.getInt("failed"));
                    }
                }
                
                // --- 5. SUBJECT WITH HIGHEST FAILURE RATE ---
                // --- 5. SUBJECT WITH HIGHEST FAILURE RATE (Robust Version) ---
                // --- 5. SUBJECT WITH HIGHEST FAILURE RATE ---
String failRateSql = "SELECT m.subject_code, s.subject_name, COUNT(*) as fail_count " +
                     "FROM semester_marks m " +
                     "JOIN subjects s ON TRIM(m.subject_code) = TRIM(s.subject_code) " +
                     "WHERE TRIM(m.semester) = ? " +
                     "AND TRIM(m.roll_no) IN (SELECT roll_no FROM students WHERE TRIM(class_id) = ?) " +
                     "AND TRIM(m.grade) = 'F' " + 
                     "GROUP BY m.subject_code, s.subject_name " +
                     "ORDER BY fail_count DESC LIMIT 1";

try (PreparedStatement psFail = con.prepareStatement(failRateSql)) {
    psFail.setString(1, sem.trim());
    psFail.setString(2, classId.trim());
    ResultSet rsFail = psFail.executeQuery();
    
    if (rsFail.next()) {
        // Concatenate code and name for the UI
        result.put("toughestSubject", rsFail.getString("subject_code") + ": " + rsFail.getString("subject_name"));
        result.put("highestFailCount", rsFail.getInt("fail_count"));
    } else {
        result.put("toughestSubject", "None");
        result.put("highestFailCount", 0);
    }
}
                // 3. SUBJECT-WISE PASS %
                JSONArray subStats = new JSONArray();
                String rateSql = "SELECT subject_code, ROUND((COUNT(CASE WHEN grade!='F' THEN 1 END)*100.0/COUNT(*)),1) as rate " +
                 "FROM semester_marks WHERE TRIM(semester)=? " +
                 "AND TRIM(roll_no) IN (SELECT TRIM(roll_no) FROM students WHERE TRIM(class_id)=?) " +
                 "GROUP BY subject_code";
                try (PreparedStatement ps = con.prepareStatement(rateSql)) {
                    ps.setString(1, sem); ps.setString(2, classId);
                    ResultSet rs = ps.executeQuery();
                    while(rs.next()) {
                        JSONObject obj = new JSONObject(); obj.put("code", rs.getString("subject_code")); obj.put("passRate", rs.getDouble("rate"));
                        subStats.put(obj);
                    }
                }
                result.put("subjectStats", subStats);

                // 4. TOPPERS
                if(subject != null && !subject.isEmpty()) {
                    JSONArray toppers = new JSONArray();
                    String topSql = "SELECT s.name, (m.isa_total + m.sea_marks) as total FROM students s JOIN semester_marks m ON s.roll_no=m.roll_no " +
                                    "WHERE m.subject_code=? AND s.class_id=? AND TRIM(m.semester)=? ORDER BY total DESC LIMIT 5";
                    try (PreparedStatement ps = con.prepareStatement(topSql)) {
                        ps.setString(1, subject); ps.setString(2, classId); ps.setString(3, sem);
                        ResultSet rs = ps.executeQuery();
                        while(rs.next()) {
                            JSONObject t = new JSONObject(); t.put("name", rs.getString("name")); t.put("score", rs.getDouble("total"));
                            toppers.put(t);
                        }
                    }
                    result.put("subjectToppers", toppers);
                }
                out.print(result.toString());
            }
        } catch (Exception e) { response.setStatus(500); out.print("{\"error\":\"" + e.getMessage() + "\"}"); }
    }
}