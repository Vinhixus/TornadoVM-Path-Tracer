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
package com.vinhderful.raytracer.renderer;

import static com.vinhderful.raytracer.misc.World.LIGHT_INDEX;
import static com.vinhderful.raytracer.utils.Angle.TO_RADIANS;

import java.util.stream.IntStream;

import com.vinhderful.raytracer.utils.BodyOps;
import com.vinhderful.raytracer.utils.Color;
import com.vinhderful.raytracer.utils.Float4Ext;

import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.math.TornadoMath;
import uk.ac.manchester.tornado.api.types.arrays.FloatArray;
import uk.ac.manchester.tornado.api.types.arrays.IntArray;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat4;
import uk.ac.manchester.tornado.api.types.vectors.Float4;

/**
 * The Renderer class contains the main parallelized render method
 */
public class Renderer {

    /**
     * Calculate the OpenGL style x coordinate of the canvas, where (0, 0) is the middle of the screen
     *
     * @param width
     *     the width of the canvas
     * @param height
     *     the height of the canvas
     * @param x
     *     the pixel's x coordinate
     * @return the normalized OpenGL-style x coordinate
     */
    public static float getNormalizedX(int width, int height, int x) {
        if (width > height)
            return (x - width * 0.5F + height * 0.5F) / height * 2 - 1;
        else
            return x * 2F / width - 1;
    }

    /**
     * Calculate the OpenGL style y coordinate of the canvas, where (0, 0) is the middle of the screen
     *
     * @param width
     *     the width of the canvas
     * @param height
     *     the height of the canvas
     * @param y
     *     the pixel's y coordinate
     * @return the normalized OpenGL-style y coordinate
     */
    public static float getNormalizedY(int width, int height, int y) {
        if (width > height)
            return -(y * 2F / height - 1);
        else
            return -((y - height * 0.5F + width * 0.5F) / width * 2 - 1);
    }

    /**
     * The main render function takes information from the INPUT BUFFERS, performs ray tracing and calculates an
     * INT_RGB value for each pixel writing to the OUTPUT BUFFER
     *
     * @param pixels
     *     OUTPUT BUFFER - int array of size width * height, where the calculated
     *     INT_RGB pixel colors are written
     * @param dimensions
     *     INPUT BUFFER - 2 element int array containing dimensions of the canvas
     *     0 - width; 1 - height
     * @param camera
     *     INPUT BUFFER - 6 element float array containing camera properties
     *     0, 1, 2 - x, y, z coordinates of position; 3, 4 - yaw, pitch; 5 - fov
     * @param rayTracingProperties
     *     INPUT BUFFER - 2 element int array containing:
     *     0 - shadow sample size; 1 - reflection bounce limit
     * @param bodyPositions
     *     INPUT BUFFER - VectorFloat4 containing positions of the objects in the scene
     * @param bodySizes
     *     INPUT BUFFER - VectorFloat4 containing sizes of the objects in the scene
     * @param bodyColors
     *     INPUT BUFFER - VectorFloat4 containing colors of the objects in the scene
     * @param bodyReflectivities
     *     INPUT BUFFER - VectorFloat4 containing reflectivities of the objects in the scene
     * @param skybox
     *     INPUT BUFFER - VectorFloat4 representing the skybox colors
     * @param skyboxDimensions
     *     INPUT BUFFER - 2 element int array containing the dimensions of the skybox image
     *     0 - skybox image width; 1 - skybox image height
     */
    public static void render(IntArray pixels,
                              IntArray dimensions,
                              FloatArray camera,
                              IntArray rayTracingProperties,
                              VectorFloat4 bodyPositions,
                              VectorFloat bodySizes,
                              VectorFloat4 bodyColors,
                              VectorFloat bodyReflectivities,
                              VectorFloat4 skybox,
                              IntArray skyboxDimensions) {

        // Relatively to the viewport, the camera will be placed in the middle, with exactly one unit of distance to
        // the viewport calculated by the field of view (camera[5] = fov)
        // https://docs.microsoft.com/en-us/windows/win32/direct3d9/viewports-and-clipping
        float relativeValue = -1 / TornadoMath.tan(camera.get(5) * 0.5F * TO_RADIANS);

        Float4 relativeCameraPosition = new Float4(0, 0, relativeValue, 0);
        Float4 cameraPosition = new Float4(camera.get(0), camera.get(1), camera.get(2), 0);

        // Get dimensions of the viewport
        int width = dimensions.get(0);
        int height = dimensions.get(1);

        // Get ray tracing properties
        int shadowSampleSize = rayTracingProperties.get(0);
        int reflectionBounceLimit = rayTracingProperties.get(1);

        // The main parallel loop - each pixel color can be calculated independently of one another
        for (@Parallel int x = 0; x < width; x++)
            for (@Parallel int y = 0; y < height; y++) {

                float normalizedX = getNormalizedX(width, height, x);
                float normalizedY = getNormalizedY(width, height, y);

                // Acquire the OpenGL-style coordinates of the pixel, where 0, 0 is the middle
                Float4 normalizedCoords = new Float4(normalizedX, normalizedY, 0, 0);

                // Acquire direction to each pixel by rotating it around the camera yaw and pitch
                Float4 rayDirection = Float4Ext.rotate(Float4.normalise(Float4.sub(normalizedCoords, relativeCameraPosition)), camera.get(3), camera.get(4));

                // Shoot ray into the scene to get the closest hit
                Float4 hit = BodyOps.getClosestHit(bodyPositions, bodySizes, cameraPosition, rayDirection);
                int hitIndex = (int) hit.getW();

                // If the ray hits an object
                if (hitIndex != -1) {

                    // If the hit object is the light source, then simply paint the light source's color
                    // This will give a flat white circle is a white sphere light
                    if (hitIndex == LIGHT_INDEX) {
                        //pixels.set(x + y * width,  Color.toInt(bodyColors.get(LIGHT_INDEX)));
                    }

                    // If the hit object is not a light source, then compute the pixel color after calculating
                    // shading reflections and shadows
                    else {
                        Float4 hitPosition = new Float4(hit.getX(), hit.getY(), hit.getZ(), 0);
                        Float4 pixelColor = RayTracer.getPixelColor(hitIndex, hitPosition, cameraPosition, rayDirection, bodyPositions, bodySizes, bodyColors, bodyReflectivities, skybox,
                                skyboxDimensions, shadowSampleSize, reflectionBounceLimit);

                        pixels.set(x + y * width, Color.toInt(pixelColor));
                    }
                }

                // If the ray doesn't hit anny objects, then draw the background skybox
                else {
                    pixels.set(x + y * width, Color.toInt(BodyOps.getSkyboxColor(skybox, skyboxDimensions, rayDirection)));
                }
            }
    }

