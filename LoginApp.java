package com.example.datasiswautama;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("🔐 Login Admin Sekolah");

        Label lblUser = new Label("Username:");
        TextField tfUser = new TextField();
        tfUser.setPromptText("Masukkan username");

        Label lblPass = new Label("Password:");
        PasswordField pfPass = new PasswordField();
        TextField tfShowPass = new TextField();
        CheckBox cbShow = new CheckBox("Tampilkan Password");

        // Toggle tampilkan password
        tfShowPass.managedProperty().bind(cbShow.selectedProperty());
        tfShowPass.visibleProperty().bind(cbShow.selectedProperty());
        pfPass.managedProperty().bind(cbShow.selectedProperty().not());
        pfPass.visibleProperty().bind(cbShow.selectedProperty().not());
        tfShowPass.textProperty().bindBidirectional(pfPass.textProperty());

        Button btnLogin = new Button("Login");

        btnLogin.setOnAction(e -> {
            String user = tfUser.getText().trim();
            String pass = pfPass.getText().trim();

            if (user.equals("admin") && pass.equals("admin123")) {
                new AdminInputApp().start(new Stage());
                stage.close();
            } else {
                new Alert(Alert.AlertType.ERROR, "❌ Username atau password salah!").showAndWait();
            }
        });

        VBox root = new VBox(10,
                lblUser, tfUser,
                lblPass, pfPass, tfShowPass,
                cbShow, btnLogin
        );
        root.setPadding(new Insets(20));

        stage.setScene(new Scene(root, 300, 260));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
