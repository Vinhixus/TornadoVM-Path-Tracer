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


import com.vinhderful.raytracer.utils.BodyOps;
import com.vinhderful.raytracer.utils.Color;
import com.vinhderful.raytracer.utils.Float4Ext;
import uk.ac.manchester.tornado.api.math.TornadoMath;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat4;
import uk.ac.manchester.tornado.api.types.vectors.Float4;

import static uk.ac.manchester.tornado.api.math.TornadoMath.floatPI;
import static uk.ac.manchester.tornado.api.math.TornadoMath.max;
import static uk.ac.manchester.tornado.api.math.TornadoMath.pow;

/**
 * The Shader class contains operations to perform Blinn-Phong shading, calculating reflections and soft shadows
 */
public class Shader {

    /**
     * In a scene where not every source that emits light can be sampled, we use a constant ambient value that signifies
     * how much an object is illuminated regardless of being in a shadow. Value should be in the range of [0, 1]
     */
    public static final float AMBIENT_STRENGTH = 0.2F;

    /**
     * Similarly to ambient strength, shadow brightness determines how bright object-cast shadows are in the scene
     */
    public static final float SHADOW_BRIGHTNESS = 0.35F;

    /**
     * The fibonacci golden angle used to uniformly sample the sphere light
     * https://en.wikipedia.org/wiki/Golden_angle
     */
    public static final float PHI = floatPI() * (3 - TornadoMath.sqrt(5));

    /**
     * The maximum reflectivity constant is derived from the Blinn-Phong specular exponents, the more shiny an object is,
     * the more reflective it should be. The reflectivity of an object is thus determined by the MAX_REFLECTIVITY
     * constant, where for each object: (its bodyReflectivity / MAX_REFLECTIVITY) will yield the reflectivity of the object.
     * The higher the MAX_REFLECTIVITY, the less reflective the objects will be.
     */
    public static final float MAX_REFLECTIVITY = 128F;

    /**
     * Given a ray and a hit object, mix ambient, diffuse and specular lighting to create a 3D shaded appearance
     * for the given object and return the color at the hit position
     * The implemented model is the Blinn-Phong lighting model:
     * https://learnopengl.com/Lighting/Basic-Lighting
     * https://learnopengl.com/Advanced-Lighting/Advanced-Lighting
     *
     * @param hitIndex
     *     the index of the hit object
     * @param hitPosition
     *     the position of the hit
     * @param rayOrigin
     *     the ray's origin
     * @param bodyPosition
     *     the position of the hit object
     * @param bodyColor
     *     the color of the hit object
     * @param bodyReflectivity
     *     the reflectivity/shininess of the hit object
     * @param lightPosition
     *     the position of the light
     * @return a Float4 containing the color of the object at the given hit position after applying the Blinn-Phong
     *     shading model
     */
    public static Float4 getBlinnPhong(int hitIndex, Float4 hitPosition, Float4 rayOrigin, Float4 bodyPosition, Float4 bodyColor, float bodyReflectivity, Float4 lightPosition) {

        // Ambient and diffuse lighting
        float diffuse = max(AMBIENT_STRENGTH, getDiffuse(hitIndex, hitPosition, bodyPosition, lightPosition));

        // Specular highlight
        float specular = getSpecular(hitIndex, hitPosition, rayOrigin, bodyPosition, bodyReflectivity, lightPosition);

        // Mix the elements
        return Color.mult(Color.add(bodyColor, specular), diffuse);
    }

    /**
     * Given hit object and the light position, apply diffuse shading according to the Blinn-Phong model and return the
     * diffuse factor of the object at the hit position
     * https://learnopengl.com/Lighting/Basic-Lighting
     *
     * @param hitIndex
     *     the index of the hit object
     * @param hitPosition
     *     the position of the hit
     * @param bodyPosition
     *     the position of the hit object
     * @param lightPosition
     *     the position of the light
     * @return a float containing the diffuse factor of the object at the given position - i.e. how dark the object
     *     is at the position according to the light
     */
    public static float getDiffuse(int hitIndex, Float4 hitPosition, Float4 bodyPosition, Float4 lightPosition) {

        // Get the normal of the object at the given point
        Float4 hitNormal = BodyOps.getNormal(hitIndex, bodyPosition, hitPosition);

        // Calculate the light direction from the given point to the light
        Float4 lightDirection = Float4.normalise(Float4.sub(lightPosition, hitPosition));

        // Dot product determines how shaded the point is - the larger the angle, the darker
        return Float4.dot(hitNormal, lightDirection);
    }

