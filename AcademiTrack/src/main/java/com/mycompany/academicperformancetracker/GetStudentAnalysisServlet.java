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

            // 1. Fetch Student Profile Info
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

            // 2. Fetch Selected Semester SGPA (Excluding Honors)
            // Ensure this query accurately reflects your college's weighted average
String sgpaQuery = "SELECT SUM(sm.grade_point * sm.credits) / SUM(sm.credits) as sgpa " +
                   "FROM semester_marks sm " +
                   "JOIN subjects sub ON sm.subject_code = sub.subject_code " +
                   "WHERE sm.roll_no = ? AND sm.semester = ? " +
                   "AND sub.is_honors = FALSE AND sm.credits > 0";
            PreparedStatement pstSgpa = con.prepareStatement(sgpaQuery);
            pstSgpa.setString(1, rollStr);
            pstSgpa.setString(2, selectedSem);
            ResultSet rsSgpa = pstSgpa.executeQuery();
            double semesterSGPA = 0.0;
            if (rsSgpa.next()) semesterSGPA = rsSgpa.getDouble("sgpa");

            // 3. Fetch Overall CGPA (Excluding Honors - Weighted)
            String cgpaQuery = "SELECT SUM(sm.grade_point * sm.credits) / SUM(sm.credits) as cgpa " +
                               "FROM semester_marks sm " +
                               "JOIN subjects sub ON sm.subject_code = sub.subject_code " +
                               "WHERE sm.roll_no = ? AND sm.credits > 0 AND sub.is_honors = FALSE";
            PreparedStatement pstCgpa = con.prepareStatement(cgpaQuery);
            pstCgpa.setString(1, rollStr);
            ResultSet rsCgpa = pstCgpa.executeQuery();
            double cumulativeCGPA = 0.0;
            if (rsCgpa.next()) cumulativeCGPA = rsCgpa.getDouble("cgpa");

            // 4. Fetch Trend Data for Chart (Excluding Honors)
            String trendQuery = "SELECT sm.semester, SUM(sm.grade_point * sm.credits) / SUM(sm.credits) as avg_gp " +
                                "FROM semester_marks sm " +
                                "JOIN subjects sub ON sm.subject_code = sub.subject_code " +
                                "WHERE sm.roll_no = ? AND sm.credits > 0 AND sub.is_honors = FALSE " +
                                "GROUP BY sm.semester ORDER BY sm.semester ASC";
            PreparedStatement pstTrend = con.prepareStatement(trendQuery);
            pstTrend.setString(1, rollStr);
            ResultSet rsTrend = pstTrend.executeQuery();
            List<String> trendData = new ArrayList<>();
            while (rsTrend.next()) {
                trendData.add(String.format("%.2f", rsTrend.getDouble("avg_gp")));
            }

            // 5. Fetch Subject Marks (Includes is_honors flag for UI sorting)
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
                String mJson = "{" +
                    "\"code\":\"" + rs2.getString("subject_code") + "\"," +
                    "\"name\":\"" + rs2.getString("subject_name").replace("\"", "\\\"") + "\"," +
                    "\"is_honors\":" + rs2.getBoolean("is_honors") + "," +
                    "\"isa\":" + rs2.getDouble("isa_total") + "," +
                    "\"sea\":" + rs2.getDouble("sea_marks") + "," +
                    "\"gp\":" + rs2.getDouble("grade_point") + "," + 
                    "\"letter\":\"" + (rs2.getString("grade") != null ? rs2.getString("grade") : "-") + "\"" +
                "}";
                marksJsonList.add(mJson);
            }

            // 6. Build Final JSON response
            String finalJson = "{" +
                "\"name\":\"" + name.replace("\"", "\\\"") + "\"," +
                "\"roll\":\"" + rollStr + "\"," +
                "\"class\":\"" + formattedClass + "\"," +
                "\"email\":\"" + rollStr + "@dbcegoa.ac.in\"," +
                "\"phone\":\"" + (phone != null ? phone : "+91 XXXXXXXXXX") + "\"," +
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