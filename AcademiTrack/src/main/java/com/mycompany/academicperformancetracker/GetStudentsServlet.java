// Used in dashboard.jsp 
// When the teacher clicks on the class name, list of student appears

package com.mycompany.academicperformancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * GetStudentsServlet
 * This servlet fetches the list of students for a specific class to populate the dashboard.
 * Calculations are omitted here as detailed analysis is handled by GetStudentAnalysisServlet.
 */
@WebServlet(name = "GetStudentsServlet", urlPatterns = {"/GetStudentsServlet"})
public class GetStudentsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // Identify which class we are fetching students for from the request parameter
        String classId = request.getParameter("class");
        List<Student> students = new ArrayList<>();

        // Basic validation: Return empty if no class ID is provided
        if (classId == null || classId.isEmpty()) {
            out.print("[]");
            return;
        }

        try {
            // Load MySQL Driver and establish connection
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // Simplified SQL query focusing only on identity details
            // Ordered by roll number to ensure a consistent list in the dashboard
            String query = "SELECT roll_no, name FROM students WHERE class_id = ? ORDER BY roll_no ASC";
            
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, classId);
            ResultSet rs = pst.executeQuery();

            // Populate the list with Student objects
            // Passing 0.0 for GPA since the dashboard does not display it
            while (rs.next()) {
                students.add(new Student(
                    rs.getInt("roll_no"), 
                    rs.getString("name"), 
                    0.0 
                ));
            }

            // Convert to JSON using Java Streams for a clean array format
            String json = "[" + students.stream()
                .map(s -> "{" +
                          "\"roll\":" + s.getRoll() + "," +
                          "\"name\":\"" + s.getName().replace("\"", "\\\"") + "\"," +
                          "\"class\":\"" + classId + "\"" +
                          "}")
                .collect(Collectors.joining(",")) + "]";

            out.print(json); 
            
            rs.close();
            pst.close();
            con.close();

        } catch (Exception e) {
            // Log error and return an empty array to prevent dashboard crashes
            e.printStackTrace();
            out.print("[]");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Reuse doGet logic for POST requests
        doGet(request, response);
    }
}