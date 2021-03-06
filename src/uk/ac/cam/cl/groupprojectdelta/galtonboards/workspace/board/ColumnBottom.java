package uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.board;

import org.joml.Vector2f;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.Ball;
import uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace.LogicalLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ColumnBottom extends Column implements LogicalLocation {

    // The world coordinates of the top of this column
    private Vector2f worldPos;

    private Set<Ball> ballSet;

    private List<String> ballsTaggedWith;

    private Bucket bucket;

    private String tag;

    // The index of this column
    private int columnIndex;

    /**
     * Create the top part of a column output of a board.
     * @param columnIndex : int - The index of this column.
     * @param bucket : Bucket - The bucket this column feeds in to.
     * @param board : Board - The board this column is a part of.
     */
    public ColumnBottom(int columnIndex, Bucket bucket, Board board) {
        super(columnIndex, bucket, board);
        ballSet = new HashSet<>();
        this.bucket = bucket;
        this.ballsTaggedWith = new ArrayList<>();
    }

    /**
     * Update the bucket that this column feeds into.
     */
    public void setBucket(Bucket bucket) {
        super.setBucket(bucket);
        this.bucket = bucket;
    }

    /**
     * Update the world coordinates of this column bottom.
     */
    @Override
    public void setPosition() {
        float xPos = (super.columnIndex + 0.5f) * Board.unitDistance + getBoard().getWorldPos().x - getBoard().getDimensions().x / 2f;
        float yPos = getBoard().getWorldPos().y - getBoard().getDimensions().y / 2f;
        worldPos =  new Vector2f(xPos, yPos);
    }

    public void setTag(String tag) {
        this.tag = tag;
        System.out.println("SET TAG: " + tag);
    }

    @Override
    public Vector2f getWorldPos() {
        return new Vector2f(worldPos);
    }

    @Override
    public Set<Ball> balls() {
        return ballSet;
    }

    @Override
    public void addBall(Ball ball) {
        ballSet.add(ball);
        bucket.addBall(ball);
    }

    @Override
    public void removeBall(Ball ball) {
        ballSet.remove(ball);
        bucket.removeBall(ball);
        if (tag != null) {
            ball.setTag(tag);
        }
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
}
