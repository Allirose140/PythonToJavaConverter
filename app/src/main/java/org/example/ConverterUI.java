// JavaFX prototype UI for the Python → Java Converter.
// Works on JDK 21 + JavaFX 21
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
import java.util.List;

public class ConverterUI extends Application {

    // UI state
    private final TreeView<String> projectTree = new TreeView<>();
    private final TextArea logArea = new TextArea();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label status = new Label("Ready");
    private final Label counters = new Label("LOC: 0 | Translated: 0 | Warnings: 0 | Errors: 0");

    private final TableView<IssueRow> issuesTable = new TableView<>();
    private final TextArea pyPreview = new TextArea();
    private final TextArea javaPreview = new TextArea();

    private File importedDir;

    public static void main(String[] args) { launch(args); }

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

        // Status bar
        HBox bottom = new HBox(10, new Label("Status:"), status, new Region(), counters, progress);
        HBox.setHgrow(bottom.getChildren().get(2), Priority.ALWAYS);
        bottom.setPadding(new Insets(6));
        progress.setPrefWidth(200);

        // Root
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setLeft(left);
        root.setCenter(tabs);
        root.setBottom(bottom);

        stage.setScene(new Scene(root, 1200, 760));
        stage.show();

        log("Ready. Import a Python project to begin.");
    }

    // Builders

    private Pane buildDashboard() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));
        box.getChildren().addAll(
                new Label("Follow the steps: Import → Translate → Compile"),
                new Label("This dashboard will show metrics after translation/compile.")
        );
        return box; // VBox is a Pane
    }

    private Node buildLogs() {
        logArea.setEditable(false);
        logArea.setWrapText(true);
        return logArea; // TextArea is a Node
    }

    private Node buildIssues() {
        TableColumn<IssueRow, String> sev  = new TableColumn<>("Severity");
        sev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        TableColumn<IssueRow, String> file = new TableColumn<>("File");
        file.setCellValueFactory(new PropertyValueFactory<>("file"));
        TableColumn<IssueRow, Integer> line = new TableColumn<>("Line");
        line.setCellValueFactory(new PropertyValueFactory<>("line"));
        TableColumn<IssueRow, String> msg  = new TableColumn<>("Message");
        msg.setCellValueFactory(new PropertyValueFactory<>("message"));

        issuesTable.getColumns().setAll(sev, file, line, msg);
        ObservableList<IssueRow> items = FXCollections.observableArrayList();
        issuesTable.setItems(items);

        issuesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, row) -> {
            if (row != null) {
                pyPreview.setText("// Python context for " + row.file + " line " + row.line + "\nprint('hello')");
                javaPreview.setText("// Generated Java snippet\nSystem.out.println(\"hello\");");
            }
        });
        return issuesTable; // TableView is a Node
    }

    private Node buildPreview() {
        pyPreview.setPromptText("Python source");
        javaPreview.setPromptText("Generated Java");

        SplitPane split = new SplitPane(
                wrap("Python", pyPreview),
                wrap("Java",   javaPreview)
        );
        split.setDividerPositions(0.5);
        return split; // SplitPane is a Node
    }

    private VBox wrap(String title, Node content) {
        VBox box = new VBox(6, new Label(title), content);
        box.setPadding(new Insets(8));
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    // Actions (stubbed for demo)

    private void onImport(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Python project folder");
        File dir = chooser.showDialog(stage);
        if (dir == null) return;
        importedDir = dir;

        TreeItem<String> root = new TreeItem<>(dir.getName());
        root.getChildren().setAll(
                new TreeItem<>("app.py"),
                new TreeItem<>("util.py"),
                new TreeItem<>("models/"),
                new TreeItem<>("tests/")
        );
        projectTree.setRoot(root);
        projectTree.setShowRoot(true);

        status("Imported: " + dir.getAbsolutePath());
        log("Validating files…");

        simulate(900, () -> {
            log("Project successfully imported.");
            status("Ready to translate.");
            counters.setText("LOC: 234 | Translated: 0 | Warnings: 0 | Errors: 0");
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
                new CheckBox("Include stdlib shims when needed"),
                new CheckBox("Generate detailed report")
        );
        d.getDialogPane().setContent(v);
        d.showAndWait();
    }

    private void onTranslate() {
        if (importedDir == null) { alert("Please import a project first."); return; }
        log("Analyzing Python source code…");
        status("Analyzing…");

        simulate(1000, () -> {
            log("Mapping types, converting indentation, rewriting classes/functions…");
            log("Generated Java source files.");
            counters.setText("LOC: 234 | Translated: 228 | Warnings: 3 | Errors: 0");
            issuesTable.getItems().setAll(
                    new IssueRow("Warning","util.py",42,"List element type inferred as Object"),
                    new IssueRow("Warning","app.py",88,"Dynamic getattr not supported; manual review"),
                    new IssueRow("Warning","models/user.py",13,"Lambda converted to anonymous class")
            );
            status("Translation complete.");
        });
    }

    private void onCompile() {
        if (importedDir == null) { alert("Please import a project first."); return; }
        log("Compiling…");
        status("Compiling…");

        simulate(900, () -> {
            log("Compiler returned: 1 error, 2 warnings.");
            issuesTable.getItems().add(0,
                    new IssueRow("Error","app.java",91,"Cannot find symbol: Optional"));
            counters.setText("LOC: 234 | Translated: 228 | Warnings: 2 | Errors: 1");
            status("Compile finished. See Issues tab.");
        });
    }

    private void onExport() {
        log("Exported report to ./converter-report.html (demo).");
        status("Report exported.");
    }

    // Helpers

    private void simulate(long millis, Runnable done) {
        progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        new Thread(() -> {
            try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                progress.setProgress(0);
                done.run();
            });
        }).start();
    }

    private void log(String s) { logArea.appendText(s + "\n"); }
    private void status(String s) { status.setText(s); }
    private void alert(String s) { new Alert(Alert.AlertType.INFORMATION, s, ButtonType.OK).showAndWait(); }

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