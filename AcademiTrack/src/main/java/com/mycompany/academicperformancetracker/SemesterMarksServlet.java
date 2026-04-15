package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

@WebServlet(name = "SemesterMarksServlet", urlPatterns = {"/SemesterMarksServlet"})
public class SemesterMarksServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String classId = request.getParameter("class");
        String sem = request.getParameter("sem");
        
        JSONObject result = new JSONObject();
        JSONObject studentsJson = new JSONObject();
        JSONObject subjectNames = new JSONObject();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 1. Fetch all subject names to build headers
            // Adding a WHERE clause to filter by the selected semester
String subSql = "SELECT subject_code, subject_name FROM subjects WHERE semester = ?";
PreparedStatement subPst = con.prepareStatement(subSql);
subPst.setString(1, sem); // 'sem' is the parameter from the request
ResultSet subRs = subPst.executeQuery();
            
            while(subRs.next()) {
                subjectNames.put(subRs.getString("subject_code"), subRs.getString("subject_name"));
            }

            // 2. Fetch students of the class and join with semester marks
            String sql = "SELECT s.roll_no, s.name, m.subject_code, m.isa_total, m.sea_marks, m.credits, m.grade_point, m.grade " +
                         "FROM students s " +
                         "LEFT JOIN semester_marks m ON s.roll_no = m.roll_no AND m.semester = ? " +
                         "WHERE s.class_id = ? ORDER BY s.roll_no";
            
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, sem);
            pst.setString(2, classId);
            ResultSet rs = pst.executeQuery();

            while(rs.next()) {
                String roll = rs.getString("roll_no");
                if(!studentsJson.has(roll)) {
                    JSONObject sObj = new JSONObject();
                    sObj.put("name", rs.getString("name"));
                    sObj.put("subjects", new JSONObject());
                    studentsJson.put(roll, sObj);
                }
                
                String subCode = rs.getString("subject_code");
                if(subCode != null) {
                    JSONObject mObj = new JSONObject();
                    mObj.put("isa", rs.getDouble("isa_total"));
                    mObj.put("sea", rs.getDouble("sea_marks"));
                    mObj.put("credits", rs.getInt("credits"));
                    mObj.put("gp", rs.getDouble("grade_point"));
                    mObj.put("grade", rs.getString("grade"));
                    studentsJson.getJSONObject(roll).getJSONObject("subjects").put(subCode, mObj);
                }
            }
            
            result.put("students", studentsJson);
            result.put("subjectNames", subjectNames);
            out.print(result.toString());
            con.close();
        } catch (Exception e) {
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}