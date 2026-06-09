import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

public class Texture {

    private BufferedImage image;
    private double tileU = 1.0; // U repetitions
    private double tileV = 1.0; // V repetitions

    public Texture(String path) throws IOException {
        image = ImageIO.read(new File(path));
        if (image == null) throw new IOException("Could not load texture: " + path);
    }

    public void setTiling(double u, double v) {
        this.tileU = u;
        this.tileV = v;
    }

    public Color getColor(double u, double v) {
        if (image == null) return Color.MAGENTA;

        // apply tiling and wrap
        u = (u * tileU) % 1.0;
        v = (v * tileV) % 1.0;
        if (u < 0) u += 1.0;
        if (v < 0) v += 1.0;

        // OBJ files have V flipped relative to image coords
        v = 1.0 - v;

        int x = Math.max(0, Math.min(image.getWidth()  - 1, (int)(u * image.getWidth())));
        int y = Math.max(0, Math.min(image.getHeight() - 1, (int)(v * image.getHeight())));

        return new Color(image.getRGB(x, y));
    }

    public int getWidth()           { return image.getWidth();  }
    public int getHeight()          { return image.getHeight(); }
    public BufferedImage getImage() { return image; }
}