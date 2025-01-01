package pokemon.files.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import pokemon.files.DSFileManager;
import pokemon.files.FileFormat;
import pokemon.files.SubSection;
import pokemon.files.headers.Header;
import pokemon.files.types.TypeEnum;
import pokemon.files.types.TypedArray;
import pokemon.files.types.TypedNumber;
import pokemon.files.types.TypedVariable;

public class NARC extends FileFormat {

	private static final String NARC_MAGIC = "NARC";

	private BTAF btaf;
	private BTNF btnf;
	private GMIF gmif;

	public NARC(int fileNumber, String[][] dirNames, String[][] fileNames, int[] fileSizes, boolean doWriteSubTables) {
		super(NARC_MAGIC, 0, 1);
		super.setByteOrder(0xFFFE);
		this.btaf = new BTAF(fileNumber);
		this.btnf = new BTNF(dirNames, fileNames, fileSizes, doWriteSubTables);
	}

	public NARC() {
		this(0, new String[0][0], new String[0][0], new int[0], false);
	}

	@Override
	protected void updateSize() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void loadData(InputStream inStream) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void storeData(OutputStream outStream) throws IOException {
		// TODO Auto-generated method stub

	}

	private static class BTAF extends SubSection {

		private static final String BTAF_MAGIC = "BTAF";
		private static final int BTAF_DEFAULT_SIZE = Header.DEFAULT_HEADER_SIZE + 0x4;
		private static final TypedNumber PADDING = new TypedNumber(0, TypeEnum.UINT16);

		private TypedNumber fileNumber;
		private TypedArray fileAddresses;

		public BTAF(int fileNumber) {
			super(BTAF_MAGIC, BTAF_DEFAULT_SIZE);
			this.fileNumber = new TypedNumber(fileNumber, TypeEnum.UINT16);
			this.fileAddresses = new TypedArray(TypeEnum.UINT64, fileNumber);

			super.setSize(BTAF_DEFAULT_SIZE + fileAddresses.length() * TypeEnum.UINT64.getByteSize());
		}

		@Override
		protected void loadData(InputStream inStream) throws IOException {
			DSFileManager.read(inStream, fileNumber);
			DSFileManager.read(inStream, TypeEnum.UINT16); // Padding
			DSFileManager.read(inStream, fileAddresses);
		}

		@Override
		protected void storeData(OutputStream outStream) throws IOException {
			DSFileManager.write(outStream, fileNumber);
			DSFileManager.write(outStream, PADDING);
			DSFileManager.write(outStream, fileAddresses);
		}

	}

	private static class BTNF extends SubSection {

		private static final String BTNF_MAGIC = "BTNF";
		private static final int BTNF_DEFAULT_SIZE = Header.DEFAULT_HEADER_SIZE;

		private TypedNumber[][] mainTables;
		private TypedVariable[][][] subTables;

