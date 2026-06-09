import java.awt.Color;
//Directional light that simulates an infinitely distant light source
public class Directionallight extends Ligth {

    private Vector3D direction;
//Standardized direction TOWARDS the light points
    public Directionallight(Vector3D direction,
                            Color color,
                            double intensity) {

        super(color, intensity);
       // It is normalized during construction to ensure that all lighting calculations
// use a unit vector without depending on the caller.
        this.direction = direction.normalize();
    }

    @Override
    public double getNDotL(Intersection intersection) {
//Light stores the direction it is pointing towards, but for the calculation
        Vector3D toLight =
                direction.negate();
//Dot product between the normal and the vector towards the light.
        return Math.max(
                intersection.normal.dot(toLight),
                0.0
        );
    }

    public Vector3D getDirection() {
        return direction;
    }
}