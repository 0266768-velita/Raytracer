import java.awt.Color;

// directional light — simulates an infinitely distant source, all rays arrive in parallel
public class Directionallight extends Ligth {

    private Vector3D direction;

    public Directionallight(Vector3D direction, Color color, double intensity) {
        super(color, intensity);
        this.direction = direction.normalize();
    }

    // Lambert diffuse factor N·L
    @Override
    public double getNDotL(Intersection intersection) {
        Vector3D toLight = direction.negate().normalize();
        return Math.max(intersection.getNormal().dot(toLight), 0.0);
    }

    // vector from the surface pointing toward the light
    @Override
    public Vector3D getLightDirection(Intersection intersection) {
        return direction.negate().normalize();
    }

    // infinitely far away
    @Override
    public double getDistance(Intersection intersection) {
        return Double.MAX_VALUE;
    }

    public Vector3D getDirection() { return direction; }

    public void setDirection(Vector3D direction) {
        this.direction = direction.normalize();
    }
}