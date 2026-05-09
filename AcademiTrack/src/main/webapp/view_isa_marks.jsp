<!-- Frontend (JSP/HTML/JS): Handles the user interface, filtering, and "Best of Two" calculations.
    Backend (Java Servlets): ISAServlet fetches marks from the database, and UpdateMarksServlet saves them back.
    Communication: Uses the fetch API to send and receive JSON data without refreshing the page. -->

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>AcademiTrack | Internal Assessment Dashboard</title>
    <link rel="stylesheet" href="view-isa--sea-marks.css">
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

//Takes best 2 IT marks and calculates average
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


//When a class or semester is selected, this function in invoked
function loadISADashboard() {
    
    //It looks for the buttons currently highlighted (class active).
    const activeClassBtn = document.querySelector('#classGroup .active');
    const activeSemBtn = document.querySelector('#semGroup .active');
    if(!activeClassBtn || !activeSemBtn) return;
    
    //It cleans the data (e.g., changing "Semester 4" to just "4").
    const selectedClass = activeClassBtn.getAttribute('data-code');
    const selectedSem = activeSemBtn.innerText.replace("Semester ", "");

    const tbody = document.querySelector('#tbody');
    const theadRow = document.querySelector('#theadRow');
    tbody.innerHTML = "<tr><td colspan='10' class='loading-overlay'>Loading...</td></tr>";

    //fetch used to call the ISAServlet. encodeURIComponent ensures that special characters in the class name don't break the URL.
    fetch('ISAServlet?class=' + encodeURIComponent(selectedClass) + '&sem=' + encodeURIComponent(selectedSem))
        .then(res => res.json())
        .then(data => {
            tbody.innerHTML = ""; 
    
            //Instead of a static table
            //the code waits to see what subjects the server sends (e.g., Maths, Physics).
            const subjects = Object.keys(data.subjectNames || {}).sort();
            
            theadRow.innerHTML = '<th>Roll No</th><th>Name</th><th>Assessment Type</th>';
            
            //It loops through the subjects array and adds a new <th> for each one.
            subjects.forEach(subCode => { 
                let fullName = data.subjectNames[subCode] || "";
                theadRow.innerHTML += '<th><span>' + subCode + '</span><span class="sub-name-header">' + fullName + '</span></th>'; 
            });

            if (!data.students || Object.keys(data.students).length === 0) {
                tbody.innerHTML = "<tr><td colspan='10'>No records found.</td></tr>";
                return;
            }
            
            //For every single student, the code creates 6 distinct table rows (tr).
            for (let roll in data.students) {
                let s = data.students[roll];
                let rowTypes = ["ISA 1", "ISA 2", "ISA 3", "AVG", "ASSIGNMENT", "TOTAL"];
                
                //Since we don't want to repeat the Roll No and Name 6 times, 
                //the code checks if (index === 0). 
                //If it's the first row (ISA 1)
                //it adds the Roll No and Name with rowspan="6", which stretches those cells downward.
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
                    
                    //It checks the type. If the row is "ISA 1"
                    //it shows the isa1 value. If it is "AVG"
                    //it calls the calculateBestTwoAvg function.
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
                        
                        //If Edit Mode is ON: It turns the cell into an <input type="number"> so you can type. 
                        if (isEditMode && type !== "AVG" && type !== "TOTAL") {
                            tr.innerHTML += '<td><input type="number" step="0.5" class="mark-input" value="' + val + '" data-roll="' + roll + '" data-sub="' + subCode + '" data-type="' + type + '"></td>';
                        } 
                        //If Edit Mode is OFF: It just displays the number as plain text.
                        else {
                            let cellClass = (type === "AVG") ? "row-avg" : (type === "TOTAL" ? "row-total" : "");
                            tr.innerHTML += '<td class="' + cellClass + '">' + val + '</td>';
                        }
                    });
                    tbody.appendChild(tr);
                });
            }
        });
}

//It creates a JSON object.
//It finds every input box on the screen (class .mark-input).
//It reads the custom data- attributes to know exactly which student and which subject that specific box belongs to.
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

//Unlike the "GET" request used to load data, this uses POST.
//It sends the entire list of marks as a JSON string in the "body" of the request.
//Once finished, it shows an alert, turns off Edit Mode, and reloads the dashboard to show the fresh, calculated totals.
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


//Button is Orange, text says "Edit Marks", table is plain text.
///Button is Green, text says "Save All Changes", table is full of input boxes.
function toggleEditMode() {
    if (isEditMode) {
        saveAllMarks();
        const btn = document.getElementById("toggleEditBtn");
        btn.innerText = "✏️ Edit Marks";
        // If we were editing, save now
        // Change button back to "Edit" style
        btn.style.background = "var(--accent-orange)";
    } else {
        isEditMode = true;
        const btn = document.getElementById("toggleEditBtn");
        btn.innerText = "💾 Save All Changes";
        // Turn on editing
        // Change button to "Save" style (Green)
        btn.style.background = "var(--success-green)";
        loadISADashboard();
    }
}

//Instead of searching the database again, this uses CSS for speed:
//It looks at a hidden attribute (data-student-name) on each row and toggles its visibility.
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