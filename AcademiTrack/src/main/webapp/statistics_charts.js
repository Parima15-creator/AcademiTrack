/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/JavaScript.js to edit this template
 */

let passFailChart, gradeDistChart;

function loadStatistics() {
    const activeClass = document.querySelector('#classGroup .active').getAttribute('data-code');
    const activeSem = "4"; // You can make this dynamic like your dashboard

    fetch(`GetStatisticsServlet?class=${activeClass}&sem=${activeSem}`)
        .then(res => res.json())
        .then(data => {
            updatePodium(data.toppers);
            renderCharts(data);
        });
}

function updatePodium(toppers) {
    if (toppers && toppers.length > 0) {
        document.getElementById('topper1').innerText = toppers[0].name;
        document.getElementById('gpa1').innerText = "GPA: " + toppers[0].gpa;
        // Repeat for topper 2 and 3...
    }
}

function renderCharts(data) {
    // 1. Pass/Fail Pie Chart
    const ctx1 = document.getElementById('passFailChart').getContext('2d');
    if(passFailChart) passFailChart.destroy();
    passFailChart = new Chart(ctx1, {
        type: 'pie',
        data: {
            labels: ['Passed', 'Failed'],
            datasets: [{
                data: [data.passCount, data.failCount],
                backgroundColor: ['#27ae60', '#e74c3c']
            }]
        }
    });
}

document.addEventListener('DOMContentLoaded', loadStatistics);
