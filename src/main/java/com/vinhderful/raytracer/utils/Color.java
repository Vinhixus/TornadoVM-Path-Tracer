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
package com.vinhderful.raytracer.utils;

import uk.ac.manchester.tornado.api.types.vectors.Float4;

import static uk.ac.manchester.tornado.api.math.TornadoMath.max;
import static uk.ac.manchester.tornado.api.math.TornadoMath.min;

/**
 * Color definitions and operations, a color is represented by RGB float values in a Float4, where
 * indices 0, 1, and 2 are red, green and blue values respectively
 */
public class Color {

    /**
     * Default colors
     */
    public static final Float4 BLACK = new Float4(0, 0, 0, 0);
    public static final Float4 WHITE = new Float4(1F, 1F, 1F, 0);
    public static final Float4 RED = new Float4(1F, 0, 0, 0);
    public static final Float4 GREEN = new Float4(0, 1F, 0, 0);
    public static final Float4 BLUE = new Float4(0, 0, 1F, 0);
    public static final Float4 GRAY = new Float4(0.5F, 0.5F, 0.5F, 0);
    public static final Float4 DARK_GRAY = new Float4(0.2F, 0.2F, 0.2F, 0);


    /**
     * Add two colors
     *
     * @param a color one
     * @param b color two
     * @return color a added to color b
     */
    public static Float4 add(Float4 a, Float4 b) {
        return new Float4(
                min(1F, max(0F, a.getX() + b.getX())),
                min(1F, max(0F, a.getY() + b.getY())),
                min(1F, max(0F, a.getZ() + b.getZ())),
                0);
    }

    /**
     * Add a scalar to a color
     *
     * @param a the color
     * @param b the scalar
     * @return scalar b added to color a
     */
    public static Float4 add(Float4 a, float b) {
        return new Float4(
                min(1F, max(0F, a.getX() + b)),
                min(1F, max(0F, a.getY() + b)),
                min(1F, max(0F, a.getZ() + b)),
                0);
    }

    /**
     * Multiply two colors
     *
     * @param a color one
     * @param b color two
     * @return color a multiplied by color b
     */
    public static Float4 mult(Float4 a, Float4 b) {
        return new Float4(
                min(1F, max(0F, a.getX() * b.getX())),
                min(1F, max(0F, a.getY() * b.getY())),
                min(1F, max(0F, a.getZ() * b.getZ())),
                0);
    }

    /**
     * Multiply a color by a scalar
     *
     * @param a the color
     * @param b the scalar
     * @return color a multiplied by scalar b
     */
    public static Float4 mult(Float4 a, float b) {
        b = min(1F, max(0F, b));
        return new Float4(a.getX() * b, a.getY() * b, a.getZ() * b, a.getW() * b);
    }

    /**
     * Mix two colors according to a ratio
     *
     * @param a first color
     * @param b second color
     * @param t the ratio
     * @return color a multiplied by (1 - t) added to color b multiplied by t
     */
    public static Float4 mix(Float4 a, Float4 b, float t) {
        return new Float4(
                t * b.getX() + (1F - t) * a.getX(),
                t * b.getY() + (1F - t) * a.getY(),
                t * b.getZ() + (1F - t) * a.getZ(),
                0
        );
    }

    /**
     * Convert a Float4 color to an INT_RGB
     *
     * @param color a color represented by a Float4
     * @return the color as an int
     */
    public static int toInt(Float4 color) {
        int r = (int) (color.getX() * 255);
        int g = (int) (color.getY() * 255);
        int b = (int) (color.getZ() * 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Convert an int color to a Float4
     *
     * @param color a color represented by an INT_RGB
     * @return the color as a Float4
     */
    public static Float4 toFloat4(int color) {
        int b = (color) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int r = (color >> 16) & 0xFF;
        return new Float4(r / 255F, g / 255F, b / 255F, 0);
    }
}
