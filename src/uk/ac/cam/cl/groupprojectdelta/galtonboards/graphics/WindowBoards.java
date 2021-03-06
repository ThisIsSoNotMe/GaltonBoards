package uk.ac.cam.cl.groupprojectdelta.galtonboards.graphics;

import java.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.liquidengine.legui.DefaultInitializer;
import org.liquidengine.legui.animation.AnimatorProvider;
import org.liquidengine.legui.component.Component;
import org.liquidengine.legui.component.Frame;
import org.liquidengine.legui.input.Mouse;
import org.liquidengine.legui.listener.processor.EventProcessorProvider;
import org.liquidengine.legui.style.color.ColorConstants;
import org.liquidengine.legui.system.layout.LayoutManager;
import org.lwjgl.BufferUtils;
import org.joml.Vector2f;
import org.liquidengine.legui.DefaultInitializer;
import org.liquidengine.legui.animation.AnimatorProvider;
import org.liquidengine.legui.component.Component;
import org.liquidengine.legui.component.Frame;
import org.liquidengine.legui.event.MouseClickEvent;
import org.liquidengine.legui.event.MouseClickEvent.MouseClickAction;
import org.liquidengine.legui.input.Mouse;
import org.liquidengine.legui.listener.processor.EventProcessorProvider;
import org.liquidengine.legui.style.color.ColorConstants;
import org.liquidengine.legui.system.layout.LayoutManager;
import org.lwjgl.BufferUtils;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.UserInput;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Configuration;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Simulation;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Workspace;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.mouse.WorkspaceSelectionHandler;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class WindowBoards extends Window {

  private UserInput userInput;
  private final Workspace workspace = Workspace.workspace;
  private float currentTime;
  private int mvpShaderLocation;

  private int programID;
  private int vertexShaderID;
  private int fragmentShaderID;
  private int vertexBuffer, uvBuffer, colourTemplateBuffer;
  private int textureID;

  private final String textureFilePath;
  private final String vertexShaderPath;
  private final String fragmentShaderPath;

  // UI
  private Frame frame;
  private DefaultInitializer initializer;
  private final Vector<Component> components = new Vector<>();
  private boolean initialized = false;

  static final float[] CLEAR_COLOUR = {0.5f, 0.5f, 0.5f, 1};

  // mouse things
  final DoubleBuffer mouseX = BufferUtils.createDoubleBuffer(1);
  final DoubleBuffer mouseY = BufferUtils.createDoubleBuffer(1);
  Vector2f mousePos = new Vector2f();

  public WindowBoards(int width, int height, String vertexShaderPath, String fragmentShaderPath, String textureFilePath) {
    super(width, height);
    this.vertexShaderPath = vertexShaderPath;
    this.fragmentShaderPath = fragmentShaderPath;
    this.textureFilePath = textureFilePath;
  }

  /**
   * Add legui component to the window
   */
  void addComponent(Component component) {
    components.add(component);
    if (initialized) {
      frame.getContainer().add(component);
    }
  }

  void removeComponents() {
    if (initialized) {
      frame.getContainer().removeAll(components);
    }
  }

  public Configuration getConfiguration() {
    return workspace.getConfiguration();
  }

  public Simulation getSimulation() {
    return workspace.getSimulation();
  }

  WorkspaceSelectionHandler getSelectionHandler() {return workspace.mouseHandler.getSelectionHandler();}

  public Camera getCamera() {
    return Camera.camera;
  }

  @Override
  void initialize(long window) {
    userInput = new UserInput(window, Camera.camera);
    programID = glCreateProgram();

    // Load, compile and attach shaders
    vertexShaderID = loadShader(vertexShaderPath, GL_VERTEX_SHADER);
    fragmentShaderID = loadShader(fragmentShaderPath, GL_FRAGMENT_SHADER);

    // Link the program
    glLinkProgram(programID);
    if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE)
      throw new RuntimeException("Unable to link shader program:");
    glUseProgram(programID);

    mvpShaderLocation = glGetUniformLocation(programID, "MVP");

    // Generate buffers
    vertexBuffer = glGenBuffers();
    uvBuffer = glGenBuffers();
    colourTemplateBuffer = glGenBuffers();

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glDisable(GL_CULL_FACE);
    glDisable(GL_DEPTH_TEST);

    // enable V-sync
    glfwSwapInterval(1);

    // Load texture
    textureID = loadTexture(textureFilePath);

    // Create the UI Frame
    frame = new Frame(getWidth(), getHeight());
    frame.getContainer().getStyle().getBackground().setColor(ColorConstants.transparent());
    frame.getContainer().setFocusable(false);
    for (Component component : components) {
      frame.getContainer().add(component);
    }

    // Create GUI initializer
    initializer = new DefaultInitializer(window, frame);

    // Initialize renderer
    initializer.getRenderer().initialize();

    initialized = true;

    glClearColor(CLEAR_COLOUR[0], CLEAR_COLOUR[1], CLEAR_COLOUR[2], CLEAR_COLOUR[3]);
  }

  @Override
  void loop(long window) {
    glfwSwapInterval(1);

    float lastTime, deltaTime;
    Matrix4f MVP = new Matrix4f();
    List<Float> auxList;
    float[] mesh, UVs, colourTemplates;

    // Clear window and setup OpenGL
    glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


    lastTime = currentTime;
    currentTime = (float) glfwGetTime();
    deltaTime = (currentTime - lastTime);
    userInput.update(deltaTime);
    workspace.update(deltaTime);

    // enable transparency
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    // use GL shader
    glUseProgram(programID);

    int[] windowWidth = new int[1];
    int[] windowHeight = new int[1];
    glfwGetWindowSize(window, windowWidth, windowHeight);
    Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0f),
        (float) windowWidth[0] / (float) windowHeight[0],
        0.1f,
        100.0f);
    projection.mul(Camera.camera.viewMatrix(), MVP);

    glUniformMatrix4fv(mvpShaderLocation, false, MVP.get(new float[16]));

    mousePos = Mouse.getCursorPosition();
    mousePos.mul(1 / (float)windowWidth[0], 1 / (float)windowHeight[0]);
    mousePos.sub(.5f, .5f);
    mousePos.mul(1.565f, 0.885f);
    Vector2f screenPos = new Vector2f(mousePos);
    Camera.camera.toWorldSpace(mousePos);
    workspace.mouseMove(mousePos, screenPos);

    int vao = glGenVertexArrays();
    glBindVertexArray(vao);

    auxList = workspace.getMesh(currentTime);
    mesh = new float[auxList.size()];
    for (int i = 0; i < auxList.size(); i++)
      mesh[i] = auxList.get(i);

    auxList = workspace.getUV();
    UVs = new float[auxList.size()];
    for (int i = 0; i < auxList.size(); i++)
      UVs[i] = auxList.get(i);

    auxList = workspace.getColourTemplate();
    colourTemplates = new float[auxList.size()];
    for (int i = 0; i < auxList.size(); i++)
      colourTemplates[i] = auxList.get(i);

    glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
    glBufferData(GL_ARRAY_BUFFER, mesh, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    glEnableVertexAttribArray(0);

    glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
    glBufferData(GL_ARRAY_BUFFER, UVs, GL_STATIC_DRAW);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
    glEnableVertexAttribArray(1);

    glBindBuffer(GL_ARRAY_BUFFER, colourTemplateBuffer);
    glBufferData(GL_ARRAY_BUFFER, colourTemplates, GL_STATIC_DRAW);
    glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
    glEnableVertexAttribArray(2);

    glBindTexture(GL_TEXTURE_2D, textureID);

    glDrawArrays(
        GL_TRIANGLES,
        0,
        mesh.length / 3
    );

    initializer.getContext().updateGlfwWindow();
    initializer.getRenderer().render(frame, initializer.getContext());

    // Process events
    initializer.getSystemEventProcessor().processEvents(frame, initializer.getContext());
    EventProcessorProvider.getInstance().processEvents();
    LayoutManager.getInstance().layout(frame);
    AnimatorProvider.getAnimator().runAnimations();

    glfwSwapBuffers(window);
    glfwPollEvents();
  }

  @Override
  void destroy(long window) {

    initializer.getRenderer().destroy();

    // Detach and delete shaders
    glDetachShader(programID, vertexShaderID);
    glDetachShader(programID, fragmentShaderID);
    glDeleteShader(vertexShaderID);
    glDeleteShader(fragmentShaderID);

    glDeleteProgram(programID);
    glfwFreeCallbacks(window);
    glfwDestroyWindow(window);

    glfwTerminate();
  }

  /**
   * Load, compile and attach a shader
   *
   * @param shaderPath: shader file name
   * @param shaderType: vertex (GL_VERTEX_SHADER) or fragment (GL_FRAGMENT_SHADER)
   * @return shader identifier
   */
  private int loadShader(String shaderPath, int shaderType) {
    // Load the shader
    String shaderSource;
    try {
      shaderSource = new String(Files.readAllBytes(Path.of(shaderPath)));
    } catch (IOException e) {
      throw new RuntimeException("Can't open fragment shader");
    }

    // Create, compile and attach the shader
    int shaderID = glCreateShader(shaderType);
    glShaderSource(shaderID, shaderSource);
    glCompileShader(shaderID);
    if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE)
      throw new RuntimeException("Error creating vertex shader\n"
          + glGetShaderInfoLog(shaderID, glGetShaderi(shaderID, GL_INFO_LOG_LENGTH)));
    glAttachShader(programID, shaderID);

    return shaderID;
  }

  /**
   * Load a texture
   *
   * @param texturePath: texture file name
   * @return texture identifier
   */
  private int loadTexture(String texturePath) {
    // Load the texture
    TextureLoader tex = new TextureLoader(texturePath);

    // Generate and bind texture
    int textureID = glGenTextures();
    glEnable(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, textureID);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tex.getWidth(), tex.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, tex.buffer());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

    return textureID;
  }

  public UserInput getUserInput() {
    return userInput;
  }

  public void mouseClickEvent(MouseClickEvent event) {
    if (event.getAction() == MouseClickAction.PRESS) {
      workspace.mouseDown(currentTime, event);
    } else if (event.getAction() == MouseClickAction.RELEASE) {
      workspace.mouseUp(currentTime, event);
    }
  }
}
