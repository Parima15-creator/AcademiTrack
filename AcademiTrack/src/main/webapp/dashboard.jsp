<%-- 
    Document   : dashboard
    Created on : 19 Mar 2026, 12:39:20 pm
    Author     : Admin
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    // Security Check: If the session variable 'Username' is missing, 
    // it means the user isn't logged in. Redirect them to the login page.
    if (session.getAttribute("Username") == null) {
        response.sendRedirect("index.html");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AcademiTrack - Dashboard</title>
    <link rel="stylesheet" href="dashboardstyle.css">
</head>
<body>

<div class="sidebar">
    <div class="sidebar-top">
        <div class="logo">🎓 AcademiTrack</div>
        <div class="menu">
            <button class="active" onclick="location.href='dashboard.jsp'">📘 Semester</button>
            <button onclick="location.href='statistics.jsp'">📊 Statistics</button>
        </div>
    </div>
    <button class="logout" onclick="location.href='LogoutServlet'">Logout</button>
</div>

<div class="main">
    <div class="user-welcome">
        <!-- $ Username is an Expression Language (EL) tag. It automatically pulls the logged-in faculty's name from the session and displays it.-->
        <span>Welcome, <strong>${Username}</strong> 👋</span>
    </div>

    <h1>Semester - IT Marks</h1>

    <div class="class-buttons">
        <!--  Each button calls the loadStudents() JavaScript function with a specific class code. -->
        <button onclick="loadStudents('FE1')">FE Comp 1</button>
        <button onclick="loadStudents('FE2')">FE Comp 2</button>
        <button onclick="loadStudents('SE1')">SE Comp 1</button>
        <button onclick="loadStudents('SE2')">SE Comp 2</button>
        <button onclick="loadStudents('TE1')">TE Comp 1</button>
        <button onclick="loadStudents('TE2')">TE Comp 2</button>
        <button onclick="loadStudents('BE1')">BE Comp 1</button>
        <button onclick="loadStudents('BE2')">BE Comp 2</button>
    </div>
    
    <!-- A green button that triggers a browser prompt to input new student details. -->
    <div class="controls" style="margin-bottom: 20px;">
        <button class="view" onclick="addStudent()" style="background: #27ae60; color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer;">
            + Add New Student
        </button>
    </div>

    <div class="search" style="margin-bottom: 30px;">
        <!-- onkeyup="filterStudents() used so that the table updates in real-time as you type. -->
        <input type="text" id="search" placeholder="🔍 Search by name or roll no" onkeyup="filterStudents()" 
               style="width: 100%; max-width: 400px; padding: 12px 20px; border-radius: 25px; border: 1px solid #ddd; outline: none;">
    </div>
    
    <!-- Direct links to other JSP pages for specific mark viewing. -->
    <div class="actions" style="margin: 30px 0; display: flex; gap: 15px;">
        <a href="view_isa_marks.jsp" class="action-btn">View ISA Marks</a>
        <a href="view_semester_marks.jsp" class="action-btn">View Semester Marks</a>
    </div>

    <table>
        <thead>
            <tr>
                <th>Roll No</th>
                <th>Name</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody id="studentTable">
            <!-- This area is empty; JavaScript will fill it with rows -->
            </tbody>
    </table>
</div>

<script>
// Your script remains the same here...
let currentList = [];

function loadStudents(cls) {
    // 1. Remove 'active' class from all buttons
    document.querySelectorAll('.class-buttons button').forEach(btn => btn.classList.remove('active'));
    
    // 2. Add 'active' class to the one that was just clicked
    event.target.classList.add('active');

    //fetch calls GetStudentsServlet. This servlet talks to the database and returns data in JSON format.
    fetch('GetStudentsServlet?class=' + cls)
        .then(response => response.json())
        .then(dbData => {
            currentList = dbData;
            display(currentList);
        });
}

//Go to Profile of each student
function viewProfile(roll) {
    window.location.href = "analysis.jsp?roll=" + roll;
}

//Clears the current table body.
//Loops through the student list and creates HTML table rows (<tr>) for each student.
function display(list) {
    const table = document.getElementById("studentTable");
    table.innerHTML = "";

    list.forEach(s => {
        table.innerHTML += '<tr>' +
            '<td>' + s.roll + '</td>' +
            '<td>' + s.name + '</td>' +
            //Adds View and Delete buttons for every student row.
            '<td>' +
                '<button class="view" onclick="viewProfile(' + s.roll + ')">View</button> ' +
                '<button class="view" style="background:#e74c3c;" onclick="deleteStudent(' + s.roll + ')">Delete</button>' +
            '</td>' +
        '</tr>';
    });
}
// 1. Logic for "+ Add New Student"
//Uses prompt() to get data from the user and sends a POST request to AddStudentServlet.
function addStudent() {
    //Enter details of student
    const rollNo = prompt("Enter Roll Number:");
    const name = prompt("Enter Student Full Name:");
    const classId = prompt("Enter Class Code (e.g., SE1, TE2):");

    if (rollNo && name && classId) {
        fetch('AddStudentServlet', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'rollNo=' + encodeURIComponent(rollNo) + 
                  '&name=' + encodeURIComponent(name) + 
                  '&classId=' + encodeURIComponent(classId)
        })
        .then(res => res.text())
        .then(msg => {
            alert(msg);
            loadStudents(classId); // Refresh that specific class list
        });
    }
}

// 2. Logic for "Delete" Button
//Shows a confirmation pop-up (confirm()). 
//If the user clicks "OK," it sends the roll number to DeleteStudentServlet to remove them from the database.
function deleteStudent(rollNo) {
    if (confirm("Are you sure? This will delete student " + rollNo + " and all their marks.")) {
        fetch('DeleteStudentServlet', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'rollNo=' + encodeURIComponent(rollNo)
        })
        .then(res => res.text())
        .then(msg => {
            alert(msg);
            location.reload(); 
        });
    }
}

//Takes the value from the search input.
//Filters the currentList array by checking if the name or roll number matches the search text.
//Re-renders the table with only the matching results.
function filterStudents() {
    const val = document.getElementById("search").value.toLowerCase();
    const filtered = currentList.filter(s =>
        s.name.toLowerCase().includes(val) ||
        s.roll.toString().includes(val)
    ); 
    display(filtered);
}
</script>

</body>
</html>
