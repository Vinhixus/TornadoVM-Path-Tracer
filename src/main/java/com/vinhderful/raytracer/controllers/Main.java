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
package com.vinhderful.raytracer.controllers;

import com.vinhderful.raytracer.Settings;
import com.vinhderful.raytracer.misc.Camera;
import com.vinhderful.raytracer.misc.World;
import com.vinhderful.raytracer.renderer.Renderer;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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
import uk.ac.manchester.tornado.api.types.vectors.Float4;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the main window, contains driver code for the ray tracer
 */
@SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
public class Main {

    /**
     * OUTPUT BUFFER
     * -------------
     * Pixel buffer containing ARGB values of pixel colors for renderer to write to
     * Size = width * height of canvas resolution
     **/
    private static IntArray OB_pixels;

    /**
     * INPUT BUFFER
     * ------------
     * The buffer containing camera properties for the renderer to read from
     * -----------------
     * camera[0]: x coordinate of position
     * camera[1]: y coordinate of position
     * camera[2]: z coordinate of position
     * camera[3]: yaw of rotation
     * camera[4]: pitch of rotation
     * camera[5]: field of view (FOV)
     **/
    private static FloatArray IB_camera;

    /**
     * INPUT BUFFER
     * ------------
     * Resolution of the canvas
     * ------------------------
     * dimensions[0]: width
     * dimensions[1]: height
     **/
    private static IntArray IB_dimensions;

    /**
     * INPUT BUFFER
     * ------------
     * The following 4 vectors define the objects in the scene.
     * Example:
     * bodyPositions.get(2), bodySizes.get(2), bodyColors.get(2), bodyReflectivities.get(2)
     * define the properties of the body at index 2
     * -----------------------------------------
     * bodyPosition: position of the body in 3D space
     * bodySize: size of the body (e.g. radius of sphere)
     * bodyColor: color of the sphere defined by R, G, B float values in the range of [0, 1]
     * bodyReflectivity: reflectivity of the object, higher is more reflective
     */
    private static VectorFloat4 IB_bodyPositions;
    private static VectorFloat IB_bodySizes;
    private static VectorFloat4 IB_bodyColors;
    private static VectorFloat IB_bodyReflectivities;


    /**
     * INPUT BUFFER
     * ------------
     * Skybox represented by a vector of Float4 values containing R, G, B values as floats in the range of [0, 1]
     **/
    private static VectorFloat4 IB_skybox;
    private static IntArray IB_skyboxDimensions;

    /**
     * INPUT BUFFER
     * ------------
     * Ray tracing properties buffer for the renderer to read from
     * -----------------------
     * rayTracingProperties[0]: Sample size of soft shadows
     * rayTracingProperties[1]: Bounce limit for reflection rays
     **/
    private static IntArray IB_rayTracingProperties;
    /**
     * JavaFX GUI elements
     */
    @FXML

    // Main pane and canvas encapsulating the viewport
    public Pane pane;
    public Canvas canvas;
    public VBox settingsPanel;
    public MenuItem settingsPanelToggle;

    // Adjustable light position
    public Slider lightXSlider;
    public Slider lightYSlider;
    public Slider lightZSlider;
    public Text lightXText;
    public Text lightYText;
    public Text lightZText;

    // Adjustable camera field of view
    public Slider cameraFOVSlider;
    public Text cameraFOVText;

    // Adjustable soft shadow sample size and reflection bounce limit
    public Slider shadowSampleSizeSlider;
    public Text shadowSampleSizeText;
    public Slider reflectionBouncesSlider;
    public Text reflectionBouncesText;

    // Frames per second text output and helper variables
    public Text fpsText;

    // Device selection dropdown
    public ComboBox<String> deviceDropdown;

    // Buttons to enable/disable physics and to randomise the positions of the spheres
    public Button physicsButton;
    public Button randomizePositionsButton;

    /**
     * Resolution of the canvas
     */
    private int width;
    private int height;

    /**
     * Camera
     */
    private Camera camera;

    /**
     * Ray tracing properties
     */
    private int shadowSampleSize;
    private int reflectionBounces;

    /**
     * The world containing the objects in the scene, the skybox and the defined animation
     */
    private World world;

    private volatile long fpsLastUpdate;
    private volatile double fps;
    private int selectedDeviceIndex;

    // Camera control
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;

    // Movement
    private boolean fwd, strafeL, strafeR, back, up, down;

    // Tornado elements
    private ArrayList<TornadoDevice> devices;
    private TornadoExecutionPlan executionPlan;
    private GridScheduler grid;

    private volatile boolean renderWithTornado;
    private volatile boolean renderWithJavaStreams;

    // PixelWriter and it's format
    private volatile PixelWriter pixelWriter;
    private volatile PixelFormat<IntBuffer> format;

