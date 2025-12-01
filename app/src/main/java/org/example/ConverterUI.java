// JavaFX UI for a minimal Python → Java converter.
package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConverterUI extends Application {

    // State
    private final TreeView<String> projectTree = new TreeView<>();
    private final TextArea logArea   = new TextArea();
    private final TextArea pyPreview    = new TextArea();
    private final TextArea javaPreview  = new TextArea();
    private final TableView<IssueRow> issuesTable = new TableView<>();
    private final Label statusLabel = new Label("Ready.");
    private final Label counters     = new Label("LOC: 0 | Translated: 0 | Warnings: 0 | Errors: 0");
    private final ProgressBar progress = new ProgressBar(0);

    private File importedDir;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Python → Java Converter");

        // Toolbar
        ToolBar toolbar = new ToolBar();
        Button importBtn = new Button("Import Project");
        Button optionsBtn = new Button("Options");
        Button translateBtn = new Button("Translate to Java");
        Button compileBtn = new Button("Compile");
        Button exportBtn = new Button("Export Report");
        toolbar.getItems().addAll(importBtn, optionsBtn, translateBtn, compileBtn, exportBtn);

        importBtn.setOnAction(e -> onImport(stage));
        optionsBtn.setOnAction(e -> showOptions());
        translateBtn.setOnAction(e -> onTranslate());
        compileBtn.setOnAction(e -> onCompile());
        exportBtn.setOnAction(e -> onExport());

        // Left explorer
        projectTree.setShowRoot(false);
        projectTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
            // When a file is clicked, show its Python source (if we can read it)
            if (sel != null && importedDir != null && sel.getChildren().isEmpty()) {
                File f = new File(importedDir, sel.getValue());
                if (f.isFile()) {
                    pyPreview.setText(readFileSafe(f));
                }
            }
        });

        VBox left = new VBox(new Label("Project Explorer"), projectTree);
        left.setPadding(new Insets(8));
        left.setSpacing(6);
        left.setPrefWidth(260);
        VBox.setVgrow(projectTree, Priority.ALWAYS);

        // Tabs
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(new Tab("Dashboard", buildDashboard()));
        tabs.getTabs().add(new Tab("Issues", buildIssues()));
        tabs.getTabs().add(new Tab("Preview", buildPreview()));
        tabs.getTabs().add(new Tab("Logs", buildLogs()));

        // Root layout
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setLeft(left);
        root.setCenter(tabs);
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1100, 700);
        stage.setScene(scene);
        stage.show();
    }

    private Node buildStatusBar() {
        HBox box = new HBox(10, statusLabel, progress, counters);
        HBox.setHgrow(progress, Priority.ALWAYS);
        box.setPadding(new Insets(6));
        return box;
    }

    private Node buildDashboard() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.getChildren().addAll(
                new Label("Overview"),
                new Label("• Imported project: (none)"),
                new Label("• Generated Java files: 0"),
                new Label("• Last compile: n/a"),
                new Separator(),
                new Label("Follow the steps: Import → Translate → Compile"),
                new Label("This dashboard will show metrics after translation/compile.")
        );
        return box;
    }

    private Node buildLogs() {
        logArea.setEditable(false);
        logArea.setWrapText(true);
        return logArea;
    }

    private Node buildIssues() {
        TableColumn<IssueRow, String> sev  = new TableColumn<>("Severity");
        sev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        sev.setPrefWidth(80);

        TableColumn<IssueRow, String> file = new TableColumn<>("File");
        file.setCellValueFactory(new PropertyValueFactory<>("file"));
        file.setPrefWidth(150);

        TableColumn<IssueRow, Integer> line = new TableColumn<>("Line");
        line.setCellValueFactory(new PropertyValueFactory<>("line"));
        line.setPrefWidth(60);

        TableColumn<IssueRow, String> msg  = new TableColumn<>("Message");
        msg.setCellValueFactory(new PropertyValueFactory<>("message"));
        msg.setPrefWidth(400);   // << THE IMPORTANT FIX
        msg.setMinWidth(300);

        issuesTable.getColumns().setAll(sev, file, line, msg);

        ObservableList<IssueRow> items = FXCollections.observableArrayList();
        issuesTable.setItems(items);

        return issuesTable;
    }

    private Node buildPreview() {
        pyPreview.setEditable(false);
        javaPreview.setEditable(false);

        SplitPane split = new SplitPane(
                wrap("Python Source", pyPreview),
                wrap("Generated Java", javaPreview)
        );
        split.setDividerPositions(0.5);
        return split;
    }

    private VBox wrap(String title, Node content) {
        VBox box = new VBox(6, new Label(title), content);
        box.setPadding(new Insets(8));
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    // ============ Actions ============

    // Import with "no Python files" error handling
    private void onImport(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Python project folder");
        File dir = chooser.showDialog(stage);
        if (dir == null) return;

        importedDir = dir;

        File[] pyFiles = dir.listFiles((d, name) -> name.endsWith(".py"));

        if (pyFiles == null || pyFiles.length == 0) {
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "No Python (.py) files found in this directory.",
                    ButtonType.OK);
            a.setHeaderText("Import Failed");
            a.showAndWait();

            projectTree.setRoot(null);
            status("Import failed.");
            log("Import failed: No Python files found.");
            return;
        }

        TreeItem<String> root = new TreeItem<>(dir.getName());
        root.getChildren().clear();
        for (File f : pyFiles) {
            root.getChildren().add(new TreeItem<>(f.getName()));
        }

        projectTree.setRoot(root);
        projectTree.setShowRoot(true);

        status("Imported: " + dir.getAbsolutePath());
        log("Validating " + pyFiles.length + " Python file(s)…");

        simulate(900, () -> {
            log("Project successfully imported.");
            status("Ready to translate.");
            counters.setText("LOC: 0 | Translated: 0 | Warnings: 0 | Errors: 0");
        });
    }

    private void showOptions() {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Translation Options");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        VBox v = new VBox(10);
        v.setPadding(new Insets(10));
        v.getChildren().addAll(
                new CheckBox("Enable strict type mapping"),
                new CheckBox("Include docstrings as JavaDoc"),
                new CheckBox("Generate null checks"),
                new CheckBox("Experimental: async/await → CompletableFuture")
        );
        d.getDialogPane().setContent(v);
        d.showAndWait();
    }

    // REAL (minimal) translation for one selected Python file
    private void onTranslate() {
        if (importedDir == null) {
            alert("Please import a project first.");
            return;
        }

        TreeItem<String> selected = projectTree.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.getChildren().isEmpty()) {
            alert("Please select a Python file in the Project Explorer.");
            return;
        }

        File pyFile = new File(importedDir, selected.getValue());
        if (!pyFile.exists()) {
            alert("Could not find file: " + pyFile.getName());
            return;
        }

        String pythonSource = readFileSafe(pyFile);
        pyPreview.setText(pythonSource);

        log("Analyzing Python source code in " + pyFile.getName() + "…");
        status("Translating…");

        simulate(1000, () -> {
            String className = toClassName(stripExtension(pyFile.getName()));
            String javaSource = translatePythonToJava(pythonSource, className);
            javaPreview.setText(javaSource);

            int loc = pythonSource.isEmpty() ? 0 : pythonSource.split("\\R").length;
            counters.setText("LOC: " + loc + " | Translated: " + loc + " | Warnings: 0 | Errors: 0");

            issuesTable.getItems().clear();
            log("Translation complete for " + pyFile.getName() + ".");
            status("Translation complete.");

            // Detect unsupported constructs
            // Detect unsupported constructs / obvious syntax issues
            ObservableList<IssueRow> issues = FXCollections.observableArrayList();
            String[] pyLines = pythonSource.split("\\R");

            for (int i = 0; i < pyLines.length; i++) {
                String line = pyLines[i].trim();

                // 1) eval / exec
                if (line.contains("eval(") || line.contains("exec(")) {
                    issues.add(new IssueRow(
                            "Error",
                            pyFile.getName(),
                            i + 1,
                            "Unsupported Python construct detected: eval/exec"
                    ));
                }

                // 2) for/while loop header missing colon at the end
                if ((line.startsWith("for ") || line.startsWith("while "))
                        && !line.endsWith(":")) {
                    issues.add(new IssueRow(
                            "Error",
                            pyFile.getName(),
                            i + 1,
                            "Possible invalid loop syntax (missing colon)."
                    ));
                }
            }

            issuesTable.setItems(issues);
        });
    }

    private void onCompile() {
        if (importedDir == null) {
            alert("Please import a project first.");
            return;
        }

        log("Compiling…");
        status("Compiling…");

        simulate(900, () -> {
            // Check if there are any existing errors in the Issues table
            boolean hasErrors = false;
            for (IssueRow row : issuesTable.getItems()) {
                if ("Error".equalsIgnoreCase(row.getSeverity())) {
                    hasErrors = true;
                    break;
                }
            }

            if (hasErrors) {
                // Simulate failed compile
                log("Compiler returned errors.");
                status("Compile failed. See Issues tab.");
            } else {
                // Simulate successful compile
                log("Compiler returned: 0 errors, 0 warnings (simulated).");
                status("Compilation successful.");
            }
        });
    }



    private void onExport() {
        if (importedDir == null) { alert("Please import a project first."); return; }
        log("Exporting report…");
        status("Exporting…");

        simulate(700, () -> {
            log("Report exported to: " + new File(importedDir, "conversion-report.html"));
            status("Report exported.");
        });
    }

    // Utility

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }

    private void status(String msg) {
        statusLabel.setText(msg);
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void simulate(int millis, Runnable completion) {
        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        new Thread(() -> {
            try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                progress.setProgress(0);
                completion.run();
            });
        }).start();
    }

    private String readFileSafe(File f) {
        try {
            return Files.readString(f.toPath());
        } catch (IOException e) {
            return "// Failed to read file: " + e.getMessage();
        }
    }

    private String stripExtension(String name) {
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(0, i) : name;
    }

    private String toClassName(String base) {
        if (base.isEmpty()) return "Generated";
        String cleaned = base.replaceAll("[^A-Za-z0-9]", "_");
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    // Very simple Python → Java translator for a subset of Python
    private String translatePythonToJava(String python, String className) {
        String[] lines = python.split("\\R");
        StringBuilder out = new StringBuilder();
        out.append("public class ").append(className).append(" {\n");

        int currentIndent = 0;

        for (String rawLine : lines) {
            String line = rawLine.replace("\t", "    ");
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue; // skip empty/comment lines
            }

            int indent = countIndent(line);

            // close blocks on dedent
            while (indent < currentIndent) {
                out.append(indentString(currentIndent - 1)).append("}\n");
                currentIndent--;
            }

            // control / def lines with colon
            if (trimmed.startsWith("def ") && trimmed.endsWith(":")) {
                String signature = trimmed.substring(4, trimmed.length() - 1); // name(args)
                int paren = signature.indexOf('(');
                String fnName = (paren > 0) ? signature.substring(0, paren).trim() : "function";
                String params = "";
                if (paren >= 0) {
                    String inside = signature.substring(paren + 1, signature.length() - 1);
                    params = convertParams(inside);
                }
                out.append(indentString(indent))
                        .append("public static void ")
                        .append(fnName)
                        .append("(").append(params).append(") {\n");
                currentIndent = indent + 1;
                continue;
            } else if ((trimmed.startsWith("if ")  ||
                    trimmed.startsWith("elif ")||
                    trimmed.startsWith("while ")||
                    trimmed.startsWith("for ")) && trimmed.endsWith(":")) {

                String keyword;
                String condition;
                if (trimmed.startsWith("elif ")) {
                    keyword = "else if";
                    condition = trimmed.substring(5, trimmed.length() - 1).trim();
                } else if (trimmed.startsWith("if ")) {
                    keyword = "if";
                    condition = trimmed.substring(3, trimmed.length() - 1).trim();
                } else if (trimmed.startsWith("while ")) {
                    keyword = "while";
                    condition = trimmed.substring(6, trimmed.length() - 1).trim();
                } else { // for
                    keyword = "for";
                    condition = trimmed.substring(4, trimmed.length() - 1).trim();
                }

                out.append(indentString(indent))
                        .append(keyword)
                        .append(" (")
                        .append(condition)
                        .append(") {\n");
                currentIndent = indent + 1;
                continue;
            } else if (trimmed.startsWith("else:")) {
                out.append(indentString(indent))
                        .append("else {\n");
                currentIndent = indent + 1;
                continue;
            }

            // normal statement line
            String javaLine = trimmed.replace("print(", "System.out.println(");
            if (!javaLine.endsWith(";")) {
                javaLine = javaLine + ";";
            }
            out.append(indentString(indent)).append(javaLine).append("\n");
        }

        // close remaining blocks
        while (currentIndent > 0) {
            out.append(indentString(currentIndent - 1)).append("}\n");
            currentIndent--;
        }

        out.append("}\n");
        return out.toString();
    }

    private int countIndent(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') count++;
            else break;
        }
        return count / 4; // assume 4-space indents
    }

    private String indentString(int level) {
        return "    ".repeat(Math.max(0, level + 1)); // +1 to indent inside class
    }

    private String convertParams(String inside) {
        inside = inside.trim();
        if (inside.isEmpty()) return "";
        String[] parts = inside.split(",");
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String p : parts) {
            String name = p.trim();
            if (name.equals("self") || name.isEmpty()) continue;
            if (!first) sb.append(", ");
            sb.append("Object ").append(name);
            first = false;
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Data Row
    public static class IssueRow {
        private final String severity, file, message;
        private final int line;
        public IssueRow(String severity, String file, int line, String message) {
            this.severity = severity; this.file = file; this.line = line; this.message = message;
        }
        public String getSeverity() { return severity; }
        public String getFile()     { return file; }
        public int    getLine()     { return line; }
        public String getMessage()  { return message; }
    }
}
