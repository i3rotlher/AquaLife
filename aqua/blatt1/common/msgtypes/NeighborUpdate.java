package aqua.blatt1.common.msgtypes;

import aqua.blatt1.common.Direction;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class NeighborUpdate implements Serializable {
    private final InetSocketAddress neighbor;
    private final Direction direction;

    public NeighborUpdate(InetSocketAddress isa, Direction dir) {this.neighbor = isa; this.direction = dir;}

    public InetSocketAddress getNeighbor() {return this.neighbor;}

    public Direction getDirection() {return this.direction;}
}