    // Control and About windows
    private Window controls;
    private Window about;


    /**
     * Initialise renderer, world, camera and populate with objects
     */
    @FXML
    public void initialize() throws Exception {

        // Build world
        System.out.println("------------------------------------");
        System.out.println("Building World:");
        world = new World();
        System.out.println("------------------------------------");

        // Initialise settings
        System.out.println("Setting up rendering environment...");
        setupRenderingEnvironment();
        System.out.println("Allocating buffers...");
        allocateBuffers();
        System.out.println("Setting up Tornado Task Graph...");
        setupTornadoTaskGraph();
        System.out.println("Getting Available Tornado Devices...");
        setAvailableDevices();
        System.out.println("Setting up operating loops...");
        setupOperatingLoops();
        System.out.println("Setting up GUI elements...");
        setupGUIElements();

        // Finish initialization
        System.out.println("------------------------------------");
        System.out.println("Opening Application...");
    }


    /**
     * IInitialise canvas, dimensions, pixel writer, format, camera and ray tracing properties
     */
    private void setupRenderingEnvironment() {

        // Set viewport (canvas) dimensions
        width = Settings.WIDTH;
        height = Settings.HEIGHT;
        canvas.setWidth(width);
        canvas.setHeight(height);

        // Setup canvas and graphics context
        GraphicsContext g = canvas.getGraphicsContext2D();
        pixelWriter = g.getPixelWriter();
        format = PixelFormat.getIntArgbPreInstance();

        // Camera
        camera = new Camera(world);

        // Ray tracing properties
        shadowSampleSize = Settings.INITIAL_SHADOW_SAMPLE_SIZE;
        reflectionBounces = Settings.INITIAL_REFLECTION_BOUNCES;
    }

    /**
     * Allocate input and output buffers for the renderer
     */
    private void allocateBuffers() {

        // Output buffer
        OB_pixels = new IntArray(width * height);

        // Input buffers
        IB_dimensions = IntArray.fromElements(width, height);
        IB_camera = camera.getBuffer();

        IB_rayTracingProperties = IntArray.fromElements(shadowSampleSize, reflectionBounces);

        IB_bodyPositions = world.getBodyPositionsBuffer();
        IB_bodySizes = world.getBodySizesBuffer();
        IB_bodyColors = world.getBodyColorsBuffer();
        IB_bodyReflectivities = world.getBodyReflectivitiesBuffer();

        IB_skybox = world.getSkyboxBuffer();


        IB_skyboxDimensions = IntArray.fromElements( world.getSkyboxDimensionsBuffer()[0],  world.getSkyboxDimensionsBuffer()[1]);


    }


    /**
     * Define Tornado task graph for Parallel Renderer.render method
     */
    private void setupTornadoTaskGraph() {

        // Define task graph
        TaskGraph ts = new TaskGraph("s0");
        ts.transferToDevice(DataTransferMode.EVERY_EXECUTION, IB_camera, IB_rayTracingProperties, IB_bodyPositions);
        ts.transferToDevice(DataTransferMode.FIRST_EXECUTION, IB_dimensions, IB_bodySizes, IB_bodyColors, IB_bodyReflectivities, IB_skybox, IB_skyboxDimensions);
        ts.task("t0", Renderer::render, OB_pixels,
                IB_dimensions, IB_camera, IB_rayTracingProperties,
                IB_bodyPositions, IB_bodySizes, IB_bodyColors, IB_bodyReflectivities,
                IB_skybox, IB_skyboxDimensions);
        ts.transferToHost(DataTransferMode.EVERY_EXECUTION, OB_pixels);

        // Define worker grid
        WorkerGrid worker = new WorkerGrid2D(IB_dimensions.get(0), IB_dimensions.get(1));
        worker.setLocalWork(16, 16, 1);
        grid = new GridScheduler();
        grid.setWorkerGrid("s0.t0", worker);
        executionPlan = new TornadoExecutionPlan(ts.snapshot());
    }


    /**
     * Populate device dropdown list, perform initial mapping to avoid runtime lag
     */
    private void setAvailableDevices() {

        // Initialise list of devices
        devices = new ArrayList<>();

        // Add sequential execution to devices list
        devices.add(null);
        deviceDropdown.getItems().add("(Pure Java) - CPU");
        deviceDropdown.getItems().add("(Java Parallel Streams) - CPU");

        // Get Tornado drivers
        TornadoRuntime runtimeCI = TornadoRuntimeProvider.getTornadoRuntime();
        int numBackends = runtimeCI.getNumBackends();

        for (int i = 0; i < numBackends; i++) {

            TornadoBackend driver = runtimeCI.getBackend(i);
            int numDevices = driver.getNumDevices();

            // Add Tornado devices, perform initial mapping
            for (int j = 0; j < numDevices; j++) {
                TornadoDevice device = driver.getDevice(j);
                devices.add(device);

                String listingName = "(" + driver.getName() + ") " + device.getPhysicalDevice().getDeviceName();
                System.out.println("-> Found: " + listingName);
                deviceDropdown.getItems().add(listingName);

                // Perform an initial mapping to avoid runtime lag
                executionPlan.withDevice(device) //
                        .withGridScheduler(grid) //
                        .execute();
            }
        }

        // Select first device (Pure Java sequential)
        selectedDeviceIndex = 0;
        deviceDropdown.getSelectionModel().selectFirst();
        renderWithTornado = false;
    }