		/**
		 * <p>
		 * Directories and files in the archive. The first index contains the root
		 * directories. Sub-directories are indexed in arrival order.
		 * </p>
		 * 
		 * <p>
		 * We can take the following example:<br>
		 * <code>
		 * /<br>
		 * |<br>
		 * |_ a<br>
		 * ~~~|_ a1<br>
		 * |<br>
		 * |_ b<br>
		 * ~~~|_ b1<br>
		 * </code> The first dirNames index will contain the root directories, so "a"
		 * and "b". The second will contain "a" directories, the third will contain "b",
		 * then "a1" and finally "b1". The fifth and sixth exist but are empty.
		 * </p>
		 * 
		 * <p>
		 * Files are indexed in arrival order too, meaning that their sizes are indexed
		 * the same way they arrive.
		 * </p>
		 * 
		 * @param dirNames  Directory names
		 * @param fileNames File names
		 * @param fileSizes File sizes
		 */
		public BTNF(String[][] dirNames, String[][] fileNames, int[] fileSizes, boolean doWriteSubTables) {
			super(BTNF_MAGIC, BTNF_DEFAULT_SIZE);

			// First are all main tables
			int dirNumber = dirNames.length;
			this.mainTables = new TypedNumber[dirNumber][3];

			// Write the first one by hand, offset is 8 * number of directories unless there
			// is no sub-table...
			int firstOffset = doWriteSubTables ? 4 : 8 * dirNumber;
			this.mainTables[0][0] = new TypedNumber(firstOffset, TypeEnum.UINT32);
			this.mainTables[0][1] = new TypedNumber(0, TypeEnum.UINT16);
			this.mainTables[0][2] = new TypedNumber(dirNumber, TypeEnum.UINT16);

			int currentFileIndex = fileNames[0].length;
			int currentDirIndex = 0;
			int currentParentDir = 0xF000;
			int nextDirCounter = dirNames[0].length; // if more than 1 dir, then root must have one
			for (int dirIndex = 1; dirIndex < dirNumber; dirIndex++) {
				// Do not forget to fill it later!
				this.mainTables[dirIndex][0] = new TypedNumber(0, TypeEnum.UINT32);
				this.mainTables[dirIndex][1] = new TypedNumber(currentFileIndex, TypeEnum.UINT16);
				this.mainTables[dirIndex][2] = new TypedNumber(currentParentDir, TypeEnum.UINT16);

				// Decrease dir counter and update if needed
				nextDirCounter--;
				// The last dir cannot be a counter nor its own sub-(sub-)directory
				while (dirIndex < dirNumber - 1 && currentDirIndex < dirIndex && nextDirCounter == 0) {
					currentParentDir++;
					nextDirCounter = dirNames[++currentDirIndex].length;
				}

				// Increment file index
				currentFileIndex += fileNames[dirIndex].length;
			}

			if (doWriteSubTables) {
				// Then sub-tables (must finish by 00)
				this.subTables = new TypedVariable[dirNumber][][];
				int dirId = 0xF001;
				int currentOffset = 8 * dirNumber;
				for (int dirIndex = 0; dirIndex < dirNumber; dirIndex++) {
					// If not the root directory, then assign offset
					if (dirIndex != 0) {
						this.mainTables[dirIndex][0].setValue(currentOffset);
					}

					int subDirNumber = dirNames[dirIndex].length;
					int subFilesNumber = fileNames[dirIndex].length;
					int entryNumber = subDirNumber + subFilesNumber;
					int entryIndex = 0;
					TypedVariable[][] dirSubTable = new TypedVariable[entryNumber][];

					// First directories (size, string, id)
					for (int i = 0; i < subDirNumber; i++) {
						TypedVariable[] subDirTable = new TypedVariable[3];
						String dirName = dirNames[dirIndex][i];
						subDirTable[0] = new TypedNumber(0x80 | dirName.length(), TypeEnum.UINT8);
						subDirTable[1] = new TypedArray(dirName);
						subDirTable[2] = new TypedNumber(dirId++, TypeEnum.UINT16);

						// Increment offset
						currentOffset += TypeEnum.UINT8.getByteSize() + dirName.length()
								+ TypeEnum.UINT16.getByteSize();

						// Add to entry dir
						dirSubTable[entryIndex++] = subDirTable;
					}

					// Then files
					for (int i = 0; i < subFilesNumber; i++) {
						TypedVariable[] subFileTable = new TypedVariable[2];
						String fileName = fileNames[dirIndex][i];
						subFileTable[0] = new TypedNumber(fileName.length() & 0x7F, TypeEnum.UINT8);
						subFileTable[1] = new TypedArray(fileName);

						// Increment offset
						currentOffset += TypeEnum.UINT8.getByteSize() + fileName.length();

						// Add to entry dir
						dirSubTable[entryIndex++] = subFileTable;
					}

					// Add to subtables
					this.subTables[dirIndex] = dirSubTable;

					// Add 1 to offset for terminator byte
					currentOffset += 1;
				}

				// The current offset holds the size of the section...
				// The size MUST BE a multiple of 4, so add padding if needed
				int size = BTNF_DEFAULT_SIZE + currentOffset;
				if ((size & 0b11) != 0) {
					size += 4 - (size & 0b11);
				}
				super.setSize(size);
			} else {
				super.setSize(BTNF_DEFAULT_SIZE + 0x8 * dirNumber);
			}
		}

