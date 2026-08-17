const API = "/api";


// ========================================
// PAGE LOAD
// ========================================

document.addEventListener("DOMContentLoaded", () => {
    loadStudents();

    // Allow Enter key to add student
    document.getElementById("studentMarks").addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
            addStudent();
        }
    });

    document.getElementById("studentName").addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
            addStudent();
        }
    });
});


// ========================================
// ADD STUDENT
// ========================================

async function addStudent() {

    const name = document.getElementById("studentName").value.trim();
    const marks = document.getElementById("studentMarks").value.trim();

    console.log("1. Button clicked");
    console.log("2. Name:", name);
    console.log("3. Marks:", marks);

    if (name === "") {
        alert("Enter student name");
        return;
    }

    if (marks === "" || Number(marks) < 0 || Number(marks) > 100) {
        alert("Enter marks between 0 and 100");
        return;
    }

    try {

        console.log("4. Sending request to Java...");

        const response = await fetch(`${API}/add`, {
    method: "POST",

    headers: {
        "Content-Type": "application/x-www-form-urlencoded"
    },

    body:
        "name=" + encodeURIComponent(name) +
        "&marks=" + encodeURIComponent(marks)
});

        console.log("5. Status:", response.status);

        const result = await response.text();

        console.log("6. Java response:", result);

        if (!response.ok) {
            alert("Java error:\n" + result);
            return;
        }

        alert("✅ Student added successfully!");

        document.getElementById("studentName").value = "";
        document.getElementById("studentMarks").value = "";

        await loadStudents();

    } catch (error) {

        console.error("7. ERROR:", error);

        alert(
            "❌ Cannot connect to Java backend.\n\n" +
            error.message
        );
    }
}

// ========================================
// LOAD STUDENTS
// ========================================

async function loadStudents() {

    try {

        const response = await fetch(`${API}/students`);


        if (!response.ok) {
            throw new Error(
                `Server returned ${response.status}`
            );
        }


        const students = await response.json();


        displayStudents(students);

        updateStatistics(students);


    } catch (error) {

        console.error("Load students error:", error);

        displayStudents([]);

        updateStatistics([]);
    }
}


// ========================================
// DISPLAY STUDENTS
// ========================================

function displayStudents(students) {

    const table = document.getElementById("studentTable");


    if (!Array.isArray(students) || students.length === 0) {

        table.innerHTML = `
            <tr>
                <td colspan="6" class="empty">

                    <div class="empty-icon">
                        🎓
                    </div>

                    <h3>
                        No students added
                    </h3>

                    <p>
                        Add a student above to start
                        tracking grades.
                    </p>

                </td>
            </tr>
        `;

        return;
    }


    table.innerHTML = "";


    students.forEach((student, index) => {

        const row = document.createElement("tr");


        const grade =
            student.grade ||
            calculateGrade(student.marks);


        const gradeClass =
            getGradeClass(grade);


        let performanceText;


        if (student.marks >= 90) {

            performanceText = "Excellent";

        } else if (student.marks >= 80) {

            performanceText = "Very Good";

        } else if (student.marks >= 70) {

            performanceText = "Good";

        } else if (student.marks >= 60) {

            performanceText = "Average";

        } else if (student.marks >= 50) {

            performanceText = "Needs Work";

        } else {

            performanceText = "Fail";
        }


        row.innerHTML = `

            <td>
                ${index + 1}
            </td>


            <td>
                <strong>
                    ${escapeHTML(student.name)}
                </strong>
            </td>


            <td>
                <strong>
                    ${student.marks}
                </strong>
                / 100
            </td>


            <td>

                <span class="grade ${gradeClass}">
                    ${grade}
                </span>

            </td>


            <td>

                <div class="performance">

                    <div class="progress">

                        <div
                            class="progress-bar"
                            style="width:${student.marks}%">
                        </div>

                    </div>

                    <span>
                        ${performanceText}
                    </span>

                </div>

            </td>


            <td>

                <button
                    class="delete-btn"
                    onclick="deleteStudent(${student.id})">

                    Delete

                </button>

            </td>
        `;


        table.appendChild(row);

    });
}


// ========================================
// DELETE STUDENT
// ========================================

async function deleteStudent(id) {

    if (!confirm("Are you sure you want to delete this student?")) {
        return;
    }


    try {

        const response = await fetch(
            `${API}/delete?id=${id}`,
            {
                method: "DELETE"
            }
        );


        if (!response.ok) {

            throw new Error(
                `Server returned ${response.status}`
            );
        }


        await loadStudents();


    } catch (error) {

        console.error("Delete error:", error);

        alert(
            "Unable to delete student.\n\n" +
            error.message
        );
    }
}


// ========================================
// CLEAR ALL STUDENTS
// ========================================

async function clearStudents() {

    if (!confirm("Delete ALL students?")) {
        return;
    }


    try {

        const response = await fetch(
            `${API}/clear`,
            {
                method: "DELETE"
            }
        );


        if (!response.ok) {

            throw new Error(
                `Server returned ${response.status}`
            );
        }


        await loadStudents();


    } catch (error) {

        console.error("Clear error:", error);

        alert(
            "Unable to clear students.\n\n" +
            error.message
        );
    }
}


// ========================================
// STATISTICS
// ========================================

function updateStatistics(students) {

    const totalElement =
        document.getElementById("totalStudents");

    const averageElement =
        document.getElementById("average");

    const highestElement =
        document.getElementById("highest");

    const lowestElement =
        document.getElementById("lowest");


    const total = students.length;


    totalElement.textContent = total;


    if (total === 0) {

        averageElement.textContent = "0";

        highestElement.textContent = "0";

        lowestElement.textContent = "0";

        return;
    }


    const marks = students.map(
        student => Number(student.marks)
    );


    const totalMarks =
        marks.reduce(
            (sum, mark) => sum + mark,
            0
        );


    const average =
        totalMarks / total;


    const highest =
        Math.max(...marks);


    const lowest =
        Math.min(...marks);


    averageElement.textContent =
        average.toFixed(1);


    highestElement.textContent =
        highest;


    lowestElement.textContent =
        lowest;
}


// ========================================
// GRADE
// ========================================

function calculateGrade(marks) {

    if (marks >= 90) {
        return "A+";
    }

    if (marks >= 80) {
        return "A";
    }

    if (marks >= 70) {
        return "B";
    }

    if (marks >= 60) {
        return "C";
    }

    if (marks >= 50) {
        return "D";
    }

    return "F";
}


// ========================================
// GRADE CSS CLASS
// ========================================

function getGradeClass(grade) {

    switch (grade) {

        case "A+":
            return "grade-a-plus";

        case "A":
            return "grade-a";

        case "B":
            return "grade-b";

        case "C":
            return "grade-c";

        case "D":
            return "grade-d";

        case "F":
            return "grade-f";

        default:
            return "";
    }
}


// ========================================
// SECURITY
// ========================================

function escapeHTML(text) {

    const div =
        document.createElement("div");

    div.textContent = text;

    return div.innerHTML;
}
