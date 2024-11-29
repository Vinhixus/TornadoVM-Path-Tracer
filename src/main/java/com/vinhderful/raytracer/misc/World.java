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

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vinhderful.raytracer.misc.bodies.Body;
import com.vinhderful.raytracer.misc.bodies.Light;
import com.vinhderful.raytracer.misc.bodies.Plane;
import com.vinhderful.raytracer.misc.bodies.Sphere;
import com.vinhderful.raytracer.utils.Color;
import com.vinhderful.raytracer.utils.Float4Ext;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat4;
import uk.ac.manchester.tornado.api.types.vectors.Float4;

/**
 * A world representing objects in the scene
 */
public class World {

    /**
     * A light is required to be at index 0
     */
    public static final int LIGHT_INDEX = 0;

    /**
     * A plane is required to be at index 1
     */
    public static final int PLANE_INDEX = 1;

    /**
     * The spheres in the scene start at index 2
     */
    public static final int SPHERES_START_INDEX = 2;

    /**
     * Objects in the scene
     */
    private final ArrayList<Body> bodies;

    /**
     * Variables encapsulating the physics
     */
    private final ScheduledExecutorService physicsService;
    private final Runnable physicsUpdate;
    private Future<?> physicsLoop;
    private boolean physicsEnabled;

    /**
     * Random generator
     */
    private final Random r;

    /**
     * Skybox, light and plane
     */
    private Skybox skybox;
    private Light light;
    private Plane plane;

    /**
     * Input buffers
     */
    private VectorFloat4 bodyPositions;
    private VectorFloat bodySizes;
    private VectorFloat4 bodyColors;
    private VectorFloat bodyReflectivities;

    /**
     * Instantiate a default world
     *
     * @throws Exception
     *     is thrown if light is not found at index 0 or plane is not found at index 1
     */
    public World() throws Exception {

        r = new Random();

        // Populate world
        bodies = new ArrayList<>();
        generateDefaultWorld();

        // Setup default physics service
        Physics physics = new Physics(this);
        physicsService = Executors.newScheduledThreadPool(1);
        physicsEnabled = false;
        physicsUpdate = physics::update;

        // Make sure we have 1 light and 1 plane at the first two indexes
        if (bodies.size() < 2)
            throw new Exception("A light source and a plane are mandatory!");
        if (!(bodies.get(LIGHT_INDEX) instanceof Light))
            throw new Exception("A light source needs to be at index " + LIGHT_INDEX + "!");
        if (!(bodies.get(PLANE_INDEX) instanceof Plane))
            throw new Exception("A plane needs to be at index " + PLANE_INDEX + "!");

        // Generate the tornado compatible vector representations of the bodies
        allocateAndInitializeBuffers();
    }

    /**
     * Helper function to generate a random float value between min and max
     *
     * @param min
     *     the minimum boundary of the random float
     * @param max
     *     the maximum boundary of the random float
     * @return a random float between min and max
     */
    private float randFloat(float min, float max) {
        return min + r.nextFloat() * (max - min);
    }

    /**
     * Helper function to generate a random position given a minimum and maximum coordinate value
     *
     * @param min
     *     the minimum coordinate value
     * @param max
     *     the maximum coordinate value
     * @return a random position bounded by min and max
     */
    private Float4 getRandomPosition(float min, float max) {
        return new Float4(randFloat(min, max), randFloat(min, max), randFloat(min, max), 0);
    }

    /**
     * Generate a pre-defined default world
     */
    private void generateDefaultWorld() {

        // Skybox
        String skyboxFileName = "Sky.jpg";
        System.out.println("-> Loading Skybox Image '" + skyboxFileName + "'...");
        skybox = new Skybox(skyboxFileName);

        System.out.println("-> Adding object to the scene...");

        // Light
        light = new Light(new Float4(0, 0, 0, 0), 1.2F, Color.WHITE);
        addBody(light);

        // Plane
        plane = new Plane(24F, 36F);
        addBody(plane);

        // Spheres
        addBody(new Sphere(new Float4(-8, -8, 0, 0), 1.75F, Color.WHITE, 16));
        addBody(new Sphere(new Float4(-4, -8, 0, 0), 1.75F, Color.RED, 24));
        addBody(new Sphere(new Float4(0, -8, 0, 0), 1.75F, Color.GREEN, 36));
        addBody(new Sphere(new Float4(4, -8, 0, 0), 1.75F, Color.BLUE, 48));
        addBody(new Sphere(new Float4(8, -8, 0, 0), 1.75F, Color.BLACK, 64));
    }

