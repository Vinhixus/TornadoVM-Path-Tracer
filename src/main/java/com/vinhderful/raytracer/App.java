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
package com.vinhderful.raytracer;

import java.io.IOException;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Java-based Ray Tracer using JavaFX
 */
public class App extends Application {

    /**
     * Launch application window
     *
     * @param args
     *     program arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initialise application window
     *
     * @param stage
     *     the stage to show
     */
    @Override
    public void start(Stage stage) {

        StackPane root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("controllers/Loading.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene = null;
        if (root != null)
            scene = new Scene(root);

        stage.setTitle("TornadoVM Ray Tracer");
        stage.getIcons().add(new Image(Objects.requireNonNull(App.class.getResourceAsStream("icon.png"))));
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Exit handler
     */
    @Override
    public void stop() {
        System.out.println("Exiting application...");
        System.exit(0);
    }
}