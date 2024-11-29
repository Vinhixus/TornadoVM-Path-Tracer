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

import com.vinhderful.raytracer.misc.World;
import uk.ac.manchester.tornado.api.math.TornadoMath;
import uk.ac.manchester.tornado.api.types.arrays.IntArray;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat4;
import uk.ac.manchester.tornado.api.types.vectors.Float4;

/**
 * Operations on the objects in the scene
 */
public class BodyOps {

    /**
     * Given a hit object's position, size and a ray's origin and direction, return the intersection point of the ray
     * and the object.
     * -------------------------------------------------------------------------------------------------------------
     * - The first three elements of the returned Float4 represents the X, Y and Z values of the intersection point
     * respectively, (-1, -1, -1) is returned if the ray does not intersect with the object.
     * - The fourth element of the returned Float4 represents a boolean in the form of a float: 0 if there was an
     * intersection, and -1 if the ray does not intersect with the object.
     *
     * @param hitIndex     the index of the object
     * @param position     the position of the object
     * @param size         the size of the object
     * @param rayOrigin    the origin of the ray
     * @param rayDirection the direction of the ray
     * @return the intersection point alongside a variable indicating whether there was an intersection
     */
    public static Float4 getIntersection(int hitIndex, Float4 position, float size,
                                         Float4 rayOrigin, Float4 rayDirection) {

        // Define Float4 to return if there is no intersection
        final Float4 NO_INTERSECTION = new Float4(-1F, -1F, -1F, -1F);

        // Light
        if (hitIndex == World.LIGHT_INDEX) {

            Float4 min = Float4.sub(position, size * 0.5F);
            Float4 max = Float4.add(position, size * 0.5F);

            float dX = 1.0F / rayDirection.getX();
            float dY = 1.0F / rayDirection.getY();
            float dZ = 1.0F / rayDirection.getZ();

            float t1 = (min.getX() - rayOrigin.getX()) * dX;
            float t2 = (max.getX() - rayOrigin.getX()) * dX;
            float t3 = (min.getY() - rayOrigin.getY()) * dY;
            float t4 = (max.getY() - rayOrigin.getY()) * dY;
            float t5 = (min.getZ() - rayOrigin.getZ()) * dZ;
            float t6 = (max.getZ() - rayOrigin.getZ()) * dZ;

            float tMin = TornadoMath.max(TornadoMath.max(TornadoMath.min(t1, t2), TornadoMath.min(t3, t4)), TornadoMath.min(t5, t6));
            float tMax = TornadoMath.min(TornadoMath.min(TornadoMath.max(t1, t2), TornadoMath.max(t3, t4)), TornadoMath.max(t5, t6));

            if (tMax < 0 || tMin > tMax)
                return NO_INTERSECTION;

            if (tMin < 0)
                return Float4.add(rayOrigin, Float4.mult(rayDirection, tMax));
            else
                return Float4.add(rayOrigin, Float4.mult(rayDirection, tMin));
        }

        // Plane
        else if (hitIndex == World.PLANE_INDEX) {
            float t = -(rayOrigin.getY() - position.getY()) / rayDirection.getY();
            if (t > 0 && Float.isFinite(t)) {
                Float4 intersection = Float4.add(rayOrigin, Float4.mult(rayDirection, t));
                if (TornadoMath.abs(intersection.getX()) > size * 0.5F || TornadoMath.abs(intersection.getZ()) > size * 0.5F)
                    return NO_INTERSECTION;
                else
                    return intersection;
            }

            return NO_INTERSECTION;
        }

        // Sphere
        else {
            float t = Float4.dot(Float4.sub(position, rayOrigin), rayDirection);
            Float4 p = Float4.add(rayOrigin, Float4.mult(rayDirection, t));

            float y = Float4Ext.distance(position, p);

            if (y < size) {
                float t1 = t - TornadoMath.sqrt(size * size - y * y);
                if (t1 > 0) return Float4.add(rayOrigin, Float4.mult(rayDirection, t1));
                else return NO_INTERSECTION;
            } else
                return NO_INTERSECTION;
        }
    }


    /**
     * Given the positions and sizes of the objects in the scene, and a shadow feeler ray represented by an origin and
     * a direction vector, alongside the position of the light, return whether the shadow feeler hits any objects,
     * thus blocking the light
     *
     * @param bodyPositions the structure representing the positions of the objects in the scene
     * @param bodySizes     the structure representing the sizes of the objects in the scene
     * @param rayOrigin     the origin point of the ray
     * @param rayDirection  the direction of the ray
     * @param lightPosition the position of the light
     * @return if the shadow feeler hits any objects
     */
    public static boolean intersects(VectorFloat4 bodyPositions, VectorFloat bodySizes,
                                     Float4 rayOrigin, Float4 rayDirection,
                                     Float4 lightPosition) {

        // Calculate the distance to the light
        float lightDistance = Float4Ext.distance(rayOrigin, lightPosition);

        // Initialise intersects boolean
        boolean intersects = false;

        // Loop over objects in the scene, excluding light and plane, break out of loop when intersection is found
        for (int i = World.SPHERES_START_INDEX; i < bodyPositions.getLength() && !intersects; i++) {

            // Calculate the intersection of the ray with the current object
            Float4 intersection = BodyOps.getIntersection(i, bodyPositions.get(i), bodySizes.get(i), rayOrigin, rayDirection);

            // If the ray hits the object, and distance to the current object is smaller than the distance to the light,
            // then the object is in the way of the light and blocking it
            if (intersection.getW() == 0 && Float4Ext.distance(intersection, rayOrigin) < lightDistance)
                intersects = true;
        }

        // Return result
        return intersects;
    }

