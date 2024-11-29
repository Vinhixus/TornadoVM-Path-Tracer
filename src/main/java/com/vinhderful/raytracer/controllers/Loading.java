/*
 * This file is part of Tornado-Ray-Tracer: A Java-based ray tracer running on TornadoVM.
 * URL: https://github.com/Vinhixus/TornadoVM-Ray-Tracer
 *
 * Copyright (c) 2021-2022, Vinh Pham Van
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vinhderful.raytracer.controllers;

import java.io.IOException;
import java.util.Objects;

import com.vinhderful.raytracer.App;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Controller class for Loading window popping up while main program is loading
 */
public class Loading {

    /**
     * JavaFX GUI elements
     */
    @FXML
    public StackPane stackPane;
    public Text text;

    /**
     * Initializer, gets called first when the FXML is loaded
     */
    @FXML
    public void initialize() {

        // Task to create the main window
        Task<Parent> createMainWindow = new Task<>() {

            @Override
            protected Parent call() {

                BorderPane root = null;
                try {
                    root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Main.fxml")));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return root;
            }
        };

        // Show stage when FXML is loaded
        createMainWindow.setOnSucceeded(e -> {
            BorderPane root = (BorderPane) createMainWindow.getValue();
            Scene scene = null;
            if (root != null)
                scene = new Scene(root);

            Stage stage = new Stage();
            stage.setTitle("TornadoVM Ray Tracer");
            stage.getIcons().add(new Image(Objects.requireNonNull(App.class.getResourceAsStream("icon.png"))));
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            Stage loadingStage = (Stage) stackPane.getScene().getWindow();
            loadingStage.close();
        });

        // Load FXML on new thread
        new Thread(createMainWindow).start();
    }
}
