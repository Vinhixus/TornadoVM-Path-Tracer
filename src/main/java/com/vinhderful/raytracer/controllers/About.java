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

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;

/**
 * Controller class for About window
 */
public class About {

    /**
     * JavaFX GUI elements
     */
    @FXML
    public Hyperlink email;
    public Hyperlink linkedIn;

    /**
     * Open a given uri
     *
     * @param uri
     *     uri string
     */
    private void openURL(String uri) {
        // TODO implement code to open URIs
    }

    /**
     * Open LinkedIn page
     */
    public void openLinkedIn() {
        openURL("https://www.linkedin.com/in/" + linkedIn.getText() + "/");
    }

    /**
     * Open email
     */
    public void openEmail() {
        openURL("mailto:" + email.getText());
    }
}
