package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * ISAServlet handles the retrieval of academic marks (In-Semester Assessment).
 * It dynamically constructs a JSON object containing both subject definitions 
 * and student performance data based on the selected class and semester.
 */
@WebServlet(name = "ISAServlet", urlPatterns = {"/ISAServlet"})
public class ISAServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        // Set response type to JSON so the calling JavaScript knows how to parse it
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // Retrieve filters from the request (e.g., ?class=SE1&sem=Semester 2)
        String selectedClass = request.getParameter("class");
        
        // DATA CLEANING: Uses Regex to strip non-numeric characters. 
        // Converts "Semester 2" into "2" to match database integer columns.
        String selectedSem = request.getParameter("sem").replaceAll("[^0-9]", "");

        try {
            // Standard JDBC connection setup
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 1: DYNAMIC SUBJECT HEADERS 
            // We need to know which subjects exist for this semester to build the table columns.
            StringBuilder subNamesJson = new StringBuilder("\"subjectNames\": {");
            String subQuery = "SELECT subject_code, subject_name FROM subjects WHERE semester = ?";
            PreparedStatement subPst = con.prepareStatement(subQuery);
            subPst.setString(1, selectedSem);
            ResultSet subRs = subPst.executeQuery();
            
            boolean firstSub = true;
            while (subRs.next()) {
                if (!firstSub) subNamesJson.append(","); // Add comma between JSON entries
                
                // Format: "CS101":"Data Structures"
                subNamesJson.append("\"").append(subRs.getString("subject_code"))
                           .append("\":\"").append(subRs.getString("subject_name")).append("\"");
                firstSub = false;
            }
            subNamesJson.append("},");

            // 2: STUDENT DATA AGGREGATION
            /* 
             * SQL LOGIC: 
             * We use a LEFT JOIN because we want to see ALL students in the class, 
             * even if they don't have marks recorded in 'isa_details' yet.
             * We filter marks by semester to ensure we don't mix Sem 1 and Sem 2 data.
             */
            String dataQuery = "SELECT s.roll_no, s.name, i.subject_code, " +
                               "i.isa1, i.isa2, i.isa3, i.assignment " +
                               "FROM students s " +
                               "LEFT JOIN isa_details i ON s.roll_no = i.roll_no AND i.semester = ? " +
                               "WHERE s.class_id = ? " + 
                               "ORDER BY s.roll_no ASC";
            
            PreparedStatement pst = con.prepareStatement(dataQuery);
            pst.setString(1, selectedSem);
            pst.setString(2, selectedClass);
            ResultSet rs = pst.executeQuery();

            // 3: NESTED JSON CONSTRUCTION 
            // We are building a "Map of Maps" in JSON.
            StringBuilder studentJson = new StringBuilder("\"students\": {");
            String currentRoll = ""; // Tracker to detect when we move to the next student in the result set
            boolean firstStud = true;

            while (rs.next()) {
                String roll = rs.getString("roll_no");
                String code = rs.getString("subject_code");
                
                // If the roll number changes, we start a new student object
                if (!roll.equals(currentRoll)) {
                    if (!firstStud) studentJson.append("} },");
                    // Start new student entry: "Roll01": {"name": "John", "subjects": { ...
                    studentJson.append("\"").append(roll).append("\":{\"name\":\"").append(rs.getString("name")).append("\",\"subjects\":{");
                    currentRoll = roll;
                    firstStud = false;
                } else if (code != null) {
                    // If we are still on the same student, add a comma before the next subject
                    if (!studentJson.toString().endsWith("{")) studentJson.append(",");
                }
                
                // If there are marks for this specific subject/student combination
                if (code != null) {
                    // Format: "SUB101": {"isa1": 15.0, "isa2": 18.5, ...}
                    studentJson.append("\"").append(code).append("\":{\"isa1\":").append(rs.getFloat("isa1"))
                               .append(",\"isa2\":").append(rs.getFloat("isa2"))
                               .append(",\"isa3\":").append(rs.getFloat("isa3"))
                               .append(",\"assignment\":").append(rs.getFloat("assignment")).append("}");
                }
            }
            
            // Close the final student object
            if (!firstStud) studentJson.append("} }");
            studentJson.append("}");
            
            // 4: FINAL OUTPUT 
            // Combine JSON response
            out.print("{" + subNamesJson.toString() + studentJson.toString() + "}");
            con.close();
            
        } catch (Exception e) { 
            // Error handling: Return a JSON error object so the frontend doesn't crash
            out.print("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}"); 
        }
    }
}