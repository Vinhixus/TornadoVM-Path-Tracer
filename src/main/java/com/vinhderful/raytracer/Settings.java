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
package com.vinhderful.raytracer;

import uk.ac.manchester.tornado.api.types.vectors.Float3;

/**
 * Settings class contains variables users can tweak to change the rendering environment and the GUI
 */
public class Settings {

    /**
     * Width of the viewport resolution
     */
    public static final int WIDTH = 1280;

    /**
     * Height of the viewport resolution
     */
    public static final int HEIGHT = 736;

    /**
     * Initial shadow sample size the program opens up with
     */
    public static final int INITIAL_SHADOW_SAMPLE_SIZE = 1;

    /**
     * Maximum shadow sample size the user can set the slider to
     */
    public static final int MAX_SHADOW_SAMPLE_SIZE = 400;

    /**
     * Initial reflection bounces the program opens up with
     */
    public static final int INITIAL_REFLECTION_BOUNCES = 1;

    /**
     * Maximum reflection bounces the user can set the slider to
     */
    public static final int MAX_REFLECTION_BOUNCES = 6;

    /**
     * Initial position of the camera
     */
    public static final Float3 INITIAL_CAMERA_POSITION = new Float3(0, -5, -15);

    /**
     * Initial yaw of the camera (horizontal rotation)
     */
    public static final float INITIAL_CAMERA_YAW = 0;

    /**
     * Initial pitch of the camera (vertical rotation)
     */
    public static final float INITIAL_CAMERA_PITCH = 7;

    /**
     * Initial field of view of the camera (horizontal angle determining the part of the world we can see)
     */
    public static final float INITIAL_CAMERA_FOV = 50;
}
