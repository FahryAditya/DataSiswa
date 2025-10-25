package com.example.datasiswautama;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Siswa {

    private final StringProperty nama;
    private final StringProperty kelas;
    private final StringProperty jurusan;
    private final StringProperty sekolah;

    // 🔹 Constructor
    public Siswa(String nama, String kelas, String jurusan, String sekolah) {
        this.nama = new SimpleStringProperty(nama);
        this.kelas = new SimpleStringProperty(kelas);
        this.jurusan = new SimpleStringProperty(jurusan);
        this.sekolah = new SimpleStringProperty(sekolah);
    }

    // === Getter dan Setter ===

    public String getNama() {
        return nama.get();
    }

    public void setNama(String nama) {
        this.nama.set(nama);
    }

    public StringProperty namaProperty() {
        return nama;
    }

    public String getKelas() {
        return kelas.get();
    }

    public void setKelas(String kelas) {
        this.kelas.set(kelas);
    }

    public StringProperty kelasProperty() {
        return kelas;
    }

    public String getJurusan() {
        return jurusan.get();
    }

    public void setJurusan(String jurusan) {
        this.jurusan.set(jurusan);
    }

    public StringProperty jurusanProperty() {
        return jurusan;
    }

    public String getSekolah() {
        return sekolah.get();
    }

    public void setSekolah(String sekolah) {
        this.sekolah.set(sekolah);
    }

    public StringProperty sekolahProperty() {
        return sekolah;
    }
}
