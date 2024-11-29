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
 * Represents a sphere light using position, radius and color
 */
public class Light extends Body {


    /**
     * Construct a light given its position, radius and color
     *
     * @param position position represented by a vector in 3d space
     * @param radius   radius of the sphere
     * @param color    color of the body represented by RGB values
     */
    public Light(Float4 position, float radius, Float4 color) {
        super(position, radius, color, 0);
    }
}
