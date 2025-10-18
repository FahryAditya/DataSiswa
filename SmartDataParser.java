package com.example.datasiswautama;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

public class SmartDataParser extends Application {

    private TextArea inputArea;
    private TreeTableView<Student> treeTable;
    private ObservableList<Student> dataList;
    private Set<String> existingNames;
    private TextField nameField;
    private ComboBox<String> groupSelect, jurusanSelect, tingkatSelect;
    private Label countLabel;
    private ProgressIndicator progressIndicator;

    private static final Set<String> JURUSAN_SMK_AIRLANGGA = Set.of("AKL", "DKV", "MPLB", "PPLG", "TJKT");
    private static final Set<String> JURUSAN_SMK_KESEHATAN = Set.of("AKC", "FKK", "TLM");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("🏫 Smart Data Parser Sekolah ");

        dataList = FXCollections.observableArrayList();
        existingNames = new HashSet<>();

        // === Input Otomatis ===
        inputArea = new TextArea();
        inputArea.setPromptText("Masukkan data siswa (contoh: Rina Safitri kelas 11 PPLG)");
        inputArea.setWrapText(true);
        inputArea.setPrefHeight(120);

        Button btnProcess = new Button("⚙️ Proses Otomatis");
        btnProcess.setOnAction(e -> {
            showProgress(true);
            processText();
            refreshTreeWithAnimation();
            showProgress(false);
        });

        // === Input Manual ===
        nameField = new TextField();
        nameField.setPromptText("Nama siswa");

        groupSelect = new ComboBox<>();
        groupSelect.getItems().addAll("SMK Airlangga", "SMK Kesehatan Airlangga");
        groupSelect.setPromptText("Pilih Sekolah");

        jurusanSelect = new ComboBox<>();
        jurusanSelect.setPromptText("Pilih Jurusan");

        tingkatSelect = new ComboBox<>();
        tingkatSelect.getItems().addAll("10", "11", "12");
        tingkatSelect.setPromptText("Pilih Kelas");

        groupSelect.valueProperty().addListener((obs, oldVal, val) -> {
            jurusanSelect.getItems().clear();
            if ("SMK Airlangga".equals(val))
                jurusanSelect.getItems().addAll(JURUSAN_SMK_AIRLANGGA);
            else if ("SMK Kesehatan Airlangga".equals(val))
                jurusanSelect.getItems().addAll(JURUSAN_SMK_KESEHATAN);
        });

        Button btnAdd = new Button("➕ Tambah Manual");
        btnAdd.setOnAction(e -> {
            addManual();
            refreshTreeWithAnimation();
        });

        HBox manualBox = new HBox(10, nameField, groupSelect, jurusanSelect, tingkatSelect, btnAdd);
        manualBox.setPadding(new Insets(10));

        // === Progress Indicator ===
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(30, 30);

        // === TreeTable ===
        TreeTableColumn<Student, String> noCol = new TreeTableColumn<>("No");
        noCol.setPrefWidth(50);
        noCol.setCellValueFactory(param -> {
            TreeItem<Student> item = param.getValue();
            if (item.getParent() != null && item.getParent().getParent() != null) {
                int index = item.getParent().getChildren().indexOf(item) + 1;
                return new SimpleStringProperty(String.valueOf(index));
            }
            return new SimpleStringProperty("");
        });

        TreeTableColumn<Student, String> nameCol = new TreeTableColumn<>("Nama / Level");
        nameCol.setCellValueFactory(c -> c.getValue().getValue().namaProperty());
        nameCol.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        nameCol.setPrefWidth(300);

        TreeTableColumn<Student, String> kelasCol = new TreeTableColumn<>("Kelas");
        kelasCol.setCellValueFactory(c -> c.getValue().getValue().kelasProperty());
        kelasCol.setPrefWidth(80);

        TreeTableColumn<Student, String> jurusanCol = new TreeTableColumn<>("Jurusan");
        jurusanCol.setCellValueFactory(c -> c.getValue().getValue().jurusanProperty());
        jurusanCol.setPrefWidth(100);

        treeTable = new TreeTableView<>();
        treeTable.getColumns().addAll(noCol, nameCol, kelasCol, jurusanCol);
        treeTable.setShowRoot(false);
        treeTable.setEffect(new DropShadow(5, Color.GRAY));

