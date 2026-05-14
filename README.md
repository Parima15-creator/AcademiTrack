# AcademiTrack - Academic Performance Tracker
AcademiTrack is a Java-based web application designed for faculty members to efficiently track and analyze student academic performance. It allows faculty to manage student records, input internal and semester marks, and visualize performance trends through interactive charts.

## 🚀 Features
- Secure Authentication: Faculty-specific login and registration system.

- Student Management: Full CRUD (Create, Read, Update, Delete) capabilities for student records across different classes (FE, SE, TE, BE).

- Performance Visualization: Interactive graphs and charts powered by Chart.js for student progress analysis.

- Real-time Search: Quick filtering of student lists by name or roll number.

- Session Security: Protected dashboard access ensuring only logged-in faculty can view data.

## 🛠️ Tech Stack
- Frontend: HTML5, CSS3, JavaScript (ES6+).

- Backend: Java Servlets (Jakarta EE).

- Database: MySQL.

- Server: Apache Tomcat 10+.

- Libraries: Chart.js (via CDN), JDBC (MySQL Connector).

📋 Installation & Setup
To run this project locally, follow these steps:

1. Prerequisites
XAMPP (or any MySQL & Apache server).

JDK 17 or higher.

Apache Tomcat 10.1 (to support jakarta.servlet packages).

2. Database Setup
Open phpMyAdmin (localhost/phpmyadmin).

Create a new database named academic_tracker.

Navigate to the Database/ folder in this repository.

Import the academic_tracker.sql file into your newly created database.

3. Project Configuration
Open the project in NetBeans or IntelliJ.

Ensure the MySQL Connector JAR is added to the project libraries.

Check LoginServlet.java and SignupServlet.java to verify the database connection string:

jdbc:mysql://localhost:3306/academic_tracker, user: "root", password: "".

4. Running the Application
Right-click the project in your IDE and select Run.

The application will deploy on Tomcat and open in your default browser at http://localhost:8080/AcademicPerformanceTracker/.

📂 Project Structure
Plaintext
├── AcademiTrack/              # Main Web Project
│   ├── src/java/              # Java Servlets & Logic
│   └── web/                   # HTML, JSP, CSS, and JS files
├── Database/                  # SQL Database Export
├── README.md                  # Project Documentation
└── .gitignore                 # Files to exclude from Git
✒️ Author
[Your Name] - Initial Work - [Your GitHub Profile Link]
