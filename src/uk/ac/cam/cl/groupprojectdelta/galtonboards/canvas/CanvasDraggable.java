package uk.ac.cam.cl.groupprojectdelta.galtonboards.canvas;

import org.joml.Vector2f;

public interface CanvasDraggable {
  public void startDrag(boolean left);
  public void moveDrag(Vector2f delta);
  public void endDrag();
}
