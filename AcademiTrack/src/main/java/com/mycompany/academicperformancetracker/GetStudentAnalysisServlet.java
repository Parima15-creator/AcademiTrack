package com.mycompany.academicperformancetracker;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet(name = "GetStudentAnalysisServlet", urlPatterns = {"/GetStudentAnalysisServlet"})
public class GetStudentAnalysisServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String rollStr = request.getParameter("roll");
        String semStr = request.getParameter("sem"); 
        
        if (rollStr == null || rollStr.isEmpty()) {
            out.print("{\"error\":\"No roll number provided\"}");
            return;
        }

        String selectedSem = (semStr == null || semStr.isEmpty()) ? "1" : semStr;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 1. Student Profile Info
            String studentQuery = "SELECT name, class_id, phone FROM students WHERE roll_no = ?";
            PreparedStatement pst1 = con.prepareStatement(studentQuery);
            pst1.setString(1, rollStr);
            ResultSet rs1 = pst1.executeQuery();
            
            String name = "Unknown Student", classId = "N/A", phone = "+91 XXXXXXXXXX";
            if (rs1.next()) {
                name = rs1.getString("name");
                classId = rs1.getString("class_id");
                phone = rs1.getString("phone");
            }
            String formattedClass = (classId != null && classId.length() >= 3) 
                ? classId.substring(0, 2) + " COMP " + classId.substring(2) : classId;

            // 2. Selected Semester SGPA
            String sgpaQuery = "SELECT SUM(sm.grade_point * sm.credits) / SUM(sm.credits) as sgpa " +
                               "FROM semester_marks sm JOIN subjects sub ON sm.subject_code = sub.subject_code " +
                               "WHERE sm.roll_no = ? AND sm.semester = ? AND sub.is_honors = 0";
            PreparedStatement pstSgpa = con.prepareStatement(sgpaQuery);
            pstSgpa.setString(1, rollStr);
            pstSgpa.setString(2, selectedSem);
            ResultSet rsSgpa = pstSgpa.executeQuery();
            double semesterSGPA = 0.0;
            if (rsSgpa.next()) semesterSGPA = rsSgpa.getDouble("sgpa");

            // 3. Overall CGPA
            String cgpaQuery = "SELECT SUM(sm.grade_point * sm.credits) as total_points, SUM(sm.credits) as total_credits " +
                               "FROM semester_marks sm JOIN subjects sub ON sm.subject_code = sub.subject_code " +
                               "WHERE sm.roll_no = ? AND sub.is_honors = 0 AND sm.credits > 0";
            PreparedStatement pstCgpa = con.prepareStatement(cgpaQuery);
            pstCgpa.setString(1, rollStr);
            ResultSet rsCgpa = pstCgpa.executeQuery();
            double cumulativeCGPA = 0.0;
            if (rsCgpa.next()) {
                double tp = rsCgpa.getDouble("total_points");
                double tc = rsCgpa.getDouble("total_credits");
                if (tc > 0) cumulativeCGPA = tp / tc;
            }     

            // 4. Trend Data
            String trendQuery = "SELECT SUM(sm.grade_point * sm.credits) / SUM(sm.credits) as avg_gp " +
                                "FROM semester_marks sm JOIN subjects sub ON sm.subject_code = sub.subject_code " +
                                "WHERE sm.roll_no = ? AND sm.credits > 0 AND sub.is_honors = 0 " +
                                "GROUP BY sm.semester ORDER BY sm.semester ASC";
            PreparedStatement pstTrend = con.prepareStatement(trendQuery);
            pstTrend.setString(1, rollStr);
            ResultSet rsTrend = pstTrend.executeQuery();
            List<String> trendData = new ArrayList<>();
            while (rsTrend.next()) trendData.add(String.format("%.2f", rsTrend.getDouble("avg_gp")));

            // 5. Subject Marks (FIXED: Added marksJsonList.add)
            String marksQuery = "SELECT sub.subject_code, sub.subject_name, sub.is_honors, " +
                   "sm.isa_total, sm.sea_marks, sm.grade_point, sm.grade " +
                   "FROM subjects sub " +
                   "LEFT JOIN semester_marks sm ON sub.subject_code = sm.subject_code AND sm.roll_no = ? " +
                   "WHERE sub.semester = ?";
            PreparedStatement pst2 = con.prepareStatement(marksQuery);
            pst2.setString(1, rollStr);
            pst2.setString(2, selectedSem);
            ResultSet rs2 = pst2.executeQuery();
            
            List<String> marksJsonList = new ArrayList<>();
            while (rs2.next()) {
                String gradeLetter = rs2.getString("grade");
                if (gradeLetter == null || gradeLetter.isEmpty()) gradeLetter = "-";

                String mJson = "{" +
                    "\"code\":\"" + rs2.getString("subject_code") + "\"," +
                    "\"name\":\"" + rs2.getString("subject_name").replace("\"", "\\\"") + "\"," +
                    "\"is_honors\":" + rs2.getBoolean("is_honors") + "," +
                    "\"isa\":" + rs2.getDouble("isa_total") + "," +
                    "\"sea\":" + rs2.getDouble("sea_marks") + "," +
                    "\"gp\":" + rs2.getDouble("grade_point") + "," + 
                    "\"letter\":\"" + gradeLetter + "\"" +
                "}";
                marksJsonList.add(mJson); // THIS LINE WAS MISSING
            }

            // 6. Build Final JSON
            String finalJson = "{" +
                "\"name\":\"" + name.replace("\"", "\\\"") + "\"," +
                "\"roll\":\"" + rollStr + "\"," +
                "\"class\":\"" + formattedClass + "\"," +
                "\"email\":\"" + rollStr + "@dbcegoa.ac.in\"," +
                "\"phone\":\"" + phone + "\"," +
                "\"sgpa\":" + String.format("%.2f", semesterSGPA) + "," +
                "\"cgpa\":" + String.format("%.2f", cumulativeCGPA) + "," +
                "\"trend\":[" + String.join(",", trendData) + "]," +
                "\"marks\":[" + String.join(",", marksJsonList) + "]" +
            "}";

            out.print(finalJson);
            con.close();

        } catch (Exception e) {
            out.print("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}