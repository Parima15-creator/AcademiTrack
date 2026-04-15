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

@WebServlet(name = "GetStudentsServlet", urlPatterns = {"/GetStudentsServlet"})
public class GetStudentsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String classId = request.getParameter("class");
        List<Student> students = new ArrayList<>();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/academic_tracker", "root", "");

            // 4. IMPROVED QUERY: Calculates GPA and joins with marks automatically
// Change "ORDER BY gpa DESC" to "ORDER BY s.roll_no ASC"
// Change the ORDER BY in your query string
String query = "SELECT s.roll_no, s.name, IFNULL(AVG(m.grade_point), 0.0) as gpa " +
               "FROM students s " +
               "LEFT JOIN semester_marks m ON s.roll_no = m.roll_no " +
               "WHERE s.class_id = ? " +
               "GROUP BY s.roll_no, s.name " +
               "ORDER BY s.roll_no ASC"; // This ensures numerical order in the table
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, classId);
            ResultSet rs = pst.executeQuery();

            // 5. Populate student list with GPA
            while (rs.next()) {
                students.add(new Student(
                    rs.getInt("roll_no"), 
                    rs.getString("name"), 
                    rs.getDouble("gpa")
                ));
            }

            // 6. Convert to JSON including the GPA field
            String json = "[" + students.stream()
                .map(s -> "{\"roll\":" + s.getRoll() + 
                          ",\"name\":\"" + s.getName() + 
                          "\",\"gpa\":" + String.format("%.2f", s.getGpa()) + 
                          ",\"class\":\"" + classId + "\"}")
                .collect(Collectors.joining(",")) + "]";

            out.print(json);
            
            rs.close();
            pst.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}