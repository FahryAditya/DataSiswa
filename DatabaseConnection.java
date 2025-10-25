package com.example.datasiswautama;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // tambahkan serverTimezone dan disable SSL jika lokal
    private static final String URL = "jdbc:mysql://localhost:3306/db_siswa_airlangga?useSSL=false&serverTimezone=UTC";
    private static final String USER = "app_user";
    private static final String PASS = "passwordku123";
\
    public static Connection getConnection() {
        try {
            // pastikan driver JDBC dimuat (opsional di Java modern, tapi membantu debug)
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException cnfe) {
                System.err.println("MySQL JDBC Driver tidak ditemukan. Tambahkan connector ke classpath.");
                cnfe.printStackTrace();
                // lanjutkan untuk mencoba koneksi (biasanya gagal jika driver hilang)
            }

            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            System.err.println("❌ Gagal koneksi ke database: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
