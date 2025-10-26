
package com.example.datasiswautama;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Panel extends Application {

    // 🔹 Data siswa sementara (belum ke database)
    public static ObservableList<Siswa> dataSiswa = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        stage.setTitle("🧑‍💼 Panel Admin - Input Data Siswa");

        // 🔸 Input fields
        TextField tfNama = new TextField();
        tfNama.setPromptText("Nama Siswa");

        ComboBox<String> cbKelas = new ComboBox<>();
        cbKelas.getItems().addAll("10", "11", "12");
        cbKelas.setPromptText("Pilih Kelas");

        ComboBox<String> cbJurusan = new ComboBox<>();
        cbJurusan.getItems().addAll("PPLG", "DKV", "TJKT", "MPLB", "AKL", "TLM", "AKC", "FKK");
        cbJurusan.setPromptText("Pilih Jurusan");

        ComboBox<String> cbSekolah = new ComboBox<>();
        cbSekolah.getItems().addAll("SMK Airlangga", "SMK Kesehatan Airlangga");
        cbSekolah.setPromptText("Pilih Sekolah");

        // 🔸 Buttons
        Button btnSimpan = new Button("💾 Simpan Manual");
        Button btnMassInput = new Button("📊 Input Skala Besar");
        Button btnLihatData = new Button("📄 Lihat Data");
        Button btnLogout = new Button("🚪 Logout");

        // 🔸 Layout tombol
        HBox buttonBox = new HBox(10, btnSimpan, btnMassInput, btnLihatData, btnLogout);
        VBox root = new VBox(10,
                new Label("Nama:"), tfNama,
                new Label("Kelas:"), cbKelas,
                new Label("Jurusan:"), cbJurusan,
                new Label("Sekolah:"), cbSekolah,
                buttonBox
        );
        root.setPadding(new Insets(15));

        // 🔹 Tombol Simpan Manual
        btnSimpan.setOnAction(e -> {
            String nama = tfNama.getText().trim();
            String kelas = cbKelas.getValue();
            String jurusan = cbJurusan.getValue();
            String sekolah = cbSekolah.getValue();

            if (nama.isEmpty() || kelas == null || jurusan == null || sekolah == null) {
                new Alert(Alert.AlertType.WARNING, "⚠️ Lengkapi semua field!").showAndWait();
                return;
            }

            // Tambahkan ke list sementara
            dataSiswa.add(new Siswa(nama, kelas, jurusan, sekolah));

            new Alert(Alert.AlertType.INFORMATION, "✅ Data siswa berhasil ditambahkan ke daftar sementara!").showAndWait();

            // Reset form
            tfNama.clear();
            cbKelas.setValue(null);
            cbJurusan.setValue(null);
            cbSekolah.setValue(null);
        });

        // 🔹 Tombol Lihat Data
        btnLihatData.setOnAction(e -> {
            try {
                new DataSiswaView().start(new Stage());
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Gagal membuka Data Siswa: " + ex.getMessage()).showAndWait();
            }
        });

        // 🔹 Tombol Input Skala Besar
        btnMassInput.setOnAction(e -> {
            try {
                new SmartDataParser().start(new Stage());
                stage.close();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Gagal membuka SmartDataParser: " + ex.getMessage()).showAndWait();
            }
        });

        // 🔹 Tombol Logout
        btnLogout.setOnAction(e -> {
            try {
                new LoginApp().start(new Stage());
                stage.close();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Gagal logout: " + ex.getMessage()).showAndWait();
            }
        });

        // 🔹 Setup scene
        Scene scene = new Scene(root, 450, 380);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
