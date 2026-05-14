package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

/**
 * SemesterMarksServlet retrieves final semester-end performance data.
 * It combines student info, subject info, and final marks into a nested JSON structure.
 */
@WebServlet(name = "SemesterMarksServlet", urlPatterns = {"/SemesterMarksServlet"})
public class SemesterMarksServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Set response type to JSON for the frontend dashboard
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // Get filters from the request (e.g., SE_COMP and 3)
        String classId = request.getParameter("class");
        String sem = request.getParameter("sem");
        
        // Initialize JSON objects using the org.json library
        JSONObject result = new JSONObject();       // Root object
        JSONObject studentsJson = new JSONObject(); // Map of student roll numbers
        JSONObject subjectNames = new JSONObject();  // Map of subject codes to full names

        try {
            // Establish Database Connection
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // --- SECTION 1: DYNAMIC SUBJECT HEADERS ---
            // Fetches subject codes and names specific to the selected semester.
            // This allows the frontend to know exactly which columns to create.
            String subSql = "SELECT subject_code, subject_name FROM subjects WHERE semester = ?";
            PreparedStatement subPst = con.prepareStatement(subSql);
            subPst.setString(1, sem); // 'sem' is the parameter from the request
            ResultSet subRs = subPst.executeQuery();
            
            while(subRs.next()) {
                // Populates subjectNames: { "CS301": "Operating Systems", ... }
                subjectNames.put(subRs.getString("subject_code"), subRs.getString("subject_name"));
            }

            // --- SECTION 2: JOINED DATA RETRIEVAL ---
            /* 
             * We join 'students' with 'semester_marks' using a LEFT JOIN.
             * This ensures students appear in the list even if their marks haven't been uploaded yet.
             * We filter by both Semester and Class ID.
             */
            String sql = "SELECT s.roll_no, s.name, m.subject_code, m.isa_total, m.sea_marks, m.credits, m.grade_point, m.grade, m.cleared_in_sem " +
             "FROM students s " +
             "LEFT JOIN semester_marks m ON s.roll_no = m.roll_no AND m.semester = ? " +
             "WHERE s.class_id = ? ORDER BY s.roll_no";
            
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, sem);
            pst.setString(2, classId);
            ResultSet rs = pst.executeQuery();
            
            // SECTION 3: NESTED JSON MAPPING
            while(rs.next()) {
                String roll = rs.getString("roll_no");
                
                // If this is the first time we see this student, initialize their object
                if(!studentsJson.has(roll)) {
                    JSONObject sObj = new JSONObject();
                    sObj.put("name", rs.getString("name"));
                    sObj.put("subjects", new JSONObject()); // Nested object for individual subject marks
                    studentsJson.put(roll, sObj);
                }
                
                String subCode = rs.getString("subject_code");
                // If mark data exists for this subject (subCode won't be null due to LEFT JOIN)
                if(subCode != null) {
                    JSONObject mObj = new JSONObject();
                    mObj.put("isa", rs.getDouble("isa_total"));
                    mObj.put("sea", rs.getDouble("sea_marks"));
                    mObj.put("credits", rs.getInt("credits"));
                    mObj.put("gp", rs.getDouble("grade_point"));
                    mObj.put("grade", rs.getString("grade"));

                    // Track if they passed in the regular sem or a remedial/makeup
                    mObj.put("cleared_in_sem", rs.getInt("cleared_in_sem")); 
                    
                    // Drill down into the student object and add this subject's data
                    studentsJson.getJSONObject(roll).getJSONObject("subjects").put(subCode, mObj);
                }
            }
            
            // 4: ASSEMBLE AND SEND
            result.put("students", studentsJson);
            result.put("subjectNames", subjectNames);
            
            // result.toString() converts the JSON hierarchy into a flat string for transmission
            out.print(result.toString());
            con.close();
            
        } catch (Exception e) {
            
            // Standard JSON error response
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}