package jetbrains.gis.tileprotocol

class TileCoordinates(private val x: Int, private val y: Int, private val z: Int) {

    override fun toString(): String {
        return "$z-$x-$y"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TileCoordinates

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}
