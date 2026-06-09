import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;

public class RayTracer {

    private static final String OBJ_FILE  = "models/objflow.obj";
    private static final String MTL_FILE  = "models/pavlion_clean.mtl";
    private static final String HDRI_PATH = "models/textures/sky.exr";

    private static final double SHADOW_BIAS = 1e-4;
    private static final int    MAX_DEPTH   = 3;

    private HDRILight hdri = null;

    private final Map<String, Texture> textureMap = new LinkedHashMap<>();

    private Map<String, Material> materials;

    // Balloon colors used for the decoration
    private static final java.awt.Color[] BALLOON_COLORS = {
            new java.awt.Color(255,   0,   0),
            new java.awt.Color(  0,  80, 255),
            new java.awt.Color(255,   0, 180),
            new java.awt.Color(255, 230,   0),
            new java.awt.Color(  0, 230,   0),
            new java.awt.Color(150,   0, 255),
            new java.awt.Color(255, 100,   0),
            new java.awt.Color(255, 255, 255),
            new java.awt.Color(  0, 240, 255),
            new java.awt.Color(255,   0, 255),
            new java.awt.Color(100, 255,   0),
            new java.awt.Color(255,  50,  50)
    };

    private static int    balloonIndex   = 0;
    private static String lastBalloonObj = "";

