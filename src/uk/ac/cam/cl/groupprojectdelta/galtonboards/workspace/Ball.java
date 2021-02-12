package uk.ac.cam.cl.groupprojectdelta.galtonboards.workspace;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class Ball {
    Vector2f position;
    List<LogicalLocation> logLocs; // all pegs/buckets the ball will encounter on its path
    int logLocI; // current index into logLocs
    Simulation simulation; // Simulation object that contains this ball

    public Ball(LogicalLocation startingPoint, Simulation sim) {
        simulation = sim;
        logLocI = 0;
        position = startingPoint.getWorldPos();
        logLocs = getLogicalPath(startingPoint);
    }

    List<LogicalLocation> getLogicalPath(LogicalLocation start) {
        List<LogicalLocation> locs = new ArrayList<>();
        locs.add(start);
        int i = 0;
        while (true) {
            if (locs.get(i) instanceof Bucket) {
                Bucket b = (Bucket) locs.get(i);
                if (b.getOutput() == null) { // this is the final bucket
                    return locs; // ONLY EXIT CONDITION - code doesn't get here, you have an infinite loop
                } else {
                    locs.add((LogicalLocation)b.getOutput());
                }
            } else if (locs.get(i) instanceof Peg) {
                Peg p = (Peg) locs.get(i);
                boolean takeLeft = Math.random() < p.leftProb();
                boolean pegIsNext = p.getLeftBucketIndex() == -1;
                if (pegIsNext) {
                    if (takeLeft) {
                        locs.add(p.getLeft());
                    } else {
                        locs.add(p.getRight());
                    }
                } else { // bucket is next
                    if (takeLeft) {
                        locs.add(p.getLeftBucket());
                    } else {
                        locs.add(p.getRightBucket());
                    }
                }
            }
            ++i;
        }
    }

    private void moveTowardsNextLoc(float f) {
        // f is the fraction of the distance between the current and next logical location to move now.
        if (logLocI == logLocs.size() - 1) return; // already in its final bucket
        LogicalLocation currentLoc = logLocs.get(logLocI);
        LogicalLocation nextLoc = logLocs.get(logLocI + 1);
        float currentToNextDistance = nextLoc.getWorldPos().sub(currentLoc.getWorldPos()).length();
        float distToMove = f * currentToNextDistance; // distance between current and next logical location
        Vector2f remainingTrip = nextLoc.getWorldPos().sub(position);
        float tripLeft = 1 - remainingTrip.length() / distToMove; // fraction of trip to next logLoc that's left
        Vector2f dir = remainingTrip.normalize();
        Vector2f moved = dir.mul(Math.max(tripLeft, f) * currentToNextDistance);
        position = position.add(moved);
        if (tripLeft < f) { // then we've reached the next logical location
            if (nextLoc instanceof Bucket && !nextLoc.getBoard().isOpen()) {
                return; // we've reached the end of a bucket that's closed
            }
            position = nextLoc.getWorldPos();
            currentLoc.balls().remove(this);
            nextLoc.balls().add(this);
            logLocI += 1;
            moveTowardsNextLoc(f - tripLeft);
        }
    }
}