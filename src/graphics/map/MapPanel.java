package graphics.map;

/*******************************************************************************
 * Copyright (c) 2008, 2012 Stepan Rutz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stepan Rutz - initial implementation
 *    Olav 		  - markers, scale, implemented for Statens Kartverk's mapservice.
 *******************************************************************************/



import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Font;

import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;

import java.net.URL;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;

import javax.swing.JComponent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import routes.Route;



/**
 * Olav 
 * This is a helper class, used to display the map tiles downloaded from Statens Kartverk in a very useful way. Used by
 * SheepWatch for displaying the position of sheeps on a map.
 *  It is made by Stepan Rutz and licensed under Eclipse Public License.
 *  The original source code can be found at http://mappanel.sourceforge.net/
 * 
 * Changes done to the source code:
 * 		- Removed searchpanel, did not work properly and not needed.					(removed)
 * 		- Implemented a way to display markers.											(OverlayMarkerPanel, line 1164)
 * 		- Implemented a way to display a sheeps path									(OverlayMarkerPanel, line 1164)
 * 		- Implemented measure/scale line which can be turned on and off in the menubar	(OverLayPanel, line 1337)
 * 		- Changed the MapPanel from retreiving its tiles from openstreetmap,			
 * 		  to retrieving them from Statens Kartverk.										(TileServer[] getTileString method, line 155 and 193)
 * 		- Implemented a way to display the id of a marker/sheep in a tooltip			
 * 		  when hoovering over it using the mouse.										(mouseMoved method, line 1099)
 * 		
 * We used MapPanel in our project mainly because it is very easy to integrate it in into a swing gui, 
 * and because MapPanel extend JPanel.
 * It is also written natively in Java. 	
 * The big downside is the lack of markers, which had to be implemented.	
 *
 */


public class MapPanel extends JPanel {

    private static final Logger log = Logger.getLogger(MapPanel.class.getName());

    public static final class TileServer {
        private final String url;
        private final int maxZoom;
        private boolean broken;

        private TileServer(String url, int maxZoom) {
            this.url = url;
            this.maxZoom = maxZoom;
        }

        public String toString() {
            return url;
        }

        public int getMaxZoom() {
            return maxZoom;
        }
        public String getURL() {
            return url;
        }

        public boolean isBroken() {
            return broken;
        }

        public void setBroken(boolean broken) {
            this.broken = broken;
        }
    }

    /* constants ... */
    private static final TileServer[] TILESERVERS = {
    	new TileServer("http://tile.opencyclemap.org/cycle/", 18),
    	new TileServer("http://otile1.mqcdn.com/tiles/1.0.0/osm/", 18),
    	new TileServer("http://tile.openstreetmap.org/", 18),
        new TileServer("http://opencache.statkart.no/gatekeeper/gk/gk.open_gmaps?layers=topo2", 17),
        new TileServer("http://opencache.statkart.no/gatekeeper/gk/gk.open_gmaps?layers=toporaster2", 17),
        new TileServer("http://opencache.statkart.no/gatekeeper/gk/gk.open_gmaps?layers=europa", 17),
        new TileServer("http://opencache.statkart.no/gatekeeper/gk/gk.open_gmaps?layers=kartdata2", 12),
        
        
    };

   
    private static final int PREFERRED_WIDTH = 320;
    private static final int PREFERRED_HEIGHT = 200;


    private static final int ANIMATION_FPS = 15, ANIMATION_DURARTION_MS = 500;
    


    /* basically not be changed */
    private static final int TILE_SIZE = 256;
    private static final int CACHE_SIZE = 256;
    private static final int MAGNIFIER_SIZE = 100;

    //-------------------------------------------------------------------------
    // tile url construction.
    // change here to support some other tile
    
    /**
     * Olav<p>
     * Create a urlString which is necessary to download the right tile from the right tileserver.
     * Statkart's cache service splits the world into tiles. At zoomlevel 0, there is 1 tile. At zoomlevel 1, there's 
     * 4 tiles and so on. At zoomlevel 17 (Maxzoom of topo2) there are 17 179 869 184 tiles to choose from. 
     * basically every time you zoom, each tile is split up into 4 tiles.
     * @param tileServer - The different type of tiles available from statkart. Toporaster, europa etc
     * @param xtile - the row of statkart's tilegrid 
     * @param ytile - the column of statkart's tilegrid
     * @param zoom - what zoomlevel statkart should use
     * @return An url string used to request a tile from statkart's map service.
     */
    public static String getTileString(TileServer tileServer, int xtile, int ytile, int zoom) {
    	String number;
    	String url;
    	if(tileServer.getURL().startsWith("http://tile")) {
    		number = ("" + zoom + "/" + xtile + "/" + ytile);
    		url = tileServer.getURL() + number + ".png";
    	}
    	else if( tileServer.getURL().equals("http://otile1.mqcdn.com/tiles/1.0.0/osm/")){
    		number = ("" + zoom + "/" + xtile + "/" + ytile);
    		url = tileServer.getURL() + number + ".png";
    	}
    	else{
    		number = "&zoom=" + zoom + "&x="+xtile+"&y="+ytile; 
    		url = tileServer.getURL() + number;
            
    	}
    	
        return url;
    }

    //-------------------------------------------------------------------------
    // map impl.

    private Dimension mapSize = new Dimension(0, 0);
    private Point mapPosition = new Point(0, 0);
    private int zoom;

    private TileServer tileServer = TILESERVERS[0];

    private DragListener mouseListener = new DragListener();
    private TileCache cache = new TileCache();
    private Stats stats = new Stats();
    private OverlayPanel overlayPanel = new OverlayPanel();
    private ControlPanel controlPanel = new ControlPanel();
    private OverlayMarkerPanel overlayMarkerPanel = new OverlayMarkerPanel(); //OLAV: til marker

    private boolean useAnimations = true;
    private Animation animation;

    protected double smoothScale = 1.0D;
    private int smoothOffset = 0;
    private Point smoothPosition, smoothPivot;
    private Rectangle magnifyRegion;
    private Marker[] markerList = new Marker[0]; //OLAV: listen med info
    private HashMap<String, Route> routes = new HashMap<String, Route>();

   


	public MapPanel() {
        this(new Point(8282, 5179), 6);
    }

