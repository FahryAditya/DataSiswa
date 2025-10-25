package com.example.datasiswautama;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DataSiswaView extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("📄 Data Siswa (Daftar Sementara)");

        TableView<Siswa> table = new TableView<>(AdminInputApp.dataSiswa);

        TableColumn<Siswa, String> colNama = new TableColumn<>("Nama");
        colNama.setCellValueFactory(data -> data.getValue().namaProperty());

        TableColumn<Siswa, String> colKelas = new TableColumn<>("Kelas");
        colKelas.setCellValueFactory(data -> data.getValue().kelasProperty());

        TableColumn<Siswa, String> colJurusan = new TableColumn<>("Jurusan");
        colJurusan.setCellValueFactory(data -> data.getValue().jurusanProperty());

        TableColumn<Siswa, String> colSekolah = new TableColumn<>("Sekolah");
        colSekolah.setCellValueFactory(data -> data.getValue().sekolahProperty());

        table.getColumns().addAll(colNama, colKelas, colJurusan, colSekolah);

        VBox root = new VBox(10, new Label("Daftar Data Siswa (Belum ke Database)"), table);
        root.setPadding(new Insets(15));

        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }
}
