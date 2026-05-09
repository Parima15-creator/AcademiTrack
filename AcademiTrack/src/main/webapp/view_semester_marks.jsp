<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>AcademiTrack | Semester Results Dashboard</title>
    <link rel="stylesheet" href="view-isa--sea-marks.css">
    <style>
        th { background: var(--primary-blue); color: white; padding: 12px; border: 1px solid #ddd; }
        td { border: 1px solid #ddd; padding: 8px; text-align: center; }
        .row-total { background-color: #eef2ff; font-weight: bold; color: var(--primary-blue); }
        .sub-name-header { display: block; font-size: 0.8em; font-weight: normal; margin-top: 4px; color: #e0e0e0; }
        .mark-input { width: 50px; text-align: center; padding: 4px; border: 1px solid #3498db; border-radius: 4px; }
        .loading-overlay { text-align: center; padding: 40px; color: #666; }
        .header-container { display: flex; align-items: center; justify-content: center; gap: 6px; margin-bottom: 4px; }
    </style>
</head>
<body>

<div class="dashboard-card">
    <div class="header-section">
        <a href="dashboard.jsp" class="back-btn">← Back</a>
        <h2>Semester Results Dashboard</h2>
        <div class="btn-container">
            <!-- This button switches between View and Edit/Save modes -->
            <button id="toggleEditBtn" class="edit-btn" onclick="toggleEditMode()"> Edit Semester Marks</button>
        </div>
    </div>
    
    <!-- Navigation for selecting Class (e.g., SE1) and Semester (1-8) -->
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

    <table id="semTable">
        <thead>
            <tr id="theadRow">
                <th>Roll No</th>
                <th>Name</th>
                <th>Result Type</th>
            </tr>
        </thead>
        <tbody id="tbody">
            <!-- Subject headers will be injected here by JavaScript -->
            <tr><td colspan="10" class="loading-overlay">Select class and semester to load data.</td></tr>
        </tbody>
    </table>
</div>

<script>
// State variable to track if we are currently editing marks
let isEditMode = false;

// The 6 categories of data displayed for every student per subject
const rowTypes = ["ISA", "SEA", "Total", "Credits Earned", "Grade Point", "Grade"];

//Fetches data from the server and builds the table dynamically
function loadSemesterDashboard() {
    // 1. Identify which Class and Semester are currently selected
    const activeClassBtn = document.querySelector('#classGroup .active');
    const activeSemBtn = document.querySelector('#semGroup .active');
    if(!activeClassBtn || !activeSemBtn) return;

    const selectedClass = activeClassBtn.getAttribute('data-code');
    const selectedSem = activeSemBtn.innerText.replace("Semester ", "");

    const tbody = document.querySelector('#tbody');
    const theadRow = document.querySelector('#theadRow');
    
    // Show loading state while waiting for the Servlet
    tbody.innerHTML = "<tr><td colspan='10' class='loading-overlay'>Loading...</td></tr>";

    // 2. Fetch JSON data from the SemesterMarksServlet
    fetch('SemesterMarksServlet?class=' + encodeURIComponent(selectedClass) + '&sem=' + encodeURIComponent(selectedSem))
        .then(res => res.json())
        .then(data => {
            tbody.innerHTML = "";  // Clear the loading message
    
            // Extract and sort subject codes (e.g., [CS1, CS2, CS3])
            const subjects = Object.keys(data.subjectNames || {}).sort();
            
            // 3. Rebuild the Table Header based on the subjects returned
            theadRow.innerHTML = '<th>Roll No</th><th>Name</th><th>Result Type</th>';
            subjects.forEach(subCode => { 
                let fullName = data.subjectNames[subCode] || "";
                theadRow.innerHTML += 
                    '<th>' +
                        '<div class="header-container">' +
                            '<span>' + subCode + '</span>' +
                        '</div>' +
                        '<span class="sub-name-header">' + fullName + '</span>' +
                    '</th>'; 
            });
            
            // Handle case where no students exist for the selection
            if (!data.students || Object.keys(data.students).length === 0) {
                tbody.innerHTML = "<tr><td colspan='10'>No records found.</td></tr>";
                return;
            }
            
            // 4. Build the Table Body
            for (let roll in data.students) {
                let s = data.students[roll];
                
                // For every student, create 6 rows (ISA, SEA, Total, etc.)
                rowTypes.forEach((type, index) => {
                    let tr = document.createElement('tr');
                    tr.className = 'student-row';
                    
                    // These attributes help with the search/filter feature
                    tr.setAttribute('data-student-name', s.name.toLowerCase());
                    tr.setAttribute('data-roll', roll);
                    
                    // If it's the first row of the student's block, add Roll and Name with rowspan=6
                    if (index === 0) {
                        tr.innerHTML += '<td rowspan="6">' + roll + '</td>';
                        tr.innerHTML += '<td rowspan="6">' + s.name + '</td>';
                    }
                    tr.innerHTML += '<td>' + type + '</td>';
                    
                    // 5. Fill in the marks for each subject in this row
                    subjects.forEach(subCode => {
                        let m = s.subjects[subCode] || { isa:0, sea:0, credits:0, gp:0, grade:'-' };
                        let val = 0;
                        
                        // Determine which value to show based on the current row type
                        if (type === "ISA") val = m.isa;
                        else if (type === "SEA") val = m.sea;
                        else if (type === "Total") val = (parseFloat(m.isa) || 0) + (parseFloat(m.sea) || 0);
                        else if (type === "Credits Earned") val = m.credits;
                        else if (type === "Grade Point") val = m.gp;
                        else if (type === "Grade") val = m.grade;
                        
                        // 6. Handle Edit Mode: Replace text with <input> fields
                        // Note: "Total" is always read-only as it's a sum
                        if (isEditMode && type !== "Total") {
                            let inputType = (type === "Grade") ? "text" : "number";
                            tr.innerHTML += '<td><input type="' + inputType + '" class="mark-input" value="' + val + '" data-roll="' + roll + '" data-sub="' + subCode + '" data-type="' + type + '"></td>';
                        } else {
                            // Apply styling for non-editable rows
                            let cellClass = (type === "Grade" || type === "Total") ? "row-total" : "";
                            tr.innerHTML += '<td class="' + cellClass + '">' + val + '</td>';
                        }
                    });
                    tbody.appendChild(tr);
                });
            }
        });
}

//Gathers all data from input fields and sends it to the server via AJAX POST
function saveSemesterMarks() {
    const activeSem = document.querySelector('#semGroup .active').innerText.replace("Semester ", "").trim();
    const newSubHeaders = document.querySelectorAll('th[data-new-code]');
    let newSubjectsList = [];
    newSubHeaders.forEach(th => {
        newSubjectsList.push({
            code: th.getAttribute('data-new-code'),
            name: th.getAttribute('data-new-name')
        });
    });

    // Prepare the JSON object to send to the backend
    let marksData = { semester: activeSem, newSubjects: newSubjectsList, marks: [] };
    
    // Find all input fields and extract their values
    document.querySelectorAll('.mark-input').forEach(input => {
        const type = input.getAttribute('data-type').trim();
        // We only save raw data (ISA, SEA, Credits, etc.). We don't save "Total" as the server can recalculate it.
        if (type !== "Total") {
            marksData.marks.push({ 
                roll: input.getAttribute('data-roll'), 
                sub: input.getAttribute('data-sub'), 
                type: type, 
                val: input.value 
            });
        }
    });

    // Send the data to the UpdateSemesterMarksServlet
    fetch('UpdateSemesterMarksServlet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(marksData)
    })
    .then(res => res.text())
    .then(msg => {
        alert(msg); // Display success/error message from server
        isEditMode = false;  // Exit edit mode
        loadSemesterDashboard(); // Reload to reflect changes
    });
}

//Toggles the UI state between "View" and "Edit"
function toggleEditMode() {
    const btn = document.getElementById("toggleEditBtn");
    if (isEditMode) {
        // If we were in edit mode and button is clicked, it means "Save"
        saveSemesterMarks();
        btn.innerText = "✏️ Edit Semester Marks";
        btn.style.background = "var(--accent-orange)";
    } else {
        // Enter edit mode
        isEditMode = true;
        btn.innerText = "💾 Save Semester Marks";
        btn.style.background = "var(--success-green)";
        loadSemesterDashboard();
    }
}

//Local search function to filter students by name without a server reques
function filterTable() {
    let input = document.getElementById("tableSearch").value.toLowerCase();
    document.querySelectorAll(".student-row").forEach(row => {
        row.style.display = row.getAttribute('data-student-name').includes(input) ? "" : "none";
    });
}

//Event listeners for Class/Semester buttons
document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        this.parentElement.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        loadSemesterDashboard();
    });
});

// Initial load when the page finishes loading
document.addEventListener('DOMContentLoaded', loadSemesterDashboard);
</script>
</body>
</html>