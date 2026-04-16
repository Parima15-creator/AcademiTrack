<%-- 
    Document   : dashboard
    Created on : 19 Mar 2026, 12:39:20 pm
    Author     : Admin
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    if (session.getAttribute("facultyName") == null) {
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
        <span>Welcome, <strong>${facultyName}</strong> 👋</span>
    </div>

    <h1>Semester - IT Marks</h1>

    <div class="class-buttons">
        <button onclick="loadStudents('FE1')">FE Comp 1</button>
        <button onclick="loadStudents('FE2')">FE Comp 2</button>
        <button onclick="loadStudents('SE1')">SE Comp 1</button>
        <button onclick="loadStudents('SE2')">SE Comp 2</button>
        <button onclick="loadStudents('TE1')">TE Comp 1</button>
        <button onclick="loadStudents('TE2')">TE Comp 2</button>
        <button onclick="loadStudents('BE1')">BE Comp 1</button>
        <button onclick="loadStudents('BE2')">BE Comp 2</button>
    </div>

    <div class="controls" style="margin-bottom: 20px;">
        <button class="view" onclick="addStudent()" style="background: #27ae60; color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer;">
            + Add New Student
        </button>
    </div>

    <div class="search" style="margin-bottom: 30px;">
        <input type="text" id="search" placeholder="🔍 Search by name or roll no" onkeyup="filterStudents()" 
               style="width: 100%; max-width: 400px; padding: 12px 20px; border-radius: 25px; border: 1px solid #ddd; outline: none;">
    </div>

    <div class="podium">
        <div class="rank second">🥈 2nd<br>-</div>
        <div class="rank first">🥇 1st<br>-</div>
        <div class="rank third">🥉 3rd<br>-</div>
    </div>

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

    fetch('GetStudentsServlet?class=' + cls)
        .then(response => response.json())
        .then(dbData => {
            currentList = dbData;
            display(currentList);
            updatePodium();
        });
}
function clearTable() {
    currentList = []; // Empty the array
    display(currentList); // Update the UI
    updatePodium(); // Reset the ranks
}

function viewProfile(roll) {
    window.location.href = "analysis.jsp?roll=" + roll;
}

function display(list) {
    const table = document.getElementById("studentTable");
    table.innerHTML = "";

    list.forEach(s => {
        table.innerHTML += '<tr>' +
            '<td>' + s.roll + '</td>' +
            '<td>' + s.name + '</td>' +
            '<td>' +
                '<button class="view" onclick="viewProfile(' + s.roll + ')">View</button> ' +
                '<button class="view" style="background:#e74c3c;" onclick="deleteStudent(' + s.roll + ')">Delete</button>' +
            '</td>' +
        '</tr>';
    });
}
// 1. Logic for "+ Add New Student"
function addStudent() {
    const rollNo = prompt("Enter Roll Number:");
    const name = prompt("Enter Student Full Name:");
    // Manually asking for the class code (e.g., SE1, FE2)
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
function updatePodium() {
    const first = document.querySelector(".first");
    const second = document.querySelector(".second");
    const third = document.querySelector(".third");

    if (!currentList || currentList.length === 0) {
        first.innerHTML = "1st<br>-";
        second.innerHTML = "2nd<br>-";
        third.innerHTML = "3rd<br>-";
        return;
    }

    // This creates a separate sorted version ONLY for the podium
    // It does not change the order of the main student table
    const rankedList = [...currentList].sort((a, b) => b.gpa - a.gpa);

    first.innerHTML = `1st<br>${rankedList[0] ? rankedList[0].name : "-"}<br>GPA: ${rankedList[0] ? rankedList[0].gpa : "0.00"}`;
    second.innerHTML = `2nd<br>${rankedList[1] ? rankedList[1].name : "-"}<br>GPA: ${rankedList[1] ? rankedList[1].gpa : "0.00"}`;
    third.innerHTML = `3rd<br>${rankedList[2] ? rankedList[2].name : "-"}<br>GPA: ${rankedList[2] ? rankedList[2].gpa : "0.00"}`;
}

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
