package uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.board;

import org.joml.Vector2f;
import org.joml.Vector2i;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Ball;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.LogicalLocation;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Simulation;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Workspace;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.mouse.WorkspaceSelectable;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.graphics.Drawable;

import java.util.*;

public class Peg implements WorkspaceSelectable, LogicalLocation, Drawable {

    // The position of this peg in the board's grid
    private Vector2i gridPos;
    private Vector2f worldPos;

    // The probability that a ball falls to the left
    private float probability;

    // The board that this peg is on
    private Board board;

    private final float RADIUS = 0.4f;

    private Set<Ball> ballsAtPeg;

    List<String> ballsTaggedWith;

    /*
    =====================================================================
                             CONSTRUCTORS
    =====================================================================
     */

    /**
     * Default constructor for a root peg with an equal left to right probability.
     * @param board : Board - The board that this peg is a part of.
     */
    public Peg(Board board) {
        this(0.5f, 0, board);
    }

    /**
     * Construct a peg of index pegInd with a custom probability for where the balls fall.
     * @param probability : float - The probability that a ball falls left (in the range of [0,1]).
     * @param pegInd : int - The index of this peg in the board's peg list.
     * @param board : Board - The board that this peg is a part of.
     */
    public Peg(float probability, int pegInd, Board board) {
        /* For triangle numbers: n(n+1)/2 = x where n = #pegs on bottom row,
           therefore for row of peg with id x, row = floor((-1 + sqrt(1+8x)) / 2) */
        int row = (int) Math.floor((-1 + Math.sqrt(1 + 8*pegInd)) / 2);
        int rowInd = pegInd - ((row * (row+1)) / 2);
        this.gridPos = new Vector2i(row,rowInd);
        this.probability = probability;
        this.board = board;
        this.ballsAtPeg = new HashSet<>();
        this.ballsTaggedWith = new ArrayList<>();
        setPosition();
    }

    /*
    =====================================================================
                                SETTERS
    =====================================================================
     */

    /**
     * Calculate the world position of this peg.
     */
    void setPosition() {
        // Assume that the board's world point is measured from the very centre of the board
        float xPos = (gridPos.y - gridPos.x * 0.5f) * Board.unitDistance + board.getWorldPos().x;
        float yPos = (-1 - gridPos.x * (float) Math.sqrt(3) / 2f) * Board.unitDistance + board.getWorldPos().y + board.getDimensions().y / 2f;
        this.worldPos = new Vector2f(xPos, yPos);
    }



    /**
     * Helper method to convert a Vector2i grid position into a list index. Static to save on memory.
     * @param gp : Vector2i - The grid position to convert.
     * @return An index corresponding to gp in a list.
     */
    private static int gridPosToIndex(Vector2i gp) {
        return (gp.x * (gp.x + 1)) / 2 + gp.y;
    }

    /**
     * Allow for the probability of a peg to be changed.
     * @param probability : float - The new probability of the ball falling left.
     */
    public void setProbability(float probability) {
        this.probability = probability;
    }

    /*
    =====================================================================
                            PUBLIC GETTERS
    =====================================================================
     */

    /**
     * Get the next peg if a ball falls to the left of the current peg.
     * @return The peg to the left on the next row down. Null if no peg exists.
     */
    public Peg getLeft() {
        //TODO - If this is computationally expensive, could save this as a variable
        Vector2i leftPos = new Vector2i(gridPos.x + 1, gridPos.y);
        return board.getPeg(gridPosToIndex(leftPos));
    }

    /**
     * Get the next peg if a ball falls to the right of the current peg.
     * @return The peg to the right on the next row down. Null if no peg exists.
     */
    public Peg getRight() {
        //TODO - If this is computationally expensive, could save this as a variable
        Vector2i rightPos = new Vector2i(gridPos.x + 1, gridPos.y + 1);
        return board.getPeg(gridPosToIndex(rightPos));
    }

    /**
     * Get the probability that a ball falls to the left.
     * @return Probability that a ball falls to the left.
     */
    public float leftProb() {
        return probability;
    }

    /**
     * Get the probability that a ball falls to the right.
     * @return Probability that a ball falls to the right.
     */
    public float rightProb() {
        return 1-probability;
    }

    /**
     * Get the Vector2i grid position of this peg (root position is <0,0>).
     * @return The grid position.
     */
    public Vector2i getGridPos() {
        return new Vector2i(gridPos);
    }

