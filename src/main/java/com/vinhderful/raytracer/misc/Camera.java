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

import static com.vinhderful.raytracer.utils.Angle.TO_RADIANS;
import static uk.ac.manchester.tornado.api.math.TornadoMath.max;
import static uk.ac.manchester.tornado.api.math.TornadoMath.min;

import com.vinhderful.raytracer.Settings;
import uk.ac.manchester.tornado.api.math.TornadoMath;
import uk.ac.manchester.tornado.api.types.arrays.FloatArray;
import uk.ac.manchester.tornado.api.types.vectors.Float3;

/**
 * Represents a scene camera with:
 * position, yaw, pitch and field of view,
 * movement speed and mouse sensitivity for user control
 */
public class Camera {

    /**
     * Mouse sensitivity for look-around
     * Move speed for keyboard input movement
     */
    private static final float MOUSE_SENSITIVITY = 0.5F;
    private static final float MOVE_SPEED = 0.1F;
    // Up direction
    private final Float3 upVector;
    private final float planeHeight;
    /**
     * Position, yaw, pitch and field of view
     */
    private Float3 position;
    private float yaw;
    private float pitch;
    private float fov;
    private float moveSpeed;
    /**
     * The double buffer as input buffer for rendering
     */
    private FloatArray buffer;

    /**
     * Create a camera given a world to know where the plane height is,
     * camera will not be allowed to go under the plane
     *
     * @param world
     *     the world containing the plane
     */
    public Camera(World world) {
        position = Settings.INITIAL_CAMERA_POSITION;
        yaw = Settings.INITIAL_CAMERA_YAW;
        pitch = Settings.INITIAL_CAMERA_PITCH;
        fov = Settings.INITIAL_CAMERA_FOV;

        // Up direction is positive y direction
        upVector = new Float3(0, 1F, 0);
        moveSpeed = MOVE_SPEED;
        planeHeight = world.getPlane().getPosition().getY() + 0.001F;

        allocateAndInitializeBuffer();
    }

    /**
     * Allocate memory space and initialise the input buffer
     */
    private void allocateAndInitializeBuffer() {
        buffer = FloatArray.fromElements(position.getX(), position.getY(), position.getZ(), yaw, pitch, fov);
    }

    /**
     * Return the memory address of the input buffer
     *
     * @return the pointer pointing to the input buffer array
     */
    public FloatArray getBuffer() {
        return buffer;
    }

    /**
     * Update the input buffer for rendering
     */
    public void updateBuffer() {
        buffer.set(0, position.getX());
        buffer.set(1, position.getY());
        buffer.set(2, position.getZ());
        buffer.set(3, yaw);
        buffer.set(4, pitch);
        buffer.set(5, fov);
    }

    /**
     * Update camera position on movement
     */
    public void updatePositionOnMovement(boolean fwd, boolean back, boolean strafeL, boolean strafeR, boolean up, boolean down) {

        // Get yaw and pitch in radians
        float _yaw = yaw * TO_RADIANS;
        float _pitch = -pitch * TO_RADIANS;

        // Calculate forward pointing vector from yaw and pitch
        Float3 fwdVector = Float3.normalise(new Float3(TornadoMath.sin(_yaw) * TornadoMath.cos(_pitch), TornadoMath.sin(_pitch), TornadoMath.cos(_yaw) * TornadoMath.cos(_pitch)));

        // Calculate left and right pointing vector from forward and up vectors
        Float3 leftVector = Float3.normalise(Float3.cross(fwdVector, upVector));
        Float3 rightVector = Float3.normalise(Float3.cross(upVector, fwdVector));

        // Depending on key pressed, update camera position
        if (fwd)
            position = Float3.add(position, Float3.mult(fwdVector, moveSpeed));
        if (back)
            position = Float3.sub(position, Float3.mult(fwdVector, moveSpeed));
        if (strafeL)
            position = Float3.add(position, Float3.mult(leftVector, moveSpeed));
        if (strafeR)
            position = Float3.add(position, Float3.mult(rightVector, moveSpeed));
        if (up)
            position = Float3.add(position, Float3.mult(upVector, moveSpeed));
        if (down)
            position = Float3.sub(position, Float3.mult(upVector, moveSpeed));

        // Limit camera to above plane
        if (position.getY() < planeHeight)
            position.setY(planeHeight);
    }

    /**
     * Update camera position on mouse dragged
     */
    public void updatePositionOnMouseDragged(double mousePosX, double mousePosY, double mouseOldX, double mouseOldY) {

        // Add mouse displacement in x direction to camera yaw
        yaw += (mousePosX - mouseOldX) * MOUSE_SENSITIVITY;

        // Add mouse displacement in y direction to camera pitch
        // Limit y direction lookaround to a 180-degree angle
        pitch = (float) min(89.99, max(-89.99, pitch + (mousePosY - mouseOldY) * MOUSE_SENSITIVITY));
    }

    /**
     * Return the current camera field of view
     *
     * @return the camera view of view
     */
    public float getFov() {
        return fov;
    }

    /**
     * Set the camera field of view to a given value
     *
     * @param fov
     *     the desired fov value
     */
    public void setFov(float fov) {
        this.fov = fov;
    }

    /**
     * Set move speed to twice as fast
     */
    public void run() {
        moveSpeed = MOVE_SPEED * 2;
    }

    /**
     * Set move speed to normal
     */
    public void walk() {
        moveSpeed = MOVE_SPEED;
    }
}
