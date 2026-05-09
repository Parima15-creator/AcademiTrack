<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AcademiTrack | Student Analysis</title>
    <link rel="stylesheet" href="analysisstyle.css">
    <!-- Imported chart.js library for statistic graph -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>

<div class="container">
    <div class="top-nav">
        <button class="back-btn" onclick="location.href='dashboard.jsp'">⬅ Back to Dashboard</button>
    </div>
    
    <!-- Inside id="disp-name", id="disp-contact", id="disp-meta" JavaScript will inject the student's name, email, roll number, and class here once the data is fetched -->

    <header class="profile-card">
        <div class="student-info">
            <h1 id="disp-name">Loading Student...</h1>
            <p id="disp-contact">Please wait...</p>
            <p id="disp-meta"></p>
        </div>

        <!-- stats-container: Displays two prominent boxes for SGPA and CGPA -->
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
                    <!-- A group of buttons (Sem 1–4). script uses data-sem attribute to know which semester's data to request. -->
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
            
            <!-- This is hidden by default (display:none). It only appears if the student is enrolled in "Honors" courses. -->
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
                    <!-- To display the chart -->
                    <canvas id="performanceChart"></canvas>
                </div>
            </div>
            <div class="top-subjects" style="margin-top:25px;">
                <h3>Top 5 Subjects</h3>
                <!-- An empty <ul> that will be populated with the student’s 5 highest-scoring subjects. -->
                <ul id="topSubjectsList"></ul>
            </div>
        </aside>
    </main>
</div>

<script>
//A global variable to hold the Chart.js instance
let myChart; 
const urlParams = new URLSearchParams(window.location.search);
//Grabs the roll number from the browser's URL
const rollNo = urlParams.get('roll');

//Fetches student data and marks for a specific semester
function loadAnalysis(roll, sem) {
    //fetch(...): Sends a request to a Java Servlet (GetStudentAnalysisServlet) passing the Roll No and Semester.
    //data.error check: If the server sends back an error, it stops execution.
    fetch(`GetStudentAnalysisServlet?roll=\${roll}&sem=\${sem}`)
        .then(response => response.json())
        .then(data => {
            if (data.error) { 
                console.error(data.error); 
                return; 
            }

            // 1. Update Profile Card Information
            //Profile Update: Uses innerText and innerHTML to fill the profile card with the student's personal details from the JSON response.
                       
            document.getElementById('disp-name').innerText = data.name;
            document.getElementById('disp-contact').innerHTML = `<strong>Email:</strong> \${data.email} | <strong>Phone:</strong> \${data.phone}`;
            document.getElementById('disp-meta').innerHTML = `<strong>Roll No:</strong> \${data.roll} | <strong>Class:</strong> \${data.class}`;
            document.getElementById('disp-sgpa').innerText = parseFloat(data.sgpa || 0).toFixed(2);
            document.getElementById('disp-cgpa').innerText = parseFloat(data.cgpa || 0).toFixed(2);

            // 2. Clear and Prepare Tables
            const regularBody = document.getElementById('studentMarksBody');
            const honorsBody = document.getElementById('honorsMarksBody');
            const hSection = document.getElementById('honorsSection');
            
            //Table Clearing: It wipes the current table rows (innerHTML = "") to prepare for new data.
            regularBody.innerHTML = "";
            honorsBody.innerHTML = "";
            hSection.style.display = "none";
            let honorsFound = false;

            // 3. Populate Marks Rows
            //data.marks.forEach(...): Loops through every subject in the marks array:
            //Calculates the total (ISA + SEA).
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
                
                //Checks if m.is_honors is true. If so, it adds the row to the Honors table; otherwise, it goes to the Regular table.
                if (m.is_honors) { 
                    honorsBody.innerHTML += rowHtml; 
                    honorsFound = true; 
                } else { 
                    regularBody.innerHTML += rowHtml; 
                }
            });
            if (honorsFound) hSection.style.display = "block";

            // 4. Update Top 5 Subjects List
            const topList = document.getElementById('topSubjectsList');
            topList.innerHTML = "";
            
            // Sort by total marks descending
            //sortedMarks = [...data.marks].sort((a, b): Creates a copy of the marks and sorts them from highest to lowest score.
            const sortedMarks = [...data.marks].sort((a, b) => {
                const totalA = (parseFloat(a.isa) || 0) + (parseFloat(a.sea) || 0);
                const totalB = (parseFloat(b.isa) || 0) + (parseFloat(b.sea) || 0);
                return totalB - totalA;
            });

            //.slice(0, 5): Takes the top 5 and appends them to the sidebar list.
            sortedMarks.slice(0, 5).forEach(m => {
                const totalMarks = (parseFloat(m.isa) || 0) + (parseFloat(m.sea) || 0);
                topList.innerHTML += `
                    <li>
                        <span>\${m.name}</span> 
                        <strong>\${totalMarks.toFixed(1)}</strong>
                    </li>`;
            });

            // 5. Update Line Chart Trend
            //updateChart(data.trend): Passes the historical SGPA data to the graphing function.
            updateChart(data.trend);
        })
        .catch(err => console.error("Fetch error:", err));
}


//Initializes or updates the Chart.js line graph
function updateChart(trendData) {
    const canvas = document.getElementById('performanceChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    
    // Prepare 4 slots for 4 semesters
    //chartValues: Creates an array of 4 slots. If a student only has data for Sem 1 and 2, slots 3 and 4 remain null.
    let chartValues = [null, null, null, null];
    trendData.forEach((val, i) => { if(i < 4) chartValues[i] = val; });
    
    //myChart.destroy(): Crucial step. If a chart already exists
    //Then it must be deleted before drawing a new one to prevent "ghosting" or overlapping data.
    if (myChart) myChart.destroy();

    //new Chart(...): 
    //type: 'line': Sets the chart type.
    //tension: 0.4: Curves the lines slightly for a modern look.
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
                pointBackgroundColor: '#283593',
                spanGaps: true
            }]
        },
        options: {
    responsive: true,
    maintainAspectRatio: false,
    
    //scales: { y: { min: 0, max: 10 } }: Ensures the Y-axis always shows the standard 0–10 GPA scale.
    scales: {
        y: {
            min: 0,
            max: 10,
            beginAtZero: true,
            afterFit: (scale) => {
                scale.width = 40; // Gives the labels breathing room
            },
            ticks: {
                stepSize: 1,
                autoSkip: false, // Prevents Chart.js from "guessing"
                callback: function(value) {
                    return value; // Explicitly returns every integer
                }
            }
        }
    }
}
    });
}


//Event Listeners for Semester Selection
//semSelector button: Adds a "click" listener to every semester button. When clicked, it:
//Removes the active (highlighted) class from all buttons.
//Adds active to the clicked button.
//Calls loadAnalysis with the new semester number.

window.onload: When the page finishes loading, it automatically triggers loadAnalysis for Semester 4 by default.
document.querySelectorAll('#semSelector button').forEach(btn => {
    btn.addEventListener('click', function() {
        document.querySelectorAll('#semSelector button').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        loadAnalysis(rollNo, this.dataset.sem);
    });
});

/**
 * Initial load on page entry
 */
window.onload = () => { 
    if(rollNo) {
        loadAnalysis(rollNo, "4"); // Defaults to Semester 4
    } else {
        alert("No student selected!");
    }
};
</script>
</body>
</html>