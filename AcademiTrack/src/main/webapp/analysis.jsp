<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AcademiTrack | Student Analysis</title>
    <link rel="stylesheet" href="analysisstyle.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>

<div class="container">
    <div class="top-nav">
        <button class="back-btn" onclick="location.href='dashboard.jsp'">⬅ Back to Dashboard</button>
    </div>

    <header class="profile-card">
        <div class="student-info">
            <h1 id="disp-name">Loading Student...</h1>
            <p id="disp-contact">Please wait...</p>
            <p id="disp-meta"></p>
        </div>

        <div class="stats-container">
            <div class="stat-box sgpa-box">
                <span class="label">Semester SGPA</span>
                <span class="score" id="disp-sgpa">0.00</span>
            </div>
            <div class="stat-box cgpa-box">
                <span class="label">Overall CGPA</span>
                <span class="score" id="disp-cgpa">0.00</span>
            </div>
        </div>
    </header>

    <main class="dashboard-grid">
        <div class="main-content">
            <section class="marks-section">
                <div class="section-header">
                    <h3>Semester Performance</h3>
                    <div class="sem-selector" id="semSelector">
                        <button data-sem="1">Sem 1</button>
                        <button data-sem="2">Sem 2</button>
                        <button data-sem="3">Sem 3</button>
                        <button data-sem="4" class="active">Sem 4</button>
                    </div>
                </div>
                <table class="marks-table">
                    <thead>
                        <tr>
                            <th>Subject Code</th>
                            <th>Subject Name</th>
                            <th>ISA</th> <th>SEA</th> <th>Total</th>
                            <th>Grade Point</th>
                            <th>Grade</th>
                        </tr>
                    </thead>
                    <tbody id="studentMarksBody"></tbody>
                </table>
            </section>

            <section class="marks-section honors-section" id="honorsSection" style="display:none; margin-top:30px;">
                <div class="section-header">
                    <h3 style="color: #6a1b9a;">Honours Performance</h3>
                </div>
                <table class="marks-table honors-table">
                    <thead>
                         <tr>
                            <th>Subject Code</th>
                            <th>Subject Name</th>
                            <th>ISA</th> <th>SEA</th> <th>Total</th>
                            <th>Grade Point</th>
                            <th>Grade</th>
                        </tr>
                    </thead>
                    <tbody id="honorsMarksBody"></tbody>
                </table>
            </section>
        </div>

        <aside class="analytics-sidebar">
            <div class="chart-container">
                <h3>Performance Trend</h3>
                <div class="canvas-wrapper">
                    <canvas id="performanceChart"></canvas>
                </div>
            </div>
            <div class="top-subjects" style="margin-top:25px;">
                <h3>Top 5 Subjects</h3>
                <ul id="topSubjectsList"></ul>
            </div>
        </aside>
    </main>
</div>

<script>
let myChart; 
const urlParams = new URLSearchParams(window.location.search);
const rollNo = urlParams.get('roll');

function loadAnalysis(roll, sem) {
    fetch(`GetStudentAnalysisServlet?roll=\${roll}&sem=\${sem}`)
        .then(response => response.json())
        .then(data => {
            if (data.error) { console.error(data.error); return; }

            // 1. Update Profile Card
            document.getElementById('disp-name').innerText = data.name;
            document.getElementById('disp-contact').innerHTML = `<strong>Email:</strong> \${data.email} | <strong>Phone:</strong> \${data.phone}`;
            document.getElementById('disp-meta').innerHTML = `<strong>Roll No:</strong> \${data.roll} | <strong>Class:</strong> \${data.class}`;
            document.getElementById('disp-sgpa').innerText = parseFloat(data.sgpa || 0).toFixed(2);
            document.getElementById('disp-cgpa').innerText = parseFloat(data.cgpa || 0).toFixed(2);

            // 2. Prepare Tables
            const regularBody = document.getElementById('studentMarksBody');
            const honorsBody = document.getElementById('honorsMarksBody');
            const hSection = document.getElementById('honorsSection');
            
            regularBody.innerHTML = "";
            honorsBody.innerHTML = "";
            hSection.style.display = "none";
            let honorsFound = false;

            // 3. Populate Marks
            // 3. Populate Marks
data.marks.forEach(m => {
    const total = (parseFloat(m.isa) || 0) + (parseFloat(m.sea) || 0);
    const rowHtml = `
        <tr>
            <td>\${m.code}</td>
            <td>\${m.name}</td>
            <td>\${m.isa}</td>
            <td>\${m.sea}</td>
            <td><strong>\${total.toFixed(1)}</strong></td>
            <td><span class="gp-badge">\${m.gp}</span></td> 
            <td><strong style="color:var(--primary-blue)">\${m.letter}</strong></td>
        </tr>`;
    
    if (m.is_honors) { 
        honorsBody.innerHTML += rowHtml; 
        honorsFound = true; 
    } else { 
        regularBody.innerHTML += rowHtml; 
    }
});
            if (honorsFound) hSection.style.display = "block";

            // 4. FIX: TOP 5 SUBJECTS LOGIC (Added this back)
            const topList = document.getElementById('topSubjectsList');
            topList.innerHTML = "";
            
            // Sort marks by total (ISA + SEA) descending
            const sortedMarks = [...data.marks].sort((a, b) => {
                const totalA = (parseFloat(a.isa) || 0) + (parseFloat(a.sea) || 0);
                const totalB = (parseFloat(b.isa) || 0) + (parseFloat(b.sea) || 0);
                return totalB - totalA;
            });

            // Take top 5 and display
            sortedMarks.slice(0, 5).forEach(m => {
                const totalMarks = (parseFloat(m.isa) || 0) + (parseFloat(m.sea) || 0);
                topList.innerHTML += `
                    <li>
                        <span>\${m.name}</span> 
                        <strong>\${totalMarks.toFixed(1)}</strong>
                    </li>`;
            });

            // 5. Update Chart
            updateChart(data.trend);
        })
        .catch(err => console.error("Fetch error:", err));
}

function updateChart(trendData) {
    const canvas = document.getElementById('performanceChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    
    let chartValues = [null, null, null, null];
    trendData.forEach((val, i) => { if(i < 4) chartValues[i] = val; });

    if (myChart) myChart.destroy();
    myChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4'],
            datasets: [{
                label: 'SGPA',
                data: chartValues,
                borderColor: '#283593',
                backgroundColor: 'rgba(40, 53, 147, 0.1)',
                tension: 0.4,
                fill: true,
                spanGaps: true
            }]
        },
        options: { 
            responsive: true, 
            maintainAspectRatio: false, 
            scales: { y: { min: 0, max: 10 } } 
        }
    });
}

document.querySelectorAll('#semSelector button').forEach(btn => {
    btn.addEventListener('click', function() {
        document.querySelectorAll('#semSelector button').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        loadAnalysis(rollNo, this.dataset.sem);
    });
});

window.onload = () => { if(rollNo) loadAnalysis(rollNo, "4"); };
</script>
</body>
</html>