    /**
     * Set up the logic and rendering loops
     */
    private void setupOperatingLoops() {
        // Define main animation loop - gets called every frame
        new AnimationTimer() {

            @Override
            public void handle(long now) {

                // Update camera Position
                camera.updatePositionOnMovement(fwd, back, strafeL, strafeR, up, down);

                // Set the pixels on the canvas when render is ready

                int[] temp =  new int[OB_pixels.getSize()];

                for (int i = 0; i < OB_pixels.getSize(); i++) {
                    temp[i] = OB_pixels.get(i);
                }


                pixelWriter.setPixels(0, 0, width, height, format, temp, 0, width);

                // Render
                render();

                // Record fps
                fps = 1_000_000_000.0 / (System.nanoTime() - fpsLastUpdate);
                fpsLastUpdate = System.nanoTime();
            }
        }.start();

        // Output fps every half seconds
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        Runnable fpsTextSetter = () -> Platform.runLater(() -> fpsText.setText(String.format("%.2f", fps)));
        ses.scheduleAtFixedRate(fpsTextSetter, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Set up slider listeners and define actions on change
     */
    private void setupGUIElements() {

        // Adjustable light position
        Float4 lightPosition = world.getLight().getPosition();

        lightXSlider.setValue(lightPosition.getX());
        lightXSlider.valueProperty().addListener((observable, oldValue, newValue) -> lightPosition.setX(newValue.floatValue()));
        lightXText.textProperty().bind(lightXSlider.valueProperty().asString("%.2f"));

        lightYSlider.setValue(lightPosition.getY());
        lightYSlider.valueProperty().addListener((observable, oldValue, newValue) -> lightPosition.setY(newValue.floatValue()));
        lightYText.textProperty().bind(lightYSlider.valueProperty().asString("%.2f"));

        lightZSlider.setValue(lightPosition.getZ());
        lightZSlider.valueProperty().addListener((observable, oldValue, newValue) -> lightPosition.setZ(newValue.floatValue()));
        lightZText.textProperty().bind(lightZSlider.valueProperty().asString("%.2f"));

        // Adjustable camera field of view
        cameraFOVSlider.setValue(camera.getFov());
        cameraFOVSlider.valueProperty().addListener((observable, oldValue, newValue) -> camera.setFov(newValue.floatValue()));
        cameraFOVText.textProperty().bind(cameraFOVSlider.valueProperty().asString("%.2f"));

        // Adjustable ray tracing properties
        shadowSampleSizeSlider.setValue(shadowSampleSize);
        shadowSampleSizeSlider.setMax(10);
        shadowSampleSizeSlider.setMajorTickUnit(1);
        shadowSampleSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> shadowSampleSize = newValue.intValue());
        shadowSampleSizeText.textProperty().bind(shadowSampleSizeSlider.valueProperty().asString("%.0f"));

        reflectionBouncesSlider.setValue(reflectionBounces);
        reflectionBouncesSlider.setMax(Settings.MAX_REFLECTION_BOUNCES);
        reflectionBouncesSlider.valueProperty().addListener((observable, oldValue, newValue) -> reflectionBounces = newValue.intValue());
        reflectionBouncesText.textProperty().bind(reflectionBouncesSlider.valueProperty().asString("%.0f"));

        // Setup Controls and About windows
        controls = new Window("Controls", "Controls.fxml", "icon.png");
        about = new Window("About", "About.fxml", "icon.png");
    }


    /**
     * Render one frame: Populate pixels array and set pixels on the canvas
     */
    private void render() {

        // Copy data to input buffers
        camera.updateBuffer();
        world.updateBodyPositionBuffer();

        IB_rayTracingProperties.set(0, shadowSampleSize);
        IB_rayTracingProperties.set(1, reflectionBounces);

        // Render to output buffer
        if (renderWithTornado) {
            executionPlan.execute();
        } else if (renderWithJavaStreams) {
            Renderer.renderWithParallelStreams(OB_pixels, IB_dimensions, IB_camera, IB_rayTracingProperties,
                        IB_bodyPositions, IB_bodySizes, IB_bodyColors, IB_bodyReflectivities,
                        IB_skybox, IB_skyboxDimensions);
        } else {
            Renderer.render(OB_pixels, IB_dimensions, IB_camera, IB_rayTracingProperties,
                    IB_bodyPositions, IB_bodySizes, IB_bodyColors, IB_bodyReflectivities,
                    IB_skybox, IB_skyboxDimensions);
        }
    }

    /**
     * Define action on mouse drag
     *
     * @param mouseEvent mouse event
     */
    public void mouseDragged(MouseEvent mouseEvent) {

        // Record mouse position
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = mouseEvent.getX();
        mousePosY = mouseEvent.getY();

        camera.updatePositionOnMouseDragged(mousePosX, mousePosY, mouseOldX, mouseOldY);
    }

    /**
     * Define action of mouse press
     *
     * @param mouseEvent mouse event
     */
    public void mousePressed(MouseEvent mouseEvent) {

        // Record mouse position
        mousePosX = mouseEvent.getX();
        mousePosY = mouseEvent.getY();
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;

        // Hide mouse cursor while controlling camera
        canvas.setCursor(Cursor.NONE);
    }

    /**
     * Define action on mouse release
     */
    public void mouseReleased() {
        canvas.setCursor(Cursor.DEFAULT);
    }


    /**
     * Define action on key press
     *
     * @param keyEvent key event
     */
    public void keyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case Q:
                up = true;
                break;
            case E:
                down = true;
                break;
            case W:
                fwd = true;
                break;
            case S:
                back = true;
                break;
            case A:
                strafeL = true;
                break;
            case D:
                strafeR = true;
                break;
            case SHIFT:
                camera.run();
                break;
        }
    }

    /**
     * Define action on key release
     *
     * @param keyEvent key event
     */
    public void keyReleased(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case Q:
                up = false;
                break;
            case E:
                down = false;
                break;
            case W:
                fwd = false;
                break;
            case S:
                back = false;
                break;
            case A:
                strafeL = false;
                break;
            case D:
                strafeR = false;
                break;
            case SHIFT:
                camera.walk();
                break;
        }
    }

    /**
     * Define action on device dropdown selection
     */
    public void selectDevice() {
        // Get selection from dropdown box
        selectedDeviceIndex = deviceDropdown.getSelectionModel().getSelectedIndex();

        if (selectedDeviceIndex == 0) {
            // Limit shadow sample size when running Java multi-thread
            shadowSampleSizeSlider.setMax(10);
            shadowSampleSizeSlider.setMajorTickUnit(1);
            shadowSampleSizeSlider.setMinorTickCount(0);
            renderWithTornado = false;
            renderWithJavaStreams = false;
        } else if (selectedDeviceIndex == 1) {
            // Limit shadow sample size when rendering sequentially
            shadowSampleSizeSlider.setMax(10);
            shadowSampleSizeSlider.setMajorTickUnit(1);
            shadowSampleSizeSlider.setMinorTickCount(0);
            renderWithTornado = false;
            renderWithJavaStreams = true;
        } else if (selectedDeviceIndex > 1) {
            // Map task graph to selected device if selected device is tornado device
            TornadoDevice device = devices.get(selectedDeviceIndex - 1);
            shadowSampleSizeSlider.setMax(Settings.MAX_SHADOW_SAMPLE_SIZE);
            shadowSampleSizeSlider.setMajorTickUnit(50);
            shadowSampleSizeSlider.setMinorTickCount(50);
            renderWithTornado = true;
            renderWithJavaStreams = false;
            executionPlan.withDevice(device);
        }
    }

    /**
     * Define action enabling/disabling physics
     */
    public void togglePhysics() {
        world.togglePhysics();
        physicsButton.setText(world.isPhysicsEnabled() ? "Disable" : "Enable");
        randomizePositionsButton.setDisable(!world.isPhysicsEnabled());
    }

    /**
     * Randomise the positions of the spheres in the scene
     */
    public void randomizePositions() {
        world.randomizePositions();
    }

    /**
     * Toggle right-side setting panel
     */
    public void toggleSettingsPanel() {
        boolean isVisible = settingsPanel.isVisible();
        settingsPanel.setVisible(!isVisible);
        settingsPanel.setManaged(!isVisible);
        settingsPanelToggle.setText(isVisible ? "Show Settings Panel" : "Hide Settings Panel");
        settingsPanel.getParent().getScene().getWindow().sizeToScene();
    }

    /**
     * Show Controls window
     */
    public void showControlsWindow() {
        controls.show();
    }

    /**
     * Show About window
     */
    public void showAboutWindow() {
        about.show();
    }

    /**
     * Exit handler
     */
    public void exit() {
        System.exit(0);
    }
}
