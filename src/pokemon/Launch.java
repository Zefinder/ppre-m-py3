package pokemon;

import java.io.IOException;

import pokemon.files.archive.NARC;
import pokemon.files.types.TypeEnum;
import pokemon.files.types.TypedNumber;
import pokemon.frame.MainFrame;

public class Launch {

	public static void main(String[] args) throws IOException {
		MainFrame frame = new MainFrame("aaa", "C:\\Users\\adric\\Desktop\\pokemon test\\test");
		frame.initFrame();
		
//		NARC narc = new NARC(5, new String[][] { { "a", "c", "b" }, { "a1" }, {}, { "b1" }, {}, {} },
//				new String[][] { { "a.a" }, {}, {}, { "b.b" }, { "aa.a" }, { "bb1.b", "bb2.b" } },
//				new int[] { 10, 10, 10, 10, 10 });
	}

}
