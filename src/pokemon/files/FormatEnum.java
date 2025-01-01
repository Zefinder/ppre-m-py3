package pokemon.files;

import java.util.Map;

import pokemon.files.graphics.NCGR;
import pokemon.files.graphics.NCLR;
import pokemon.files.graphics.NSCR;
import pokemon.utils.MapBuilder;

public enum FormatEnum {
	
	PALETTE(NCLR.class), TILE(NCGR.class), SCREEN(NSCR.class), UNKNOWN(null);
	
	private static final Map<String, FormatEnum> extensionMap = new MapBuilder<String, FormatEnum>()
			.put("nclr", PALETTE)
			.put("rlcn", PALETTE)
			.put("ncgr", TILE)
			.put("rgcn", TILE)
			.put("nscr", SCREEN)
			.put("rcsn", SCREEN)
			.build();

	private Class<? extends FileFormat> formatClass;

	private FormatEnum(Class<? extends FileFormat> formatClass) {
		this.formatClass = formatClass;
	}
	
	public Class<? extends FileFormat> getFormatClass() {
		return formatClass;
	}
	
	public static FormatEnum getFromExtension(String extension) {
		return extensionMap.getOrDefault(extension, UNKNOWN);
	}

}
