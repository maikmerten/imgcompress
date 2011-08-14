package de.maikmerten.imgcompress;

import java.awt.image.BufferedImage;

/**
 *
 * @author maik
 */
public class ImageUtil {

	public static int[][][] readImageData(BufferedImage bi) {
		int width = bi.getWidth();
		int height = bi.getHeight();

		System.out.println();

		int channels = bi.getRaster().getNumBands();
		System.out.println("Detected " + channels + " channels");



		int[][][] data = new int[width][height][channels];

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int c = 0; c < channels; ++c) {
					data[x][y][c] = bi.getRaster().getSample(x, y, c);
				}
			}
		}

		return data;
	}

	public static BufferedImage writeImageData(int[][][] data) {
		int width = data.length;
		int height = data[0].length;
		int channels = data[0][0].length;

		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (channels >= 2) {
			type = BufferedImage.TYPE_INT_RGB;
		}
		if (channels == 4) {
			type = BufferedImage.TYPE_INT_ARGB;
		}
		BufferedImage out = new BufferedImage(width, height, type);


		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int c = 0; c < channels; ++c) {
					out.getRaster().setSample(x, y, c, data[x][y][c]);
				}
			}
		}
		return out;
	}
}
