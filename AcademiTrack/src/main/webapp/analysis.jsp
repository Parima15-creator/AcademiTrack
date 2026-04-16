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
                            <th>Grade Point / Letter</th>
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
                    <tbody id="honorsMarksBody"></tbody>
                </table>
            </section>
        </div>

        <aside class="analytics-sidebar">
            <div class="chart-container">
                <h3>Performance Trend</h3>
                <canvas id="performanceChart"></canvas>
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
// Capture rollNo safely
const rollNo = urlParams.get('roll');

function loadAnalysis(roll, sem) {
    // CRITICAL: Added \ before ${roll} and ${sem} for JSP compatibility
    fetch(`GetStudentAnalysisServlet?roll=\${roll}&sem=\${sem}`)
        .then(response => response.json())
        .then(data => {
            if (data.error) { 
                console.error("Servlet Error:", data.error);
                alert(data.error); 
                return; 
            }

            // Update Profile Info - Added \ escapes
            document.getElementById('disp-name').innerText = data.name;
            document.getElementById('disp-contact').innerHTML = `<strong>Email:</strong> \${data.email} | <strong>Phone:</strong> \${data.phone}`;
            document.getElementById('disp-meta').innerHTML = `<strong>Roll No:</strong> \${data.roll} | <strong>Class:</strong> \${data.class}`;
            
            document.getElementById('disp-sgpa').innerText = (data.sgpa || 0).toFixed(2);
            document.getElementById('disp-cgpa').innerText = (data.cgpa || 0).toFixed(2);

            const regularBody = document.getElementById('studentMarksBody');
            const honorsBody = document.getElementById('honorsMarksBody');
            const hSection = document.getElementById('honorsSection');
            
            regularBody.innerHTML = "";
            if(honorsBody) honorsBody.innerHTML = "";
            if(hSection) hSection.style.display = "none";

            let honorsFound = false;

            data.marks.forEach(m => {
                const total = (parseFloat(m.isa) || 0) + (parseFloat(m.sea) || 0);
                // Escaped all variables in the table rows
                const rowHtml = `
                    <tr>
                        <td>\${m.code}</td>
                        <td>\${m.name}</td>
                        <td>\${m.isa}</td>
                        <td>\${m.sea}</td>
                        <td><strong>\${total.toFixed(1)}</strong></td>
                        <td><span class="gp-badge">\${m.gp}</span> <strong>\${m.letter}</strong></td>
                    </tr>`;

                if (m.is_honors) {
                    if(honorsBody) honorsBody.innerHTML += rowHtml;
                    honorsFound = true;
                } else {
                    regularBody.innerHTML += rowHtml;
                }
            });

            if (honorsFound && hSection) hSection.style.display = "block";
            
            // --- TOP 5 SUBJECTS LOGIC ---
            const topList = document.getElementById('topSubjectsList');
            topList.innerHTML = "";

            const sortedMarks = [...data.marks].sort((a, b) => {
                const totalA = (parseFloat(a.isa) || 0) + (parseFloat(a.sea) || 0);
                const totalB = (parseFloat(b.isa) || 0) + (parseFloat(b.sea) || 0);
                return totalB - totalA; 
            });

            sortedMarks.slice(0, 5).forEach(m => {
                const totalMarks = (parseFloat(m.isa) || 0) + (parseFloat(m.sea) || 0);
                topList.innerHTML += `
                    <li>
                        <span>\${m.name}</span> 
                        <strong>\${totalMarks.toFixed(1)}</strong>
                    </li>`;
            });

            updateChart(data.trend);
        })
        .catch(err => console.error("Fetch error:", err));
}

function updateChart(trendData) {
    const ctx = document.getElementById('performanceChart').getContext('2d');
    
    // Create an array of 4 nulls, then fill in what we have from the Servlet
    // This ensures Sem 3 data doesn't accidentally show up in the Sem 2 slot
    let chartData = [null, null, null, null];
    
    // If your trendData from servlet is just an array of values [9.8, 9.7, 9.85]
    // we map them to the correct index.
    trendData.forEach((val, index) => {
        chartData[index] = val;
    });

    if (myChart) myChart.destroy();
    
    myChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4'],
            datasets: [{
                label: 'SGPA',
                data: chartData,
                borderColor: '#283593',
                backgroundColor: 'rgba(40, 53, 147, 0.1)',
                borderWidth: 3,
                pointBackgroundColor: '#283593',
                pointRadius: 5,
                pointHoverRadius: 7,
                tension: 0.3, // Adds a smooth curve
                fill: true,
                spanGaps: true // Connects the line even if a middle semester is missing
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: false,
                    min: 0,
                    max: 10,
                    ticks: { stepSize: 1 }
                }
            },
            plugins: {
                legend: { display: false } // Cleans up the UI
            }
        }
    });
}

document.querySelectorAll('#semSelector button').forEach(btn => {
    btn.addEventListener('click', function() {
        document.querySelectorAll('#semSelector button').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        // Use the captured rollNo variable
        loadAnalysis(rollNo, this.dataset.sem);
    });
});

window.onload = () => {
    if(rollNo) {
        loadAnalysis(rollNo, "4");
    } else {
        console.error("No roll number found in URL");
    }
};
</script>
</body>
</html>