    public RayTracer() throws Exception {

        materials = new LinkedHashMap<>();

        // Load materials from the MTL file
        loadMtl(MTL_FILE, materials);

        System.out.println("Materials loaded: " + materials.size());

        File objFile = new File(OBJ_FILE);

        List<Object3D> objectList = new ArrayList<>();

        // Load the OBJ model
        if (objFile.exists()) {

            ObjLoader loader = new ObjLoader();
            loader.load(OBJ_FILE);

            List<Vector3D> verts = loader.getVertices();

            // Calculate model bounds
            double minX = Double.MAX_VALUE,  maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE,  maxY = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE,  maxZ = -Double.MAX_VALUE;

            for (Vector3D v : verts) {
                minX = Math.min(minX, v.x);  maxX = Math.max(maxX, v.x);
                minY = Math.min(minY, v.y);  maxY = Math.max(maxY, v.y);
                minZ = Math.min(minZ, v.z);  maxZ = Math.max(maxZ, v.z);
            }

            double cx = (minX + maxX) / 2;
            double cy = (minY + maxY) / 2;
            double cz = (minZ + maxZ) / 2;

            // Scale the model to fit the scene
            double maxSize = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
            double scale   = 3.0 / maxSize;

            System.out.printf("Bounds: X=%.2f  Y=%.2f  Z=%.2f%n",
                    maxX - minX, maxY - minY, maxZ - minZ);

            System.out.printf("Scale: %.6f  Final: %.2f x %.2f x %.2f%n",
                    scale,
                    (maxX - minX) * scale,
                    (maxY - minY) * scale,
                    (maxZ - minZ) * scale);

            Material defaultMat = materials.isEmpty()
                    ? Material.diffuse(new Color(200, 195, 185))
                    : materials.values().iterator().next();

            List<Vector3D> norms = loader.getNormals();
            List<Vector2D> uvs   = loader.getTextureCoordinates();

            // Fill shared geometry pools
            Triangle.initPools(verts.size(), norms.size(), uvs.size());

            for (Vector3D v : verts) {
                Vector3D vt = tr(v, cx, cy, cz, scale);
                Triangle.GVX[Triangle.vTop] = vt.x;
                Triangle.GVY[Triangle.vTop] = vt.y;
                Triangle.GVZ[Triangle.vTop] = vt.z;
                Triangle.vTop++;
            }

            for (Vector3D n : norms) {
                Triangle.GNX[Triangle.nTop] = n.x;
                Triangle.GNY[Triangle.nTop] = n.y;
                Triangle.GNZ[Triangle.nTop] = n.z;
                Triangle.nTop++;
            }

            for (Vector2D uv : uvs) {
                Triangle.GUU[Triangle.tTop] = (float) uv.getU();
                Triangle.GUV[Triangle.tTop] = (float) uv.getV();
                Triangle.tTop++;
            }

            System.out.println("Pools loaded: "
                    + Triangle.vTop + " verts, "
                    + Triangle.nTop + " normals, "
                    + Triangle.tTop + " uvs");

            int triCount = 0;

            // Convert faces into triangles
            for (Face face : loader.getFaces()) {

                int[] vi = face.getVertexIndices();
                int[] ni = face.getNormalIndices();
                int[] ti = face.getUvIndices();

                if (vi == null || vi.length < 3) continue;

                String   matName = face.getMaterialName();
                Material mat     = (matName != null && materials.containsKey(matName))
                        ? materials.get(matName)
                        : defaultMat;

                for (int i = 1; i < vi.length - 1; i++) {

                    int i0 = vi[0], i1 = vi[i], i2 = vi[i + 1];
                    int n0 = -1,    n1 = -1,    n2 = -1;
                    int t0 = -1,    t1 = -1,    t2 = -1;

                    if (ni != null && ni.length > i + 1
                            && ni[0] >= 0 && ni[i] >= 0 && ni[i + 1] >= 0) {
                        n0 = ni[0];  n1 = ni[i];  n2 = ni[i + 1];
                    }

                    if (ti != null && ti.length > i + 1
                            && ti[0] >= 0 && ti[i] >= 0 && ti[i + 1] >= 0) {
                        t0 = ti[0];  t1 = ti[i];  t2 = ti[i + 1];
                    }

                    Texture tex = (matName != null) ? textureMap.get(matName) : null;

                    // Give each balloon a unique color
                    if ("baloon".equals(matName)) {

                        String objName = face.getObjectName() != null
                                ? face.getObjectName() : "";

                        if (!objName.equals(lastBalloonObj)) {
                            lastBalloonObj = objName;
                            balloonIndex   = (balloonIndex + 1) % BALLOON_COLORS.length;
                        }

                        java.awt.Color bc = BALLOON_COLORS[balloonIndex];

                        mat = new Material(
                                bc,
                                mat.getAmbient(),
                                mat.getDiffuse(),
                                mat.getSpecular(),
                                mat.getShininess(),
                                0.35,
                                0.0,
                                1.0
                        );
                    }

                    objectList.add(new Triangle(i0, i1, i2,
                            n0, n1, n2,
                            t0, t1, t2,
                            mat, tex));
                    triCount++;
                }
            }

            System.out.println("Triangles built: " + triCount);
        }

        // Create the camera
        Camera camera = new Camera(new Vector3D(0.0, 0.3, -5.0), 65.0);

        // Create scene lights
        List<Light> lights = new ArrayList<>();

        lights.add(new DirectionalLight(
                new Vector3D(-1, -1, -1),
                Color.WHITE,
                0.85
        ));

        // FIX: PointLight capital L (was Pointlight)
        lights.add(new PointLight(
                new Vector3D(2, 3, -2),
                new Color(255, 240, 220),
                0.6
        ));

        lights.add(new PointLight(
                new Vector3D(-2, 2, -1),
                new Color(220, 230, 255),
                0.4
        ));

        // Create the scene
        // FIX: Scene constructor now receives camera + List<Object3D> + List<Light>
        Scene scene = new Scene(camera, objectList, lights);

        // Load the HDRI
        hdri = new HDRILight(HDRI_PATH);

        // FIX: render() signature is render(Camera, Scene, int, int)
        // Save the result to disk here instead of inside render()
        BufferedImage result = render(camera, scene, 1280, 720);
        ImageIO.write(result, "png", new File("render.png"));
        System.out.println("Image saved to render.png");
    }


    // Load materials from an MTL file

