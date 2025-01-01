package pokemon.panel.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;

import pokemon.event.EventListener;
import pokemon.event.EventManager;
import pokemon.event.palette.PaletteOpenedEvent;
import pokemon.event.palette.PaletteSelectedEvent;
import pokemon.event.tile.TileOpenedEvent;
import pokemon.event.tile.TileSelectedEvent;
import pokemon.event.ui.TreeFileOpened;
import pokemon.files.FileFormat;
import pokemon.files.FormatEnum;
import pokemon.files.graphics.NCGR;
import pokemon.files.graphics.NCLR;
import pokemon.files.graphics.NSCR;
import pokemon.logic.Palette;
import pokemon.logic.ScreenData;
import pokemon.logic.Tile;
import pokemon.panel.graphics.PalettePanel;
import pokemon.panel.graphics.ScreenPanel;
import pokemon.panel.graphics.TilePanel;

public class EditionPanel extends JDesktopPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3697366543301729496L;
	private static final int MAX_DISPLAY_X = 33;
	private static final int MAX_DISPLAY_Y = 26;

	private Map<String, Palette> paletteMap;
	private Map<String, Tile[]> tilesMap;
	private Palette currentPalette;
	private Tile[] currentTiles;

	// Normal light grey panel but implements open events and opens internal frames
	public EditionPanel() {
		this.setBackground(Color.lightGray);

		this.paletteMap = new HashMap<String, Palette>();
		this.tilesMap = new HashMap<String, Tile[]>();
		this.currentPalette = Palette.DEFAULT_PALETTE;
		this.currentTiles = Tile.DEFAULT_TILES;

		EventManager.getInstance().registerListener(this);
	}

	private void openInternalFrame(String title, JComponent panel, Dimension dimension) {
		JScrollPane scroll = new JScrollPane(panel);
		scroll.setPreferredSize(dimension);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getHorizontalScrollBar().setUnitIncrement(16);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JInternalFrame internalFrame = new JInternalFrame(title, true, true, false, true);
		internalFrame.add(scroll);
		internalFrame.pack(); // Size defined by its panel

		this.add(internalFrame);
		internalFrame.setVisible(true);
	}

	private void openInternalPatternFrame(NCLR nclr, String paletteName) {
		// Do nothing if palette opened
		if (!paletteMap.containsKey(paletteName)) {
			Palette palette = nclr.createPalette();
			boolean isPaletteSelected = false;
			paletteMap.put(paletteName, palette);

			if (currentPalette == Palette.DEFAULT_PALETTE) {
				currentPalette = palette;
				isPaletteSelected = true;
			}

			EventManager.getInstance().throwEvent(new PaletteOpenedEvent(paletteName, palette, isPaletteSelected));
			PalettePanel panel = new PalettePanel(palette);
			openInternalFrame(paletteName, panel, panel.getPreferredSize());
		}
	}

	private void openInternalTileFrame(NCGR ncgr, String tilesName) {
		if (!tilesMap.containsKey(tilesName)) {
			Tile[] tiles = ncgr.createTiles();
			boolean areTilesSelected = false;
			tilesMap.put(tilesName, tiles);

			if (currentTiles == Tile.DEFAULT_TILES) {
				currentTiles = tiles;
				areTilesSelected = true;
			}

			int tileX = ncgr.getTileX();
			int tileY = ncgr.getTileY();

			EventManager.getInstance().throwEvent(
					new TileOpenedEvent(tilesName, ncgr.getColorBitDepth(), tileX, tileY, areTilesSelected));

			int width = Math.min(8 * MAX_DISPLAY_X * 3, 8 * tileX * 5);
			int height = Math.min(8 * MAX_DISPLAY_Y * 3, 8 * tileY * 5);
			TilePanel panel = new TilePanel(this, tilesName, tiles, tileX, tileY, currentPalette);
			openInternalFrame(tilesName, panel, new Dimension(width, height));
		}
	}

	private void openInternalScreenFrame(NSCR nscr, String screenName) {
		ScreenData[] screenData = nscr.createScreenData();
		int screenWidth = nscr.getScreenWidth();
		int screenHeight = nscr.getScreenHeight();

		int width = Math.min(8 * MAX_DISPLAY_X * 3, 8 * screenWidth * 5);
		int height = Math.min(8 * MAX_DISPLAY_Y * 3, 8 * screenHeight * 5);
		ScreenPanel panel = new ScreenPanel(this, screenData, currentPalette, currentTiles, screenWidth, screenHeight);
		openInternalFrame(screenName, panel, new Dimension(width, height));
	}

	public Palette getCurrentPalette() {
		return currentPalette;
	}

	public Tile[] getCurrentTiles() {
		return currentTiles;
	}

	@EventListener
	public void onFileOpened(TreeFileOpened event) throws IOException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String extension = event.getFileName().substring(event.getFileName().lastIndexOf('.') + 1);

		// Get format from extension, if unknown just give up...
		FormatEnum format = FormatEnum.getFromExtension(extension.toLowerCase());
		if (format == FormatEnum.UNKNOWN) {
			return;
		}

		// Get empty constructor and create object
		Class<? extends FileFormat> formatClass = format.getFormatClass();
		FileFormat fileFormat = formatClass.getConstructor().newInstance();
		DataInputStream inStream = new DataInputStream(new FileInputStream(event.getPath().toFile()));
		fileFormat.load(inStream);
		inStream.close();

		switch (format) {
		case PALETTE:
			openInternalPatternFrame((NCLR) fileFormat, event.getFileName());
			break;

		case TILE:
			openInternalTileFrame((NCGR) fileFormat, event.getFileName());
			break;

		case SCREEN:
			openInternalScreenFrame((NSCR) fileFormat, event.getFileName());
			break;

		default:
			// Should not go here since unknown have been treated earlier
			break;
		}
	}

	@EventListener
	public void onPaletteSelected(PaletteSelectedEvent event) {
		if (paletteMap.containsKey(event.getPaletteName())) {
			currentPalette = paletteMap.get(event.getPaletteName());
		}
	}

	public void onTileSelectedEvent(TileSelectedEvent event) {
		if (tilesMap.containsKey(event.getTileName())) {
			currentTiles = tilesMap.get(event.getTileName());
		}
	}

}
