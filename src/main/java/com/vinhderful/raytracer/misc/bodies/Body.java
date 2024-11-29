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
package com.vinhderful.raytracer.misc.bodies;

import uk.ac.manchester.tornado.api.types.vectors.Float4;

/**
 * Abstract class representing a solid body in a 3D space using its
 * position, size, color and reflectivity.
 * Class is extended by Light, Plane and Sphere
 */
public abstract class Body {

    private final float size;
    private final Float4 color;
    private final float reflectivity;

    private Float4 previousPosition;
    private Float4 position;

    /**
     * Create a body with given position, size, color and reflectivity
     *
     * @param position
     *     position represented by a vector in 3d space
     * @param size
     *     scale of the body
     * @param color
     *     color of the body represented by RGB values
     * @param reflectivity
     *     reflectivity of the object
     */
    public Body(Float4 position, float size, Float4 color, float reflectivity) {
        this.position = position;
        this.previousPosition = position.duplicate();
        this.size = size;
        this.color = color;
        this.reflectivity = reflectivity;
    }

    /**
     * Return the position of the body
     *
     * @return the position of the body
     */
    public Float4 getPosition() {
        return position;
    }

    /**
     * Set the position of this body to a given value
     *
     * @param position
     *     the value to set the position of this body to
     */
    public void setPosition(Float4 position) {
        this.position = position;
    }

    /**
     * Return the previous position of the body
     *
     * @return the previous position of the body
     */
    public Float4 getPreviousPosition() {
        return previousPosition;
    }

    /**
     * Set the position of this body to a given value
     *
     * @param position
     *     the value to set the position of this body to
     */
    public void setPreviousPosition(Float4 position) {
        this.previousPosition = position;
    }

    /**
     * Return the size of the body
     *
     * @return the size of the body
     */
    public float getSize() {
        return size;
    }

    /**
     * Return the color of the body
     *
     * @return the color of the body
     */
    public Float4 getColor() {
        return color;
    }

    /**
     * Return the reflectivity of the body
     *
     * @return the reflectivity of the body
     */
    public float getReflectivity() {
        return reflectivity;
    }
}