    /**
     * Given the positions and sizes of the objects in the scene, and a ray represented by an origin and a direction
     * vector, return the position of the first point the ray hits, alongside the index of the hit object.
     * -------------------------------------------------------------------------------------------------------------
     * - The first three elements of the returned Float4 represents the X, Y and Z values of the hit position
     * respectively, (-1, -1, -1) is returned if no objects are hit.
     * - The fourth element of the returned Float4 represents the index of the hit object, -1 is returned if no objects
     * are hit
     *
     * @param bodyPositions the structure representing the positions of the objects in the scene
     * @param bodySizes     the structure representing the sizes of the objects in the scene
     * @param rayOrigin     the origin point of the ray
     * @param rayDirection  the direction of the ray
     * @return the closest hit position alongside the hit objects index
     */
    public static Float4 getClosestHit(VectorFloat4 bodyPositions, VectorFloat bodySizes,
                                       Float4 rayOrigin, Float4 rayDirection) {

        // Initialise closes hit as a no-hit
        Float4 closestHit = new Float4(-1F, -1F, -1F, -1F);
        float closestHitDistance = Float.MAX_VALUE;

        // Loop over objects in the scene
        for (int i = 0; i < bodyPositions.getLength(); i++) {

            // Calculate the intersection of the ray with the current object
            Float4 intersection = getIntersection(i, bodyPositions.get(i), bodySizes.get(i), rayOrigin, rayDirection);
            float intersectionDistance = Float4Ext.distance(intersection, rayOrigin);

            // If the ray hits the object, and the previous closest hit distance is larger than the distance of
            // the ray origin and the current object, then the current hit is the closest hit
            if (intersection.getW() == 0 && (closestHit.getW() == -1F || closestHitDistance > intersectionDistance)) {
                closestHit = new Float4(intersection.getX(), intersection.getY(), intersection.getZ(), i);
                closestHitDistance = intersectionDistance;
            }
        }

        // Return the resulting closest hit
        return closestHit;
    }

    /**
     * Given a position of an object and a point on it's surface, return the normal vector of the object's surface at
     * the given point
     *
     * @param hitIndex the index of the object
     * @param position the position of the object
     * @param point    the point of hte surface
     * @return the normal vector of the surface
     */
    public static Float4 getNormal(int hitIndex, Float4 position, Float4 point) {

        // Plane normal is an up vector in the y direction
        if (hitIndex == World.PLANE_INDEX) {
            return new Float4(0, 1F, 0, 0);
        }

        // Sphere normal will be the direction from sphere origin to the surface point
        else return Float4.normalise(Float4.sub(point, position));
    }

    /**
     * Given ab object's index, a point on the surface and the set of body colors of the objects in the scene,
     * return the color of the object at the given point on the object's surface
     *
     * @param hitIndex   the index of the object
     * @param point      the point of hte surface
     * @param bodyColors the structure containing the colors of the objects in the scene
     * @return the color of the object at the given point on the surface
     */
    public static Float4 getColor(int hitIndex, Float4 point, VectorFloat4 bodyColors) {

        // Get checkerboard pattern for plane
        if (hitIndex == World.PLANE_INDEX) {

            if ((int) (TornadoMath.floor(point.getX()) + TornadoMath.floor(point.getZ())) % 2 == 0)
                // GRAY
                return new Float4(0.4F, 0.4F, 0.4F, 0);
            else
                // DARK GRAY
                return new Float4(0.2F, 0.2F, 0.2F, 0);
        }

        // Else simply return body color
        else return bodyColors.get(hitIndex);
    }

    /**
     * Get the color of the skybox at a certain point on the surface
     * given by the direction pointing from the origin of the sphere to the surface point
     *
     * @param skybox           the structure representing the pixel colors of the skybox image
     * @param skyBoxDimensions the int array containing the width and the height of the skybox image
     * @param direction        the direction from the sphere origin to the surface
     * @return the UV-mapped color of the skybox at in the specified direction
     */
    public static Float4 getSkyboxColor(VectorFloat4 skybox, IntArray skyBoxDimensions, Float4 direction) {

        // Convert unit vector to texture coordinates
        // https://en.wikipedia.org/wiki/UV_mapping#Finding_UV_on_a_sphere
        float u = 0.5F + TornadoMath.atan2(direction.getZ(), direction.getX()) / (2 * TornadoMath.floatPI());
        float v = 0.5F - TornadoMath.asin(direction.getY()) / TornadoMath.floatPI();

        // Get color from the skybox VectorFloat4
        int x = (int) (u * (skyBoxDimensions.get(0) - 1));
        int y = (int) (v * (skyBoxDimensions.get(1) - 1));
        return skybox.get(x + y * skyBoxDimensions.get(0));
    }
}
