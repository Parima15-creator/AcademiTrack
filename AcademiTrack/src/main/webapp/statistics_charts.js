let currentClass = null;
let pfChart, compareChart;

// Function called when a class button is clicked
function selectClass(btn, cls) {
    // 1. Update UI: remove active class from all, add to clicked
    document.querySelectorAll('.class-buttons button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    
    currentClass = cls;
    loadStats(); // Trigger fetch
}

// Function called when Semester dropdown changes
function changeSemester() {
    if (currentClass) {
        loadStats();
    }
}

function loadStats() {
    const sem = document.getElementById('semSelect').value;
    const sub = document.getElementById('subjectSelect').value || "";

    // Fetch data from Servlet
    fetch(`GetStatisticsServlet?class=${currentClass}&sem=${sem}&subject=${sub}`)
        .then(res => res.json())
        .then(data => {
            // Update Reattempt Metric
            document.getElementById('reattemptVal').innerText = data.reattemptSuccess || 0;

            // Render Pass/Fail Donut
            renderPFChart(data.passCount, data.failCount);

            // Render Class Comparison (e.g., SE1 vs SE2)
            renderCompareChart(data.comparison);

            // Update Subject Dropdown (if subjects exist for this sem)
            updateSubjectDropdown(data.subjects);
            
            // Update Toppers Table
            updateToppersTable(data.subjectToppers);
        })
        .catch(err => console.error("Error loading stats:", err));
}

function renderPFChart(pass, fail) {
    const ctx = document.getElementById('passFailChart').getContext('2d');
    if (pfChart) pfChart.destroy();
    pfChart = new Chart(ctx, {
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

function renderCompareChart(compData) {
    const ctx = document.getElementById('compareChart').getContext('2d');
    if (compareChart) compareChart.destroy();
    
    compareChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: compData.map(d => d.class),
            datasets: [{
                label: 'Average GPA',
                data: compData.map(d => d.avgGpa),
                backgroundColor: '#3498db'
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}