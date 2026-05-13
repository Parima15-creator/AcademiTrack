let selectedClass = null;
let selectedSem = null;
let chartInstances = {};

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
    
    // Clear dropdown so it repopulates
    document.getElementById('subjectSelect').innerHTML = '<option value="">Select Subject to View Toppers</option>';
    loadStats();
}

/**
 * Fetches and renders all statistical data for the selected Class and Semester.
 */
function loadStats() {
    if (!selectedClass || !selectedSem) return;
    
    const subSelect = document.getElementById('subjectSelect');
    const sub = subSelect.value;

    fetch(`GetStatisticsServlet?class=${selectedClass}&sem=${selectedSem}&subject=${sub}`)
        .then(res => res.json())
        .then(data => {
            // 1. Basic Numeric Metrics
            document.getElementById('totalAppeared').innerText = data.totalAppeared || 0;
            document.getElementById('passMetric').innerText = data.passCount || 0;
            document.getElementById('failMetric').innerText = data.failCount || 0;
            
            // Update the Recovery Metric
            document.getElementById('recoveryMetric').innerText = data.recoveredCount || 0;
            
            let passPercent = data.totalAppeared > 0 ? ((data.passCount / data.totalAppeared) * 100).toFixed(1) : 0;
            document.getElementById('passPercentage').innerText = passPercent + "%";

            // 2. Highest Failure Detail
            const subLabel = document.getElementById('toughestSubject');
            const countLabel = document.getElementById('failCountLabel');

            if (data.highestFailCount > 0) {
                subLabel.innerText = data.toughestSubject;
                countLabel.innerText = data.highestFailCount + " student(s) failed this subject";
            } else {
                subLabel.innerText = "None (All Passed)";
                countLabel.innerText = "0 students failed";
            }

            // 3. Subject Dropdown
            if (subSelect.options.length <= 1 && data.subjects) {
                data.subjects.forEach(s => {
                    subSelect.add(new Option(`${s.code} - ${s.name}`, s.code));
                });
            }

            // 4. Render 3-Way Status Chart (Doughnut)
            const statusLabels = ['Passed', 'Failed', 'Recovered'];
            const statusData = [data.passCount, data.failCount, data.recoveredCount || 0];
            const statusColors = ['#27ae60', '#e74c3c', '#f1c40f']; // Green, Red, Yellow
            
            renderChart('pfChart', 'doughnut', statusLabels, statusData, statusColors, 'Students');
            
            // 5. Render Subject-wise Pass % (Bar)
            const subLabels = data.subjectStats ? data.subjectStats.map(s => s.code) : [];
            const subPassRates = data.subjectStats ? data.subjectStats.map(s => s.passRate) : [];
            renderChart('subjectPassChart', 'bar', subLabels, subPassRates, '#3498db', 'Pass %');

            // 6. Update Toppers Table
            updateToppersTable(data.subjectToppers);
        })
        .catch(err => console.error("Fetch Error:", err));
}

function updateToppersTable(toppers) {
    const tBody = document.getElementById('topperBody');
    tBody.innerHTML = (toppers && toppers.length > 0) 
        ? toppers.map((t, i) => `<tr><td><strong>#${i+1}</strong></td><td>${t.name}</td><td><span class="score-badge">${t.score.toFixed(1)}</span></td></tr>`).join('')
        : "<tr><td colspan='3' style='text-align:center;'>Select a subject to see toppers</td></tr>";
}

function renderChart(id, type, labels, data, colors, labelName) {
    if (chartInstances[id]) chartInstances[id].destroy();
    const ctx = document.getElementById(id).getContext('2d');
    
    // Determine the max scale: 100 for percentage charts, auto for count charts
    const yAxesConfig = (type === 'bar' && labelName.includes('%')) 
                        ? { beginAtZero: true, max: 100 } 
                        : { beginAtZero: true };

    chartInstances[id] = new Chart(ctx, {
        type: type,
        data: {
            labels: labels,
            datasets: [{ 
                label: labelName, 
                data: data, 
                backgroundColor: colors, 
                borderWidth: 1 
            }]
        },
        options: { 
            responsive: true, 
            maintainAspectRatio: false,
            scales: type === 'bar' ? { y: yAxesConfig } : {},
            plugins: { 
                legend: { 
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        padding: 20
                    }
                } 
            }
        }
    });
}