    /**
     * Using the grid position and the board's world position, calculate the world position of the peg.
     * @return The peg's world position.
     */
    public Vector2f getWorldPos() {
        return new Vector2f(worldPos);
    }

    @Override
    public Set<Ball> balls() {
        return ballsAtPeg;
    }

    @Override
    public void addBall(Ball ball) {
        ballsAtPeg.add(ball);
    }

    @Override
    public void removeBall(Ball ball) {
        ballsAtPeg.remove(ball);
    }

    @Override
    public List<String> getGivenTags() {
        return ballsTaggedWith;
    }

    @Override
    public void setGivenTags(List<String> newTagList) {
        ballsTaggedWith = newTagList;
    }

    @Override
    public void clearGivenTags() {
        ballsTaggedWith = new ArrayList<>();
    }

    /**
     * If this peg is on the last row, then return the output column that a left falling ball will go into.
     * @return The output column (implicit bucket) index for output to the left.
     */
    public int getLeftColumnIndex() {
        if (gridPos.x == board.getIsoGridWidth() - 1) {
            return (gridPos.y);
        }
        if (board.getIsoGridWidth() == 0) {
            return 0;
        }
        // This peg is not on the last row so don't return a valid bucket
        return -1;
    }

    /**
     * If this peg is on the last row, then return the output column that a right falling ball will go into.
     * @return The output column (implicit bucket) index for output to the right.
     */
    public int getRightColumnIndex() {
        if (gridPos.x == board.getIsoGridWidth() - 1) {
            return (gridPos.y + 1);
        }
        // This peg is not on the last row so don't return a valid bucket
        return -1;
    }

    /**
     * Get the column to the left of this peg if the peg is on the bottom row.
     * @return
     */
    public ColumnTop getLeftColumn() {
        return board.getColumnTop(getLeftColumnIndex());
    }

    /**
     * Get the column to the right of this peg if the peg is on the bottom row.
     * @return
     */
    public ColumnTop getRightColumn() {
        return board.getColumnTop(getRightColumnIndex());
    }

    //OBSOLETE
    @Override
    public boolean containsPoint(Vector2f point) {
        return getWorldPos().distance(point) < RADIUS;
    }

    @Override
    public boolean intersectsRegion(Vector2f from, Vector2f to) {
        Vector2f topleft = new Vector2f(RADIUS, RADIUS);
        Vector2f bottomright = new Vector2f(RADIUS, RADIUS);
        topleft.add(getWorldPos());
        bottomright.add(getWorldPos());
        return from.x < bottomright.x
            && from.y < bottomright.y
            && to.x > topleft.x
            && to.y > topleft.y;
    }

    public Bucket getLeftBucket() {
        return board.getBucket(getLeftColumnIndex());
    }

    //OBSOLETE
    public Bucket getRightBucket() {
        return board.getBucket(getRightColumnIndex());
    }

    /**
     * Getter for the board this peg is on.
     * @return The board.
     */
    @Override
    public Board getBoard() {
        return board;
    }

    @Override
    public List<Float> getMesh(float time) {

        List<Float> points;
        Vector2f bound = new Vector2f();

        Vector2f dimensions = new Vector2f(0.1f);
        worldPos.add(dimensions, bound);

        //  +----+
        //  |1 / |
        //  | / 2|
        //  +----+

        float ratio = probability;

        points = new ArrayList<>(Arrays.asList(
                // Face 1
                worldPos.x, worldPos.y, z,
                bound.x, worldPos.y, z,
                ratio * bound.x + (1 - ratio) * worldPos.x, bound.y, z
        ));

        return points;
    }

    @Override
    public List<Float> getUV() {

        final float top = 0.5f;
        final float bottom = 0.75f;
        final float left = 0.75f;
        final float right = 1f;

        List<Float> UVs = List.of(
                // face 1
                top,left,
                bottom,left,
                bottom,right
        );
        return UVs;
    }

    @Override
    public List<Float> getColourTemplate() {
        return List.of(
                1f,1f,1f,
                1f,1f,1f,
                1f,1f,1f
        );
    }

        /*
    =====================================================================
                               MOUSE EVENTS
    =====================================================================
     */

    @Override
    public void select() {
        //TODO: start drawing highlight around peg to show that it's selected.
    }

    public void deselect() {
        //TODO: stop drawing highlight around peg to show that it's not selected.
    }
}