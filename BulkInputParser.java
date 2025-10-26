package com.example.datasiswautama;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;

public class BulkInputParser extends Application {

    private TextArea inputArea;
    private TreeTableView<Student> treeTable;
    private ObservableList<Student> dataList;
    private Set<String> existingNames;
    private Label countLabel;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("📊 Bulk Input Data Siswa");

        dataList = FXCollections.observableArrayList();
        existingNames = new HashSet<>();

        inputArea = new TextArea();
        inputArea.setPromptText("Masukkan data siswa tiap baris: Nama,Kelas,Jurusan");
        inputArea.setPrefRowCount(6);

        Button btnProcess = new Button("⚙️ Proses Otomatis");
        btnProcess.setOnAction(e -> processBulkInput());

        // TreeTable
        treeTable = new TreeTableView<>();
        treeTable.setShowRoot(false);
        treeTable.setEditable(true);

        TreeTableColumn<Student,String> noCol = new TreeTableColumn<>("No");
        noCol.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(dataList.indexOf(c.getValue().getValue())+1)
        ));
        noCol.setPrefWidth(50);

        TreeTableColumn<Student,String> nameCol = new TreeTableColumn<>("Nama");
        nameCol.setCellValueFactory(c -> c.getValue().getValue().namaProperty());
        nameCol.setPrefWidth(200);

        TreeTableColumn<Student,String> kelasCol = new TreeTableColumn<>("Kelas");
        kelasCol.setCellValueFactory(c -> c.getValue().getValue().kelasProperty());
        kelasCol.setPrefWidth(80);

        TreeTableColumn<Student,String> jurusanCol = new TreeTableColumn<>("Jurusan");
        jurusanCol.setCellValueFactory(c -> c.getValue().getValue().jurusanProperty());
        jurusanCol.setPrefWidth(120);

        treeTable.getColumns().addAll(noCol,nameCol,kelasCol,jurusanCol);

        countLabel = new Label("👥 Total siswa: 0");

        VBox root = new VBox(8, inputArea, btnProcess, treeTable, countLabel);
        root.setPadding(new Insets(12));

        stage.setScene(new Scene(root, 500, 400));
        stage.show();
    }

    private void processBulkInput() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;

        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length < 3) continue;

            String nama = parts[0].trim();
            String kelas = parts[1].trim();
            String jurusan = parts[2].trim();

            if (!existingNames.contains(nama.toLowerCase())) {
                Student s = new Student(nama, kelas, jurusan);
                dataList.add(s);
                existingNames.add(nama.toLowerCase());
            }
        }
        refreshTree();
    }

    private void refreshTree() {
        TreeItem<Student> root = new TreeItem<>(new Student("","",""));
        for (Student s : dataList) root.getChildren().add(new TreeItem<>(s));
        treeTable.setRoot(root);
        treeTable.refresh();
        countLabel.setText("👥 Total siswa: " + dataList.size());
    }

    public static class Student {
        private final StringProperty nama, kelas, jurusan;

        public Student(String nama, String kelas, String jurusan) {
            this.nama = new SimpleStringProperty(nama);
            this.kelas = new SimpleStringProperty(kelas);
            this.jurusan = new SimpleStringProperty(jurusan);
        }

        public StringProperty namaProperty() { return nama; }
        public StringProperty kelasProperty() { return kelas; }
        public StringProperty jurusanProperty() { return jurusan; }
    }
}
