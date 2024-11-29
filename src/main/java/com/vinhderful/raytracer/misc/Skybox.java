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
package com.vinhderful.raytracer.misc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import javax.imageio.ImageIO;

import com.vinhderful.raytracer.utils.Color;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat4;

/**
 * Represents a spherical skybox initialised by image
 */
public class Skybox {

    private BufferedImage image;
    private VectorFloat4 buffer;

    /**
     * Read the given resource into a BufferedImage
     *
     * @param resourceName
     *     the ray to the resource
     */
    public Skybox(String resourceName) {

        image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        try {
            image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(resourceName)));
        } catch (IOException | IllegalArgumentException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        allocateAndInitializeBuffer();
    }

    /**
     * Allocate memory space and initialise the input buffer
     */
    private void allocateAndInitializeBuffer() {

        int width = image.getWidth();
        int height = image.getHeight();

        buffer = new VectorFloat4(width * height); // 8192 , 4096
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                buffer.set(x + y * width, Color.toFloat4(image.getRGB(x, y)));
            }
        }
    }

    /**
     * Return the memory address of the input buffer
     *
     * @return the pointer pointing to the VectorFloat4 input buffer
     */
    public VectorFloat4 getBuffer() {
        return buffer;
    }

    /**
     * Return the dimensions as an input buffer
     *
     * @return the dimensions int array containing [0] = width, [1] = height
     */
    public int[] getDimensionsBuffer() {
        return new int[] { image.getWidth(), image.getHeight() };
    }

}
