import java.util.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.Rectangle;

import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WorldMap {
    public String filename;
    public ArrayList<Marker> markers;
    public final int SCALE = 1;
    public final int MARKER_PADDING = 5; //Pixels
    public final int BOX_PADDING = 1; //Pixels
    public final int IMAGE_PADDING = 10; //Pixels 
    public final int CHUNK_SIZE = 16; //Pixels
    public int iconSize;
    public int numIcons;
    public String iconsFile;
    public boolean DEBUG = false;
    public boolean overtopCarto;
    public String cartographMap;
    public String worldDir;
    public String homesFile;
    public String labelsFile;
    public String playerPosFile;
    public Properties properties;
    public boolean standAlone;
    
    public ArrayList<String> extraFoldersInWorld;
    
    private static WorldMap instance;
    protected static final Logger log = Logger.getLogger("Minecraft");
    
    public static void main(String args[]) throws Exception {
        WorldMap wm = new WorldMap(false);
        if (wm.load()) {
            if (wm.loadMarkers())
                wm.render();
        }
    }
    
    public static WorldMap getInstance() {
        if (instance == null) {
            instance = new WorldMap(true);
        }
        return instance;
    }
    
    public WorldMap(boolean isPlugin) {
        standAlone = !isPlugin;
        extraFoldersInWorld = new ArrayList<String>();
        markers = new ArrayList<Marker>();
        properties = new Properties();
    }
    
    public boolean load() {
        try {
            File f = new File("mapper.properties");
            if (f.exists())
                properties.load(new FileInputStream("mapper.properties"));
            else
                f.createNewFile();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while creating mapper properties file.", e);
        }
        
        DEBUG = Boolean.parseBoolean(properties.getProperty("debug", "false"));
        
        String[] tokens = properties.getProperty("extrafoldersinworld", "").split(",");
        for (String token : tokens)
            extraFoldersInWorld.add(token);
        
        filename = properties.getProperty("filename", "map.png");
        iconsFile = properties.getProperty("icons", "icons.png");
        worldDir = properties.getProperty("worlddir");
        cartographMap = properties.getProperty("cartographmap");
        overtopCarto = Boolean.parseBoolean(properties.getProperty("overtopcarto", "false"));
        homesFile = properties.getProperty("homes", "homes.txt");
        labelsFile = properties.getProperty("labels", "mapper-labels.txt");
        playerPosFile = properties.getProperty("playerpos", "mapper-playerpos.txt");
        
        String[] filesToCheck = { homesFile, labelsFile, playerPosFile };
        for (String f : filesToCheck) {
            try {
                File fileCreator = new File(f);
                if (!fileCreator.exists())
                    fileCreator.createNewFile();
            } catch (IOException e) {
                if (standAlone)
                    e.printStackTrace();
                else
                    log.log(Level.SEVERE, "Exception while creating mapper file.", e);
            }
        }
        
        try {
            properties.store(new FileOutputStream("mapper.properties"), null);
        } catch (Exception e) {
            if (standAlone)
                e.printStackTrace();
            else
                log.log(Level.SEVERE, "Exception while saving mapper properties file.", e);
        }
        
        return true;
    }
    
    public boolean loadMarkers() {
        if (Boolean.parseBoolean(properties.getProperty("displayorigin", "false"))) {
            this.addMarker(0, 0, "Origin");
            if (standAlone)
                System.out.println("Displaying Origin.");
        }
        if (Boolean.parseBoolean(properties.getProperty("displayhomes", "false"))) { 
            this.loadHomes();
            if (standAlone)
                System.out.println("Displaying Homes.");
        }
        if (Boolean.parseBoolean(properties.getProperty("displaylabels", "false"))) {
            this.loadLabels(labelsFile);
            if (standAlone)
                System.out.println("Displaying Labels.");
        }
        if (Boolean.parseBoolean(properties.getProperty("displayplayerpos", "false"))) {
            this.loadLabels(playerPosFile);
            if (standAlone)
                System.out.println("Displaying Player positions.");
        }
        
        return true;
    }
    
    public BufferedImage[] splitIcons(BufferedImage img) {  
        iconSize = img.getHeight();
        if (img.getWidth()/iconSize != img.getWidth()/(iconSize+0.0) )
            System.out.println("Icons are not square, problems may ensue.");
        numIcons = img.getWidth()/iconSize; 
        System.out.println("NumIcons: " + Integer.toString(numIcons)); 
        if (numIcons == 0) {
            if (standAlone)
                System.out.println("Since there's no icons, all markers are red dots.");
        }
        BufferedImage[] imgs = new BufferedImage[numIcons];  
        for(int x = 0; x < numIcons; x++) {  
            imgs[x] = new BufferedImage(iconSize, iconSize, img.getType());  
            // Tell the graphics to draw only one block of the image  
            Graphics2D g = imgs[x].createGraphics();  
            g.drawImage(img, 0, 0, iconSize, iconSize, iconSize*x, 0, iconSize*x+iconSize, iconSize, null);
            if (DEBUG) {
                try {
                    ImageIO.write(imgs[x], "PNG", new File(Integer.toString(x)+".png"));            
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            g.dispose();  
        }  
        return imgs;  
    }
    
    public BufferedImage loadImage(String path) {  
        BufferedImage img = null;  
        try {  
            img = ImageIO.read(new File(path));  
        } catch (Exception e) {
            if (standAlone)
                e.printStackTrace();  
            else
                log.log(Level.SEVERE, "Exception while loading image: " + path, e);
        }  
        return img;  
    }  
    
    public void render() {
        try {
            BufferedImage[] icons = splitIcons(loadImage(iconsFile));
            
            if (markers.size() > 0) {
                Font font = new Font("Arial", Font.BOLD, 10);
                int stringWidth;
                int[] worldSize = null;
                
                BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D ig2 = tempImage.createGraphics();
                ig2.setFont(font);
                FontMetrics fontMetrics = ig2.getFontMetrics();
                
                
                
                Marker[] furthest = new Marker[4];
                int[] furthestWidth = new int[4]; // Con: 2 unused variables, Pro: simplicity
                int fontHeight = fontMetrics.getHeight();
                for (int d = 0; d < 4; d++) {
                    for (Marker mark : markers) {
                        if (furthest[d] == null) {
                            furthest[d] = (Marker)mark.clone();
                            furthestWidth[d] = fontMetrics.stringWidth(mark.text);
                        }
                        if (DEBUG && standAlone)
                                System.out.print(Integer.toString(d) + ": " + mark.x + "," + mark.y + "   vs  " + Integer.toString(furthest[d].x) + "," + Integer.toString(furthest[d].y));   
                        switch (d) {
                            case 0: //Left
                                stringWidth = fontMetrics.stringWidth(mark.text);
                                if (furthest[d].x - furthestWidth[d]/2 > mark.x - stringWidth/2) {
                                    furthest[d] = (Marker)mark.clone();
                                    furthestWidth[d] = stringWidth;
                                    if (DEBUG && standAlone)
                                            System.out.print("  -Is now furthest left");
                                }
                                break;
                            case 1: //Top
                                if (furthest[d].y > mark.y) {
                                    furthest[d] = (Marker)mark.clone();
                                    if (DEBUG && standAlone)
                                        System.out.print("  -Is now furthest up");
                                }
                                break;
                            case 2: //Right
                                stringWidth = fontMetrics.stringWidth(mark.text);
                                if (furthest[d].x + furthestWidth[d]/2 < mark.x + stringWidth/2) {
                                    furthest[d] = (Marker)mark.clone();
                                    furthestWidth[d] = stringWidth;
                                    if (DEBUG && standAlone)
                                        System.out.print("  -Is now furthest right");
                                }
                                break;
                            case 3: //Bottom
                                if (furthest[d].y < mark.y) {
                                    furthest[d] = (Marker)mark.clone();
                                    if (DEBUG && standAlone)
                                        System.out.print("  -Is now furthest down");   
                                }
                                break;
                        }
                        if (DEBUG && standAlone)
                            System.out.print("\n"); 
                    }
                }
                if (DEBUG && standAlone) {
                    String[] dir = {"left", "top", "right", "bottom"};
                    int d = 0;
                    System.out.println("Marker determining image size [left, top, right, bottom]:");
                    for (Marker mark : furthest) {
                        System.out.println("\t" + dir[d] + " = " + mark.text + ": " + Integer.toString(mark.x) + " " + Integer.toString(mark.y));
                        d += 1;
                    }
                }
                
                if (overtopCarto) {
                    worldSize = getWorldSize(worldDir);
                    if (standAlone)
                        System.out.println("World Size (in Chunks): L=" + Integer.toString(worldSize[0]) + " T=" + Integer.toString(worldSize[1]) + " R=" + Integer.toString(worldSize[2]) + " B=" + Integer.toString(worldSize[3]));
                    for (int n=0;n<4;n++)
                        worldSize[n] *= CHUNK_SIZE;
                    if (DEBUG && standAlone) {    
                        for (int n=0;n<4;n++)
                            System.out.println(Integer.toString(worldSize[n]) + "   vs  " + Integer.toString(furthest[n].x) + "," + Integer.toString(furthest[n].y));
                    }
                    
                    ///*
                    if (worldSize[0] < furthest[0].x) {
                        furthest[0].x = worldSize[0];
                        furthestWidth[0] = 0;
                        if (DEBUG && standAlone)
                            System.out.println("l");
                    }
                    if (worldSize[1] < furthest[1].y) {
                        furthest[1].y = worldSize[1];
                        if (DEBUG && standAlone)
                            System.out.println("t");
                    }
                    if (worldSize[2] > furthest[2].x) {
                        furthest[2].x = worldSize[2];
                        furthestWidth[2] = 0;
                        if (DEBUG && standAlone)
                            System.out.println("r");
                    }
                    if (worldSize[3] > furthest[3].y) {
                        furthest[3].y = worldSize[3];
                        if (DEBUG && standAlone)
                            System.out.println("b");
                    }
                    //*/
                }
                
                // Calculations = [total distance] * scale + text offsets + image padding
                int width = (Math.abs(furthest[0].x) + Math.abs(furthest[2].x));
                int height = (Math.abs(furthest[1].y) + Math.abs(furthest[3].y));
                int[] origin = {Math.abs(furthest[0].x), Math.abs(furthest[1].y) };
                if (standAlone) {
                    System.out.println("Dimensions: "+Integer.toString(width)+", "+Integer.toString(height));
                    System.out.println("Origin: "+Integer.toString(origin[0])+", "+Integer.toString(origin[1]));
                }
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                
                ig2 = image.createGraphics();
                
                if (overtopCarto) {
                    BufferedImage carto = loadImage(cartographMap);
                    //Since Cartograph has the map pointing east...
                    /*
                    AffineTransform tx = new AffineTransform();
                    //tx.scale(-1, -1);
                    //tx.shear(0, 0);
                    //tx.translate(0, 0);
                    tx.rotate(Math.toRadians(90), carto.getWidth()/2, carto.getHeight()/2);

                    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
                    carto = op.filter(carto, null);
                    */
                    //ig2.drawImage(carto, null, origin[0]-Math.abs(worldSize[0]), origin[1]-Math.abs(worldSize[1]));
                    //ig2.drawImage(carto, null, origin[0]-Math.abs(furthest[0].x), origin[1]-Math.abs(furthest[1].y));
                    ig2.drawImage(carto, null, 0, 0);
                    //ig2.drawImage(carto.getSubimage(origin[0]-Math.abs(worldSize[0]), origin[0]-Math.abs(worldSize[0])), null, origin[0]-Math.abs(worldSize[0]), origin[1]-Math.abs(worldSize[1]));
                }
                ig2.setFont(font);
                fontMetrics = ig2.getFontMetrics();
                if (standAlone)
                    System.out.println("NumMarkers: " + markers.size());
                for (Marker mark : markers) {
                    int xOffset = origin[0] + mark.x;
                    int yOffset = /*height -*/ (origin[1] + mark.y);
                    
                    if (numIcons == 0 || mark.icon >= numIcons) {
                        ig2.setPaint(Color.red);
                        ig2.draw( new Rectangle(xOffset, yOffset, 1, 1) );
                    } else {
                        ig2.drawImage(icons[mark.icon], null, xOffset-iconSize/2, yOffset-iconSize/2);
                    }
                    stringWidth = fontMetrics.stringWidth(mark.text);
                    float alpha = .3f;
                    ig2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    ig2.setPaint(Color.black);
                    ig2.fillRect(xOffset - stringWidth/2 - BOX_PADDING, yOffset - MARKER_PADDING - fontHeight - BOX_PADDING, stringWidth + BOX_PADDING*2, fontHeight + BOX_PADDING*2);
                    //alpha = 1.0f;
                    //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    ig2.setPaint(Color.white);
                    ig2.drawString(mark.text, xOffset - stringWidth/2, yOffset - MARKER_PADDING);
                }
                ImageIO.write(image, "PNG", new File(filename));
            }
        } catch (Exception e) {
            if (standAlone)
                e.printStackTrace();
            else
                log.log(Level.SEVERE, "Exception while loading mapper.", e);
        }
    }
    
    public void addMarker(int x, int y, String s) {
        addMarker(x, y, s, 0);
    }
    
    public void addMarker(int x, int y, String s, int i) {
        markers.add( new Marker(x, y, s, i));
    }
    
    public void loadHomes() {
        try {  
            BufferedReader homes = new BufferedReader(new FileReader(homesFile));  
            String[] tokens; 
            
            String line = "";
            while ( (line = homes.readLine()) != null ) {
                tokens = line.split(":");
                //Hey0's home's flat file format saves as [name:x,y,z:rotX:rotY:?] where the ? is empty making the token length 6 usually.
                if (tokens.length == 6 || tokens.length == 7) { 
                    try {
                        addMarker((int)Double.parseDouble(tokens[1]), (int)Double.parseDouble(tokens[3]), tokens[0], 1);
                    } catch(Exception e) {}
                }
            }
            
            homes.close();
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }
    
    public void loadLabels(String file) {
        try {  
            BufferedReader homes = new BufferedReader(new FileReader(file));  
            String[] tokens; 
            
            String line = "";
            while ( (line = homes.readLine()) != null ) {
                tokens = line.split(":");
                if (tokens.length == 4) {
                    try {
                        addMarker((int)Double.parseDouble(tokens[1]), (int)Double.parseDouble(tokens[2]), tokens[0], Integer.parseInt(tokens[3]));
                    } catch(Exception e) {}
                }
            }
            
            homes.close();
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }
    
    public int fromBase36(String number) {
        String baseDigits = "0123456789abcdefghijklmnopqrstuvwxyz";
        int value = 0;
        int multiplier = 1;
        boolean negative = false;
        int iterator = number.length();
        
        while (iterator > 0) {
            if (iterator == 1 && number.startsWith("-")) {
                negative = true;
            } else {
                value += baseDigits.indexOf(number.substring(iterator-1, iterator)) * multiplier;
                multiplier *= 36; 
            }
            --iterator;
        }
        
        if (negative)
            value *= -1;
        
        return value;
    }
    
    public int[] getWorldSize(String folder) {
        int[] furthest = { 0, 0, 0, 0 };
        ArrayList<int[]> chunkPos = new ArrayList<int[]>();
        
        try {
            File root = new File(folder);
            File[] rootFiles = root.listFiles();
            for (File a : rootFiles) {
                if (a.isDirectory() && !extraFoldersInWorld.contains(a.getName())) {
                    File[] chunkFolders = a.listFiles();
                    for (File b : chunkFolders) {
                        if (b.isDirectory()) {
                            File[] chunkList = b.listFiles();
                            String chunk = chunkList[0].getName();
                            String[] tokens = chunk.split("\\.");
                            int[] coord = { fromBase36(tokens[1]), fromBase36(tokens[2]) };
                            chunkPos.add( coord );
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (chunkPos.size() > 0) {
            for (int d = 0; d < 4; d++) {
                if (d % 2 == 0) //Even, aka top/bottom or y
                    furthest[d] = chunkPos.get(0)[1];
                else //Odd, aka right/left or x
                    furthest[d] = chunkPos.get(0)[0];
                }     
                    
            for (int d = 0; d < 4; d++) { 
                for (int[] chunk : chunkPos) {
                    switch (d) {
                        case 0: //Left
                            if (furthest[d] > chunk[0])
                                furthest[d] = chunk[0];
                            break;
                        case 1: //Top
                            if (furthest[d] > chunk[1])
                                furthest[d] = chunk[1];
                            break;
                        case 2: //Right
                            if (furthest[d] < chunk[0])
                                furthest[d] = chunk[0];
                            break;
                        case 3: //Bottom
                            if (furthest[d] < chunk[1])
                                furthest[d] = chunk[1];
                            break;
                    }
                }
            }
        }
        
        return furthest;
    }
}

class Marker implements Cloneable {
    public int x, y;
    public String text;
    public int icon;
    
    public Marker(int x, int y, String s, int i) {
        this.x = x;
        this.y = y;
        text = s;
        icon = i;
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // This should never happen
            throw new InternalError(e.toString());
        }
    }
}