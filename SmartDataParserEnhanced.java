package com.example.datasiswautama;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.*;

public class SmartDataParserEnhanced extends Application {

    private TextArea inputArea;
    private TableView<Student> tableView;
    private ObservableList<Student> dataList;
    private TextField searchField;
    private Label countLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("📘 Smart Data Parser Sekolah");

        // 📝 Area input teks
        inputArea = new TextArea();
        inputArea.setPromptText("Masukkan teks data siswa di sini...\nContoh: Rina kelas 10 umur 16 tahun, Bima kelas 11 umur 17 tahun");
        inputArea.setWrapText(true);
        inputArea.setPrefHeight(120);

        // 🔘 Tombol utama
        Button btnProcess = new Button("⚙️ Proses Data");
        Button btnSave = new Button("💾 Simpan Data");
        Button btnClear = new Button("🧹 Hapus Semua");
        Button btnReset = new Button("🔄 Reset Input");
        Button btnExit = new Button("🚪 Keluar");

        btnProcess.setOnAction(e -> processText());
        btnSave.setOnAction(e -> saveData());
        btnClear.setOnAction(e -> dataList.clear());
        btnReset.setOnAction(e -> inputArea.clear());
        btnExit.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(10, btnProcess, btnSave, btnClear, btnReset, btnExit);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setStyle("-fx-alignment: center;");

        // 🔍 Pencarian
        searchField = new TextField();
        searchField.setPromptText("🔍 Cari nama siswa...");

        // 📊 Tabel data
        tableView = new TableView<>();
        dataList = FXCollections.observableArrayList();

        // Kolom tabel
        TableColumn<Student, String> nameCol = new TableColumn<>("Nama");
        nameCol.setCellValueFactory(c -> c.getValue().namaProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> e.getRowValue().setNama(e.getNewValue()));
        nameCol.setPrefWidth(180);

        TableColumn<Student, String> classCol = new TableColumn<>("Kelas");
        classCol.setCellValueFactory(c -> c.getValue().kelasProperty());
        classCol.setCellFactory(TextFieldTableCell.forTableColumn());
        classCol.setOnEditCommit(e -> e.getRowValue().setKelas(e.getNewValue()));
        classCol.setPrefWidth(100);

        TableColumn<Student, String> ageCol = new TableColumn<>("Usia");
        ageCol.setCellValueFactory(c -> c.getValue().usiaProperty());
        ageCol.setCellFactory(TextFieldTableCell.forTableColumn());
        ageCol.setOnEditCommit(e -> e.getRowValue().setUsia(e.getNewValue()));
        ageCol.setPrefWidth(100);

        tableView.getColumns().addAll(nameCol, classCol, ageCol);
        tableView.setEditable(true);
        tableView.setPlaceholder(new Label("Belum ada data siswa."));

        // 🗑 Klik kanan untuk hapus
        MenuItem deleteItem = new MenuItem("Hapus Data Ini");
        deleteItem.setOnAction(e -> {
            Student selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) dataList.remove(selected);
        });
        tableView.setContextMenu(new ContextMenu(deleteItem));

        // 🔎 Filter pencarian
        FilteredList<Student> filteredData = new FilteredList<>(dataList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(s -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return s.getNama().toLowerCase().contains(lower)
                        || s.getKelas().toLowerCase().contains(lower)
                        || s.getUsia().toLowerCase().contains(lower);
            });
        });
        SortedList<Student> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        // 🧾 Label jumlah siswa
        countLabel = new Label();
        countLabel.textProperty().bind(Bindings.size(dataList).asString("👥 Total siswa: %d"));

        // 📦 Layout utama
        VBox topBox = new VBox(10,
                new Label("Masukkan Data Siswa:"), inputArea,
                new Label("Cari Data:"), searchField, buttonBox
        );
        topBox.setPadding(new Insets(15));

        VBox centerBox = new VBox(10, tableView, countLabel);
        centerBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(centerBox);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #E3F2FD, #FFFFFF);");

        Scene scene = new Scene(root, 700, 550);
        stage.setScene(scene);
        stage.show();
    }

    // 🔹 Fungsi untuk memproses teks input
    private void processText() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            showAlert("❌ Error", "Teks masih kosong!", Alert.AlertType.ERROR);
            return;
        }

        // Regex yang lebih fleksibel
        Pattern pattern = Pattern.compile("(\\w+)\\s*(?:,|)\\s*kelas\\s*(\\d+)\\s*(?:,|)\\s*umur\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        boolean found = false;
        while (matcher.find()) {
            found = true;
            String nama = matcher.group(1);
            String kelas = matcher.group(2);
            String usia = matcher.group(3);

            // Hindari duplikat
            boolean exists = dataList.stream().anyMatch(s ->
                    s.getNama().equalsIgnoreCase(nama) &&
                            s.getKelas().equals(kelas) &&
                            s.getUsia().equals(usia)
            );
            if (!exists) dataList.add(new Student(nama, kelas, usia));
        }

        if (!found) {
            showAlert("ℹ️ Info", "Tidak ada data valid ditemukan!\nGunakan format seperti: Rina kelas 10 umur 16 tahun", Alert.AlertType.INFORMATION);
        } else {
            showAlert("✅ Sukses", "Data berhasil diproses dan ditampilkan.", Alert.AlertType.INFORMATION);
        }
    }

    // 🔹 Simpan data ke file
    private void saveData() {
        if (dataList.isEmpty()) {
            showAlert("⚠️ Peringatan", "Tidak ada data untuk disimpan!", Alert.AlertType.WARNING);
            return;
        }

        try (FileWriter writer = new FileWriter("data_siswa.txt")) {
            for (Student s : dataList) {
                writer.write(s.getNama() + ", Kelas " + s.getKelas() + ", Usia " + s.getUsia() + " tahun\n");
            }
            showAlert("💾 Sukses", "Data berhasil disimpan ke file 'data_siswa.txt'", Alert.AlertType.INFORMATION);
        } catch (IOException e) {
            showAlert("❌ Error", "Gagal menyimpan data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // 🔹 Menampilkan alert
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 🔹 Kelas Model Data
    public static class Student {
        private final StringProperty nama;
        private final StringProperty kelas;
        private final StringProperty usia;

        public Student(String nama, String kelas, String usia) {
            this.nama = new SimpleStringProperty(nama);
            this.kelas = new SimpleStringProperty(kelas);
            this.usia = new SimpleStringProperty(usia);
        }

        public StringProperty namaProperty() { return nama; }
        public StringProperty kelasProperty() { return kelas; }
        public StringProperty usiaProperty() { return usia; }

        public String getNama() { return nama.get(); }
        public String getKelas() { return kelas.get(); }
        public String getUsia() { return usia.get(); }

        public void setNama(String nama) { this.nama.set(nama); }
        public void setKelas(String kelas) { this.kelas.set(kelas); }
        public void setUsia(String usia) { this.usia.set(usia); }
    }
}
