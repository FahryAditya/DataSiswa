package com.example.datasiswautama;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DataSiswaView extends Application {

    // Pastikan ada list publik yang bisa diakses
    public static ObservableList<Siswa> dataSiswa = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        stage.setTitle("📄 Data Siswa (Daftar Sementara)");

        TableView<Siswa> table = new TableView<>(dataSiswa);

        TableColumn<Siswa, String> colNama = new TableColumn<>("Nama");
        colNama.setCellValueFactory(data -> data.getValue().namaProperty());

        TableColumn<Siswa, String> colKelas = new TableColumn<>("Kelas");
        colKelas.setCellValueFactory(data -> data.getValue().kelasProperty());

        TableColumn<Siswa, String> colJurusan = new TableColumn<>("Jurusan");
        colJurusan.setCellValueFactory(data -> data.getValue().jurusanProperty());

        TableColumn<Siswa, String> colSekolah = new TableColumn<>("Sekolah");
        colSekolah.setCellValueFactory(data -> data.getValue().sekolahProperty());

        table.getColumns().addAll(colNama, colKelas, colJurusan, colSekolah);

        VBox root = new VBox(10, new Label("Daftar Data Siswa "), table);
        root.setPadding(new Insets(15));

        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }

    // Contoh kelas Siswa (jika belum ada di project)
    public static class Siswa {
        private final javafx.beans.property.SimpleStringProperty nama;
        private final javafx.beans.property.SimpleStringProperty kelas;
        private final javafx.beans.property.SimpleStringProperty jurusan;
        private final javafx.beans.property.SimpleStringProperty sekolah;

        public Siswa(String nama, String kelas, String jurusan, String sekolah) {
            this.nama = new javafx.beans.property.SimpleStringProperty(nama);
            this.kelas = new javafx.beans.property.SimpleStringProperty(kelas);
            this.jurusan = new javafx.beans.property.SimpleStringProperty(jurusan);
            this.sekolah = new javafx.beans.property.SimpleStringProperty(sekolah);
        }

        public javafx.beans.property.StringProperty namaProperty() { return nama; }
        public javafx.beans.property.StringProperty kelasProperty() { return kelas; }
        public javafx.beans.property.StringProperty jurusanProperty() { return jurusan; }
        public javafx.beans.property.StringProperty sekolahProperty() { return sekolah; }
    }
}
