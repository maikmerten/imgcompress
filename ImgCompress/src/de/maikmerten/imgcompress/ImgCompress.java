package de.maikmerten.imgcompress;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 *
 * @author maik
 */
public class ImgCompress {

	public static final int TOP = 0;
	public static final int LEFT = 1;
	public static final int AVG_LEFT_LEFTTOP_TOP = 2;
	public static final int PAETH = 3;

	public static boolean predictorWorks(int[][][] data, int x, int y, int c, int pred) {
		switch (pred) {
			case LEFT:
				return x > 0;
			case TOP:
				return y > 0;
			case AVG_LEFT_LEFTTOP_TOP:
			case PAETH:
				return (x > 0 && y > 0);
			default:
				return false;
		}
	}

	public static int getPredictedValue(int[][][] data, int x, int y, int c, int pred) {
		if (!predictorWorks(data, x, y, c, pred)) {
			return 127;
		}

		switch (pred) {
			case LEFT:
				return data[x - 1][y][c];
			case TOP:
				return data[x][y - 1][c];
			case AVG_LEFT_LEFTTOP_TOP:
				return (data[x - 1][y][c] + data[x][y - 1][c] + data[x - 1][y - 1][c]) / 3;
			case PAETH:
				int l = data[x - 1][y][c]; // top
				int t = data[x][y - 1][c]; // left
				int tl = data[x - 1][y - 1][c]; // top-left
				int p = l + t - tl; // initial predictor
				int pl = Math.abs(p - l); // prediction error left
				int pt = Math.abs(p - t); // prediction error top
				int ptl = Math.abs(p - tl); // prediction error top-left
				if (pl <= pt && pl <= ptl) {
					return l;
				} else if (pt <= ptl) {
					return t;
				} else {
					return tl;
				}
			default:
				return 127;
		}
	}

	public static int selectPredictor(int[][][] data, int x, int y, int c) {

		int pred = 0;
		int error = 100000;
		int newerror = error;

		// first column usually does best with top predictor
		// also have some "impossible" things map there
		if (x <= 0 || y < 0 || x >= data.length || y >= data[0].length) {
			return TOP;
		}

		// first line usually does best with left predictor
		if (y == 0) {
			return LEFT;
		}

		int target = data[x][y][c];

		// determine best predictor
		for (int i = 0; i <= 3; ++i) {
			int predval = getPredictedValue(data, x, y, c, i);
			newerror = Math.abs(target - predval);
			if (newerror <= error) { // predictors are sorted by sophistication
				pred = i;
				error = newerror;
			}
		}

		return pred;
	}

	public static int encodeResidue(int residue, int predval) {
		int boundary = Math.min(predval, 255 - predval) * 2;

		int res = residue >= 0 ? residue * 2 : (-residue) * 2 - 1;
		return res > boundary ? Math.abs(residue) + (boundary / 2) : res;
	}

	public static int decodeResidue(int residue, int predval) {

		int numnegative = predval;
		int numpositive = 255 - predval;
		int boundary = Math.min(numnegative, numpositive) * 2;

		if (residue > boundary) {
			int res = residue - (boundary / 2);
			return res > numpositive ? -res : res;
		} else {
			if (residue % 2 == 0) {
				// positive
				return residue / 2;
			} else {
				// negative
				return -((residue / 2) + 1);
			}
		}
	}

	public static int[][][] filter(int[][][] data) {
		int width = data.length;
		int height = data[0].length;
		int channels = data[0][0].length;

		int[][][] residues = new int[width][height][channels];
		long[] totalerrors = new long[channels];


		int pred = 0;

		for (int x = 0; x < data.length; ++x) {
			for (int y = 0; y < data[0].length; ++y) {
				for (int c = 0; c < channels; ++c) {

					// assume that the best predictor for the left neighbour
					// also works nicely here
					pred = selectPredictor(data, x - 1, y, c);

					int target = data[x][y][c];
					int predval = getPredictedValue(data, x, y, c, pred);

					int error = Math.abs(target - predval);
					totalerrors[c] += error;

					int residue = target - predval;
					residue = encodeResidue(residue, predval);
					residues[x][y][c] = residue;

					// now reconstruct for testing
					residue = decodeResidue(residue, predval);
					int value = predval + residue;
					if (value - target != 0) {
						System.out.println("Reconstruction failed! Difference = " + (value - target));
						System.out.println("Predictor: " + predval);
						System.out.println("Residue: " + residue);
					}

				}
			}
		}

		for (int c = 0; c < channels; ++c) {
			System.out.println("Average residue absolute: " + totalerrors[c] * 1.0 / (width * height));
		}

		return residues;

	}

	public static int[][][] reconstruct(int[][][] residues) {
		int width = residues.length;
		int height = residues[0].length;
		int channels = residues[0][0].length;

		// copy data to keep original intact
		int[][][] data = new int[width][height][channels];
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int c = 0; c < channels; ++c) {
					data[x][y][c] = residues[x][y][c];
				}
			}
		}

		// undo prediction
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int c = 0; c < channels; ++c) {
					int residue = data[x][y][c];
					int pred = selectPredictor(data, x - 1, y, c);

					int predval = getPredictedValue(data, x, y, c, pred);

					residue = decodeResidue(residue, predval);

					int value = predval + residue;
					data[x][y][c] = value;
				}
			}
		}

		return data;
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 2) {
			System.err.println("Usage: ImgCompress [--reconstruct] infile outfile");
		}


		boolean compress = true;

		String in = null;
		String out = null;

		for (String arg : args) {
			if (arg.equals("--reconstruct")) {
				compress = false;
				continue;
			}
			if (in == null) {
				in = arg;
				continue;
			}

			if (out == null) {
				out = arg;
				continue;
			}
		}

		System.out.println("Input file: " + in + "       Output file: " + out);
		

		File input = new File(in);
		File output = new File(out);


		// read input image
		BufferedImage bi_in = ImageIO.read(input);
		int[][][] data = ImageUtil.readImageData(bi_in);

		// filter and write residues to disk
		int[][][] filtered = null;
		if (compress) {
			filtered = filter(data);
		} else {
			filtered = reconstruct(data);
		}
		BufferedImage bi_filtered = ImageUtil.writeImageData(filtered);
		ImageIO.write(bi_filtered, "png", output);

	}
}
