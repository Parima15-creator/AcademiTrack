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
        /* Change this in your style section */
        .stat-card.recovery-card, 
            #recoveryMetricContainer, 
            .stat-card[style*="border-left"] { 
                border-left: none !important; 
            }

            /* Polish the failure highlight inside the card */
            #highestFailContainer {
                border-top: 1px dashed #eee;
                margin-top: 15px;
                padding-top: 10px;
            }
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
    
    <div class="class-buttons" id="classGroup">
        <button onclick="selectClass(this, 'FE1')">FE Comp 1</button>
        <button onclick="selectClass(this, 'FE2')">FE Comp 2</button>
        <button onclick="selectClass(this, 'SE1')">SE Comp 1</button>
        <button onclick="selectClass(this, 'SE2')">SE Comp 2</button>
        <button onclick="selectClass(this, 'TE1')">TE Comp 1</button>
        <button onclick="selectClass(this, 'TE2')">TE Comp 2</button>
        <button onclick="selectClass(this, 'BE1')">BE Comp 1</button>
        <button onclick="selectClass(this, 'BE2')">BE Comp 2</button>
    </div>
    
    <div id="semSection" style="display: none; margin-top: 20px;">
        <h3>Select Semester</h3>
        <div class="class-buttons" id="semGroup">
            <% for(int i=1; i<=8; i++) { %>
                <button onclick="selectSemester(this, '<%=i%>')">Sem <%=i%></button>
            <% } %>
        </div>
    </div>

    <div id="statsContent" style="display: none; margin-top: 30px;">

        <div style="margin-bottom: 25px;">
            <select id="subjectSelect" class="action-btn" onchange="loadStats()">
                <option value="">Select Subject to View Toppers</option>
            </select>
        </div>  

        <div class="stat-card full-width" style="margin-bottom: 25px;">
            <h3>Subject Toppers (Top 5)</h3>
            <table class="topper-table">
                <thead>
                    <tr><th>RANK</th><th>NAME</th><th>TOTAL MARKS</th></tr>
                </thead>
                <tbody id="topperBody"></tbody>
            </table>
        </div>


        <div class="stats-grid">
            <div class="stat-card">
            <h3>Overall Class Toppers</h3>
            <div id="classToppersList"> <p style="text-align:center; color:#999;">Loading toppers...</p>
            </div>
        </div>
            <div class="stat-card" style="text-align: center;">
                <h3>Total Appeared</h3>
                <div class="metric-value" id="totalAppeared">0</div>
            </div>
            <div class="stat-card" style="text-align: center;">
                <h3>Passed Students</h3>
                <div class="metric-value" id="passMetric" style="color: #27ae60;">0</div>
            </div>
            <div class="stat-card" style="text-align: center;">
                <h3>Failed Students</h3>
                <div class="metric-value" id="failMetric" style="color: #e74c3c;">0</div>

                <div id="highestFailContainer" style="margin-top: 15px; padding-top: 10px; border-top: 1px solid #eee;">
                    <p style="font-size: 0.9rem; color: #666; margin-bottom: 5px;">Highest Failure in:</p>
                    <div id="toughestSubject" style="font-weight: bold; color: #e74c3c; font-size: 1.1rem;">-</div>
                    <p id="failCountLabel" style="font-size: 0.8rem; color: #888;">0 students failed</p>
                </div>
            </div>
            
            
            <div class="stat-card" style="text-align: center;"> <h3>Backlog Recovery</h3>
                <div class="metric-value" id="recoveryMetric" style="color: #f1c40f;">0</div> <p>Students cleared their previous Backlogs</p>
            </div>
            
            <div class="stat-card" style="text-align: center;">
                <h3>Pass Percentage</h3>
                <div class="metric-value" id="passPercentage">0%</div>
            </div>

            <div class="stat-card">
                <h3>Pass vs. Fail Ratio</h3>
                <div class="chart-container"><canvas id="pfChart"></canvas></div>
            </div>
            <div class="stat-card">
                <h3>Subject-wise Pass %</h3>
                <div class="chart-container"><canvas id="subjectPassChart"></canvas></div>
            </div>
        </div>
    </div>
<script src="statistics_charts.js"></script>
</body>
</html>