        // === FIX: Warna teks tetap hitam meski diklik ===
        treeTable.setRowFactory(tv -> new TreeTableRow<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    String baseColor = switch (item.getKelompok()) {
                        case "SMK Airlangga" -> "#E3F2FD";
                        case "SMK Kesehatan Airlangga" -> "#E8F5E9";
                        default -> "#FFFFFF";
                    };
                    setStyle("-fx-background-color: " + baseColor + ";"
                            + "-fx-text-fill: black;"
                            + "-fx-selection-bar: derive(" + baseColor + ", -10%);"
                            + "-fx-selection-bar-text: black;");
                }
            }
        });

        // === Tombol Aksi ===
        Button btnSave = new Button("💾 Simpan ke File");
        btnSave.setOnAction(e -> saveData());

        Button btnClear = new Button("🧹 Hapus Semua");
        btnClear.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Apakah Anda yakin ingin menghapus semua data?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    dataList.clear();
                    existingNames.clear();
                    refreshTreeWithAnimation();
                }
            });
        });

        Button btnRefresh = new Button("🔄 Segarkan Tampilan");
        btnRefresh.setOnAction(e -> refreshTreeWithAnimation());

        Button btnLoadExample = new Button("📝 Muat Contoh Data");
        btnLoadExample.setOnAction(e -> loadExampleData());

        countLabel = new Label("👥 Total siswa: 0");
        countLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        HBox buttonBox = new HBox(10, btnProcess, btnSave, btnClear, btnRefresh, btnLoadExample, progressIndicator);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(10,
                new Label("📥 Input Otomatis:"), inputArea,
                manualBox,
                buttonBox,
                new Label("📊 Daftar Data Siswa"),
                treeTable,
                countLabel
        );
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #FFFFFF, #F1F8E9);");

        Scene scene = new Scene(root, 1000, 650);
        stage.setScene(scene);
        stage.show();

        refreshTreeWithAnimation();
    }

    private void addManual() {
        String nama = nameField.getText().trim();
        String sekolah = groupSelect.getValue();
        String jurusan = jurusanSelect.getValue();
        String kelas = tingkatSelect.getValue();

        if (nama.isEmpty() || sekolah == null || jurusan == null || kelas == null) {
            showAlert("⚠️ Peringatan", "Lengkapi semua field!", Alert.AlertType.WARNING);
            return;
        }

        if (existingNames.contains(nama.toLowerCase())) {
            showAlert("⚠️ Duplikat", "Nama siswa sudah ada!", Alert.AlertType.WARNING);
            return;
        }

        Student s = new Student(nama, kelas, jurusan, sekolah);
        dataList.add(s);
        existingNames.add(nama.toLowerCase());

        nameField.clear();
        groupSelect.setValue(null);
        jurusanSelect.getItems().clear();
        tingkatSelect.setValue(null);
        updateCount();
    }

    private void processText() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            showAlert("❌ Error", "Teks masih kosong!", Alert.AlertType.ERROR);
            return;
        }

        Pattern pattern = Pattern.compile("(\\w+\\s?\\w*)\\s*(?:,|)\\s*kelas\\s*(\\d{1,2})\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String nama = matcher.group(1).trim();
            String kelas = matcher.group(2);
            String jurusan = matcher.group(3).toUpperCase();
            String sekolah = determineSekolah(jurusan);

            if (sekolah != null && !existingNames.contains(nama.toLowerCase())) {
                Student s = new Student(nama, kelas, jurusan, sekolah);
                dataList.add(s);
                existingNames.add(nama.toLowerCase());
            }
        }
        updateCount();
    }

    private String determineSekolah(String jurusan) {
        if (JURUSAN_SMK_AIRLANGGA.contains(jurusan)) return "SMK Airlangga";
        if (JURUSAN_SMK_KESEHATAN.contains(jurusan)) return "SMK Kesehatan Airlangga";
        return null;
    }

    private void loadExampleData() {
        List<Student> examples = Arrays.asList(
                new Student("Rina Safitri", "11", "PPLG", "SMK Airlangga"),
                new Student("Ahmad Fauzi", "10", "AKL", "SMK Airlangga"),
                new Student("Siti Nurhaliza", "12", "TJKT", "SMK Airlangga"),
                new Student("Budi Santoso", "11", "AKC", "SMK Kesehatan Airlangga"),
                new Student("Maya Sari", "10", "FKK", "SMK Kesehatan Airlangga"),
                new Student("Dika Pratama", "12", "TLM", "SMK Kesehatan Airlangga")
        );

        for (Student s : examples) {
            if (!existingNames.contains(s.getNama().toLowerCase())) {
                dataList.add(s);
                existingNames.add(s.getNama().toLowerCase());
            }
        }
        refreshTreeWithAnimation();
        showAlert("📝 Contoh Data", "Data siswa contoh telah dimuat!", Alert.AlertType.INFORMATION);
    }

    private void refreshTreeWithAnimation() {
        Map<String, Map<String, Map<String, List<Student>>>> grouped = new TreeMap<>();

        for (Student s : dataList) {
            grouped
                    .computeIfAbsent(s.getKelompok(), k -> new TreeMap<>())
                    .computeIfAbsent(s.getJurusan(), j -> new TreeMap<>())
                    .computeIfAbsent(s.getKelas(), c -> new ArrayList<>())
                    .add(s);
        }

        TreeItem<Student> root = new TreeItem<>(new Student("", "", "", ""));
        for (var sekolahEntry : grouped.entrySet()) {
            int totalSekolah = sekolahEntry.getValue().values().stream()
                    .flatMap(j -> j.values().stream())
                    .mapToInt(List::size).sum();
            TreeItem<Student> sekolahNode = new TreeItem<>(
                    new Student("Sekolah: " + sekolahEntry.getKey() + " (Total " + totalSekolah + ")", "", "", sekolahEntry.getKey()));

            for (var jurusanEntry : sekolahEntry.getValue().entrySet()) {
                int totalJurusan = jurusanEntry.getValue().values().stream().mapToInt(List::size).sum();
                TreeItem<Student> jurusanNode = new TreeItem<>(
                        new Student("Jurusan: " + jurusanEntry.getKey() + " (Total " + totalJurusan + ")", "", jurusanEntry.getKey(), sekolahEntry.getKey()));

                for (var kelasEntry : jurusanEntry.getValue().entrySet()) {
                    TreeItem<Student> kelasNode = new TreeItem<>(
                            new Student("Kelas: " + kelasEntry.getKey(), kelasEntry.getKey(), jurusanEntry.getKey(), sekolahEntry.getKey()));

                    for (Student s : kelasEntry.getValue()) {
                        kelasNode.getChildren().add(new TreeItem<>(s));
                    }
                    jurusanNode.getChildren().add(kelasNode);
                }
                sekolahNode.getChildren().add(jurusanNode);
            }
            root.getChildren().add(sekolahNode);
        }

        treeTable.setRoot(root);
        treeTable.refresh();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), treeTable);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        updateCount();
    }

    private void updateCount() {
        long totalAirlangga = dataList.stream().filter(s -> s.getKelompok().equals("SMK Airlangga")).count();
        long totalKesehatan = dataList.stream().filter(s -> s.getKelompok().equals("SMK Kesehatan Airlangga")).count();
        countLabel.setText(String.format("👥 Total siswa: %d (SMK Airlangga: %d | SMK Kesehatan Airlangga: %d)",
                dataList.size(), totalAirlangga, totalKesehatan));

        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.3), countLabel);
        scale.setFromX(1.0);
        scale.setToX(1.1);
        scale.setFromY(1.0);
        scale.setToY(1.1);
        scale.setCycleCount(2);
        scale.setAutoReverse(true);
        scale.play();
    }

    private void saveData() {
        if (dataList.isEmpty()) {
            showAlert("⚠️ Peringatan", "Tidak ada data untuk disimpan!", Alert.AlertType.WARNING);
            return;
        }
        try (FileWriter writer = new FileWriter("data_siswa_final.txt")) {
            for (Student s : dataList) {
                writer.write(String.format("%s, Kelas %s, Jurusan %s, %s%n",
                        s.getNama(), s.getKelas(), s.getJurusan(), s.getKelompok()));
            }
            showAlert("💾 Sukses", "Data disimpan ke file 'data_siswa_final.txt'", Alert.AlertType.INFORMATION);
        } catch (IOException e) {
            showAlert("❌ Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showProgress(boolean show) {
        progressIndicator.setVisible(show);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Student {
        private final StringProperty nama, kelas, jurusan, kelompok;

        public Student(String nama, String kelas, String jurusan, String kelompok) {
            this.nama = new SimpleStringProperty(nama);
            this.kelas = new SimpleStringProperty(kelas);
            this.jurusan = new SimpleStringProperty(jurusan);
            this.kelompok = new SimpleStringProperty(kelompok);
        }

        public StringProperty namaProperty() {
            return nama;
        }

        public StringProperty kelasProperty() {
            return kelas;
        }

        public StringProperty jurusanProperty() {
            return jurusan;
        }

        public StringProperty kelompokProperty() {
            return kelompok;
        }

        public String getNama() {
            return nama.get();
        }

        public String getKelas() {
            return kelas.get();
        }

        public String getJurusan() {
            return jurusan.get();
        }

        public String getKelompok() {
            return kelompok.get();
        }
    }
}