    /**
     * Randomize the positions of the spheres in the scene
     */
    public void randomizePositions() {

        for (int i = SPHERES_START_INDEX; i < bodies.size(); i++) {
            float radius = bodies.get(i).getSize();
            float boundary = plane.getSize() * 0.5F - radius;

            Float4 position;

            if (i == SPHERES_START_INDEX) {
                position = getRandomPosition(-boundary, boundary);
            } else {

                // Make sure spheres don't overlap
                while (true) {
                    position = getRandomPosition(-boundary, boundary);

                    boolean overlaps = false;
                    for (int j = SPHERES_START_INDEX; j < i; j++)
                        if (Float4Ext.distance(position, bodies.get(j).getPosition()) < radius + bodies.get(j).getSize()) {
                            overlaps = true;
                            break;
                        }
                    if (!overlaps)
                        break;
                }
            }

            bodies.get(i).setPosition(position);
            bodies.get(i).setPreviousPosition(position.duplicate());
        }
    }

    /**
     * Return if the physics is currently enabled
     *
     * @return if the physics is enabled
     */
    public boolean isPhysicsEnabled() {
        return physicsEnabled;
    }

    /**
     * Enable physics
     */
    public void enablePhysics() {
        physicsLoop = physicsService.scheduleAtFixedRate(physicsUpdate, 0, 16_666_666, TimeUnit.NANOSECONDS);
        physicsEnabled = true;
    }

    /**
     * Disable physics
     */
    public void disablePhysics() {
        physicsLoop.cancel(true);
        physicsEnabled = false;
    }

    /**
     * Toggle between enabled/disabled physics
     */
    public void togglePhysics() {
        if (physicsEnabled)
            disablePhysics();
        else
            enablePhysics();
    }

    /**
     * Allocate and initialise the input buffers for rendering
     */
    private void allocateAndInitializeBuffers() {
        System.out.println("-> Allocating object representation buffers...");

        int numBodies = bodies.size();

        bodyPositions = new VectorFloat4(numBodies);
        bodySizes = new VectorFloat(numBodies);
        bodyColors = new VectorFloat4(numBodies);
        bodyReflectivities = new VectorFloat(numBodies);

        for (int i = 0; i < numBodies; i++) {
            Body body = bodies.get(i);

            bodyPositions.set(i, body.getPosition());
            bodySizes.set(i, body.getSize());
            bodyColors.set(i, body.getColor());
            bodyReflectivities.set(i, body.getReflectivity());
        }
    }

    /**
     * Copy the data to input buffers
     */
    public void updateBodyPositionBuffer() {
        if (physicsEnabled)
            for (int i = 0; i < bodies.size(); i++)
                bodyPositions.set(i, bodies.get(i).getPosition().duplicate());
        else
            bodyPositions.set(LIGHT_INDEX, light.getPosition().duplicate());
    }

    public ArrayList<Body> getBodies() {
        return bodies;
    }

    /**
     * Return the memory address to the input buffer representing body positions
     *
     * @return the VectorFloat4 representing body positions
     */
    public VectorFloat4 getBodyPositionsBuffer() {
        return bodyPositions;
    }

    /**
     * Return the memory address to the input buffer representing body sizes
     *
     * @return the VectorFloat representing body sizes
     */
    public VectorFloat getBodySizesBuffer() {
        return bodySizes;
    }

    /**
     * Return the memory address to the input buffer representing body colors
     *
     * @return the VectorFloat representing body colors
     */
    public VectorFloat4 getBodyColorsBuffer() {
        return bodyColors;
    }

    /**
     * Return the memory address to the input buffer representing body reflectivities
     *
     * @return the VectorFloat representing body reflectivities
     */
    public VectorFloat getBodyReflectivitiesBuffer() {
        return bodyReflectivities;
    }

    /**
     * Return the memory address to the input buffer representing the skybox
     *
     * @return the VectorFloat4 representing skybox
     */
    public VectorFloat4 getSkyboxBuffer() {
        return skybox.getBuffer();
    }

    /**
     * Return the memory address to the input buffer representing skybox dimensions
     *
     * @return the int array containing [0] = width of skybox image, [1] = height of skybox image
     */
    public int[] getSkyboxDimensionsBuffer() {
        return skybox.getDimensionsBuffer();
    }

    /**
     * Return the light source in the world
     *
     * @return the light source
     */
    public Light getLight() {
        return light;
    }

    /**
     * Return the plane
     *
     * @return the plane
     */
    public Plane getPlane() {
        return plane;
    }

    /**
     * Add a body to the world
     *
     * @param body
     *     the body to add
     */
    private void addBody(Body body) {
        bodies.add(body);
    }
}
