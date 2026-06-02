import java.awt.Color;

public class Pointlight extends Ligth {

    private Vector3D position;
//Create a focused light in the indicated position.
    public Pointlight(Vector3D position, Color color, double intensity) {
        super(color, intensity);
        this.position = position;
    }
//Calculate the diffuse lighting factor N·L with distance attenuation
    @Override
    public double getNDotL(Intersection intersection) {

        Vector3D toLight =
                position.subtract(intersection.point);

        double distance =
                toLight.length();
//It is normalized after extracting the distance so as not to lose it
        toLight =
                toLight.normalize();
//Dot product between the surface normal and the direction of the light
        double nDotL =
                Math.max(
                        intersection.normal.dot(toLight),
                        0.0
                );

        double attenuation =
                1.0 / (1.0 + 0.05 * distance * distance);
//3D coordinates of light
        return nDotL * attenuation;
    }

    public Vector3D getPosition() {
        return position;
    }
}