    public static void renderWithParallelStreams(IntArray pixels, IntArray dimensions, FloatArray camera, IntArray rayTracingProperties, VectorFloat4 bodyPositions, VectorFloat bodySizes,
            VectorFloat4 bodyColors, VectorFloat bodyReflectivities, VectorFloat4 skybox, IntArray skyboxDimensions) {

        // Relatively to the viewport, the camera will be placed in the middle, with exactly one unit of distance to
        // the viewport calculated by the field of view (camera[5] = fov)
        // https://docs.microsoft.com/en-us/windows/win32/direct3d9/viewports-and-clipping
        Float4 relativeCameraPosition = new Float4(0, 0, -1 / TornadoMath.tan(camera.get(5) * 0.5F * TO_RADIANS), 0);
        Float4 cameraPosition = new Float4(camera.get(0), camera.get(1), camera.get(2), 0);

        // Get dimensions of the viewport
        int width = dimensions.get(0);
        int height = dimensions.get(1);

        // Get ray tracing properties
        int shadowSampleSize = rayTracingProperties.get(0);
        int reflectionBounceLimit = rayTracingProperties.get(1);

        // The main parallel loop - each pixel color can be calculated independently of one another
        IntStream.range(0, width).parallel().forEach(x -> {
            IntStream.range(0, height).parallel().forEach(y -> {
                // Acquire the OpenGL-style coordinates of the pixel, where 0, 0 is the middle
                Float4 normalizedCoords = new Float4(getNormalizedX(width, height, x), getNormalizedY(width, height, y), 0, 0);

                // Acquire direction to each pixel by rotating it around the camera yaw and pitch
                Float4 rayDirection = Float4Ext.rotate(Float4.normalise(Float4.sub(normalizedCoords, relativeCameraPosition)), camera.get(3), camera.get(4));

                // Shoot ray into the scene to get the closest hit
                Float4 hit = BodyOps.getClosestHit(bodyPositions, bodySizes, cameraPosition, rayDirection);
                int hitIndex = (int) hit.getW();

                // If the ray hits an object
                if (hitIndex != -1) {

                    // If the hit object is the light source, then simply paint the light source's color
                    // This will give a flat white circle is a white sphere light
                    if (hitIndex == LIGHT_INDEX) {
                        //pixels.set(x + y * width, Color.toInt(bodyColors.get(LIGHT_INDEX)));
                    }

                    // If the hit object is not a light source, then compute the pixel color after calculating
                    // shading reflections and shadows
                    else {
                        Float4 hitPosition = new Float4(hit.getX(), hit.getY(), hit.getZ(), 0);
                        Float4 pixelColor = RayTracer.getPixelColor(hitIndex, hitPosition, cameraPosition, rayDirection, bodyPositions, bodySizes, bodyColors, bodyReflectivities, skybox,
                                skyboxDimensions, shadowSampleSize, reflectionBounceLimit);

                        pixels.set(x + y * width, Color.toInt(pixelColor));
                    }
                }

                // If the ray doesn't hit anny objects, then draw the background skybox
                else {
                    pixels.set(x + y * width, Color.toInt(BodyOps.getSkyboxColor(skybox, skyboxDimensions, rayDirection)));
                }
            });
        });
    }
}
