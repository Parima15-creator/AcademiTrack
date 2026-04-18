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
    <title>AcademiTrack | Performance Analytics</title>
    <link rel="stylesheet" href="dashboardstyle.css">
    <style>
        /* Specific Analytics Overrides */
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 25px; margin-top: 20px; }
        .stat-card { background: white; padding: 25px; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.05); }
        .full-width { grid-column: 1 / -1; }
        .chart-container { position: relative; height: 300px; width: 100%; }
        .filter-row { display: flex; gap: 20px; margin-bottom: 25px; align-items: center; }
        .custom-select { padding: 10px 20px; border-radius: 12px; border: 1px solid #ddd; outline: none; background: white; color: #1f356d; font-weight: 500; }
        .metric-value { font-size: 3rem; font-weight: 800; color: #2c4aa5; margin: 10px 0; }
        .topper-table th { background: #4e73df; }
    </style>
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

    <div class="class-buttons" id="classGroup">
        <button onclick="selectClass(this, 'FE1')">FE Comp 1</button>
        <button onclick="selectClass(this, 'FE2')">FE Comp 2</button>
        <button onclick="selectClass(this, 'SE1')">SE Comp 1</button>
        <button onclick="selectClass(this, 'SE2')">SE Comp 2</button>
        </div>

    <div id="semSection" style="display: none; margin-top: 20px;">
        <h3>Select Semester</h3>
        <div class="class-buttons" id="semGroup">
            <button onclick="selectSemester(this, '1')">Sem 1</button>
            <button onclick="selectSemester(this, '2')">Sem 2</button>
            <button onclick="selectSemester(this, '3')">Sem 3</button>
            <button onclick="selectSemester(this, '4')">Sem 4</button>
        </div>
    </div>

    <div id="statsContent" style="display: none; margin-top: 30px;">
        <div class="filter-row">
            <select id="subjectSelect" class="custom-select" onchange="loadStats()">
                <option value="">View Toppers for Subject</option>
            </select>
        </div>

        <div class="stats-grid">
            <div class="stat-card">
                <h3>Pass vs. Fail Ratio</h3>
                <div class="chart-container"><canvas id="pfChart"></canvas></div>
            </div>

            <div class="stat-card" style="text-align: center;">
                <h3>Reattempt Recovery</h3>
                <div class="metric-value" id="reattemptCount">0</div>
                <p>Students cleared on reattempt</p>
            </div>

            <div class="stat-card full-width">
                <h3>Class Comparison: COMP 1 vs. COMP 2</h3>
                <div class="chart-container"><canvas id="compareChart"></canvas></div>
            </div>
            
            <div class="stat-card full-width">
                <table class="topper-table">
                    <thead>
                        <tr><th>Rank</th><th>Name</th><th>Marks</th></tr>
                    </thead>
                    <tbody id="topperBody"></tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script>
let selectedClass = null;
let selectedSem = null;
let charts = {};

function selectClass(btn, cls) {
    // UI Update
    document.querySelectorAll('#classGroup button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    
    selectedClass = cls;
    
    // Reveal Semester Section
    document.getElementById('semSection').style.display = 'block';
    
    // Reset selections below if class changes
    selectedSem = null;
    document.querySelectorAll('#semGroup button').forEach(b => b.classList.remove('active'));
    document.getElementById('statsContent').style.display = 'none';
}

function selectSemester(btn, sem) {
    // UI Update
    document.querySelectorAll('#semGroup button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    
    selectedSem = sem;
    
    // Reveal Stats and Load Data
    document.getElementById('statsContent').style.display = 'block';
    loadStats();
}

function loadStats() {
    if (!selectedClass || !selectedSem) return;

    const sub = document.getElementById('subjectSelect').value;

    fetch(`GetStatisticsServlet?class=${selectedClass}&sem=${selectedSem}&subject=${sub}`)
        .then(res => res.json())
        .then(data => {
            // FIX: Check if data exists before setting to avoid "undefined"
            document.getElementById('reattemptCount').innerText = data.reattemptSuccess || 0;

            // Update Subject Dropdown
            const subDropdown = document.getElementById('subjectSelect');
            if (subDropdown.options.length <= 1 && data.subjects) {
                data.subjects.forEach(s => subDropdown.add(new Option(s, s)));
            }

            // Render Charts and Tables
            renderPFChart(data.passCount || 0, data.failCount || 0);
            renderCompareChart(data.comparison || []);
            updateTopperTable(data.subjectToppers || []);
        })
        .catch(err => console.error("Data Fetch Error:", err));
}

// ... (renderPFChart and renderCompareChart functions stay the same)

function renderChart(id, type, labels, data, colors) {
    if (chartInstances[id]) chartInstances[id].destroy();
    const ctx = document.getElementById(id).getContext('2d');
    chartInstances[id] = new Chart(ctx, {
        type: type,
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors,
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } }
        }
    });
}

document.addEventListener('DOMContentLoaded', loadStats);
</script>
</body>
</html>