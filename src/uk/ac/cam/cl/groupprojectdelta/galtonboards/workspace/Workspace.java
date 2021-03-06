package uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace;

import java.util.List;
import org.joml.Vector2f;
import org.liquidengine.legui.event.MouseClickEvent;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.graphics.Drawable;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.mouse.ClickableMap;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.mouse.Cursor;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.mouse.WorkspaceClickable;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.mouse.WorkspaceMouseHandler;

public class Workspace implements Drawable {
  public static final Workspace workspace = new Workspace();

  private final Configuration configuration = Configuration.defaultConfig;
  public final WorkspaceMouseHandler mouseHandler = new WorkspaceMouseHandler(configuration);
  private final Simulation simulation = new Simulation(configuration);
  private final Cursor cursor = new Cursor();


  public Configuration getConfiguration() {
    return configuration;
  }

  public Simulation getSimulation() {
    return simulation;
  }

  public void update(float deltaTime) {
    simulation.update(deltaTime);
  }

  public void mouseDown(float time, MouseClickEvent event) {
    mouseHandler.mouseDown(time, event);
  }

  public void mouseUp(float time, MouseClickEvent event) {
    mouseHandler.mouseUp(time, event);
  }

  public void setClickableMap(ClickableMap clickableMap) {
    mouseHandler.setCurrentClickableMap(clickableMap);
  }

  public void resetClickableMAp() {
    mouseHandler.setCurrentClickableMap(configuration);
  }

  public ClickableMap getClickableMap() {
    return mouseHandler.getCurrentClickableMap();
  }

  public void mouseMove(Vector2f pos, Vector2f screenPos) {
    mouseHandler.mouseMove(pos, screenPos);
    cursor.setPosition(pos);
  }

  @Override
  public List<Float> getMesh(float time) {
    List<Float> mesh = configuration.getMesh(time);
    mesh.addAll(simulation.getMesh(time));
    mesh.addAll(cursor.getMesh(time));
    mesh.addAll(mouseHandler.getMesh(time));
    return mesh;
  }

  @Override
  public List<Float> getUV() {
    List<Float> uv = configuration.getUV();
    uv.addAll(simulation.getUV());
    uv.addAll(cursor.getUV());
    uv.addAll(mouseHandler.getUV());
    return uv;
  }

  @Override
  public List<Float> getColourTemplate() {
    List<Float> ct = configuration.getColourTemplate();
    ct.addAll(simulation.getColourTemplate());
    ct.addAll(cursor.getColourTemplate());
    ct.addAll(mouseHandler.getColourTemplate());
    return ct;
  }
}
