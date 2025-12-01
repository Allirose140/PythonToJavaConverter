Python → Java Converter

A desktop application for importing Python projects, translating Python code into Java, reviewing issues, and generating conversion reports. Built with JavaFX and Gradle.

**Overview**

This tool provides a simplified environment for experimenting with Python-to-Java translation. It is not intended to be a fully correct or comprehensive compiler. Instead, it demonstrates:

- Importing Python files or entire project directories

- Translating basic Python constructs into Java

- Displaying Python and Java code side-by-side

- Detecting unsupported features or simple syntax problems

- Tracking metrics such as lines of code (LOC), warnings, and errors

- Simulating compilation and reporting results

- Generating conversion summary reports

------------------------------------------------------------------------------------------------------------------------------------

**Running the Application**
Requirements

- Java 21 or later


**Commands**

Windows:

.\gradlew run


Mac/Linux:

./gradlew run



**Test Cases**

The following manual test cases verify the application's functional behavior:

TC_01 – Import valid Python project

TC_02 – Import invalid or empty directory

TC_03 – Translate simple Python file

TC_04 – Detect unsupported Python constructs

TC_05 – Verify metrics and LOC counting

TC_06 – Successful compilation

TC_07 – Compilation failure with error list

TC_08 – Generate conversion report

Limitations

Only supports a subset of Python syntax

Generated Java is not fully valid for arbitrary Python input

Compilation is simulated and does not invoke a real Java compiler

No complex type inference or advanced parsing

-----------------------------------------------------------------------------------------
USER MANUAL

## Installation

No installation is required. The application runs directly using the included Gradle wrapper.

### Requirements
- Java 21 or later

### Setup
1. Download or clone the project directory.
2. Verify that Java is installed:
   \`\`\`
   java -version
   \`\`\`

### Running the Application

Navigate to the project folder in your terminal and run:

#### Windows:
\`\`\`
.\gradlew run
\`\`\`

#### macOS/Linux:
\`\`\`
./gradlew run
\`\`\`

Gradle will automatically download dependencies and launch the application.

---

## Usage Instructions

### 1. Import a Python Project
- Click **Import Project**.
- Select a folder containing one or more `.py` files.
- Imported files appear in the Project Explorer on the left.

### 2. View Python Source
- Click any Python file in the Project Explorer to display its contents.

### 3. Translate to Java
- Select a file and click **Translate to Java**.
- The **Preview** tab displays Python (left) and generated Java (right).
- Translation metrics (LOC, warnings, errors) update automatically.

### 4. Review Issues
- Open the **Issues** tab to view:
    - Unsupported constructs (e.g., `eval`, `exec`)
    - Simple syntax problems (e.g., missing colons in loops)
- Each issue includes severity, file name, line number, and message.

### 5. Simulate Compilation
- Click **Compile**.
- If errors exist in the Issues tab, compilation fails.
- If no errors exist, compilation succeeds.
- Detailed messages appear in the **Logs** tab.

### 6. Export a Report
- Click **Export Report**.
- A summary report is generated containing:
    - Metrics (LOC, translated lines, warnings, errors)
    - Issues detected
    - Compilation status

---

## Limitations
- Supports only basic Python syntax.
- Generated Java is not guaranteed to compile for all Python inputs.
- Compilation is simulated and does not invoke a real Java compiler.


