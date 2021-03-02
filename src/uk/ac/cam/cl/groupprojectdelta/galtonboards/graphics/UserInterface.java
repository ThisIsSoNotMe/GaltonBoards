package uk.ac.cam.cl.groupprojectdelta.galtonboards.graphics;

import org.joml.Vector2f;
import org.liquidengine.legui.component.*;
import org.liquidengine.legui.component.event.selectbox.SelectBoxChangeSelectionEvent;
import org.liquidengine.legui.component.event.slider.SliderChangeValueEvent;
import org.liquidengine.legui.component.optional.TextState;
import org.liquidengine.legui.component.optional.align.HorizontalAlign;
import org.liquidengine.legui.component.optional.align.VerticalAlign;
import org.liquidengine.legui.event.Event;
import org.liquidengine.legui.event.MouseClickEvent;
import org.liquidengine.legui.event.ScrollEvent;
import org.liquidengine.legui.icon.CharIcon;
import org.liquidengine.legui.icon.Icon;
import org.liquidengine.legui.listener.EventListener;
import org.liquidengine.legui.listener.MouseClickEventListener;
import org.liquidengine.legui.listener.ScrollEventListener;
import org.liquidengine.legui.style.Border;
import org.liquidengine.legui.style.border.SimpleLineBorder;
import org.liquidengine.legui.style.color.ColorConstants;
import org.liquidengine.legui.style.font.FontRegistry;
import org.liquidengine.legui.style.font.TextDirection;
import org.lwjgl.opengl.GL;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Configuration;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Workspace;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.board.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class UserInterface {
  public static UserInterface userInterface;
  public Panel editPanel;
  public Slider probabilitySlider;

  private final WindowBoards windowBoards;

  public Configuration getConfiguration() {
    return windowBoards.getConfiguration();
  }

  UserInterface(WindowBoards windowBoards) {
    this.windowBoards = windowBoards;
  }

  /**
   * Initialize OpenGL and all the windows
   * Then, run the main loop
   */

  public void start() {
    final int editPanelWidth = 400;

    // Panels for boards and UI sections

    Panel rightPanel = getRightPanel(editPanelWidth);
    Panel leftPanel = getLeftPanel();
    editPanel = getEditPanel(editPanelWidth);

    windowBoards.addComponent(rightPanel);
    windowBoards.addComponent(leftPanel);
    windowBoards.addComponent(editPanel);

      // Select board SelectBox

    EventListener<SelectBoxChangeSelectionEvent<String>> selectEL = new EventListener<>() {
      @Override
      public void process(SelectBoxChangeSelectionEvent<String> event) {
        if (event.getNewValue().equals(event.getOldValue())) {
          return;
        }
        windowBoards.getSimulation().stop();
        windowBoards.getConfiguration().setConfiguration(event.getNewValue());
        windowBoards.getSimulation().run();
      }
    };

    List<String> labels = new ArrayList<>(Configuration.savedConfigurations.keySet());
    windowBoards.addComponent(makeSelectBox(128, 16, 96, 192, labels, selectEL));


    System.setProperty("joml.nounsafe", Boolean.TRUE.toString());
    System.setProperty("java.awt.headless", Boolean.TRUE.toString());
    // Initialize OpenGL
    if (!org.lwjgl.glfw.GLFW.glfwInit()) {
      throw new RuntimeException("Can't initialize GLFW");
    }

    // Set window hints
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);

    // Initialize window for the boards
    long windowBoardsID = glfwCreateWindow(windowBoards.getWidth(), windowBoards.getHeight(), "Galton Boards", NULL, NULL);
    if (windowBoardsID == NULL) throw new RuntimeException("Failed to create the GLFW window for the boards");
    glfwShowWindow(windowBoardsID);
    glfwMakeContextCurrent(windowBoardsID);
    GL.createCapabilities();
    windowBoards.initialize(windowBoardsID);

    // Main loop
    while (!glfwWindowShouldClose(windowBoardsID)) {
      windowBoards.loop(windowBoardsID);
    }

    // Destroy windows
    windowBoards.destroy(windowBoardsID);
  }

  private Panel getLeftPanel() {
    Panel leftPanel = new Panel(0, 0, 320, windowBoards.getHeight());
    leftPanel.getStyle().getBackground().setColor(ColorConstants.gray());
    leftPanel.getStyle().setBorder(new SimpleLineBorder());

    // Buttons for play/pause/stop

    leftPanel.add(makeButton(64, 32, 32, 0xF40A,
            (MouseClickEventListener) event -> windowBoards.getSimulation().run()));
    leftPanel.add(makeButton(64, 128, 32, 0xF3E4,
            (MouseClickEventListener) event -> windowBoards.getSimulation().pause()));
    leftPanel.add(makeButton(64, 224, 32, 0xF4DB,
            (MouseClickEventListener) event -> windowBoards.getSimulation().stop()));

    Slider simSpeedSlider = new Slider(80, 300, 160, 30);
    simSpeedSlider.setMinValue(0.05f);
    simSpeedSlider.setMaxValue(15);
    simSpeedSlider.getListenerMap().addListener(SliderChangeValueEvent.class, this::speedSliderChangeEvent);

    leftPanel.add(simSpeedSlider);

    Slider ballSpawnSlider = new Slider(80, 400, 160, 30);
    ballSpawnSlider.setMinValue(0.5f);
    ballSpawnSlider.setMaxValue(30);
    ballSpawnSlider.getListenerMap().addListener(SliderChangeValueEvent.class, this::spawnSliderChangeEvent);

    leftPanel.add(ballSpawnSlider);

    // Button for adding boards
    leftPanel.add(makeAddBoardButton(80, 500, 160, 40, "Add a Binomial board",
                                     BinomialBoard.class));
    leftPanel.add(makeAddBoardButton(80, 550, 160, 40, "Add a Gaussian board",
                                     GaussianBoard.class));
    leftPanel.add(makeAddBoardButton(80, 600, 160, 40, "Add a Geometric board",
                                     GeometricBoard.class));
    leftPanel.add(makeAddBoardButton(80, 650, 160, 40, "Add a Uniform board",
                                     UniformBoard.class));

    return leftPanel;
  }

  public void speedSliderChangeEvent(SliderChangeValueEvent<Slider> event) {
    Workspace.workspace.getSimulation().speed = event.getNewValue();
  }

  public void spawnSliderChangeEvent(SliderChangeValueEvent<Slider> event) {
    Workspace.workspace.getSimulation().timeBetweenBalls = 1 / event.getNewValue() / event.getNewValue();
    Workspace.workspace.getSimulation().timeTillNextBall *= (event.getOldValue() * event.getOldValue());
    Workspace.workspace.getSimulation().timeTillNextBall /= (event.getNewValue() * event.getNewValue());
  }

  private Panel getRightPanel(int editPanelWidth) {
    Panel rightPanel = new Panel(320, 0, windowBoards.getWidth() - 320 - editPanelWidth, windowBoards.getHeight());
    rightPanel.getStyle().getBackground().setColor(ColorConstants.transparent());
    rightPanel.getStyle().setBorder(new SimpleLineBorder());
    rightPanel.getListenerMap().addListener(MouseClickEvent.class,  (MouseClickEventListener) windowBoards::mouseClickEvent);
    rightPanel.getListenerMap().addListener(ScrollEvent.class,  (ScrollEventListener) event -> windowBoards.getUserInput().scroll(event));

    // Zoom buttons

    rightPanel.add(makeButton(24, 108, windowBoards.getHeight() - 32, 0xF374,
            (MouseClickEventListener) event -> System.out.println("TODO")));
    rightPanel.add(makeButton(24, 108, windowBoards.getHeight() - 64, 0xF415,
            (MouseClickEventListener) event -> System.out.println("TODO")));

    // Movement buttons

    float movement = 0.5f;
    rightPanel.add(makeButton(24, 24, windowBoards.getHeight() - 64, 0xF141,
            makeMovementCallback(windowBoards.getCamera(), movement, 0)));
    rightPanel.add(makeButton(24, 52, windowBoards.getHeight() - 32, 0xF140,
            makeMovementCallback(windowBoards.getCamera(), 0, -movement)));
    rightPanel.add(makeButton(24, 52, windowBoards.getHeight() - 64, 0xF44A,
            (MouseClickEventListener) event -> windowBoards.getCamera().Reset()));
    rightPanel.add(makeButton(24, 52, windowBoards.getHeight() - 96, 0xF143,
            makeMovementCallback(windowBoards.getCamera(), 0, movement)));
    rightPanel.add(makeButton(24, 80, windowBoards.getHeight() - 64, 0xF142,
            makeMovementCallback(windowBoards.getCamera(), -movement, 0)));

    return rightPanel;
  }

  private Panel getEditPanel(int editPanelWidth) {
    Panel editPanel = new Panel(windowBoards.getWidth() - editPanelWidth, 0, editPanelWidth, windowBoards.getHeight());
    editPanel.getStyle().getBackground().setColor(ColorConstants.lightGray());
    editPanel.getStyle().setBorder(new SimpleLineBorder());

    Label selectedLabel = new Label(100, 50, 200, 100);
    selectedLabel.setTextState(new TextState("Test"));
    editPanel.add(selectedLabel);

    probabilitySlider = new Slider(100, 150, 200, 30);
    probabilitySlider.setMaxValue(1);
    probabilitySlider.getListenerMap().addListener(SliderChangeValueEvent.class, this::sliderChangeEvent);
    editPanel.add(probabilitySlider);

    return editPanel;
  }

  public void sliderChangeEvent(SliderChangeValueEvent<Slider> event) {
    if (Workspace.workspace.mouseHandler.selectedClickable instanceof Board) {
      for (Peg peg : ((Board) Workspace.workspace.mouseHandler.selectedClickable).getPegs()) {
        peg.setProbability(1 - event.getNewValue());
      }
    }
  }

  private static MouseClickEventListener makeMovementCallback(Camera camera, float dx, float dy) {
    return event -> {
    if (event.getAction().equals(MouseClickEvent.MouseClickAction.CLICK)) {
        camera.setPosition(
              camera.getPosition().add(dx, dy, 0)
        );
      }
    };
  }

  private static Button makeButton(int size, int xPos, int yPos, int iconCode, EventListener<MouseClickEvent> cb) {
    Icon iconRun = new CharIcon(new Vector2f(size, size), FontRegistry.MATERIAL_DESIGN_ICONS,
            (char) iconCode, ColorConstants.black());
    iconRun.setHorizontalAlign(HorizontalAlign.CENTER);
    iconRun.setVerticalAlign(VerticalAlign.MIDDLE);
    Button button = new Button("", xPos, yPos, size, size);
    button.getStyle().getBackground().setIcon(iconRun);
    button.getStyle().setBorder(new SimpleLineBorder(ColorConstants.black(), 1));
    button.getListenerMap().addListener(MouseClickEvent.class, cb);
    return button;
  }

  private Button makeAddBoardButton(int xPos, int yPos, int width, int height, String boardName,
                                    Class<? extends Board> boardClass) {
    Button button = new Button(boardName, xPos, yPos, width, height);
    button.getStyle().setBorder(new SimpleLineBorder(ColorConstants.black(), 1));
    button.getListenerMap().addListener(MouseClickEvent.class, event -> {
      if (event.getAction().equals(MouseClickEvent.MouseClickAction.CLICK)) {
        try {
          getConfiguration().addBoard(boardClass.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    return button;
  }

  private static SelectBox<String> makeSelectBox (int width, int height, int xPos, int yPos,Iterable<String> labels,
                                                  EventListener<SelectBoxChangeSelectionEvent<String>> callback) {
    SelectBox<String> selectBox = new SelectBox<String>(xPos, yPos, width, height);
    selectBox.getStyle().setBorder(new SimpleLineBorder(ColorConstants.black(), 1));
    for (String label : labels) {
      selectBox.addElement(label);
    }
    selectBox.addSelectBoxChangeSelectionEventListener(callback);
    return selectBox;
  }

}