    public MapPanel(Point mapPosition, int zoom) {
        
    	setUseAnimations(false);
        setLayout(new MapLayout());
        setOpaque(true);
        setBackground(new Color(0xc0, 0xc0, 0xc0));
        add(overlayPanel);
        add(controlPanel);
        add(overlayMarkerPanel);//OLAV: 
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        addMouseWheelListener(mouseListener);
        setZoom(zoom);
        setMapPosition(mapPosition);
        checkTileServers();
        checkActiveTileServer();
    }

    private void checkTileServers() {
        for (TileServer tileServer : TILESERVERS) {
            String urlstring = getTileString(tileServer, 1, 1, 1);
            try {
                URL url = new URL(urlstring);
                Object content = url.getContent();
            } catch (Exception e) {
                log.log(Level.SEVERE, "failed to get content from url " + urlstring);
                tileServer.setBroken(true);
            }
        }
    }

    private void checkActiveTileServer() {
        if (getTileServer() != null && getTileServer().isBroken()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(MapPanel.this),
                            "The tileserver \"" + getTileServer().getURL() + "\" could not be reached.\r\nMaybe configuring a http-proxy is required.",
                            "TileServer not reachable.", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    public void nextTileServer() {
        int index = Arrays.asList(TILESERVERS).indexOf(getTileServer());
        if (index == -1)
            return;
        setTileServer(TILESERVERS[(index + 1) % TILESERVERS.length]);
        repaint();
    }

    public TileServer getTileServer() {
        return tileServer;
    }
    
    /**
     * Olav<p>
     * setMarkers update the markerlist. And repaints all the markers.
     * For example if there is an change of postion of a sheep. You call this method to update all the markers, 
     * and repaints the overlay to show the change in the map-viewer <p>
     * If isMarker is true the map will display markerpoints, if false overlayPanel assumes these new markers
     * are points logging the movement of a single object. index 0 is in this case the most recent coordinates.
     * @param marker position of markers / position of path vertexes
     * @param isMarker - Decides how to interpret the points. True = markers False = vertexes in a path 
     */
    public void setMarkers(Marker[] marker, boolean isMarker) {
    	this.markerList = marker;
    	
    	
    	getOverlayMarkerPanel().repaint();
    }
    
    /**
     * Olav<p>
     * Set InitialMarkers should be used when setting the markers initially. Because it will center the mapviewer
     * to the objects. It calls setCenterPosition and continue to the setMarkers method afterwards.
     * @param marker - position of objects / position of path vertexes
     * @param isMarker - Decides how to interpret the points. True = markers False = vertexes in a path 
     */
    public void setInitialMarkers(Marker[] marker, boolean isMarker) {
    	if(marker.length > 10) {
    		int i;
    		int valueX = 0;
    		int valueY = 0;
    		for(i=0; i < 10; i++){
    			valueX += lon2position(marker[i].getLongitude(), getZoom());
    			valueY += lat2position(marker[i].getLatitude(), getZoom());
    		}
    		valueX = valueX/i;
    		valueY = valueY/i;
    		setCenterPosition(new Point(valueX, valueY));
    	}
    	else if(marker.length > 0) {
    		setCenterPosition(new Point(lon2position(marker[0].getLongitude(), getZoom()), lat2position(marker[0].getLatitude(), getZoom())));
        	
    	}
    	setMarkers(marker, isMarker);
    }
    
    /**
     * Olav<p>
     * A getter for the overlayMarkerpanel to access the markerlist. 
     * @return a list of points containing longitude and latitude.
     */
    public Marker[] getMarkers() {
    	return this.markerList;
    }
    
    
    
    public void flushMarkers() {
    	this.markerList = new Marker[0];
    }
    

    public void put(String key, Route value) {
    	this.routes.put(key, value);
    }
    
    public void remove(String key) {
    	if(this.routes.containsKey(key)) {
    		this.routes.remove(key);
    	}
    }
    
    public Route get(String key) {
    	return this.routes.get(key);
    }
    
    public Set<Entry<String, Route>> getRoutes() {
    	return this.routes.entrySet();
    }
    
	
    public void setTileServer(TileServer tileServer) {
        if(this.tileServer == tileServer)
            return;
        this.tileServer = tileServer;
        while (getZoom() > tileServer.getMaxZoom())
            zoomOut(new Point(getWidth() / 2, getHeight() / 2));
        checkActiveTileServer();
    }

    public boolean isUseAnimations() {
        return useAnimations;
    }

    public void setUseAnimations(boolean useAnimations) {
        this.useAnimations = useAnimations;
    }

    public OverlayPanel getOverlayPanel() {
        return overlayPanel;
    }
    /**
     * Olav<p>
     * Return the overlayMarkerPanel class. Draws markers upon the map
     * @return the Marker overlay class
     */
    public OverlayMarkerPanel getOverlayMarkerPanel() {
        return overlayMarkerPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    
    
    public TileCache getCache() {
        return cache;
    }
    
    public Stats getStats() {
        return stats;
    }

    public Point getMapPosition() {
        return new Point(mapPosition.x, mapPosition.y);
    }
    public double getCenterLongitudePosition() {
    	double result = position2lon(getCenterPosition().x, getZoom())*1000000;
    	result = Math.round(result);
    	result = result/1000000;
    	return result;
    }
    
    public double getCenterLatitudePostion() {
    	double result = position2lat(getCenterPosition().y, getZoom())*1000000;
    	result = Math.round(result);
    	result = result/1000000;
    	return result;
    }
    
    public void setMapPosition(Point mapPosition) {
        setMapPosition(mapPosition.x, mapPosition.y);
    }

    public void setMapPosition(int x, int y) {
        if (mapPosition.x == x && mapPosition.y == y)
            return;
        Point oldMapPosition = getMapPosition();
        mapPosition.x = x;
        mapPosition.y = y;
        firePropertyChange("mapPosition", oldMapPosition, getMapPosition());
    }

    public void translateMapPosition(int tx, int ty) {
        setMapPosition(mapPosition.x + tx, mapPosition.y + ty);
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        if (zoom == this.zoom)
            return;
        int oldZoom = this.zoom;
        this.zoom = Math.min(getTileServer().getMaxZoom(), zoom);
        mapSize.width = getXMax();
        mapSize.height = getYMax();
        firePropertyChange("zoom", oldZoom, zoom);
    }


    public void zoomInAnimated(Point pivot) {
        if (!useAnimations) {
            zoomIn(pivot);
            return;
        }
        if (animation != null)
            return;
        mouseListener.downCoords = null;
        animation = new Animation(AnimationType.ZOOM_IN, ANIMATION_FPS, ANIMATION_DURARTION_MS) {
            protected void onComplete() {
                smoothScale = 1.0d;
                smoothPosition = smoothPivot = null;
                smoothOffset = 0;
                animation = null;
            
                repaint();
               
            }
            protected void onFrame() {
                smoothScale = 1.0 + getFactor();
                repaint();
            }

        };
        smoothPosition = new Point(mapPosition.x, mapPosition.y);
        smoothPivot = new Point(pivot.x, pivot.y);
        smoothOffset = -1;
        zoomIn(pivot);
        animation.run();
    }

    public void zoomOutAnimated(Point pivot) {
        if (!useAnimations) {
            zoomOut(pivot);
            return;
        }
        if (animation != null)
            return;
        mouseListener.downCoords = null;
        animation = new Animation(AnimationType.ZOOM_OUT, ANIMATION_FPS, ANIMATION_DURARTION_MS) {
            protected void onComplete() {
                smoothScale = 1.0d;
                smoothPosition = smoothPivot = null;
                smoothOffset = 0;
                animation = null;
                repaint();
            }
            protected void onFrame() {
                smoothScale = 1 - .5 * getFactor();
                
                repaint();
            }

        };
        smoothPosition = new Point(mapPosition.x, mapPosition.y);
        smoothPivot = new Point(pivot.x, pivot.y);
        smoothOffset = 1;
        zoomOut(pivot);
        animation.run();
    }

    public void zoomIn(Point pivot) {
        if (getZoom() >= getTileServer().getMaxZoom())
            return;
        Point mapPosition = getMapPosition();
        int dx = pivot.x;
        int dy = pivot.y;
        setZoom(getZoom() + 1);
        setMapPosition(mapPosition.x * 2 + dx, mapPosition.y * 2 + dy);
        repaint();
    }

    public void zoomOut(Point pivot) {
        if (getZoom() <= 1)
            return;
        Point mapPosition = getMapPosition();
        int dx = pivot.x;
        int dy = pivot.y;
        setZoom(getZoom() - 1);
        setMapPosition((mapPosition.x - dx) / 2, (mapPosition.y - dy) / 2);
        repaint();
    }

    public int getXTileCount() {
        return (1 << zoom);
    }

    public int getYTileCount() {
        return (1 << zoom);
    }

    public int getXMax() {
        return TILE_SIZE * getXTileCount();
    }

    public int getYMax() {
        return TILE_SIZE * getYTileCount();
    }

    public Point getCursorPosition() {
        return new Point(mapPosition.x + mouseListener.mouseCoords.x, mapPosition.y + mouseListener.mouseCoords.y);
    }

    public Point getTile(Point position) {
        return new Point((int) Math.floor(((double) position.x) / TILE_SIZE),(int) Math.floor(((double) position.y) / TILE_SIZE));
    }

    public Point getCenterPosition() {
        return new Point(mapPosition.x + getWidth() / 2, mapPosition.y + getHeight() / 2);
    }

    public void setCenterPosition(Point p) {
        setMapPosition(p.x - getWidth() / 2, p.y - getHeight() / 2);
    }

    public Point.Double getLongitudeLatitude(Point position) {
        return new Point.Double(
                position2lon(position.x, getZoom()),
                position2lat(position.y, getZoom()));
    }

    public Point computePosition(Point.Double coords) {
        int x = lon2position(coords.x, getZoom());
        int y = lat2position(coords.y, getZoom());
        return new Point(x, y);
    }

    protected void paintComponent(Graphics gOrig) {
        super.paintComponent(gOrig);
        Graphics2D g = (Graphics2D) gOrig.create();
        try {
            paintInternal(g);
        } finally {
            g.dispose();
        }
    }

    private static final class Painter {
        private final int zoom;
        private float transparency = 1F;
        private double scale = 1d;
        private final MapPanel mapPanel;

        private Painter(MapPanel mapPanel, int zoom) {
            this.mapPanel = mapPanel;
            this.zoom = zoom;
        }

        public float getTransparency() {
            return transparency;
        }

        public void setTransparency(float transparency) {
            this.transparency = transparency;
        }

        public double getScale() {
            return scale;
        }

        public void setScale(double scale) {
            this.scale = scale;
        }

        private void paint(Graphics2D gOrig, Point mapPosition, Point scalePosition) {
            Graphics2D g = (Graphics2D) gOrig.create();
            try {
                if (getTransparency() < 1f && getTransparency() >= 0f) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, transparency));
                }

                if (getScale() != 1d) {
                    //Point scalePosition = new Point(component.getWidth()/ 2, component.getHeight() / 2);
                    AffineTransform xform = new AffineTransform();
                    xform.translate(scalePosition.x, scalePosition.y);
                    xform.scale(scale, scale);
                    xform.translate(-scalePosition.x, -scalePosition.y);
                    g.transform(xform);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                }
                int width = mapPanel.getWidth();
                int height = mapPanel.getHeight();
                int x0 = (int) Math.floor(((double) mapPosition.x) / TILE_SIZE);
                int y0 = (int) Math.floor(((double) mapPosition.y) / TILE_SIZE);
                int x1 = (int) Math.ceil(((double) mapPosition.x + width) / TILE_SIZE);
                int y1 = (int) Math.ceil(((double) mapPosition.y + height) / TILE_SIZE);
                
                int dy = y0 * TILE_SIZE - mapPosition.y;
                for (int y = y0; y < y1; ++y) {
                    int dx = x0 * TILE_SIZE - mapPosition.x;
                    for (int x = x0; x < x1; ++x) {
                        paintTile(g, dx, dy, x, y);
                        dx += TILE_SIZE;
                        ++mapPanel.getStats().tileCount;
                    }
                    dy += TILE_SIZE;
                }
                
                if (getScale() == 1d && mapPanel.magnifyRegion != null) {
                    Rectangle magnifyRegion = new Rectangle(mapPanel.magnifyRegion);
                    magnifyRegion.translate(-mapPosition.x, -mapPosition.y);
                    g.setColor(Color.yellow);
                    // TODO: continue here later
                    //System.err.println("fill : " + mapPosition);
                    //System.err.println("fill : " + magnifyRegion);
                    //g.fillRect(magnifyRegion.x, magnifyRegion.y, magnifyRegion.width, magnifyRegion.height);
                }
            } finally {
                g.dispose();
            }
        }

        private void paintTile(Graphics2D g, int dx, int dy, int x, int y) {
            boolean DEBUG = false;
            boolean DRAW_IMAGES = true;
            boolean DRAW_OUT_OF_BOUNDS = false;

            boolean imageDrawn = false;
            int xTileCount = 1 << zoom;
            int yTileCount = 1 << zoom;
            boolean tileInBounds = x >= 0 && x < xTileCount && y >= 0 && y < yTileCount;
            boolean drawImage = DRAW_IMAGES && tileInBounds;
            if (drawImage) {
                TileCache cache = mapPanel.getCache();
                TileServer tileServer = mapPanel.getTileServer();
                Image image = cache.get(tileServer, x, y, zoom);
                if (image == null) {
                    final String url = getTileString(tileServer, x, y, zoom);
                    try {
                        //System.err.println("loading: " + url);
                        image = Toolkit.getDefaultToolkit().getImage(new URL(url));
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "failed to load url \"" + url + "\"", e);
                    }
                    if (image != null)
                        cache.put(tileServer, x, y, zoom, image);
                }
                if (image != null) {
                    g.drawImage(image, dx, dy, mapPanel);
                    imageDrawn = true;
                }
            }
            if (DEBUG && (!imageDrawn && (tileInBounds || DRAW_OUT_OF_BOUNDS))) {
                g.setColor(Color.blue);
                g.fillRect(dx + 4, dy + 4, TILE_SIZE - 8, TILE_SIZE - 8);
                g.setColor(Color.gray);
                String s = "T " + x + ", " + y + (!tileInBounds ? " #" : "");
                g.drawString(s, dx + 4+ 8, dy + 4 + 12);
            }
        }


    }

    private void paintInternal(Graphics2D g) {
        stats.reset();
        long t0 = System.currentTimeMillis();

        if (smoothPosition != null) {
            {
                Point position = getMapPosition();
                Painter painter = new Painter(this, getZoom());
                painter.paint(g, position, null);
            }
            Point position = new Point(smoothPosition.x, smoothPosition.y);
            Painter painter = new Painter(this, getZoom() + smoothOffset);
            painter.setScale(smoothScale);
            
            float t = (float) (animation == null ? 1f : 1 - animation.getFactor());
            painter.setTransparency(t);
            painter.paint(g, position, smoothPivot);
            if (animation != null && animation.getType() == AnimationType.ZOOM_IN) {
                int cx = smoothPivot.x, cy = smoothPivot.y;
                drawScaledRect(g, cx, cy, animation.getFactor(), 1 + animation.getFactor());
            } else if (animation != null && animation.getType() == AnimationType.ZOOM_OUT) {
                int cx = smoothPivot.x, cy = smoothPivot.y;
                drawScaledRect(g, cx, cy, animation.getFactor(), 2 - animation.getFactor());
            }
            //System.err.println("smoothScale" + smoothScale);
        }

        if (smoothPosition == null) {
            Point position = getMapPosition();
            Painter painter = new Painter(this, getZoom());
            painter.paint(g, position, null);
        }

        long t1 = System.currentTimeMillis();
        stats.dt = t1 - t0;
    }


    private void drawScaledRect(Graphics2D g, int cx, int cy, double f, double scale) {
        AffineTransform oldTransform = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        g.translate(-cx, -cy);
        int c = 0x80 + (int) Math.floor(f * 0x60);
        if (c < 0) c = 0;
        else if (c > 255) c = 255;
        Color color = new Color(c, c, c);
        g.setColor(color);
        g.drawRect(cx - 40, cy - 30, 80, 60);
        g.setTransform(oldTransform);
    }

   //-------------------------------------------------------------------------
    // utils
    public static String format(double d) {
        return String.format("%.5f", d);
    }

    public static double getN(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return n;
    }

    public static double position2lon(int x, int z) {
        double xmax = TILE_SIZE * (1 << z);
        return x / xmax * 360.0 - 180;
    }

    public static double position2lat(int y, int z) {
        double ymax = TILE_SIZE * (1 << z);
        return Math.toDegrees(Math.atan(Math.sinh(Math.PI - (2.0 * Math.PI * y) / ymax)));
    }

    public static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    public static double tile2lat(int y, int z) {
        return Math.toDegrees(Math.atan(Math.sinh(Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z))));
    }

    public static int lon2position(double lon, int z) {
        double xmax = TILE_SIZE * (1 << z);
        return (int) Math.floor((lon + 180) / 360 * xmax);
    }

    public static int lat2position(double lat, int z) {
        double ymax = TILE_SIZE * (1 << z);
        return (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * ymax);
    }

    public static String getTileNumber(TileServer tileServer, double lat, double lon, int zoom) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        return getTileString(tileServer, xtile, ytile, zoom);
    }

    private static void drawBackground(Graphics2D g, int width, int height) {
        Color color1 = Color.black;
        Color color2 = new Color(0x30, 0x30, 0x30);
        color1 = new Color(0xc0, 0xc0, 0xc0);
        color2 = new Color(0xe0, 0xe0, 0xe0);
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.75f));
        g.setPaint(new GradientPaint(0, 0, color1, 0, height, color2));
        g.fillRoundRect(0, 0, width, height, 4, 4);
        g.setComposite(oldComposite);
    }

    private static void drawRollover(Graphics2D g, int width, int height) {
        Color color1 = Color.white;
        Color color2 = new Color(0xc0, 0xc0, 0xc0);
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.25f));
        g.setPaint(new GradientPaint(0, 0, color1, width, height, color2));
        g.fillRoundRect(0, 0, width, height, 4, 4);
        g.setComposite(oldComposite);
    }

    private static BufferedImage flip(BufferedImage image, boolean horizontal, boolean vertical) {
        int width = image.getWidth(), height = image.getHeight();
        if (horizontal) {
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width / 2; ++x) {
                    int tmp = image.getRGB(x, y);
                    image.setRGB(x, y, image.getRGB(width - 1 - x, y));
                    image.setRGB(width - 1 - x, y, tmp);
                }
            }
        }
        if (vertical) {
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height / 2; ++y) {
                    int tmp = image.getRGB(x, y);
                    image.setRGB(x, y, image.getRGB(x, height - 1 - y));
                    image.setRGB(x, height - 1 - y, tmp);
                }
            }
        }
        return image;
    }

    private static BufferedImage makeIcon(Color background) {
        final int WIDTH = 16, HEIGHT = 16;
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < HEIGHT; ++y)
            for (int x = 0; x < WIDTH; ++x)
                image.setRGB(x, y, 0);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(background);
        g2d.fillOval(0, 0, WIDTH - 1, HEIGHT - 1);

        double hx = 4;
        double hy = 4;
        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
              double dx = x - hx;
              double dy = y - hy;
              double dist = Math.sqrt(dx * dx + dy * dy);
              if (dist > WIDTH) {
                 dist = WIDTH;
              }
              int color = image.getRGB(x, y);
              int a = (color >>> 24) & 0xff;
              int r = (color >>> 16) & 0xff;
              int g = (color >>> 8) & 0xff;
              int b = (color >>> 0) & 0xff;
              double coef = 0.7 - 0.7 * dist / WIDTH;
              image.setRGB(x, y, (a << 24) | ((int) (r + coef * (255 - r)) << 16) | ((int) (g + coef * (255 - g)) << 8) | (int) (b + coef * (255 - b)));
           }
        }
        g2d.setColor(Color.gray);
        g2d.drawOval(0, 0, WIDTH - 1, HEIGHT - 1);
        return image;
    }

    private static BufferedImage makeXArrow(Color background) {
        BufferedImage image = makeIcon(background);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillPolygon(new int[] { 10, 4, 10} , new int[] { 5, 8, 11 }, 3);
        image.flush();
        return image;

    }
    private static BufferedImage makeYArrow(Color background) {
        BufferedImage image = makeIcon(background);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillPolygon(new int[] { 5, 8, 11} , new int[] { 10, 4, 10 }, 3);
        image.flush();
        return image;
    }
    private static BufferedImage makePlus(Color background) {
        BufferedImage image = makeIcon(background);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillRect(4, 7, 8, 2);
        g.fillRect(7, 4, 2, 8);
        image.flush();
        return image;
    }
    private static BufferedImage makeMinus(Color background) {
        BufferedImage image = makeIcon(background);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillRect(4, 7, 8, 2);
        image.flush();
        return image;
    }


    //-------------------------------------------------------------------------
    // helpers
    private enum AnimationType {
        ZOOM_IN, ZOOM_OUT
    }
    
    private static abstract class Animation implements ActionListener {

        private final AnimationType type;
        private final Timer timer;
        private long t0 = -1L;
        private long dt;
        private final long duration;

        public Animation(AnimationType type, int fps, long duration) {
            this.type = type;
            this.duration = duration;
            int delay = 1000 / fps;
            timer = new Timer(delay, this);
            timer.setCoalesce(true);
            timer.setInitialDelay(0);
        }
        
        public AnimationType getType() {
            return type;
        }

        protected abstract void onComplete();

        protected abstract void onFrame();

        public double getFactor() {
            return (double) getDt() / getDuration();
        }

        public void actionPerformed(ActionEvent e) {
            if (getDt() >= duration) {
                kill();
                onComplete();
                return;
            }
            onFrame();
        }

        public long getDuration() {
            return duration;
        }

        public long getDt() {
            if (!timer.isRunning())
                return dt;
            long now = System.currentTimeMillis();
            if (t0 < 0)
                t0 = now;
            return now - t0 + dt;
        }

        public void run() {
            if (timer.isRunning())
                return;
            timer.start();
        }

        public void kill() {
            if (!timer.isRunning())
                return;
            dt = getDt();
            timer.stop();
        }
    }

    private static class Tile {
        private final String key;
        public final int x, y, z;
        public Tile(String tileServer, int x, int y, int z) {
            this.key = tileServer;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + x;
            result = prime * result + y;
            result = prime * result + z;
            return result;
        }
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Tile other = (Tile) obj;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (x != other.x)
                return false;
            if (y != other.y)
                return false;
            if (z != other.z)
                return false;
            return true;
        }

    }

    private static class TileCache {
        private LinkedHashMap<Tile,Image> map = new LinkedHashMap<Tile,Image>(CACHE_SIZE, 0.75f, true) {
            protected boolean removeEldestEntry(java.util.Map.Entry<Tile,Image> eldest) {
                boolean remove = size() > CACHE_SIZE;
                return remove;
            }
        };
        public void put(TileServer tileServer, int x, int y, int z, Image image) {
            map.put(new Tile(tileServer.getURL(), x, y, z), image);
        }
        public Image get(TileServer tileServer, int x, int y, int z) {
            //return map.get(new Tile(x, y, z));
            Image image = map.get(new Tile(tileServer.getURL(), x, y, z));
            return image;
        }
        public int getSize() {
            return map.size();
        }
    }

    private static class Stats {
        private int tileCount;
        private long dt;
        private Stats() {
            reset();
        }
        private void reset() {
            tileCount = 0;
            dt = 0;
        }
    }
    
   //OLAV: Fjernet CustomSplitScreen
    private class DragListener extends MouseAdapter implements MouseMotionListener, MouseWheelListener {
        private Point mouseCoords;
        private Point downCoords;
        private Point downPosition;

        public DragListener() {
            mouseCoords = new Point();
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                zoomInAnimated(new Point(mouseCoords.x, mouseCoords.y));
            } else if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() >= 2) {
                zoomOutAnimated(new Point(mouseCoords.x, mouseCoords.y));
            } else if (e.getButton() == MouseEvent.BUTTON2) {
                setCenterPosition(getCursorPosition());
                repaint();
            }
        }

        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                downCoords = e.getPoint();
                downPosition = getMapPosition();
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                int cx = getCursorPosition().x;
                int cy = getCursorPosition().y;
                magnifyRegion = new Rectangle(cx - MAGNIFIER_SIZE / 2, cy - MAGNIFIER_SIZE / 2, MAGNIFIER_SIZE, MAGNIFIER_SIZE);
                repaint();
            }
        }

        public void mouseReleased(MouseEvent e) {
            //setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            handleDrag(e);
            downCoords = null;
            downPosition = null;
            magnifyRegion = null;
        }

        public void mouseMoved(MouseEvent e) {
            handlePosition(e);
            
            for(int i =0; i<getMarkers().length; i++) {
            	int x = lon2position(getMarkers()[i].getLongitude(), getZoom());
            	int y = lat2position(getMarkers()[i].getLatitude(), getZoom());
            	int mx =mapPosition.x+e.getX();
            	int my= mapPosition.y +e.getY();
            	
            	if(x < mx+7 && x > mx-7 && y < my+7 && y > my-14) {
            		if(!(getMarkers()[i].getId() == 0)) {
            			setToolTipText("" +getMarkers()[i].getId());
            		}	
            	}
            	ToolTipManager.sharedInstance().mouseMoved(e);
            }
        }

        public void mouseDragged(MouseEvent e) {
            //setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            handlePosition(e);
            handleDrag(e);
        }
 
        public void mouseExited(MouseEvent e) {
            //setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        public void mouseEntered(MouseEvent me) {
            super.mouseEntered(me);
        }

        private void handlePosition(MouseEvent e) {
            mouseCoords = e.getPoint();
            if (overlayPanel.isVisible())
                MapPanel.this.repaint();
        }

        private void handleDrag(MouseEvent e) {
            if (downCoords != null) {
                int tx = downCoords.x - e.getX();
                int ty = downCoords.y - e.getY();
                setMapPosition(downPosition.x + tx, downPosition.y + ty);
                repaint();
            } else if (magnifyRegion != null) {
                int cx = getCursorPosition().x;
                int cy = getCursorPosition().y;
                magnifyRegion = new Rectangle(cx - MAGNIFIER_SIZE / 2, cy - MAGNIFIER_SIZE / 2, MAGNIFIER_SIZE, MAGNIFIER_SIZE);
                repaint();
            }
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            int rotation = e.getWheelRotation();
            if (rotation < 0)
                zoomInAnimated(new Point(mouseCoords.x, mouseCoords.y));
            else
                zoomOutAnimated(new Point(mouseCoords.x, mouseCoords.y));
        }
    }
    
    /**
     * Olav<p>
     * 
     * Draw markers in an overlay over the map panel.
     */
    public final class OverlayMarkerPanel extends JPanel {
    	
        private OverlayMarkerPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(MapPanel.PREFERRED_HEIGHT, MapPanel.PREFERRED_WIDTH));
        }

        protected void paintComponent(Graphics gOrig) {
            super.paintComponent(gOrig);
            Graphics2D g = (Graphics2D) gOrig.create();
            try {
                paintOverlay(g);
            } finally {
                g.dispose();
            }
        }
        
       
        private void paintOverlay(Graphics2D g) {
        		for(Entry<String, Route> entry : getRoutes()) {
        			drawLine(g, lon2position(entry.getValue().getStartDestination().getLongitude(), getZoom()),
        					lat2position(entry.getValue().getStartDestination().getLatitude(), getZoom()),
        					lon2position(entry.getValue().getEndDestination().getLongitude(), getZoom()),
        					lat2position(entry.getValue().getEndDestination().getLatitude(), getZoom()));
        		
        		
        		}
        		//setStroke (new BasicStroke (2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f));
            	//g.setPaint(Color.RED);
            	//g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        		//g.drawArc(0, 0, 500, 400, 90, 50);
        		//g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
        	
        }
        
        
        private void drawLine(Graphics2D g, int x1, int y1, int x2, int y2) {
        	g.setStroke (new BasicStroke (2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f));
        	g.setPaint(Color.RED);
        	g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        	g.drawArc(x1-mapPosition.x, y1-mapPosition.y, 200, 400, 20, 30);
        	//g.drawLine(x1-mapPosition.x, y1-mapPosition.y, x2-mapPosition.x, y2-mapPosition.y);
        }
        
		/**
		 * Paint markers draws polygon-markers on the map. If either the mList length exceed the MAX_MARKER_COUNT or
		 * the zoom level of the map is under 10 it draws an circle to indicate where the herd is.
		 * 
		 * If mlist length is below the MAX_MARKER_COUNT or the zoom is over 10 it draws individual markers of each
		 * mList entry. The method does this by iterating over the array and generating a polygon drawn on the 
		 * map. setPaint, fill, setStroke, rendringHint codelines enhances the look of a marker.
		 * @param g
		 * @param mList
		 */
        private void paintMarkers(Graphics2D g, Marker[] mList) {
        	if(getZoom() > 10) {
        		for(int i = 0; i< mList.length; i++) {
            		Polygon triangle = createMarkerPolygon(mList[i].getLongitude(), mList[i].getLatitude());
            		 g.setPaint(Color.RED);
                     g.fill(triangle);
                     g.setPaint(Color.BLACK);
                     g.setStroke (new BasicStroke (3.0f, BasicStroke.CAP_BUTT,
                             BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f));
                     g.setRenderingHint (RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
                     g.drawPolygon(triangle); 	
            	}
        	}
        	else {
        		int radius = 30*getZoom();
        		int x = lon2position(mList[0].getLongitude(), getZoom());
                int y = lat2position(mList[0].getLatitude(), getZoom());
                g.setRenderingHint (RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke (new BasicStroke (3.0f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f));
                g.setPaint(Color.RED);
        		g.drawOval( x- mapPosition.x -radius/2, y - mapPosition.y - radius/2, radius, radius);
        		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)0.2f));
        		g.fillOval(x-mapPosition.x - radius/2,y- mapPosition.y-radius/2 , radius, radius);
        	}
        }
        
        
       /**
        * createMarkerPolygon change the longitude and latitude of a point to 4 corners of  of a polygon, creates
        * the polygon and returns it.
        * @param longitude - the longitude of the marker
        * @param latitude  - the latitude of the marker
        * @return A polygon triangle fitted to the map.
        */
       private Polygon createMarkerPolygon(double longitude, double latitude) {
    	   int x = lon2position(longitude, getZoom());
           int y = lat2position(latitude, getZoom());
           Point p1 = new Point(x-8 -mapPosition.x, y-20 -mapPosition.y);
           Point p2 = new Point(x-mapPosition.x,y -mapPosition.y);
           Point p3 = new Point(x +8 -mapPosition.x, y-20 -mapPosition.y);
           Point p4 = new Point(x  -mapPosition.x, y-25 -mapPosition.y);
           int[] xVertex = { p1.x, p2.x, p3.x, p4.x };
           int[] yVertex = { p1.y, p2.y, p3.y, p4.y };
           	return new Polygon(xVertex, yVertex, xVertex.length);
       }
    }
 // ENDOVERLAYMARKERPANEL ----------------------------------------------------------------
    
    public final class OverlayPanel extends JPanel {

public static final double TILE_IN_METERS = 5545984;
    	
        private OverlayPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(370, 12 * 16 + 12));
        }

        @Override
		protected void paintComponent(Graphics gOrig) {
            super.paintComponent(gOrig);
            Graphics2D g = (Graphics2D) gOrig.create();
            try {
                paintOverlay(g);
            } finally {
                g.dispose();
            }
        }

        private void paintOverlay(Graphics2D g) {
        	int scaleLength = ((TILE_SIZE)/2)+16;
        	Font font = new Font(g.getFont().getFontName(), Font. PLAIN, 12);
        	g.setFont(font);
            g.setColor(Color.black);
            g.drawString( (int)(TILE_IN_METERS/ Math.pow(2, getZoom()-1)) + " meter", 20, 180);
            g.setStroke (new BasicStroke (2.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f));
            g.drawLine(10, this.getHeight()-20, scaleLength,  this.getHeight()-20);
            g.drawLine(10, this.getHeight()-30, 10, this.getHeight()-10);
            g.drawLine(scaleLength, this.getHeight()-30, scaleLength, this.getHeight()-10);
        }

    }
    
    public final class ControlPanel extends JPanel {

        protected static final int MOVE_STEP = 32;

        private JButton makeButton(Action action) {
            JButton b = new JButton(action);
            b.setFocusable(false);
            b.setText(null);
            b.setContentAreaFilled(false);
            b.setBorder(BorderFactory.createEmptyBorder());
            BufferedImage image = (BufferedImage) ((ImageIcon)b.getIcon()).getImage();
            BufferedImage hl = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) hl.getGraphics();
            g.drawImage(image, 0, 0, null);
            drawRollover(g, hl.getWidth(), hl.getHeight());
            hl.flush();
            b.setRolloverIcon(new ImageIcon(hl));
            return b;
        }

        public ControlPanel() {
            setOpaque(false);
            setForeground(Color.white);
            setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            setLayout(new BorderLayout());

            Action zoomInAction = new AbstractAction() {
                {
                    String text = "Zoom In";
                    putValue(Action.NAME, text);
                    putValue(Action.SHORT_DESCRIPTION, text);
                    putValue(Action.SMALL_ICON, new ImageIcon(flip(makePlus(new Color(0xc0, 0xc0, 0xc0)), false, false)));
                }

                public void actionPerformed(ActionEvent e) {
                    zoomInAnimated(new Point(MapPanel.this.getWidth() / 2, MapPanel.this.getHeight() / 2));
                }
            };
            Action zoomOutAction = new AbstractAction() {
                {
                    String text = "Zoom Out";
                    putValue(Action.NAME, text);
                    putValue(Action.SHORT_DESCRIPTION, text);
                    putValue(Action.SMALL_ICON, new ImageIcon(flip(makeMinus(new Color(0xc0, 0xc0, 0xc0)), false, false)));
                }

                public void actionPerformed(ActionEvent e) {
                    zoomOutAnimated(new Point(MapPanel.this.getWidth() / 2, MapPanel.this.getHeight() / 2));
                }
            };

            Action upAction = new AbstractAction() {
                {
                    String text = "Up";
                    putValue(Action.NAME, text);
                    putValue(Action.SHORT_DESCRIPTION, text);
                    putValue(Action.SMALL_ICON, new ImageIcon(flip(makeYArrow(new Color(0xc0, 0xc0, 0xc0)), false, false)));
                }

                public void actionPerformed(ActionEvent e) {
                    translateMapPosition(0, -MOVE_STEP);
                    MapPanel.this.repaint();
                }
            };
            Action downAction = new AbstractAction() {
                {
                    String text = "Down";
                    putValue(Action.NAME, text);
                    putValue(Action.SHORT_DESCRIPTION, text);
                    putValue(Action.SMALL_ICON, new ImageIcon(flip(makeYArrow(new Color(0xc0, 0xc0, 0xc0)), false, true)));
                }

                public void actionPerformed(ActionEvent e) {
                    translateMapPosition(0, +MOVE_STEP);
                    MapPanel.this.repaint();
                }
            };
            Action leftAction = new AbstractAction() {
                {
                    String text = "Left";
                    putValue(Action.NAME, text);
                    putValue(Action.SHORT_DESCRIPTION, text);
                    putValue(Action.SMALL_ICON, new ImageIcon(flip(makeXArrow(new Color(0xc0, 0xc0, 0xc0)), false, false)));
                }

                public void actionPerformed(ActionEvent e) {
                    translateMapPosition(-MOVE_STEP, 0);
                    MapPanel.this.repaint();
                }
            };
            Action rightAction = new AbstractAction() {
                {
                    String text = "Right";
                    putValue(Action.NAME, text);
                    putValue(Action.SHORT_DESCRIPTION, text);
                    putValue(Action.SMALL_ICON, new ImageIcon(flip(makeXArrow(new Color(0xc0, 0xc0, 0xc0)), true, false)));
                }

                public void actionPerformed(ActionEvent e) {
                    translateMapPosition(+MOVE_STEP, 0);
                    MapPanel.this.repaint();
                }
            };
            JPanel moves = new JPanel(new BorderLayout());
            moves.setOpaque(false);
            JPanel zooms = new JPanel(new BorderLayout(0, 0));
            zooms.setOpaque(false);
            zooms.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
            moves.add(makeButton(upAction), BorderLayout.NORTH);
            moves.add(makeButton(leftAction), BorderLayout.WEST);
            moves.add(makeButton(downAction), BorderLayout.SOUTH);
            moves.add(makeButton(rightAction), BorderLayout.EAST);
            zooms.add(makeButton(zoomInAction), BorderLayout.NORTH);
            zooms.add(makeButton(zoomOutAction), BorderLayout.SOUTH);
            add(moves, BorderLayout.NORTH);
            add(zooms, BorderLayout.SOUTH);
        }

        public void paint(Graphics gOrig) {
            Graphics2D g = (Graphics2D) gOrig.create();
            try {
                int w = getWidth(), h = getHeight();
                drawBackground(g, w, h);
            } finally {
                g.dispose();
            }
            super.paint(gOrig);
        }
    }


    private final class MapLayout implements LayoutManager {

        public void addLayoutComponent(String name, Component comp) {
        }
        public void removeLayoutComponent(Component comp) {
        }
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(1, 1);
        }
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        }
        public void layoutContainer(Container parent) {
            int width = parent.getWidth();
            {//OLAV: OverlayMarkerPanel
                Dimension psize = overlayMarkerPanel.getPreferredSize();
                overlayMarkerPanel.setBounds(0, 0, parent.getWidth(), parent.getHeight());
                
            }
            {
            	Dimension psize = overlayPanel.getPreferredSize();
                overlayPanel.setBounds(20, parent.getHeight() - psize.height-20, psize.width, psize.height);
            }
            {
                Dimension psize = controlPanel.getPreferredSize();
                controlPanel.setBounds(20, 20, psize.width, psize.height);
            }
            
            
        }
    }

   
    public static final class Gui extends JPanel {
    	//OLAV: Må fjerne customsplitPane. DONE
        private final MapPanel mapPanel;
      

        public Gui() {
            this(new MapPanel());
        }

        public Gui(MapPanel mapPanel) {
            super(new BorderLayout());
            this.mapPanel = mapPanel;
            mapPanel.getOverlayPanel().setVisible(false);
            mapPanel.getOverlayMarkerPanel().setVisible(true);//OLAV: 
            mapPanel.setMinimumSize(new Dimension(1, 1));
            
            
            
            add(mapPanel);
        }

       
        public MapPanel getMapPanel() {
            return mapPanel;
        }
        
        public JMenuBar createMenuBar() {
            JFrame frame = null;
            if (SwingUtilities.getWindowAncestor(mapPanel) instanceof JFrame)
                frame = (JFrame) SwingUtilities.getWindowAncestor(mapPanel);
            final JFrame frame_ = frame;
            JMenuBar menuBar = new JMenuBar();
            {
                JMenu fileMenu = new JMenu("Fil");
                fileMenu.setMnemonic(KeyEvent.VK_F);
                fileMenu.add(new AbstractAction() {
                    {
                        putValue(Action.NAME, "Avslutt");
                        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
                        setEnabled(frame_ != null);
                    }
                    public void actionPerformed(ActionEvent e) {
                        if (frame_ != null)
                            frame_.dispose();
                    }
                });
                menuBar.add(fileMenu);
            }
            {
                JMenu viewMenu = new JMenu("Vis");
                viewMenu.setMnemonic(KeyEvent.VK_V);

                JCheckBoxMenuItem animations = new JCheckBoxMenuItem(new AbstractAction() {
                    {
                        putValue(Action.NAME, "Animasjon");
                        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
                    }
                    public void actionPerformed(ActionEvent e) {
                        mapPanel.setUseAnimations(!mapPanel.isUseAnimations());
                    }

                });
                animations.setSelected(false);
                viewMenu.add(animations);
               
                viewMenu.add(new JCheckBoxMenuItem(new AbstractAction() {
                    {
                        putValue(Action.NAME, "Målestokk");
                        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
                    }
                    public void actionPerformed(ActionEvent e) {
                        mapPanel.getOverlayPanel().setVisible(!mapPanel.getOverlayPanel().isVisible());
                    }

                }));
                JCheckBoxMenuItem controlPanelMenuItem = new JCheckBoxMenuItem(new AbstractAction() {
                    {
                        putValue(Action.NAME, "Kontrollpanel");
                        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
                    }
                    public void actionPerformed(ActionEvent e) {
                        mapPanel.getControlPanel().setVisible(!mapPanel.getControlPanel().isVisible());
                    }
                });
                controlPanelMenuItem.setSelected(true);
                viewMenu.add(controlPanelMenuItem);
                menuBar.add(viewMenu);
            }
            {
                JMenu tileServerMenu = new JMenu("Kartlag");
                tileServerMenu.setMnemonic(KeyEvent.VK_T);
                ButtonGroup bg = new ButtonGroup();
                for (final TileServer curr : TILESERVERS) {
                    JCheckBoxMenuItem item = new JCheckBoxMenuItem(curr.getURL());
                    bg.add(item);
                    
                    item.setSelected(curr.equals(mapPanel.getTileServer()));
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            mapPanel.setTileServer(curr);
                            mapPanel.repaint();
                        }
                    });
                    tileServerMenu.add(item);
                }
                menuBar.add(tileServerMenu);
            }
            
            
            return menuBar;
        }


    }
    
    public static MapPanel createMapPanel(Point mapPosition, int zoom) {
        MapPanel mapPanel = new MapPanel(mapPosition, zoom);
        mapPanel.getOverlayPanel().setVisible(false);
        ((JComponent)mapPanel.getControlPanel()).setVisible(false);
        return mapPanel;
    }

    public static Gui createGui(Point mapPosition, int zoom) {
        MapPanel mapPanel = createMapPanel(mapPosition, zoom);
        return new MapPanel.Gui(mapPanel);
    }
    
    public static void launchUI() {

        final JFrame frame = new JFrame();
        frame.setTitle("Map Panel");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(800, 600);
        frame.setLocation((sz.width - frame.getWidth()) / 2, (sz.height - frame.getHeight())/2);

        Gui gui = new Gui();
        frame.getContentPane().add(gui, BorderLayout.CENTER);

        JMenuBar menuBar = gui.createMenuBar();
        frame.setJMenuBar(menuBar);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // ignore
                }
                launchUI();
            }
        });
    }
}



