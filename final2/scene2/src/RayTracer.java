import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class RayTracer {

    private static final String OBJ_FILE = "C:/Raytracerrepo/final2/scene2/models/Untitled.obj";
    private static final String MTL_FILE  = "C:/Raytracerrepo/final2/scene2/models/Untitled.mtl";

    public RayTracer() throws Exception {

        materials = new LinkedHashMap<>();
        loadMtl(MTL_FILE, materials);
        System.out.println("Materials: " + materials.size());

        File objFile = new File(OBJ_FILE);
        List<Object3D> objectList = new ArrayList<>();

        if (objFile.exists()) {
            ObjLoader loader = new ObjLoader();
            loader.load(OBJ_FILE);

            List<Vector3D> verts = loader.getVertices();

            double minX=Double.MAX_VALUE, maxX=-Double.MAX_VALUE;
            double minY=Double.MAX_VALUE, maxY=-Double.MAX_VALUE;
            double minZ=Double.MAX_VALUE, maxZ=-Double.MAX_VALUE;
            for (Vector3D v : verts) {
                minX=Math.min(minX,v.x); maxX=Math.max(maxX,v.x);
                minY=Math.min(minY,v.y); maxY=Math.max(maxY,v.y);
                minZ=Math.min(minZ,v.z); maxZ=Math.max(maxZ,v.z);
            }

            double cx=(minX+maxX)/2, cy=(minY+maxY)/2, cz=(minZ+maxZ)/2;
            double sizeX=maxX-minX, sizeY=maxY-minY, sizeZ=maxZ-minZ;
            double maxSize=Math.max(sizeX,Math.max(sizeY,sizeZ));
            double scale = 3.0 / maxSize;

            System.out.printf("Bounds: X=%.2f Y=%.2f Z=%.2f%n", sizeX, sizeY, sizeZ);
            System.out.printf("Scale: %.6f  Final: %.2f x %.2f x %.2f%n",
                    scale, sizeX*scale, sizeY*scale, sizeZ*scale);

            Material defaultMat = materials.isEmpty()
                    ? Material.diffuse(new Color(200,195,185))
                    : materials.values().iterator().next();

            List<Vector3D> norms = loader.getNormals();
            List<Vector2D> uvs   = loader.getTextureCoordinates();

            // fill shared geometry pools once with transformed verts, normals and UVs
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
                Triangle.GUU[Triangle.tTop] = (float)uv.getU();
                Triangle.GUV[Triangle.tTop] = (float)uv.getV();
                Triangle.tTop++;
            }

            System.out.println("Pools filled: " + Triangle.vTop + " verts, "
                    + Triangle.nTop + " normals, " + Triangle.tTop + " uvs");

            // build triangles from indices only
            int triCount = 0;
            for (Face face : loader.getFaces()) {
                int[] vi=face.getVertexIndices(), ni=face.getNormalIndices(), ti=face.getUvIndices();
                if (vi==null||vi.length<3) continue;
                String matName=face.getMaterialName();
                Material mat=(matName!=null&&materials.containsKey(matName))
                        ?materials.get(matName):defaultMat;
                for (int i=1; i<vi.length-1; i++) {
                    int i0=vi[0], i1=vi[i], i2=vi[i+1];
                    int n0=-1,n1=-1,n2=-1;
                    if (ni!=null&&ni.length>i+1&&ni[0]>=0&&ni[i]>=0&&ni[i+1]>=0){
                        n0=ni[0]; n1=ni[i]; n2=ni[i+1];
                    }
                    int t0=-1,t1=-1,t2=-1;
                    if (ti!=null&&ti.length>i+1&&ti[0]>=0&&ti[i]>=0&&ti[i+1]>=0){
                        t0=ti[0]; t1=ti[i]; t2=ti[i+1];
                    }
                    Texture tex = (matName != null) ? textureMap.get(matName) : null;

                    // debug: flag when HourGlass01 has no texture
                    if ("HourGlass01".equals(matName) && tex == null) {
                        System.out.println("  DEBUG: HourGlass01 has no texture in textureMap!");
                    }

                    // rotate colors across balloon objects
                    if ("baloon".equals(matName)) {
                        String objName = face.getObjectName() != null ? face.getObjectName() : "";
                        if (!objName.equals(lastBalloonObj)) {
                            lastBalloonObj = objName;
                            balloonIndex = (balloonIndex + 1) % BALLOON_COLORS.length;
                        }
                        java.awt.Color bc = BALLOON_COLORS[balloonIndex];
                        mat = new Material(bc, mat.getAmbient(), mat.getDiffuse(),
                                mat.getSpecular(), mat.getShininess(), 0.35, 0.0, 1.0);
                    }
                    objectList.add(new Triangle(i0,i1,i2,n0,n1,n2,t0,t1,t2,mat,tex));
                    triCount++;
                }
            }
            System.out.println("Triangles: " + triCount);

        } else {
            System.out.println("ERROR: OBJ file not found: " + OBJ_FILE);
        }

        // camera — inside the greenhouse at eye level, looking toward the tree
        // model is ~3 units tall; Y=-1.2 is roughly eye height, Z=-3.2 pulls back to see the full dome
        Camera cam = new Camera(new Vector3D(0.3, -1.2, -3.2), 55.0);

        // main sun — near-vertical, warm white, strong like noon sunlight
        Directionallight sun = new Directionallight(
                new Vector3D(-0.1, -1.0, 0.1),
                new Color(255, 248, 220),
                1.8);

        // sky fill — soft light blue, fills shadow areas
        Directionallight sky = new Directionallight(
                new Vector3D(0.0, -0.6, 0.0),
                new Color(200, 225, 255),
                0.5);

        // warm ground bounce
        Directionallight bounce = new Directionallight(
                new Vector3D(0.0, 0.7, 0.1),
                new Color(255, 235, 180),
                0.25);

        // warm corner point — lower left
        Pointlight warmCorner = new Pointlight(
                new Vector3D(-2.5, 1.5, -2.0),
                new Color(255, 180, 80),
                1.5);

        // sun spot — simulates a ray of sunlight through the dome glass, hits the left plants
        Pointlight sunSpot = new Pointlight(
                new Vector3D(-1.8, -0.5, 0.5),
                new Color(255, 255, 200),
                2.5);

        // background accent lights
        Directionallight bgCyan = new Directionallight(
                new Vector3D(1.0, 0.5, 1.0),
                new Color(0, 180, 255),
                0.3);
        Directionallight bgPurple = new Directionallight(
                new Vector3D(-1.0, 0.5, 1.0),
                new Color(180, 0, 255),
                0.3);
        Directionallight bgPink = new Directionallight(
                new Vector3D(0.0, -1.0, 1.0),
                new Color(255, 0, 150),
                0.2);

        // magic glow on the hourglass
        Pointlight magic = new Pointlight(
                new Vector3D(0.0, -2.0, -0.5),
                new Color(255, 255, 255),
                2.0);

        // electric blue rim — dramatic edge lighting
        Pointlight rimBlue = new Pointlight(
                new Vector3D(3.0, -1.5, 2.0),
                new Color(0, 80, 255),
                1.8);

        // deep purple from below — cosmic energy feel
        Pointlight purple = new Pointlight(
                new Vector3D(-2.5, -4.0, -0.5),
                new Color(200, 0, 255),
                1.5);

        // bright cyan from upper left
        Pointlight cyan = new Pointlight(
                new Vector3D(-1.0, 0.5, -1.5),
                new Color(0, 255, 255),
                1.3);

        // neon pink from the right
        Pointlight pink = new Pointlight(
                new Vector3D(2.0, -2.5, -1.0),
                new Color(255, 0, 180),
                1.2);

        // soft gold — front-lights the hourglass
        Pointlight gold = new Pointlight(
                new Vector3D(0.0, -2.5, -4.0),
                new Color(255, 200, 50),
                1.0);

        Scene scene = new Scene(
                objectList.toArray(new Object3D[0]),
                new Ligth[]{bgCyan, bgPurple, bgPink, magic, rimBlue, purple, cyan, pink, gold},
                new Color(8, 5, 25)
        );

        // HDRI used for reflections only — background stays black
        hdri = new HDRILight(HDRI_PATH);
        System.out.println("HDRI loaded for reflections.");

        new File("C:/Raytracerrepo/final2/scene2/out").mkdirs();

        System.out.println("\n=== Render 4K DCI 4096x2160 ===");
        BufferedImage preview = render(cam, scene, 4096, 2160);
        ImageIO.write(preview, "png", new File("C:/Raytracerrepo/final2/scene2/out/preview.png"));
        System.out.println("Saved: out/preview.png");
    }

    private Vector3D tr(Vector3D v, double cx, double cy, double cz, double scale) {
        return new Vector3D((v.x-cx)*scale, (v.y-cy)*scale, (v.z-cz)*scale);
    }

    private float clampF(float v) { return Math.max(0f, Math.min(1f, v)); }

    // texture map keyed by material name
    private final Map<String,Texture> textureMap = new LinkedHashMap<>();
    private Map<String,Material> materials;

    private void loadMtl(String path, Map<String,Material> mats) {
        File f = new File(path);
        if (!f.exists()) { System.out.println("MTL not found: " + path); return; }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line, name = null;
            Color diff = new Color(200,195,190);
            double amb=0.12, spec=0.30, shin=32.0;
            String mapKd = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] t = line.split("\\s+");
                switch (t[0]) {
                    case "newmtl":
                        if (name != null) saveMtl(mats, name, diff, amb, spec, shin, mapKd);
                        name=t[1]; diff=new Color(200,195,190); amb=0.25; spec=0.30; shin=32.0; mapKd=null;
                        break;
                    case "Kd":
                        if (t.length>=4) diff=new Color(clampF(Float.parseFloat(t[1])),
                                clampF(Float.parseFloat(t[2])), clampF(Float.parseFloat(t[3])));
                        break;
                    case "Ka":
                        if (t.length>=4) {
                            amb=(Double.parseDouble(t[1])+Double.parseDouble(t[2])+Double.parseDouble(t[3]))/3.0;
                            amb=Math.max(0.08,Math.min(0.18,amb));
                        }
                        break;
                    case "Ks":
                        if (t.length>=4) spec=(Double.parseDouble(t[1])+Double.parseDouble(t[2])+Double.parseDouble(t[3]))/3.0;
                        break;
                    case "Ns":
                        try { shin=Math.max(1,Double.parseDouble(t[1])); } catch(Exception e2){}
                        break;
                    case "map_Kd":
                        // handle optional flags like -bm before the actual path
                        if (t.length >= 2) {
                            int si = 1;
                            while (si < t.length-1 && t[si].startsWith("-")) si += 2;
                            if (si < t.length) {
                                StringBuilder sb = new StringBuilder(t[si]);
                                for (int i=si+1; i<t.length; i++) sb.append(" ").append(t[i]);
                                mapKd = sb.toString();
                            }
                        }
                        break;
                }
            }
            if (name != null) saveMtl(mats, name, diff, amb, spec, shin, mapKd);
        } catch (Exception e) { System.out.println("MTL error: " + e.getMessage()); }
    }

    private void saveMtl(Map<String,Material> mats, String name, Color diff,
                         double amb, double spec, double shin, String mapKd) {
        double refl=0, refr=0, ior=1;
        String nl = name.toLowerCase();
        if (nl.equals("glass"))                                              { refl=0.6; refr=0.35; ior=1.52; diff=new Color(180,220,200); }
        else if (nl.contains("glass_frames") || nl.equals("brown_metal"))   { refl=0.5; spec=1.0; shin=200; }
        else if (nl.equals("grey_metal"))                                    { refl=0.45; spec=0.9; shin=150; }
        else if (nl.equals("white_metal"))                                   { refl=0.4; spec=0.85; shin=120; }
        else if (nl.contains("material__10") || nl.contains("material__11")) { refl=0.4; spec=0.8; shin=128; }
        else if (nl.startsWith("bark"))                                      { spec=0.1; shin=16; }
        else if (nl.startsWith("leaf"))                                      { spec=0.15; shin=20; }
        else if (nl.startsWith("sakura"))                                    { spec=0.25; shin=40; }
        else if (nl.equals("tiles_floor"))                                   { refl=0.15; spec=0.5; shin=64; }
        else if (nl.startsWith("wire_"))                                     { refl=0.4; refr=0.5; ior=1.52; }
        else if (nl.equals("gold.001"))                                      { refl=0.5; spec=1.0; shin=256; }
        else if (nl.equals("material"))                                      { spec=0.1; shin=8; }
        mats.put(name, new Material(diff, amb, 0.85, spec, shin, refl, refr, ior>1?ior:1));
        if (mapKd != null) loadTexture(name, mapKd);
    }

    private void loadTexture(String matName, String texPath) {
        // parse optional MTL texture flags: -s (tiling), -o, -mm, -bm
        double tileU = 1.0, tileV = 1.0;
        String actualPath = texPath.trim();

        while (actualPath.startsWith("-")) {
            int spaceIdx = actualPath.indexOf(' ');
            if (spaceIdx < 0) break;
            String flag = actualPath.substring(0, spaceIdx).toLowerCase();
            actualPath = actualPath.substring(spaceIdx + 1).trim();
            if (flag.equals("-s") || flag.equals("-o") || flag.equals("-mm")) {
                // skip the numeric values following the flag
                int numCount = flag.equals("-mm") ? 2 : 3;
                for (int i = 0; i < numCount; i++) {
                    spaceIdx = actualPath.indexOf(' ');
                    if (spaceIdx < 0) break;
                    String num = actualPath.substring(0, spaceIdx).trim();
                    try {
                        double val = Double.parseDouble(num);
                        if (flag.equals("-s") && i == 0) tileU = val;
                        if (flag.equals("-s") && i == 1) tileV = val;
                        actualPath = actualPath.substring(spaceIdx + 1).trim();
                    } catch (NumberFormatException e2) { break; }
                }
            } else if (flag.equals("-bm")) {
                spaceIdx = actualPath.indexOf(' ');
                if (spaceIdx >= 0) actualPath = actualPath.substring(spaceIdx + 1).trim();
            }
        }

        String fileName = new File(actualPath).getName();
        String[] candidates = {
                actualPath,
                "models/textures/color/" + fileName,
                "models/" + fileName,
                "C:/" + fileName,
                fileName
        };
        for (String candidate : candidates) {
            try {
                File tf = new File(candidate);
                if (tf.exists()) {
                    Texture tex = new Texture(candidate);
                    tex.setTiling(tileU, tileV);
                    textureMap.put(matName, tex);
                    System.out.println("  Texture OK: " + matName + " -> " + fileName
                            + " tile=" + tileU + "x" + tileV);
                    return;
                }
            } catch (Exception e) { /* try next candidate */ }
        }
        System.out.println("  Texture MISS: " + matName + " -> " + fileName);
    }

    private BufferedImage render(Camera cam, Scene scene, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Cores: " + cores + " — " + w + "x" + h);

        ExecutorService pool = Executors.newFixedThreadPool(cores);
        AtomicInteger done = new AtomicInteger(0);
        AtomicInteger hits = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(h);

        AtomicBoolean rendering = new AtomicBoolean(true);
        long renderStart = System.currentTimeMillis();

        // progress bar thread
        Thread progressThread = new Thread(() -> {
            while (rendering.get()) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                int d = done.get();
                int pct = (d * 100) / h;
                long elapsed = (System.currentTimeMillis() - renderStart) / 1000;
                long eta = (d > 0) ? (elapsed * (h - d)) / d : 0;
                int filled = pct / 5;
                StringBuilder bar = new StringBuilder("[");
                for (int i = 0; i < 20; i++) bar.append(i < filled ? "█" : "░");
                bar.append("]");
                System.out.printf("\r%s %3d%%  %ds elapsed  ETA: %ds   ", bar, pct, elapsed, eta);
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();

        // 4x MSAA — 2x2 subpixel grid
        final double[][] AA_OFFSETS = {{0.25,0.25},{0.75,0.25},{0.25,0.75},{0.75,0.75}};
        final int AA_SAMPLES = 4;

        for (int py = 0; py < h; py++) {
            final int row = py;
            pool.submit(() -> {
                int rh = 0;
                for (int px = 0; px < w; px++) {
                    double rr=0, rg=0, rb=0;
                    for (double[] off : AA_OFFSETS) {
                        Ray ray = cam.generateRay(px, row, w, h, off[0], off[1]);
                        Intersection hit = scene.intersect(ray);
                        Color color = scene.getBackground();
                        if (hit != null) {
                            rh++;
                            color = shade(hit, scene.getLights(), scene, ray, 0);
                        } else {
                            color = Color.BLACK; // pure black background
                        }
                        rr += color.getRed();
                        rg += color.getGreen();
                        rb += color.getBlue();
                    }
                    // average the 4 AA samples
                    Color finalColor = new Color(
                            (int)Math.min(255, rr/AA_SAMPLES),
                            (int)Math.min(255, rg/AA_SAMPLES),
                            (int)Math.min(255, rb/AA_SAMPLES));
                    img.setRGB(px, row, finalColor.getRGB());
                }
                hits.addAndGet(rh);
                done.incrementAndGet();
                latch.countDown();
            });
        }

        try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        rendering.set(false);
        progressThread.interrupt();
        pool.shutdown();

        long totalSec = (System.currentTimeMillis() - renderStart) / 1000;
        System.out.printf("%n[████████████████████] 100%%  Done in %ds%n", totalSec);
        System.out.printf("Intersections: %d / %d%n", hits.get(), w * h);

        return img;
    }

    private static final int MAX_DEPTH = 3;

    // vibrant balloon colors — assigned per object
    private static final java.awt.Color[] BALLOON_COLORS = {
            new java.awt.Color(255,   0,   0),  // pure red
            new java.awt.Color(  0,  80, 255),  // electric blue
            new java.awt.Color(255,   0, 180),  // neon pink
            new java.awt.Color(255, 230,   0),  // pure yellow
            new java.awt.Color(  0, 230,   0),  // neon green
            new java.awt.Color(150,   0, 255),  // electric purple
            new java.awt.Color(255, 100,   0),  // pure orange
            new java.awt.Color(255, 255, 255),  // pure white
            new java.awt.Color(  0, 240, 255),  // neon cyan
            new java.awt.Color(255,   0, 255),  // pure magenta
            new java.awt.Color(100, 255,   0),  // neon lime
            new java.awt.Color(255,  50,  50),  // neon coral
    };
    private static int balloonIndex = 0;
    private static String lastBalloonObj = "";
    private static final double SHADOW_BIAS = 1e-4;
    private static final String HDRI_PATH = "C:/Raytracerrepo/final2/scene2/models/sky2/sky2.exr";
    private HDRILight hdri = null;

    private Color shade(Intersection it, List<Ligth> lights, Scene scene, Ray inRay, int depth) {
        Material mat = it.getMaterial();
        if (mat == null) return Color.MAGENTA;

        Color base = mat.getColor();
        Texture activeTex = null;

        // option 1 — texture stored directly on the Triangle
        Object3D obj = it.getObject();
        if (obj instanceof Triangle) {
            activeTex = ((Triangle) obj).getTexture();
        }

        // option 2 — look up by material reference in textureMap
        if (activeTex == null) {
            for (Map.Entry<String,Texture> entry : textureMap.entrySet()) {
                if (materials.containsKey(entry.getKey()) &&
                        materials.get(entry.getKey()) == mat) {
                    activeTex = entry.getValue();
                    break;
                }
            }
        }

        if (activeTex != null) base = activeTex.getColor(it.getU(), it.getV());

        double ak = mat.getAmbient();
        double dk = mat.getDiffuse();
        double sk = mat.getSpecular();
        double sh = mat.getShininess();
        double rk = mat.getReflection();
        double tk = mat.getRefraction();

        double br = base.getRed()/255.0, bg = base.getGreen()/255.0, bb = base.getBlue()/255.0;

        // ambient — tinted by the HDRI color
        Color ambientHdri = (hdri != null) ? hdri.getAmbientColor() : new Color(200,225,255);
        double r = br*ak * (ambientHdri.getRed()/255.0   * 1.8);
        double g = bg*ak * (ambientHdri.getGreen()/255.0 * 1.8);
        double b = bb*ak * (ambientHdri.getBlue()/255.0  * 1.8);

        Vector3D N = it.getNormal();
        Vector3D P = it.getPoint();

        // nudge point along normal to avoid self-intersection
        Vector3D Poff = new Vector3D(
                P.x + N.x * SHADOW_BIAS,
                P.y + N.y * SHADOW_BIAS,
                P.z + N.z * SHADOW_BIAS);

        // view direction — from hit point toward camera
        Vector3D V = new Vector3D(
                -inRay.getDirection().x,
                -inRay.getDirection().y,
                -inRay.getDirection().z).normalize();

        // direct lighting with soft shadows
        for (Ligth light : lights) {
            double ndl = light.getNDotL(it);
            if (ndl <= 0) continue;

            Vector3D Ldir     = light.getLightDirection(it);
            double lightDist  = light.getDistance(it);
            double li         = light.getIntensity();
            double lr         = light.getColor().getRed()/255.0;
            double lg2        = light.getColor().getGreen()/255.0;
            double lb         = light.getColor().getBlue()/255.0;

            // 2 jittered shadow samples — fast but smooth
            int SHADOW_SAMPLES = 2;
            double shadowFactor = 0.0;
            // perpendicular vectors to spread shadow samples around Ldir
            Vector3D perp1 = Math.abs(Ldir.x) < 0.9
                    ? new Vector3D(0,Ldir.z,-Ldir.y).normalize()
                    : new Vector3D(-Ldir.z,0,Ldir.x).normalize();
            Vector3D perp2 = Ldir.cross(perp1).normalize();
            double spread = 0.04;
            for (int s = 0; s < SHADOW_SAMPLES; s++) {
                double ox = (s==0?-1:1)*spread;
                double oy = (s==0?1:-1)*spread;
                Vector3D jittered = new Vector3D(
                        Ldir.x + perp1.x*ox + perp2.x*oy,
                        Ldir.y + perp1.y*ox + perp2.y*oy,
                        Ldir.z + perp1.z*ox + perp2.z*oy).normalize();
                Ray shadowRay = new Ray(Poff, jittered);
                if (!scene.isOccluded(shadowRay, lightDist - SHADOW_BIAS)) {
                    shadowFactor += 1.0 / SHADOW_SAMPLES;
                }
            }
            // keep at least 20% light in shadows — approximates indirect bounces
            shadowFactor = 0.20 + shadowFactor * 0.80;
            if (shadowFactor <= 0.01) continue;

            // diffuse (Lambertian)
            double diff = ndl * dk * li * shadowFactor;
            r += br*diff*lr; g += bg*diff*lg2; b += bb*diff*lb;

            // specular (Blinn-Phong)
            if (sk > 0 && shadowFactor > 0.5) {
                Vector3D H = Ldir.add(V).normalize();
                double ndh = Math.max(N.dot(H), 0.0);
                double spec = Math.pow(ndh, sh) * sk * li * shadowFactor;
                r += spec*lr; g += spec*lg2; b += spec*lb;
            }
        }

        // reflection — R = D - 2(D·N)N
        if (rk > 0 && depth < MAX_DEPTH) {
            Vector3D D = inRay.getDirection();
            double dn = D.dot(N);
            Vector3D reflDir = new Vector3D(
                    D.x - 2*dn*N.x,
                    D.y - 2*dn*N.y,
                    D.z - 2*dn*N.z).normalize();
            Ray reflRay = new Ray(Poff, reflDir);
            Intersection reflHit = scene.intersect(reflRay);
            if (reflHit != null) {
                Color rc = shade(reflHit, lights, scene, reflRay, depth+1);
                r += rk * rc.getRed()/255.0;
                g += rk * rc.getGreen()/255.0;
                b += rk * rc.getBlue()/255.0;
            } else {
                // sample HDRI for missed reflections
                Color envColor = (hdri != null)
                        ? hdri.sample(reflDir.x, reflDir.y, reflDir.z)
                        : scene.getBackground();
                r += rk * envColor.getRed()/255.0;
                g += rk * envColor.getGreen()/255.0;
                b += rk * envColor.getBlue()/255.0;
            }
        }

        // refraction (glass / transparent surfaces)
        if (tk > 0 && depth < MAX_DEPTH) {
            double ior = mat.getRefractiveIndex();
            Vector3D D = inRay.getDirection();
            double cosi = -Math.max(-1, Math.min(1, D.dot(N)));
            double etai = 1.0, etat = ior;
            Vector3D Nrefr = N;
            // flip normal if ray is leaving the surface
            if (cosi < 0) { cosi=-cosi; Nrefr=new Vector3D(-N.x,-N.y,-N.z); double tmp=etai; etai=etat; etat=tmp; }
            double eta = etai/etat;
            double k = 1 - eta*eta*(1 - cosi*cosi);
            if (k >= 0) {
                double c2 = Math.sqrt(k);
                Vector3D refrDir = new Vector3D(
                        eta*D.x + (eta*cosi - c2)*Nrefr.x,
                        eta*D.y + (eta*cosi - c2)*Nrefr.y,
                        eta*D.z + (eta*cosi - c2)*Nrefr.z).normalize();
                Vector3D Prefr = new Vector3D(
                        P.x - Nrefr.x*SHADOW_BIAS,
                        P.y - Nrefr.y*SHADOW_BIAS,
                        P.z - Nrefr.z*SHADOW_BIAS);
                Ray refrRay = new Ray(Prefr, refrDir);
                Intersection refrHit = scene.intersect(refrRay);
                if (refrHit != null) {
                    Color tc = shade(refrHit, lights, scene, refrRay, depth+1);
                    r += tk * tc.getRed()/255.0;
                    g += tk * tc.getGreen()/255.0;
                    b += tk * tc.getBlue()/255.0;
                }
            }
        }

        // gamma correction (gamma 2.2)
        r = Math.pow(Math.min(r, 1.0), 1.0/2.2);
        g = Math.pow(Math.min(g, 1.0), 1.0/2.2);
        b = Math.pow(Math.min(b, 1.0), 1.0/2.2);
        return new Color((float)Math.max(0,r), (float)Math.max(0,g), (float)Math.max(0,b));
    }

    // bloom post-process — extracts bright pixels, blurs them and adds back to the original
    private BufferedImage applyBloom(BufferedImage src, int w, int h, float threshold, int radius) {
        BufferedImage bloom = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // step 1 — isolate pixels above the brightness threshold
        int[][] bright = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                java.awt.Color c = new java.awt.Color(src.getRGB(x, y));
                float brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 765.0f;
                bright[x][y] = brightness > threshold ? src.getRGB(x, y) : 0;
            }
        }

        // step 2 — separable box blur on the bright layer
        float[][] blurR = new float[w][h];
        float[][] blurG = new float[w][h];
        float[][] blurB = new float[w][h];

        // horizontal pass
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float r=0, g=0, b=0, count=0;
                for (int dx = -radius; dx <= radius; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= w) continue;
                    int rgb = bright[nx][y];
                    r += (rgb >> 16) & 0xFF;
                    g += (rgb >>  8) & 0xFF;
                    b +=  rgb        & 0xFF;
                    count++;
                }
                blurR[x][y]=r/count; blurG[x][y]=g/count; blurB[x][y]=b/count;
            }
        }

        // vertical pass + composite back onto the original
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float r=0, g=0, b=0, count=0;
                for (int dy = -radius; dy <= radius; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) continue;
                    r += blurR[x][ny]; g += blurG[x][ny]; b += blurB[x][ny];
                    count++;
                }
                // step 3 — add bloom at 50% strength
                java.awt.Color orig = new java.awt.Color(src.getRGB(x, y));
                int fr = Math.min(255, orig.getRed()   + (int)(r/count * 0.5f));
                int fg = Math.min(255, orig.getGreen() + (int)(g/count * 0.5f));
                int fb = Math.min(255, orig.getBlue()  + (int)(b/count * 0.5f));
                bloom.setRGB(x, y, new java.awt.Color(fr, fg, fb).getRGB());
            }
        }
        System.out.println("Bloom applied.");
        return bloom;
    }

    public static void main(String[] args) throws Exception {
        new RayTracer();
    }
}