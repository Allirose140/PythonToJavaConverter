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