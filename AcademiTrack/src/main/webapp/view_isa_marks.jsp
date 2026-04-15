<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>AcademiTrack | Internal Assessment Dashboard</title>
    <style>
        :root {
            --primary-blue: #2c3e8c;
            --accent-orange: #e67e22;
            --bg-gray: #f4f7f6;
            --white: #ffffff;
            --success-green: #27ae60;
        }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: var(--bg-gray); margin: 0; padding: 20px; }
        .dashboard-card { background: var(--white); max-width: 1400px; margin: 0 auto; padding: 25px; border-radius: 12px; box-shadow: 0 4px 25px rgba(0,0,0,0.1); }
        .header-section { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
        .btn-container { display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
        .back-btn, .edit-btn { color: white; text-decoration: none; padding: 10px 20px; border-radius: 8px; font-weight: 600; border: none; cursor: pointer; transition: 0.3s; }
        .back-btn { background: var(--primary-blue); }
        .edit-btn { background: var(--accent-orange); }
        .nav-container { display: flex; flex-direction: column; gap: 15px; margin-bottom: 20px; align-items: center; }
        .btn-group { display: flex; gap: 8px; flex-wrap: wrap; }
        .filter-btn { background: #e0e0e0; color: #444; border: none; padding: 8px 16px; border-radius: 20px; cursor: pointer; transition: 0.3s; }
        .filter-btn.active { background: var(--primary-blue); color: white; }
        .search-box { width: 100%; max-width: 300px; padding: 10px; border-radius: 20px; border: 1px solid #ccc; margin-bottom: 20px; display: block; margin-left: auto; margin-right: auto; }
        table { width: 100%; border-collapse: collapse; background: white; font-size: 14px; }
        th { background: var(--primary-blue); color: white; padding: 12px; border: 1px solid #ddd; }
        td { border: 1px solid #ddd; padding: 8px; text-align: center; }
        .row-avg { background-color: #fffceb; font-weight: 600; }
        .row-total { background-color: #eef2ff; font-weight: bold; color: var(--primary-blue); }
        .sub-name-header { display: block; font-size: 0.8em; font-weight: normal; margin-top: 4px; color: #e0e0e0; }
        .mark-input { width: 50px; text-align: center; padding: 4px; border: 1px solid #3498db; border-radius: 4px; }
        .loading-overlay { text-align: center; padding: 40px; color: #666; }
    </style>
</head>
<body>

<div class="dashboard-card">
    <div class="header-section">
        <a href="dashboard.jsp" class="back-btn">← Back</a>
        <h2>Internal Assessment Dashboard</h2>
        <div class="btn-container">
            <button id="toggleEditBtn" class="edit-btn" onclick="toggleEditMode()">Edit Marks</button>
        </div>
    </div>

    <div class="nav-container">
        <div class="btn-group" id="classGroup">
            <button class="filter-btn" data-code="FE1">FE Comp 1</button>
            <button class="filter-btn" data-code="FE2">FE Comp 2</button>
            <button class="filter-btn active" data-code="SE1">SE Comp 1</button>
            <button class="filter-btn" data-code="SE2">SE Comp 2</button>
            <button class="filter-btn" data-code="TE1">TE Comp 1</button>
            <button class="filter-btn" data-code="TE2">TE Comp 2</button>
            <button class="filter-btn" data-code="BE1">BE Comp 1</button>
            <button class="filter-btn" data-code="BE2">BE Comp 2</button>
        </div>
        <div class="btn-group" id="semGroup">
            <button class="filter-btn">Semester 1</button>
            <button class="filter-btn">Semester 2</button>
            <button class="filter-btn">Semester 3</button>
            <button class="filter-btn active">Semester 4</button>
            <button class="filter-btn">Semester 5</button>
            <button class="filter-btn">Semester 6</button>
            <button class="filter-btn">Semester 7</button>
            <button class="filter-btn">Semester 8</button>
        </div>
    </div>

    <input type="text" id="tableSearch" class="search-box" placeholder="🔍 Search student name..." onkeyup="filterTable()">

    <table id="isaTable">
        <thead>
            <tr id="theadRow">
                <th>Roll No</th>
                <th>Name</th>
                <th>Assessment Type</th>
            </tr>
        </thead>
        <tbody id="tbody">
            <tr><td colspan="10" class="loading-overlay">Select class and semester to load data.</td></tr>
        </tbody>
    </table>
</div>

<script>
let isEditMode = false;

function calculateBestTwoAvg(m1, m2, m3) {
    let marks = [
        Math.ceil(parseFloat(m1) || 0), 
        Math.ceil(parseFloat(m2) || 0), 
        Math.ceil(parseFloat(m3) || 0)
    ];
    marks.sort((a, b) => b - a); 
    let avg = (marks[0] + marks[1]) / 2;
    return Math.ceil(avg);
}

function loadISADashboard() {
    const activeClassBtn = document.querySelector('#classGroup .active');
    const activeSemBtn = document.querySelector('#semGroup .active');
    if(!activeClassBtn || !activeSemBtn) return;

    const selectedClass = activeClassBtn.getAttribute('data-code');
    const selectedSem = activeSemBtn.innerText.replace("Semester ", "");

    const tbody = document.querySelector('#tbody');
    const theadRow = document.querySelector('#theadRow');
    tbody.innerHTML = "<tr><td colspan='10' class='loading-overlay'>Loading...</td></tr>";

    fetch('ISAServlet?class=' + encodeURIComponent(selectedClass) + '&sem=' + encodeURIComponent(selectedSem))
        .then(res => res.json())
        .then(data => {
            tbody.innerHTML = ""; 
            const subjects = Object.keys(data.subjectNames || {}).sort();
            
            theadRow.innerHTML = '<th>Roll No</th><th>Name</th><th>Assessment Type</th>';
            subjects.forEach(subCode => { 
                let fullName = data.subjectNames[subCode] || "";
                theadRow.innerHTML += '<th><span>' + subCode + '</span><span class="sub-name-header">' + fullName + '</span></th>'; 
            });

            if (!data.students || Object.keys(data.students).length === 0) {
                tbody.innerHTML = "<tr><td colspan='10'>No records found.</td></tr>";
                return;
            }

            for (let roll in data.students) {
                let s = data.students[roll];
                let rowTypes = ["ISA 1", "ISA 2", "ISA 3", "AVG", "ASSIGNMENT", "TOTAL"];
                
                rowTypes.forEach((type, index) => {
                    let tr = document.createElement('tr');
                    tr.className = 'student-row';
                    tr.setAttribute('data-student-name', s.name.toLowerCase());
                    tr.setAttribute('data-roll', roll);

                    if (index === 0) {
                        tr.innerHTML += '<td rowspan="6">' + roll + '</td>';
                        tr.innerHTML += '<td rowspan="6">' + s.name + '</td>';
                    }
                    tr.innerHTML += '<td>' + type + '</td>';
                    
                    subjects.forEach(subCode => {
                        let m = s.subjects[subCode] || { isa1:0, isa2:0, isa3:0, assignment:0 };
                        let val = 0;
                        if (type === "ISA 1") val = m.isa1;
                        else if (type === "ISA 2") val = m.isa2;
                        else if (type === "ISA 3") val = m.isa3;
                        else if (type === "ASSIGNMENT") val = m.assignment;
                        else if (type === "AVG") val = calculateBestTwoAvg(m.isa1, m.isa2, m.isa3);
                        else if (type === "TOTAL") {
                            let bestAvg = parseFloat(calculateBestTwoAvg(m.isa1, m.isa2, m.isa3));
                            val = (bestAvg + (parseFloat(m.assignment) || 0)).toFixed(1);
                        }

                        if (isEditMode && type !== "AVG" && type !== "TOTAL") {
                            tr.innerHTML += '<td><input type="number" step="0.5" class="mark-input" value="' + val + '" data-roll="' + roll + '" data-sub="' + subCode + '" data-type="' + type + '"></td>';
                        } else {
                            let cellClass = (type === "AVG") ? "row-avg" : (type === "TOTAL" ? "row-total" : "");
                            tr.innerHTML += '<td class="' + cellClass + '">' + val + '</td>';
                        }
                    });
                    tbody.appendChild(tr);
                });
            }
        });
}

function saveAllMarks() {
    const activeSem = document.querySelector('#semGroup .active').innerText.replace("Semester ", "");
    let marksData = { semester: activeSem, marks: [] };
    
    document.querySelectorAll('.mark-input').forEach(input => {
        marksData.marks.push({ 
            roll: input.getAttribute('data-roll'), 
            sub: input.getAttribute('data-sub'), 
            type: input.getAttribute('data-type'), 
            val: input.value || 0 
        });
    });

    fetch('UpdateMarksServlet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(marksData)
    })
    .then(res => res.text())
    .then(msg => { 
        alert(msg); 
        isEditMode = false;
        loadISADashboard(); 
    });
}

function toggleEditMode() {
    if (isEditMode) {
        saveAllMarks();
        const btn = document.getElementById("toggleEditBtn");
        btn.innerText = "✏️ Edit Marks";
        btn.style.background = "var(--accent-orange)";
    } else {
        isEditMode = true;
        const btn = document.getElementById("toggleEditBtn");
        btn.innerText = "💾 Save All Changes";
        btn.style.background = "var(--success-green)";
        loadISADashboard();
    }
}

function filterTable() {
    let input = document.getElementById("tableSearch").value.toLowerCase();
    document.querySelectorAll(".student-row").forEach(row => {
        row.style.display = row.getAttribute('data-student-name').includes(input) ? "" : "none";
    });
}

document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        this.parentElement.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        loadISADashboard();
    });
});

document.addEventListener('DOMContentLoaded', loadISADashboard);
</script>
</body>
</html>