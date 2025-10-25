package com.example.datasiswautama;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class timeout extends Application {

    private TreeTableView<Student> treeTable;
    private ObservableList<Student> dataList;
    private PauseTransition inactivityTimer;

    @Override
    public void start(Stage stage) {
        stage.setTitle("🏫 Smart Data Parser");
        dataList = FXCollections.observableArrayList();

        // ====== TABEL ======
        treeTable = new TreeTableView<>();
        treeTable.setShowRoot(false);
        treeTable.setEditable(true);

        TreeTableColumn<Student, String> nameCol = new TreeTableColumn<>("Nama");
        nameCol.setCellValueFactory(c -> c.getValue().getValue().namaProperty());
        nameCol.setPrefWidth(200);
        nameCol.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());

        TreeTableColumn<Student, String> kelasCol = new TreeTableColumn<>("Kelas");
        kelasCol.setCellValueFactory(c -> c.getValue().getValue().kelasProperty());
        kelasCol.setPrefWidth(100);

        treeTable.getColumns().addAll(nameCol, kelasCol);

        // ====== TOMBOL ======
        Button btnAdd = new Button("➕ Tambah Contoh");
        btnAdd.setOnAction(e -> addSample());

        Button btnLogout = new Button("🚪 Logout");
        btnLogout.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Yakin ingin logout?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    stage.close();
                    new LoginApp().start(new Stage());
                }
            });
        });

        HBox topBar = new HBox(10, btnAdd, btnLogout);
        topBar.setPadding(new Insets(10));

        VBox root = new VBox(10, topBar, treeTable);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.show();

        // 🔒 Setup auto logout
        setupAutoLogout(stage);
    }

    private void addSample() {
        dataList.addAll(
                new Student("Rina Safitri", "11"),
                new Student("Budi Santoso", "10")
        );
        TreeItem<Student> root = new TreeItem<>(new Student("root", ""));
        dataList.forEach(s -> root.getChildren().add(new TreeItem<>(s)));
        treeTable.setRoot(root);
    }

    // ⏱️ Auto Logout (tidak aktif selama 3 menit)
    private void setupAutoLogout(Stage stage) {
        inactivityTimer = new PauseTransition(Duration.seconds(120)); // 3 menit

        inactivityTimer.setOnFinished(e -> {
            new Alert(Alert.AlertType.INFORMATION,
                    "Anda telah logout otomatis karena tidak aktif selama 3 menit.").showAndWait();
            stage.close();
            new LoginApp().start(new Stage());
        });

        // Deteksi setiap klik / ketikan user
        stage.getScene().addEventFilter(javafx.scene.input.InputEvent.ANY, e -> resetTimer());
        resetTimer();
    }

    private void resetTimer() {
        if (inactivityTimer != null) inactivityTimer.playFromStart();
    }

    // ====== CLASS STUDENT ======
    public static class Student {
        private final SimpleStringProperty nama;
        private final SimpleStringProperty kelas;

        public Student(String nama, String kelas) {
            this.nama = new SimpleStringProperty(nama);
            this.kelas = new SimpleStringProperty(kelas);
        }

        public SimpleStringProperty namaProperty() { return nama; }
        public SimpleStringProperty kelasProperty() { return kelas; }
    }
}

