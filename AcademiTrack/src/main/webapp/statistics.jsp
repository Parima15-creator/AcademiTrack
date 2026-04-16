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
    <title>AcademiTrack - Statistics</title>
    <link rel="stylesheet" href="dashboardstyle.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>

<div class="sidebar">
    <div class="sidebar-top">
        <div class="logo">🎓 AcademiTrack</div>
        <div class="menu">
            <button onclick="location.href='dashboard.jsp'">📘 Semester</button>
            <button class="active" onclick="location.href='statistics.jsp'">📊 Statistics</button>
        </div>
    </div>
    <button class="logout" onclick="location.href='LogoutServlet'">Logout</button>
</div>

<div class="main">
    <div class="user-welcome">
        <span>Welcome, <strong>${facultyName}</strong> 👋</span>
    </div>

    <h1>Performance Analytics</h1>

    <div class="class-buttons">
        <button onclick="loadStats('FE1')">FE Comp 1</button>
        <button onclick="loadStats('FE2')">FE Comp 2</button>
        <button onclick="loadStats('SE1')">SE Comp 1</button>
        <button onclick="loadStats('SE2')">SE Comp 2</button>
        <button onclick="loadStats('TE1')">TE Comp 1</button>
        <button onclick="loadStats('TE2')">TE Comp 2</button>
        <button onclick="loadStats('BE1')">BE Comp 1</button>
        <button onclick="loadStats('BE2')">BE Comp 2</button>
    </div>

    <div class="stats-container" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-top: 30px;">
        <div class="stat-card" style="background: white; padding: 25px; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.05);">
            <h3 style="color: #1f356d; margin-top: 0;">GPA Distribution</h3>
            <div style="height: 250px; background: #f8faff; border-radius: 15px; padding: 10px;">
                <canvas id="gpaChart"></canvas>
            </div>
        </div>

        <div class="stat-card" style="background: white; padding: 25px; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.05);">
            <h3 style="color: #1f356d; margin-top: 0;">Pass vs. Fail Ratio</h3>
            <div style="height: 250px; background: #f8faff; border-radius: 15px; padding: 10px;">
                <canvas id="passFailChart"></canvas>
            </div>
        </div>
    </div>

    <div style="margin-top: 40px;">
        <h2 style="color: #1f356d;">Class Overview</h2>
        <table>
            <thead>
                <tr>
                    <th>Metric</th>
                    <th>Value</th>
                </tr>
            </thead>
            <tbody id="statsTable">
                <tr><td>Total Students</td><td id="totalStud">-</td></tr>
                <tr><td>Average GPA</td><td id="avgGpa">-</td></tr>
                <tr><td>Highest GPA</td><td id="highGpa">-</td></tr>
            </tbody>
        </table>
    </div>
</div>

<script>
    let gpaChartInstance = null;
let pfChartInstance = null;

function loadStats(cls) {
    // You might want to get the semester from a dropdown later, for now we hardcode '1'
    fetch('GetStatisticsServlet?class=' + cls + '&sem=1')
        .then(res => res.json())
        .then(data => {
            // Update Overview Table
            document.getElementById('totalStud').innerText = data.total;
            document.getElementById('avgGpa').innerText = data.avgGpa;
            
            // Render Charts
            renderGpaChart(data.gradeLabels, data.gradeData); // Bar Chart
            renderPassFailChart(data.passCount, data.failCount); // Doughnut Chart
        });
}

function renderGpaChart(labels, values) {
    const ctx = document.getElementById('gpaChart').getContext('2d');
    if (gpaChartInstance) gpaChartInstance.destroy(); // Clear old chart

    gpaChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Number of Students',
                data: values,
                backgroundColor: '#4e73df'
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

function renderPassFailChart(pass, fail) {
    const ctx = document.getElementById('passFailChart').getContext('2d');
    if (pfChartInstance) pfChartInstance.destroy();

    pfChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Pass', 'Fail'],
            datasets: [{
                data: [pass, fail],
                backgroundColor: ['#27ae60', '#e74c3c']
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}
</script>

</body>
</html>