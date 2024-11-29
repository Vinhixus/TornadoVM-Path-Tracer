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

import java.util.ArrayList;

import com.vinhderful.raytracer.misc.World;
import com.vinhderful.raytracer.renderer.Renderer;

import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoBackend;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.TornadoRuntime;
import uk.ac.manchester.tornado.api.WorkerGrid;
import uk.ac.manchester.tornado.api.WorkerGrid2D;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntimeProvider;
import uk.ac.manchester.tornado.api.types.arrays.FloatArray;
import uk.ac.manchester.tornado.api.types.arrays.IntArray;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat4;

/**
 * Benchmark class provides a no-GUI application to test the performance of the ray tracer when using hardware
 * acceleration with TornadoVM vs running sequentially.
 * The benchmark simply generates a set number of frames and records the time it took.
 */
@SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
public class Benchmark {

    private static final boolean SKIP_SEQUENTIAL = Boolean.parseBoolean(System.getProperty("skip.sequential", "False"));

    // The number of frames to generate
    private static final int FRAMES_TO_GENERATE = 100;

    // Dimensions of the viewport
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    // Ray tracing properties
    private static final int SHADOW_SAMPLE_SIZE = 200;
    private static final int REFLECTION_BOUNCES = 4;

    // Output and input buffers
    private static IntArray pixels;
    private static FloatArray camera;
    private static IntArray dimensions;
    private static IntArray rayTracingProperties;

    /**
     * Initialise rendering environment
     */
    private static void setRenderingProperties() {
        dimensions = IntArray.fromElements(WIDTH, HEIGHT);
        pixels = new IntArray(WIDTH * HEIGHT);
        rayTracingProperties = IntArray.fromElements(SHADOW_SAMPLE_SIZE, REFLECTION_BOUNCES);
        camera = FloatArray.fromElements(0, 0, -4F, 0, 0, 60);
    }

