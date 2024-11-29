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
import static com.vinhderful.raytracer.misc.World.PLANE_INDEX;
import static com.vinhderful.raytracer.renderer.Shader.AMBIENT_STRENGTH;
import static com.vinhderful.raytracer.renderer.Shader.MAX_REFLECTIVITY;
import static com.vinhderful.raytracer.renderer.Shader.getDiffuse;
import static com.vinhderful.raytracer.renderer.Shader.getShadow;
import static com.vinhderful.raytracer.renderer.Shader.getSpecular;
import static uk.ac.manchester.tornado.api.math.TornadoMath.max;

import com.vinhderful.raytracer.utils.BodyOps;
import com.vinhderful.raytracer.utils.Color;
import uk.ac.manchester.tornado.api.types.arrays.IntArray;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat4;
import uk.ac.manchester.tornado.api.types.vectors.Float4;

/**
 * The Ray Tracer class contains algorithms that bounce rays around the scene to gather color, shading and reflection
 * information
 */
public class RayTracer {

    /**
     * Given a hit object and the ray that hit the object, bounce the ray according to the reflection bounce limit
     * around the scene to gather the color of the reflection
     *
     * @param hitIndex
     *     the index of the hit object
     * @param hitPosition
     *     the position of the hit object
     * @param rayDirection
     *     the ray's direction
     * @param bodyPositions
     *     the structure containing the positions of the objects in the scene
     * @param bodySizes
     *     the structure containing the sizes of the objects in the scene
     * @param bodyColors
     *     the structure containing the colors of the objects in the scene
     * @param bodyReflectivities
     *     the structure containing the reflectivities of the objects in the scene
     * @param lightPosition
     *     the position of the light source
     * @param lightSize
     *     the size of the light source
     * @param skybox
     *     the structure the colors of the skybox image
     * @param skyboxDimensions
     *     the structure the dimensions of the skybox image
     * @param shadowSampleSize
     *     the sample size to calculate soft shadows with
     * @param reflectionBounceLimit
     *     the limit of how many times the reflection can bounce
     * @return the color of the accumulated reflection
     */
    public static Float4 getReflection(int hitIndex, Float4 hitPosition, Float4 rayDirection, VectorFloat4 bodyPositions, VectorFloat bodySizes, VectorFloat4 bodyColors,
            VectorFloat bodyReflectivities, Float4 lightPosition, float lightSize, VectorFloat4 skybox, IntArray skyboxDimensions, int shadowSampleSize, int reflectionBounceLimit) {

        // Initialise an empty reflection color, a contribution factor and shading factor
        Float4 reflectionColor = new Float4(0, 0, 0, 0);
        float contribution = 1F;
        float shading = 1F;

        // Bounce the reflection ray around the scene reflectionBounceLimit times, or until we either hit the light or
        // the reflection ray doesn't hit any objects
        for (int i = 0; i < reflectionBounceLimit && hitIndex > LIGHT_INDEX; i++) {

            // Acquire the properties of the reflection ray
            Float4 hitNormal = BodyOps.getNormal(hitIndex, bodyPositions.get(hitIndex), hitPosition);
            Float4 reflectionDir = Float4.sub(rayDirection, Float4.mult(hitNormal, 2 * Float4.dot(rayDirection, hitNormal)));
            Float4 reflectionOrigin = Float4.add(hitPosition, Float4.mult(reflectionDir, 0.001F));

            // Save the reflectivity of the current object
            float reflectivity = bodyReflectivities.get(hitIndex) / MAX_REFLECTIVITY;

            // Acquire the object the reflection ray hits
            Float4 hit = BodyOps.getClosestHit(bodyPositions, bodySizes, reflectionOrigin, reflectionDir);
            hitIndex = (int) hit.getW();

            // If we hit an object
            if (hitIndex != -1) {

                // Get the hit position, the position of the object and its color and reflectivity
                hitPosition = new Float4(hit.getX(), hit.getY(), hit.getZ(), 0);
                Float4 bodyPosition = bodyPositions.get(hitIndex);
                Float4 color = BodyOps.getColor(hitIndex, hitPosition, bodyColors);
                float bodyReflectivity = bodyReflectivities.get(hitIndex);

                // If the object is not a light source, then perform shading
                if (hitIndex > LIGHT_INDEX) {
                    float diffuse = hitIndex == PLANE_INDEX ? 1F : max(AMBIENT_STRENGTH, getDiffuse(hitIndex, hitPosition, bodyPosition, lightPosition));
                    float specular = getSpecular(hitIndex, hitPosition, reflectionOrigin, bodyPosition, bodyReflectivity, lightPosition);
                    float shadow = getShadow(hitPosition, bodyPositions, bodySizes, lightPosition, lightSize, shadowSampleSize);
                    shading = diffuse * shadow;
                    color = Color.mult(Color.add(color, specular), shading);
                }

                // If we have reached the reflection bounce limit, then the current object will contribute with the
                // entire remaining contribution factor
                if (i == reflectionBounceLimit - 1) {
                    reflectionColor = Color.add(reflectionColor, Color.mult(color, contribution));
                }

                // Otherwise, the object will contribute to the reflection color depending on the reflectivity of the
                // previous object
                else
                    reflectionColor = Color.add(reflectionColor, Color.mult(color, contribution * (1 - reflectivity)));

                // Update the remaining contribution (scale contribution according to the previous object's
                // reflectivity, and shading) and the direction of the reflection ray
                contribution *= reflectivity * shading;
                rayDirection = reflectionDir;
            }

            // If no object is hit then reflect the skybox
            else {
                Float4 color = BodyOps.getSkyboxColor(skybox, skyboxDimensions, reflectionDir);
                reflectionColor = Color.add(reflectionColor, Color.mult(color, contribution));
            }
        }

        // Return the final reflection color
        return reflectionColor;
    }

