<%@page contentType="text/html" pageEncoding="UTF-8"%>
<% if (session.getAttribute("facultyName") == null) { response.sendRedirect("index.html"); } %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>AcademiTrack | Performance Analytics</title>
    <link rel="stylesheet" href="dashboardstyle.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(450px, 1fr)); gap: 25px; margin-top: 20px; }
        .stat-card { background: white; padding: 25px; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.05); }
        .full-width { grid-column: 1 / -1; }
        .chart-container { position: relative; height: 300px; width: 100%; }
        .metric-value { font-size: 4rem; font-weight: 800; color: #2c4aa5; margin: 20px 0; }
        .score-badge { background: #eef2ff; color: #2c4aa5; padding: 5px 15px; border-radius: 20px; font-weight: bold; }
        .topper-table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        .topper-table th { text-align: left; padding: 12px; border-bottom: 2px solid #eee; color: #666; }
        .topper-table td { padding: 15px 12px; border-bottom: 1px solid #f9f9f9; }
    </style>
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
    <div class="user-welcome"><span>Welcome, <strong>${facultyName}</strong> 👋</span></div>
    <h1>Performance Analytics</h1>

    <!-- Selection Steps -->
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
        <div style="margin-bottom: 20px;">
            <select id="subjectSelect" class="action-btn" style="background: white; color: #2c4aa5; padding: 10px; border-radius: 8px;" onchange="loadStats()">
                <option value="">-- View Toppers for Subject --</option>
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
                <h3>Class Comparison (Average GPA)</h3>
                <div class="chart-container"><canvas id="compareChart"></canvas></div>
            </div>
            <div class="stat-card full-width">
                <h3>Subject Toppers (Top 5)</h3>
                <table class="topper-table">
                    <thead><tr><th>Rank</th><th>Name</th><th>Total Marks</th></tr></thead>
                    <tbody id="topperBody"></tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script>
let selectedClass = null, selectedSem = null, chartInstances = {};

function selectClass(btn, cls) {
    document.querySelectorAll('#classGroup button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    selectedClass = cls;
    document.getElementById('semSection').style.display = 'block';
    document.getElementById('statsContent').style.display = 'none';
}

function selectSemester(btn, sem) {
    document.querySelectorAll('#semGroup button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    selectedSem = sem;
    document.getElementById('statsContent').style.display = 'block';
    document.getElementById('subjectSelect').innerHTML = '<option value="">-- View Toppers for Subject --</option>';
    loadStats();
}

function loadStats() {
    if (!selectedClass || !selectedSem) return;
    const sub = document.getElementById('subjectSelect').value;

    fetch(`GetStatisticsServlet?class=${selectedClass}&sem=${selectedSem}&subject=${sub}`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('reattemptCount').innerText = data.reattemptSuccess || 0;

            const subDropdown = document.getElementById('subjectSelect');
            if (subDropdown.options.length <= 1 && data.subjects) {
                data.subjects.forEach(s => subDropdown.add(new Option(s, s)));
            }

            renderChart('pfChart', 'doughnut', ['Passed', 'Failed'], [data.passCount, data.failCount], ['#27ae60', '#e74c3c']);
            
            const compLabels = data.comparison ? data.comparison.map(c => c.class) : [];
            const compGPAs = data.comparison ? data.comparison.map(c => c.avgGpa) : [];
            renderChart('compareChart', 'bar', compLabels, compGPAs, '#4e73df');

            const tBody = document.getElementById('topperBody');
            tBody.innerHTML = "";
            if (data.subjectToppers && data.subjectToppers.length > 0) {
                data.subjectToppers.forEach((t, i) => {
                    tBody.innerHTML += `<tr><td>#${i+1}</td><td>${t.name}</td><td><span class="score-badge">${t.score.toFixed(1)}</span></td></tr>`;
                });
            } else {
                tBody.innerHTML = "<tr><td colspan='3' style='text-align:center;'>Select a subject to see toppers</td></tr>";
            }
        });
}

function renderChart(id, type, labels, data, colors) {
    if (chartInstances[id]) chartInstances[id].destroy();
    const ctx = document.getElementById(id).getContext('2d');
    chartInstances[id] = new Chart(ctx, {
        type: type,
        data: { labels: labels, datasets: [{ data: data, backgroundColor: colors, borderWidth: 0 }] },
        options: { 
            responsive: true, 
            maintainAspectRatio: false,
            scales: type === 'bar' ? { y: { beginAtZero: true, max: 10, ticks: { stepSize: 1 } } } : {}
        }
    });
}
</script>
</body>
</html>