    /**
     * Main program
     *
     * @param args
     *     program arguments
     */
    public static void main(String[] args) throws Exception {

        setRenderingProperties();

        System.out.println("-----------------------------------------");
        System.out.println("Building world...");
        World world = new World();
        VectorFloat4 skybox = world.getSkyboxBuffer();
        IntArray skyboxDimensions = IntArray.fromElements(world.getSkyboxDimensionsBuffer()[0], world.getSkyboxDimensionsBuffer()[1]);
        VectorFloat4 bodyPositions = world.getBodyPositionsBuffer();
        VectorFloat bodySizes = world.getBodySizesBuffer();
        VectorFloat4 bodyColors = world.getBodyColorsBuffer();
        VectorFloat bodyReflectivities = world.getBodyReflectivitiesBuffer();

        // Set up Tornado Task Schedule
        TaskGraph ts = new TaskGraph("s0");
        ts.transferToDevice(DataTransferMode.EVERY_EXECUTION, camera, rayTracingProperties, bodyPositions);
        ts.transferToDevice(DataTransferMode.FIRST_EXECUTION, dimensions, bodySizes, bodyColors, bodyReflectivities, skybox, skyboxDimensions);
        ts.task("t0", Renderer::render, pixels, dimensions, camera, rayTracingProperties, bodyPositions, bodySizes, bodyColors, bodyReflectivities, skybox, skyboxDimensions);
        ts.transferToHost(DataTransferMode.EVERY_EXECUTION, pixels);

        // Set up worker grid
        WorkerGrid worker = new WorkerGrid2D(WIDTH, HEIGHT);
        worker.setLocalWork(16, 16, 1);
        GridScheduler grid = new GridScheduler();
        grid.setWorkerGrid("s0.t0", worker);

        TornadoExecutionPlan rayTracingPlan = new TornadoExecutionPlan(ts.snapshot());
        rayTracingPlan.withGridScheduler(grid).execute();

        // Get Tornado devices
        System.out.println("-----------------------------------------");
        System.out.println("Getting Tornado Devices...");
        ArrayList<TornadoDevice> devices = new ArrayList<>();
        TornadoRuntime tornadoRuntime = TornadoRuntimeProvider.getTornadoRuntime();
        int numBackends = tornadoRuntime.getNumBackends();
        int deviceCount = 0;

        for (int i = 0; i < numBackends; i++) {

            TornadoBackend driver = tornadoRuntime.getBackend(i);
            int numDevices = driver.getNumDevices();

            // Exclude PTX due to unsupported intrinsic (atan2)
            if (driver.getName().toLowerCase().contains("ptx")) {
                continue;
            }

            for (int j = 0; j < numDevices; j++) {
                TornadoDevice device = driver.getDevice(j);
                devices.add(device);

                String listingName = "(" + driver.getName() + ") " + device.getPhysicalDevice().getDeviceName();
                System.out.println(deviceCount + ": " + listingName);

                deviceCount++;
            }
        }

        double sequentialTime = 0.0;
        if (!SKIP_SEQUENTIAL) {
            // ==============================================================
            // Run computation sequentially
            // ==============================================================
            System.out.println("-----------------------------------------");
            System.out.println("Running [JAVA SEQUENTIAL]");
            for (int i = 0; i < FRAMES_TO_GENERATE; i++) {
                Renderer.render(pixels, dimensions, camera, rayTracingProperties, bodyPositions, bodySizes, bodyColors, bodyReflectivities, skybox, skyboxDimensions);
            }
            long startTime = System.nanoTime();
            Renderer.render(pixels, dimensions, camera, rayTracingProperties, bodyPositions, bodySizes, bodyColors, bodyReflectivities, skybox, skyboxDimensions);
            long endTime = System.nanoTime();
            sequentialTime = (endTime - startTime) / 1000000.0;
            System.out.println("Duration: " + sequentialTime + " ms");
        }

        // ==============================================================
        // Run with Java Parallel Streams
        // ==============================================================
        System.out.println("-----------------------------------------");
        System.out.println("Running [JAVA PARALLEL STREAMS]");

        for (int i = 0; i < FRAMES_TO_GENERATE; i++) {
            Renderer.renderWithParallelStreams(pixels, dimensions, camera, rayTracingProperties, bodyPositions, bodySizes, bodyColors, bodyReflectivities, skybox, skyboxDimensions);
        }

        long startTime = System.nanoTime();
        Renderer.renderWithParallelStreams(pixels, dimensions, camera, rayTracingProperties, bodyPositions, bodySizes, bodyColors, bodyReflectivities, skybox, skyboxDimensions);
        long endTime = System.nanoTime();
        double javaStreamsTime = (endTime - startTime) / 1000000.0;
        System.out.println("Duration: " + javaStreamsTime + " ms");

        // Running Accelerated version per device
        System.out.println("-----------------------------------------");
        for (TornadoDevice device : devices) {
            rayTracingPlan.withDevice(device);
            rayTracingPlan.execute();

            // ==============================================================
            // Run computation in parallel
            // ==============================================================
            System.out.println("-----------------------------------------");
            System.out.println("Running with TornadoVM for device: " + device);

            for (int i = 0; i < FRAMES_TO_GENERATE; i++) {
                rayTracingPlan.execute();
            }
            startTime = System.nanoTime();
            rayTracingPlan.execute();
            endTime = System.nanoTime();

            double tornadoTime = (endTime - startTime) / 1000000.0;
            System.out.println("Duration: " + tornadoTime + " ms");

            // ==============================================================
            // Calculate performance increase
            // ==============================================================
            System.out.println("-----------------------------------------");
            if (!SKIP_SEQUENTIAL) {
                System.out.println("Performance increase vs sequential: " + sequentialTime / tornadoTime + "x");
            }
            System.out.println("Performance increase vs Java Streams: " + javaStreamsTime / tornadoTime + "x");
            System.out.println("-----------------------------------------");
        }
    }
}