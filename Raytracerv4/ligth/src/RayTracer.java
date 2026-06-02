import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
//Main class of the ray tracer. Configures the scene, executes the render
//pixel by pixel, and saves the result as a image
public class RayTracer {

    static final int WIDTH = 800;
    static final int HEIGHT = 800;

    public RayTracer() throws Exception {

        // camera
        camera cam = new camera(
                new Vector3D(0, 0, 0)
        );

        // ligths
        Ligth[] lights = {

                new Directionallight(
                        new Vector3D(-1.0, -1.0, -1.0),
                        Color.WHITE,
                        2.5
                ),

                new Pointlight(
                        new Vector3D(0.0, 3.0, 2.0),
                        Color.WHITE,
                        1.5
                )
        };

        // objects-sphere-triangle
        Object3D[] objects = {

                new Sphere(
                        new Vector3D(0.5, 1.0, 8.0),
                        0.8,
                        Color.RED
                ),

                new Sphere(
                        new Vector3D(0.1, 1.0, 6.0),
                        0.5,
                        Color.BLUE
                ),

                new Triangle(
                        new Vector3D(-1, 0, 5),
                        new Vector3D(1, 0, 5),
                        new Vector3D(1, -2, 5),
                        Color.GREEN
                ),

                new Triangle(
                        new Vector3D(-1, 0, 5),
                        new Vector3D(1, -2, 5),
                        new Vector3D(-1, -2, 5),
                        Color.GREEN
                )
        };

        // scene
        Scene scene = new Scene(
                objects,
                lights,
                Color.BLACK//Background color for non-intersecting pixels.
        );

        // intersection
        System.out.println(
                scene.intersect(
                        new Ray(
                                new Vector3D(0, 0, 0),
                                new Vector3D(0, 0, 1)
                        )
                )
        );

        // save the render process
        BufferedImage image = render(cam, scene);

        ImageIO.write(
                image,
                "png",
                new File("out/image.png")
        );

        System.out.println(
                "finish en out/image.png"
        );
    }

    private BufferedImage render(
            camera cam,
            Scene scene
    ) {

        BufferedImage image =
                new BufferedImage(
                        WIDTH,
                        HEIGHT,
                        BufferedImage.TYPE_INT_RGB
                );

        int hits = 0;

        for (int py = 0; py < HEIGHT; py++) {

            for (int px = 0; px < WIDTH; px++) {

                Ray ray =
                        cam.generateRay(
                                px,
                                py,
                                WIDTH,
                                HEIGHT
                        );

                Intersection hit =
                        scene.intersect(ray);

                Color pixelColor =
                        scene.background;

                if (hit != null) {

                    hits++;

                    pixelColor =
                            shade(
                                    hit,
                                    scene.lights,
                                    scene.background
                            );
                }

                image.setRGB(
                        px,
                        py,
                        pixelColor.getRGB()
                );
            }
        }



        return image;
    }

    private Color shade(
            Intersection hit,
            Ligth[] lights,
            Color background
    ) {
//Base lighting that avoids completely black shadows
        double ambient = 0.30;

        float r =
                (float) (
                        (hit.color.getRed() / 255.0)
                                * ambient
                );

        float g =
                (float) (
                        (hit.color.getGreen() / 255.0)
                                * ambient
                );

        float b =
                (float) (
                        (hit.color.getBlue() / 255.0)
                                * ambient
                );
//It accumulates the diffuse contribution of each light
        for (Ligth light : lights) {

            double nDotL =
                    light.getNDotL(hit);

            double intensity =
                    light.getIntensity()
                            * nDotL;
//The final color is the component-by-component multiplication of
//the object's color, the light intensity, and the light color
            r += (float) (
                    (hit.color.getRed() / 255.0)
                            * intensity
                            * (light.getColor().getRed() / 255.0)
            );

            g += (float) (
                    (hit.color.getGreen() / 255.0)
                            * intensity
                            * (light.getColor().getGreen() / 255.0)
            );

            b += (float) (
                    (hit.color.getBlue() / 255.0)
                            * intensity
                            * (light.getColor().getBlue() / 255.0)
            );
        }
//It prevents the accumulation of multiple lights.
        r = Math.min(r, 1.0f);
        g = Math.min(g, 1.0f);
        b = Math.min(b, 1.0f);

        return new Color(r, g, b);
    }

    private Color addColor(
            Color a,
            Color b
    ) {

        float r =
                (float) Math.min(
                        (a.getRed() + b.getRed()) / 255.0,
                        1.0
                );

        float g =
                (float) Math.min(
                        (a.getGreen() + b.getGreen()) / 255.0,
                        1.0
                );

        float bl =
                (float) Math.min(
                        (a.getBlue() + b.getBlue()) / 255.0,
                        1.0
                );

        return new Color(r, g, bl);
    }
}