		@Override
		protected void loadData(InputStream inStream) throws IOException {
			// First main table will say how many directories there is
			TypedNumber firstOffset = DSFileManager.read(inStream, TypeEnum.UINT32);
			TypedNumber firstFileId = DSFileManager.read(inStream, TypeEnum.UINT16);
			TypedNumber dirNumber = DSFileManager.read(inStream, TypeEnum.UINT16);

			this.mainTables = new TypedNumber[dirNumber.getIntValue()][3];
			mainTables[0][0] = firstOffset;
			mainTables[0][1] = firstFileId;
			mainTables[0][2] = dirNumber;

			for (int dirIndex = 1; dirIndex < dirNumber.getIntValue(); dirIndex++) {
				mainTables[dirIndex][0] = DSFileManager.read(inStream, TypeEnum.UINT32);
				mainTables[dirIndex][1] = DSFileManager.read(inStream, TypeEnum.UINT16);
				mainTables[dirIndex][2] = DSFileManager.read(inStream, TypeEnum.UINT16);
			}

			// There are 8 + 8 * dirNumber bytes used here, if section size then no
			// sub-table
			if (super.getSize() != BTNF_DEFAULT_SIZE + 8 * dirNumber.getIntValue()) {
				// Create sub-table
				this.subTables = new TypedVariable[dirNumber.getIntValue()][][];

				// Compute the first offset
				int offset = BTNF_DEFAULT_SIZE + 8 * dirNumber.getIntValue();

				for (int dirIndex = 0; dirIndex < dirNumber.getIntValue(); dirIndex++) {
					// Read until we reach the offset...
					for (int i = 0; i < mainTables[dirIndex][0].getIntValue() - offset; i++) {
						DSFileManager.read(inStream, TypeEnum.UINT8);
						offset++;
					}

					// Use a list because we don't know how many entries there are
					List<TypedVariable[]> entries = new ArrayList<TypedVariable[]>();

					// Read until the terminator is reached
					boolean terminated = false;
					do {
						TypedNumber typeByte = DSFileManager.read(inStream, TypeEnum.UINT8);
						int typeValue = typeByte.getIntValue();
						if (typeValue == 0) {
							terminated = true;
							offset += TypeEnum.UINT8.getByteSize();

						} else if (typeValue < 0x80) {
							// It is a file
							TypedVariable[] fileEntry = new TypedVariable[2];
							fileEntry[0] = typeByte;
							fileEntry[1] = DSFileManager.read(inStream, TypeEnum.UINT8, typeValue);
							offset += TypeEnum.UINT8.getByteSize() + typeValue;

							entries.add(fileEntry);

						} else if (typeValue > 0x80) {
							// It is a folder
							TypedVariable[] dirEntry = new TypedVariable[3];
							dirEntry[0] = typeByte;
							dirEntry[1] = DSFileManager.read(inStream, TypeEnum.UINT8, typeValue & 0b01111111);
							dirEntry[2] = DSFileManager.read(inStream, TypeEnum.UINT16);
							offset += TypeEnum.UINT8.getByteSize() + typeValue
									& 0b01111111 + TypeEnum.UINT16.getByteSize();

							entries.add(dirEntry);
						}
					} while (!terminated);

					TypedVariable[][] subTable = new TypedVariable[entries.size()][];
					for (int i = 0; i < entries.size(); i++) {
						subTable[i] = entries.get(i);
					}

					this.subTables[dirIndex] = subTable;
				}

				// Read the padding if the offset is lesser than the size
				for (int i = 0; i < getSize() - offset; i++) {
					DSFileManager.read(inStream, TypeEnum.UINT8);
				}
			}
		}

		@Override
		protected void storeData(OutputStream outStream) throws IOException {
			int dirNumber = this.mainTables.length;

			// Write main tables first
			for (int dirIndex = 0; dirIndex < dirNumber; dirIndex++) {
				DSFileManager.write(outStream, mainTables[dirIndex][0]);
				DSFileManager.write(outStream, mainTables[dirIndex][1]);
				DSFileManager.write(outStream, mainTables[dirIndex][2]);
			}

			// Check if size is greater than only main tables
			if (getSize() > BTNF_DEFAULT_SIZE + 8 * dirNumber) {
				// Keep in mind the offset for possible padding
				int offset = Header.DEFAULT_HEADER_SIZE + 8 * dirNumber;
				
				for (int dirIndex = 0; dirIndex < dirNumber; dirIndex++) {
					TypedVariable[][] entries = this.subTables[dirIndex];
					for (int entryIndex = 0; entryIndex < entries.length; entryIndex++) {
						// Stop thinking, write every entry
						for (TypedVariable var : entries[entryIndex]) {
							if (var instanceof TypedNumber) {
								DSFileManager.write(outStream, (TypedNumber) var);
								offset += var.getSize();
							} else if (var instanceof TypedArray) {
								DSFileManager.write(outStream, (TypedArray) var);
								offset += var.getSize();
							}
						}
					}
					
					// Write the terminator byte for the directory
					DSFileManager.write(outStream, new TypedNumber(0, TypeEnum.UINT8));
					offset += TypeEnum.UINT8.getByteSize();
				}
				
				// Do not forget the padding (fill with FFh)
				for (int i = 0; i < getSize() - offset; i++) {
					DSFileManager.write(outStream, new TypedNumber(0xFF, TypeEnum.UINT8));
				}
			}
		}
	}

	private static class GMIF extends SubSection {
		
		private static final String GMIF_MAGIC = "GMIF";
		private static final int GMIF_DEFAULT_SIZE = Header.DEFAULT_HEADER_SIZE;

		public GMIF() {
			super(GMIF_MAGIC, GMIF_DEFAULT_SIZE);
		}

		@Override
		protected void loadData(InputStream inStream) throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		protected void storeData(OutputStream outStream) throws IOException {
			// TODO Auto-generated method stub

		}

	}

}
