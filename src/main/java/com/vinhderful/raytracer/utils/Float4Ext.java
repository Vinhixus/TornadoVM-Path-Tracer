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

import uk.ac.manchester.tornado.api.math.TornadoMath;
import uk.ac.manchester.tornado.api.types.vectors.Float4;

import static com.vinhderful.raytracer.utils.Angle.TO_RADIANS;

/**
 * Additional Float4 operations
 */
public class Float4Ext {

    /**
     * Given a vector a yaw and a pitch value, return the vector rotated by the given yaw and pitch
     *
     * @param a     the vector
     * @param yaw   the yaw value in degrees
     * @param pitch the pitch value in degrees
     * @return Float4 containing the 3D vector a rotated around yaw and pitch
     */
    public static Float4 rotate(Float4 a, float yaw, float pitch) {

        // Convert yaw and pitch to radians
        float _yaw = yaw * TO_RADIANS;
        float _pitch = pitch * TO_RADIANS;

        // Perform horizontal rotation
        float _y = a.getY() * TornadoMath.cos(_pitch) - a.getZ() * TornadoMath.sin(_pitch);
        float _z = a.getY() * TornadoMath.sin(_pitch) + a.getZ() * TornadoMath.cos(_pitch);

        // Perform vertical rotation
        float _x = a.getX() * TornadoMath.cos(_yaw) + _z * TornadoMath.sin(_yaw);
        _z = -a.getX() * TornadoMath.sin(_yaw) + _z * TornadoMath.cos(_yaw);

        return new Float4(_x, _y, _z, 0);
    }

    /**
     * Return the distance in 3D space between two points
     *
     * @param a first vector
     * @param b second vector
     * @return float containing the distance between a and b
     */
    public static float distance(Float4 a, Float4 b) {
        float x = a.getX() - b.getX();
        float y = a.getY() - b.getY();
        float z = a.getZ() - b.getZ();
        return TornadoMath.sqrt(x * x + y * y + z * z);
    }

    /**
     * Calculate the cross product between two vectors
     *
     * @param a first vector
     * @param b seconds vector
     * @return float containing the cross product between a and b
     */
    public static Float4 cross(Float4 a, Float4 b) {
        return new Float4(
                a.getY() * b.getZ() - a.getZ() * b.getY(),
                a.getZ() * b.getX() - a.getX() * b.getZ(),
                a.getX() * b.getY() - a.getY() * b.getX(),
                0);
    }

    /**
     * Get and arbitrary perpendicular vector to a vector
     *
     * @param a the vector
     * @return an arbitrary perpendicular vector to a
     */
    public static Float4 perpVector(Float4 a) {
        if (a.getY() == 0 && a.getZ() == 0)
            return cross(a, new Float4(0, 1, 0, 0));

        return cross(a, new Float4(1, 0, 0, 0));
    }
}
