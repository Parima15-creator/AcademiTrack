package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet(name = "ISAServlet", urlPatterns = {"/ISAServlet"})
public class ISAServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String selectedClass = request.getParameter("class");
        // Extracting numeric semester (e.g., "Semester 2" -> "2")
        String selectedSem = request.getParameter("sem").replaceAll("[^0-9]", "");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 1. DYNAMIC SUBJECT HEADERS: Fetch subjects filtered by the selected semester
            StringBuilder subNamesJson = new StringBuilder("\"subjectNames\": {");
            String subQuery = "SELECT subject_code, subject_name FROM subjects WHERE semester = ?";
            PreparedStatement subPst = con.prepareStatement(subQuery);
            subPst.setString(1, selectedSem);
            ResultSet subRs = subPst.executeQuery();
            
            boolean firstSub = true;
            while (subRs.next()) {
                if (!firstSub) subNamesJson.append(",");
                subNamesJson.append("\"").append(subRs.getString("subject_code"))
                           .append("\":\"").append(subRs.getString("subject_name")).append("\"");
                firstSub = false;
            }
            subNamesJson.append("},");

            // 2. STUDENT DATA: Fetch students and marks for the selected semester
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

            StringBuilder studentJson = new StringBuilder("\"students\": {");
            String currentRoll = "";
            boolean firstStud = true;

            while (rs.next()) {
                String roll = rs.getString("roll_no");
                String code = rs.getString("subject_code");

                if (!roll.equals(currentRoll)) {
                    if (!firstStud) studentJson.append("} },");
                    studentJson.append("\"").append(roll).append("\":{\"name\":\"").append(rs.getString("name")).append("\",\"subjects\":{");
                    currentRoll = roll;
                    firstStud = false;
                } else if (code != null) {
                    // Check if we need a comma between subject objects
                    if (!studentJson.toString().endsWith("{")) studentJson.append(",");
                }

                if (code != null) {
                    studentJson.append("\"").append(code).append("\":{\"isa1\":").append(rs.getFloat("isa1"))
                               .append(",\"isa2\":").append(rs.getFloat("isa2"))
                               .append(",\"isa3\":").append(rs.getFloat("isa3"))
                               .append(",\"assignment\":").append(rs.getFloat("assignment")).append("}");
                }
            }
            if (!firstStud) studentJson.append("} }");
            studentJson.append("}");

            // Combine JSON response
            out.print("{" + subNamesJson.toString() + studentJson.toString() + "}");
            con.close();
            
        } catch (Exception e) { 
            out.print("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}"); 
        }
    }
}