    /**
     * Given hit object, the ray and the light position, apply specular shading according to the Blinn-Phong model and
     * return the specular factor of the object at the hit position
     * https://learnopengl.com/Advanced-Lighting/Advanced-Lighting
     *
     * @param hitIndex
     *     the index of the hit object
     * @param hitPosition
     *     the position of the hit
     * @param rayOrigin
     *     the ray's origin
     * @param bodyPosition
     *     the position of the hit object
     * @param bodyReflectivity
     *     the reflectivity/shininess of the hit object
     * @param lightPosition
     *     the position of the light
     * @return a float containing the specular factor of the object at the given position - i.e. how bright the specular
     *     highlight is at the position according to the light
     */
    public static float getSpecular(int hitIndex, Float4 hitPosition, Float4 rayOrigin, Float4 bodyPosition, float bodyReflectivity, Float4 lightPosition) {

        // Calculate direction to the camera and to the light
        Float4 viewDirection = Float4.normalise(Float4.sub(rayOrigin, hitPosition));
        Float4 lightDirection = Float4.normalise(Float4.sub(lightPosition, hitPosition));
        Float4 hitNormal = BodyOps.getNormal(hitIndex, bodyPosition, hitPosition);

        // Get the halfway direction according to the Blinn model
        Float4 halfwayDirection = Float4.normalise(Float4.add(lightDirection, viewDirection));

        // Dot product - i.e. angle between light and camera determines specular factor
        float specularFactor = max(0, Float4.dot(hitNormal, halfwayDirection));

        // Specular energy conservation
        // https://www.rorydriscoll.com/2009/01/25/energy-conservation-in-games/
        float k = (8.0F + bodyReflectivity) / (8.0F * floatPI());

        // Calculate specular factor according to the hit object's reflectivity/shininess exponent
        return k * pow(specularFactor, bodyReflectivity) * (bodyReflectivity / MAX_REFLECTIVITY);
    }

    /**
     * Given a hit object, the objects in the scene and the light, calculate a factor that determines if the point is in
     * a cast shadow. Soft shadows are calculated by sampling the light for a penumbra effect.
     * A sphere light from a circular area represented by a great circle of the sphere light facing the shadow feeler
     * ray from the hit position to the light.
     * This sphere is uniformly sampled using the sunflower seed arrangement/vogel spiral phenomenon:
     * https://www.codeproject.com/Articles/1221341/The-Vogel-Spiral-Phenomenon
     *
     * @param hitPosition
     *     the position of the hit
     * @param bodyPositions
     *     the structure representing the positions of the objects in the scene
     * @param bodySizes
     *     the structure representing the sizes of the objects in the scene
     * @param lightPosition
     *     the position of the light
     * @param lightSize
     *     the size of the light
     * @param sampleSize
     *     how many samples to take from the light for the soft shadow effect
     * @return a float factor that determines how dark the hit object should be at the hit position according to
     *     the cast shadows
     */
    public static float getShadow(Float4 hitPosition, VectorFloat4 bodyPositions, VectorFloat bodySizes, Float4 lightPosition, float lightSize, int sampleSize) {

        // Get a main shadow feeler ray direction from the hit position to the light
        Float4 n = Float4.normalise(Float4.sub(hitPosition, lightPosition));

        // Acquire two arbitrary perpendicular vectors to define a coordinate system according to where the great circle
        // is facing
        Float4 u = Float4Ext.perpVector(n);
        Float4 v = Float4Ext.cross(u, n);

        // Initialise ray hit counter
        int raysHit = 0;

        // Start sampling
        for (int i = 0; i < sampleSize; i++) {

            // Generate sampling points on a circle according to the Vogel Spiral Phenomenon
            float t = PHI * i;
            float r = TornadoMath.sqrt((float) i / sampleSize);

            float x = 2 * lightSize * r * TornadoMath.cos(t);
            float y = 2 * lightSize * r * TornadoMath.sin(t);

            // Translate the points to the great circle
            Float4 samplePoint = Float4.add(Float4.add(lightPosition, Float4.mult(u, x)), Float4.mult(v, y));

            // Define the ray direction according to the sample point
            Float4 rayDirection = Float4.normalise(Float4.sub(samplePoint, hitPosition));

            // Add a tiny offset to the ray origin to avoid hitting the same object
            Float4 rayOrigin = Float4.add(hitPosition, Float4.mult(rayDirection, 0.001F));

            // Check if ray hits an object, if yes, then the point is in cast shadow
            if (BodyOps.intersects(bodyPositions, bodySizes, rayOrigin, rayDirection, samplePoint))
                raysHit++;
        }

        // Calculate soft shadows according to how many of the sampled shadow feelers hit an object
        if (raysHit == 0)
            return 1;
        else
            return 1 - (float) raysHit / (sampleSize * (1 + SHADOW_BRIGHTNESS));
    }
}
