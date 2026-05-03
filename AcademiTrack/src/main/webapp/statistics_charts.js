let selectedClass = null;
let selectedSem = null;
let chartInstances = {};

/**
 * Step 1: Handle Class Selection
 */
function selectClass(btn, cls) {
    // UI Update: Highlight active button
    document.querySelectorAll('#classGroup button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    
    selectedClass = cls;
    
    // Show Semester section and hide previous stats
    document.getElementById('semSection').style.display = 'block';
    document.getElementById('statsContent').style.display = 'none';
    
    // Reset semester selection
    selectedSem = null;
    document.querySelectorAll('#semGroup button').forEach(b => b.classList.remove('active'));
}

/**
 * Step 2: Handle Semester Selection
 */
function selectSemester(btn, sem) {
    document.querySelectorAll('#semGroup button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    
    selectedSem = sem;
    
    // Reveal the stats dashboard
    document.getElementById('statsContent').style.display = 'block';
    
    // Reset dropdown and table for new context
    document.getElementById('subjectSelect').innerHTML = '<option value="">-- View Toppers for Subject --</option>';
    document.getElementById('topperBody').innerHTML = "<tr><td colspan='3' style='text-align:center;'>Select a subject to see toppers</td></tr>";
    
    loadStats();
}

/**
 * Step 3: Fetch and Render Data
 */
function loadStats() {
    if (!selectedClass || !selectedSem) return;

    const sub = document.getElementById('subjectSelect').value;

    // Fetching data from the Servlet
    fetch(`GetStatisticsServlet?class=${selectedClass}&sem=${selectedSem}&subject=${sub}`)
        .then(res => res.json())
        .then(data => {
            console.log("Dashboard Data Received:", data);

            // 1. Update Numeric Metrics
            document.getElementById('passMetric').innerText = data.passCount || 0;
            document.getElementById('failMetric').innerText = data.failCount || 0;
            document.getElementById('reattemptCount').innerText = data.reattemptSuccess || 0;

            // 2. Populate Subject Dropdown (if empty)
            // --- 2. POPULATE SUBJECT DROPDOWN ---
            const subDropdown = document.getElementById('subjectSelect');

            // Only populate if it's currently empty
            if (subDropdown.options.length <= 1 && data.subjects) {
                data.subjects.forEach(s => {
                    // Display format: "CMP 202 - Data Structures"
                    let displayText = `${s.code} - ${s.name}`;
                    let opt = new Option(displayText, s.code);

                    // CSS Fix: Ensure text is dark and visible
                    opt.style.color = "#2c4aa5"; 
                    opt.style.background = "white";

                    subDropdown.add(opt);
                });
            }
            // 3. Render Pass/Fail Doughnut Chart
            renderChart('pfChart', 'doughnut', ['Passed', 'Failed'], 
                        [data.passCount, data.failCount], ['#27ae60', '#e74c3c']);
            
            // 4. Render GPA Comparison Bar Chart
            const compLabels = data.comparison ? data.comparison.map(c => c.class) : [];
            const compGPAs = data.comparison ? data.comparison.map(c => c.avgGpa) : [];
            renderChart('compareChart', 'bar', compLabels, compGPAs, '#4e73df');

            // 5. Update Toppers Table
            const tBody = document.getElementById('topperBody');
            tBody.innerHTML = ""; 
            
            if (data.subjectToppers && data.subjectToppers.length > 0) {
                data.subjectToppers.forEach((t, i) => {
                    tBody.innerHTML += `
                        <tr>
                            <td><strong>#${i + 1}</strong></td>
                            <td>${t.name}</td>
                            <td><span class="score-badge">${t.score.toFixed(1)}</span></td>
                        </tr>`;
                });
            } else {
                tBody.innerHTML = "<tr><td colspan='3' style='text-align:center;'>Select a subject to see toppers</td></tr>";
            }
        })
        .catch(err => console.error("Fetch Error:", err));
}

/**
 * Helper: Chart.js Wrapper
 */
function renderChart(id, type, labels, data, colors) {
    // Prevent overlapping charts by destroying old instance
    if (chartInstances[id]) chartInstances[id].destroy();
    
    const ctx = document.getElementById(id).getContext('2d');
    chartInstances[id] = new Chart(ctx, {
        type: type,
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors,
                borderWidth: 1
            }]
        },
        options: { 
            responsive: true, 
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } }
        }
    });
}