    /**
     * Given a hit object with its index, the hit position, the ray's origin and direction, return it's color after
     * calculating reflections and shading
     *
     * @param hitIndex
     *     the index of the hit object
     * @param hitPosition
     *     the position of the hit object
     * @param rayOrigin
     *     the ray's origin
     * @param rayDirection
     *     the ray's direction
     * @param bodyPositions
     *     the structure containing the positions of the objects in the scene
     * @param bodySizes
     *     the structure containing the sizes of the objects in the scene
     * @param bodyColors
     *     the structure containing the colors of the objects in the scene
     * @param bodyReflectivities
     *     the structure containing the reflectivities of the objects in the scene
     * @param skybox
     *     the structure the colors of the skybox image
     * @param skyboxDimensions
     *     the structure the dimensions of the skybox image
     * @param shadowSampleSize
     *     the sample size to calculate soft shadows with
     * @param reflectionBounceLimit
     *     the limit of how many times the reflection can bounce
     * @return the color of the hit object with shading and reflections applied
     */
    public static Float4 getPixelColor(int hitIndex, Float4 hitPosition, Float4 rayOrigin, Float4 rayDirection, VectorFloat4 bodyPositions, VectorFloat bodySizes, VectorFloat4 bodyColors,
            VectorFloat bodyReflectivities, VectorFloat4 skybox, IntArray skyboxDimensions, int shadowSampleSize, int reflectionBounceLimit) {

        // Get the position and size of the light
        Float4 lightPosition = bodyPositions.get(LIGHT_INDEX);
        float lightSize = bodySizes.get(LIGHT_INDEX);

        // Get the hit object's position, color and reflectivity
        Float4 bodyPosition = bodyPositions.get(hitIndex);
        Float4 bodyColor = BodyOps.getColor(hitIndex, hitPosition, bodyColors);
        float bodyReflectivity = bodyReflectivities.get(hitIndex);

        // Calculate the reflection color
        Float4 reflectionColor = getReflection(hitIndex, hitPosition, rayDirection, bodyPositions, bodySizes, bodyColors, bodyReflectivities, lightPosition, lightSize, skybox, skyboxDimensions,
                shadowSampleSize, reflectionBounceLimit);

        // Mix the object's color and the reflection color according to its reflectivity
        Float4 color = Color.mix(bodyColor, reflectionColor, bodyReflectivity / MAX_REFLECTIVITY);

        // Calculate specular highlights and shading
        float diffuse = hitIndex == PLANE_INDEX ? 1F : max(AMBIENT_STRENGTH, getDiffuse(hitIndex, hitPosition, bodyPosition, lightPosition));
        float specular = getSpecular(hitIndex, hitPosition, rayOrigin, bodyPosition, bodyReflectivity, lightPosition);
        float shadow = getShadow(hitPosition, bodyPositions, bodySizes, lightPosition, lightSize, shadowSampleSize);
        float shading = diffuse * shadow;

        // Return final color with specular highlights and shading applied
        return Color.mult(Color.add(color, specular), shading);
    }
}