    private void loadMtl(
            String path,
            Map<String, Material> materials
    ) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(path));

        String line;
        String currentName = null;

        Color  color      = Color.WHITE;
        double ambient    = 0.1;
        double diffuse    = 0.9;
        double specular   = 0.2;
        double shininess  = 32.0;
        double reflection = 0.0;
        double refraction = 0.0;
        double ior        = 1.0;

        while ((line = br.readLine()) != null) {

            line = line.trim();

            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] p = line.split("\\s+");

            switch (p[0]) {

                case "newmtl":
                    // Save the current material before starting a new one
                    if (currentName != null) {
                        materials.put(currentName, new Material(
                                color, ambient, diffuse, specular,
                                shininess, reflection, refraction, ior));
                    }

                    currentName = p[1];
                    color      = Color.WHITE;
                    ambient    = 0.1;
                    diffuse    = 0.9;
                    specular   = 0.2;
                    shininess  = 32.0;
                    reflection = 0.0;
                    refraction = 0.0;
                    ior        = 1.0;
                    break;

                case "Kd":
                    color = new Color(
                            Math.min(255, (int)(Double.parseDouble(p[1]) * 255)),
                            Math.min(255, (int)(Double.parseDouble(p[2]) * 255)),
                            Math.min(255, (int)(Double.parseDouble(p[3]) * 255)));
                    break;

                case "Ns":
                    shininess  = Double.parseDouble(p[1]);  break;
                case "Ni":
                    ior        = Double.parseDouble(p[1]);  break;
                case "Pr":
                    reflection = Double.parseDouble(p[1]);  break;
                case "Pm":
                    specular   = Double.parseDouble(p[1]);  break;
            }

            // Read texture map
            if (line.startsWith("map_Kd")) {
                String  texFile = line.substring(7).trim();
                Texture texture = loadTexture(texFile);
                if (texture != null && currentName != null) {
                    textureMap.put(currentName, texture);
                }
            }
        }

        // Save the last material
        if (currentName != null) {
            materials.put(currentName, new Material(
                    color, ambient, diffuse, specular,
                    shininess, reflection, refraction, ior));
        }

        br.close();
    }


    // Load a texture image from several candidate paths

    private Texture loadTexture(String fileName) {

        String[] paths = {
                fileName,
                "models/"          + fileName,
                "models/textures/" + fileName,
                "textures/"        + fileName
        };

        for (String p : paths) {
            try {
                File f = new File(p);
                if (f.exists()) {
                    System.out.println("Texture loaded: " + p);
                    return new Texture(p);
                }
            } catch (Exception e) {
                // Try next path
            }
        }

        System.out.println("Texture not found: " + fileName);
        return null;
    }


    private BufferedImage render(Camera cam, Scene scene, int w, int h) {

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int cores = Runtime.getRuntime().availableProcessors();

        System.out.println("Cores: " + cores + " — " + w + "x" + h);

        ExecutorService pool = Executors.newFixedThreadPool(cores);

        AtomicInteger done      = new AtomicInteger(0);
        AtomicInteger hits      = new AtomicInteger(0);
        CountDownLatch latch    = new CountDownLatch(h);
        AtomicBoolean rendering = new AtomicBoolean(true);

        long renderStart = System.currentTimeMillis();

        // Progress-bar thread
        Thread progressThread = new Thread(() -> {
            while (rendering.get()) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }

                int  d       = done.get();
                int  pct     = (d * 100) / h;
                long elapsed = (System.currentTimeMillis() - renderStart) / 1000;
                long eta     = (d > 0) ? (elapsed * (h - d)) / d : 0;
                int  filled  = pct / 5;

                StringBuilder bar = new StringBuilder("[");
                for (int i = 0; i < 20; i++) bar.append(i < filled ? "█" : "░");
                bar.append("]");

                System.out.printf("\r%s %3d%%  %ds elapsed  ETA: %ds   ",
                        bar, pct, elapsed, eta);
            }
        });

        progressThread.setDaemon(true);
        progressThread.start();

        // 4-sample anti-aliasing offsets
        final double[][] AA_OFFSETS = {
                {0.25, 0.25}, {0.75, 0.25},
                {0.25, 0.75}, {0.75, 0.75}
        };
        final int AA_SAMPLES = 4;

        // Render each row in parallel
        for (int py = 0; py < h; py++) {

            final int row = py;

            pool.submit(() -> {

                int rowHits = 0;

                for (int px = 0; px < w; px++) {

                    double rr = 0, rg = 0, rb = 0;

                    for (double[] off : AA_OFFSETS) {

                        Ray          ray   = cam.generateRay(px, row, w, h, off[0], off[1]);
                        Intersection hit   = scene.intersect(ray);
                        Color        color = scene.getBackground();

                        if (hit != null) {
                            rowHits++;
                            color = shade(hit, scene.getLights(), scene, ray, 0);
                        } else if (hdri != null) {
                            color = hdri.sample(
                                    ray.getDirection().x,
                                    ray.getDirection().y,
                                    ray.getDirection().z);
                        }

                        rr += color.getRed();
                        rg += color.getGreen();
                        rb += color.getBlue();
                    }

                    // Average AA samples
                    img.setRGB(px, row, new Color(
                            (int) Math.min(255, rr / AA_SAMPLES),
                            (int) Math.min(255, rg / AA_SAMPLES),
                            (int) Math.min(255, rb / AA_SAMPLES)
                    ).getRGB());
                }

                hits.addAndGet(rowHits);
                done.incrementAndGet();
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        rendering.set(false);
        progressThread.interrupt();
        pool.shutdown();

        long totalSec = (System.currentTimeMillis() - renderStart) / 1000;

        System.out.printf("%n[████████████████████] 100%%  Finished in %ds%n", totalSec);
        System.out.printf("Intersections hit: %d / %d%n", hits.get(), w * h);

        return img;
    }



    private Color shade(
            Intersection it,
            List<Light> lights,   // fixed typo: Ligth -> Light
            Scene scene,
            Ray inRay,
            int depth
    ) {
        Material mat = it.getMaterial();

        if (mat == null) return Color.MAGENTA;

        Color   base      = mat.getColor();
        Texture activeTex = null;
        Object3D obj      = it.getObject();

        // Check if the object carries a texture directly
        if (obj instanceof Triangle) {
            activeTex = ((Triangle) obj).getTexture();
        }

        // Fall back to the material-texture map
        if (activeTex == null) {
            for (Map.Entry<String, Texture> entry : textureMap.entrySet()) {
                if (materials.containsKey(entry.getKey())
                        && materials.get(entry.getKey()) == mat) {
                    activeTex = entry.getValue();
                    break;
                }
            }
        }

        // Sample texture color
        if (activeTex != null) {
            base = activeTex.getColor(it.getU(), it.getV());
        }

        double ak = mat.getAmbient();
        double dk = mat.getDiffuse();
        double sk = mat.getSpecular();
        double sh = mat.getShininess();
        double rk = mat.getReflection();
        double tk = mat.getRefraction();

        double br = base.getRed()   / 255.0;
        double bg = base.getGreen() / 255.0;
        double bb = base.getBlue()  / 255.0;

        // Ambient light from HDRI or default sky color
        Color ambientHdri = (hdri != null)
                ? hdri.getAmbientColor()
                : new Color(200, 225, 255);

        double r = br * ak * (ambientHdri.getRed()   / 255.0 * 1.8);
        double g = bg * ak * (ambientHdri.getGreen() / 255.0 * 1.8);
        double b = bb * ak * (ambientHdri.getBlue()  / 255.0 * 1.8);

        Vector3D N    = it.getNormal();
        Vector3D P    = it.getPoint();

        // Offset point slightly to avoid self-intersections
        Vector3D Poff = new Vector3D(
                P.x + N.x * SHADOW_BIAS,
                P.y + N.y * SHADOW_BIAS,
                P.z + N.z * SHADOW_BIAS);

        // View direction
        Vector3D V = new Vector3D(
                -inRay.getDirection().x,
                -inRay.getDirection().y,
                -inRay.getDirection().z).normalize();

        // Process every light in the scene
        for (Light light : lights) {   // fixed typo: Ligth -> Light

            double ndl = light.getNDotL(it);
            if (ndl <= 0) continue;

            Vector3D Ldir      = light.getLightDirection(it);
            double   lightDist = light.getDistance(it);
            double   li        = light.getIntensity();
            double   lr        = light.getColor().getRed()   / 255.0;
            double   lg2       = light.getColor().getGreen() / 255.0;
            double   lb        = light.getColor().getBlue()  / 255.0;

            // Soft shadow — 2 jittered samples
            int    SHADOW_SAMPLES = 2;
            double shadowFactor   = 0.0;

            Vector3D perp1 = (Math.abs(Ldir.x) < 0.9)
                    ? new Vector3D( 0,  Ldir.z, -Ldir.y).normalize()
                    : new Vector3D(-Ldir.z, 0,  Ldir.x).normalize();

            Vector3D perp2  = Ldir.cross(perp1).normalize();
            double   spread = 0.04;

            for (int s = 0; s < SHADOW_SAMPLES; s++) {

                double ox = (s == 0 ? -1 : 1) * spread;
                double oy = (s == 0 ?  1 : -1) * spread;

                Vector3D jittered = new Vector3D(
                        Ldir.x + perp1.x * ox + perp2.x * oy,
                        Ldir.y + perp1.y * ox + perp2.y * oy,
                        Ldir.z + perp1.z * ox + perp2.z * oy).normalize();

                Ray shadowRay = new Ray(Poff, jittered);

                if (!scene.isOccluded(shadowRay, lightDist - SHADOW_BIAS)) {
                    shadowFactor += 1.0 / SHADOW_SAMPLES;
                }
            }

            // Keep shadows soft — never fully black
            shadowFactor = 0.35 + shadowFactor * 0.65;

            if (shadowFactor <= 0.01) continue;

            // Diffuse (Lambertian)
            double diff = ndl * dk * li * shadowFactor;
            r += br * diff * lr;
            g += bg * diff * lg2;
            b += bb * diff * lb;

            // Specular (Blinn-Phong)
            if (sk > 0 && shadowFactor > 0.5) {

                Vector3D H   = Ldir.add(V).normalize();
                double   ndh = Math.max(N.dot(H), 0.0);
                double   spec = Math.pow(ndh, sh) * sk * li * shadowFactor;

                r += spec * lr;
                g += spec * lg2;
                b += spec * lb;
            }
        }

        // Reflection
        if (rk > 0 && depth < MAX_DEPTH) {

            Vector3D D       = inRay.getDirection();
            double   dn      = D.dot(N);
            Vector3D reflDir = new Vector3D(
                    D.x - 2 * dn * N.x,
                    D.y - 2 * dn * N.y,
                    D.z - 2 * dn * N.z).normalize();

            Ray          reflRay = new Ray(Poff, reflDir);
            Intersection reflHit = scene.intersect(reflRay);

            if (reflHit != null) {
                Color rc = shade(reflHit, lights, scene, reflRay, depth + 1);
                r += rk * rc.getRed()   / 255.0;
                g += rk * rc.getGreen() / 255.0;
                b += rk * rc.getBlue()  / 255.0;
            } else {
                // Sample HDRI environment for missed reflections
                Color envColor = (hdri != null)
                        ? hdri.sample(reflDir.x, reflDir.y, reflDir.z)
                        : scene.getBackground();
                r += rk * envColor.getRed()   / 255.0;
                g += rk * envColor.getGreen() / 255.0;
                b += rk * envColor.getBlue()  / 255.0;
            }
        }

        // Refraction
        if (tk > 0 && depth < MAX_DEPTH) {

            double   ior  = mat.getRefractiveIndex();
            Vector3D D    = inRay.getDirection();
            double   cosi = -Math.max(-1, Math.min(1, D.dot(N)));

            double   etai  = 1.0;
            double   etat  = ior;
            Vector3D Nrefr = N;

            // Determine if ray is entering or leaving the surface
            if (cosi < 0) {
                cosi  = -cosi;
                Nrefr = new Vector3D(-N.x, -N.y, -N.z);
                double tmp = etai;  etai = etat;  etat = tmp;
            }

            double eta = etai / etat;
            double k   = 1 - eta * eta * (1 - cosi * cosi);

            if (k >= 0) {

                double   c2      = Math.sqrt(k);
                Vector3D refrDir = new Vector3D(
                        eta * D.x + (eta * cosi - c2) * Nrefr.x,
                        eta * D.y + (eta * cosi - c2) * Nrefr.y,
                        eta * D.z + (eta * cosi - c2) * Nrefr.z).normalize();

                Vector3D Prefr = new Vector3D(
                        P.x - Nrefr.x * SHADOW_BIAS,
                        P.y - Nrefr.y * SHADOW_BIAS,
                        P.z - Nrefr.z * SHADOW_BIAS);

                Ray          refrRay = new Ray(Prefr, refrDir);
                Intersection refrHit = scene.intersect(refrRay);

                if (refrHit != null) {
                    Color tc = shade(refrHit, lights, scene, refrRay, depth + 1);
                    r += tk * tc.getRed()   / 255.0;
                    g += tk * tc.getGreen() / 255.0;
                    b += tk * tc.getBlue()  / 255.0;
                }
            }
        }

        // Gamma correction (gamma 2.2)
        r = Math.pow(Math.min(r, 1.0), 1.0 / 2.2);
        g = Math.pow(Math.min(g, 1.0), 1.0 / 2.2);
        b = Math.pow(Math.min(b, 1.0), 1.0 / 2.2);

        return new Color(
                (float) Math.max(0, r),
                (float) Math.max(0, g),
                (float) Math.max(0, b));
    }


    // Helper — translate and scale a vertex into scene space

    private Vector3D tr(Vector3D v, double cx, double cy, double cz, double scale) {
        return new Vector3D(
                (v.x - cx) * scale,
                (v.y - cy) * scale,
                (v.z - cz) * scale);
    }


    // Program entry point

    public static void main(String[] args) throws Exception {
        new RayTracer();
    }
}