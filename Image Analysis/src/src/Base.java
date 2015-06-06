package src;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.net.URL;

import javax.swing.*;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class Base {

	public Base() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		Base b = new Base();
		b.init();
	}

	private void init() {
		URL imageURL = Base.class.getResource("square5.JPG");

		if (imageURL != null) {
			ImageIcon i = new ImageIcon(imageURL);
			BufferedImage br = toBufferedImage(i.getImage());

			Raster ri = br.getRaster();
			int[] points = findLine(ri);

			//Display original image
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JFrame f = new JFrame();
					f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					f.setSize(100, 100);
					f.setLocationByPlatform(true);

					LineLayer l = new LineLayer(br, points[0], 0, points[1], ri.getHeight() - 1, Color.red);
					f.getContentPane().add(l);

					f.setVisible(true);
				}
			});

			//statistics(ri);
		} else {
			System.out.println("File read failed");
		}
	}

	private void statistics(Raster ri) {
		int [][] containerTop = scanRow(ri, 0);

		int[] r = containerTop[0];
		int[] g = containerTop[1];
		int[] b = containerTop[2];

		printArray(r);
		printArray(g);
		printArray(b);

		DescriptiveStatistics statr = makeDS(r);
		DescriptiveStatistics statg = makeDS(g);
		DescriptiveStatistics statb = makeDS(b);

		Color[] c = makeColorArray(r, g, b);

		int avgr = (int)statr.getPercentile(50);
		int avgg = (int)statg.getPercentile(50);
		int avgb = (int)statb.getPercentile(50);

		System.out.println("Average r: " + avgr);
		System.out.println("Average g: " + avgg);
		System.out.println("Average b: " + avgb);

		Color avgColor = new Color(avgr, avgg, avgb);
		int[] minInfo = getMinInfo(r);

		System.out.println("Min: " + minInfo[0] + " at pos " + minInfo[1]);

		System.out.println("Std Dev: " + statr.getStandardDeviation());
	}

	//Returns a bufferedImage from image, from some github page
	private static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}

	private static void printArray(int[] a) {
		System.out.print("[");

		for (int i: a) {
			System.out.print(i + ", ");
		}
		System.out.println("]");
	}

	private Color[] makeColorArray(int[] r, int[] g, int[] b) {
		Color[] toReturn = new Color[r.length];

		for(int i = 0; i < r.length; i++) {
			toReturn[i] = new Color(r[i], g[i], b[i]);
		}

		return toReturn;
	}

	private DescriptiveStatistics makeDS(int[] i) {
		DescriptiveStatistics toReturn = new DescriptiveStatistics();

		for (int a: i) {
			toReturn.addValue(a);
		}

		return toReturn;
	}

	private int[] getMinInfo(int[] a) {
		int min = Integer.MAX_VALUE;
		int minIndex = -1;

		for (int i = 0; i < a.length; i++) {
			if (a[i] < min) {
				min = a[i];
				minIndex = i;
			}
		}

		return new int[] {min, minIndex};
	}

	private int[][] scanRow(Raster ri, int row) {
		int[] r = new int[48];
		int[] g = new int[48];
		int[] b = new int[48];

		ri.getSamples(0, row, 48, 1, 0, r);
		ri.getSamples(0, row, 48, 1, 1, g);
		ri.getSamples(0, row, 48, 1, 2, b);

		return new int[][] {r , g, b};
	}

	private int average(int[] a) {
		int avg = 0;

		for (int i: a) {
			avg += i;
		}

		return avg / a.length;
	}

	/**
	 * 3 arrays, must be same size
	 * @param a
	 * @return
	 */
	private int[] averageArrays(int[][] a) {
		int[] avgTop = new int[a[0].length];

		for (int i = 0; i < a[0].length; i++) {
			int x = a[0][i] +  a[1][i] +  a[2][i];

			avgTop[i] = x / 3;
		}

		return avgTop;
	}

	private int[] findLine(Raster ri) {
		int [][] containerTop = scanRow(ri, 0);

		int[] r1 = containerTop[0];
		int[] g1 = containerTop[1];
		int[] b1 = containerTop[2];

		int[] avgTop = averageArrays(new int[][] {r1, g1, b1});

		int topCoord = getMinInfo(avgTop)[1];

		int[][] containerBot = scanRow(ri, ri.getHeight() - 1);

		int[] r2 = containerBot[0];
		int[] g2 = containerBot[1];
		int[] b2 = containerBot[2];

		int[] avgBot = averageArrays(new int[][] {r2, g2, b2});
		int botCoord = getMinInfo(avgBot)[1];

		System.out.println("Top coord : " + topCoord + " bot coord " + botCoord);
		return new int[] {topCoord, botCoord};
	}

	private class LineLayer extends JComponent {
		private static final long serialVersionUID = 1L;
		final BufferedImage bgImage;
		final int x1; 
		final int y1;
		final int x2;
		final int y2;
		final Color c;

		public LineLayer(BufferedImage bgImage, int x1, int y1, int x2, int y2, Color color) {
			this.bgImage = bgImage;
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.c = color;
			repaint();
		}               

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			g2.drawImage(bgImage, 0, 0, this);
			g2.setColor(c);
			g2.drawLine(x1, y1, x2, y2);

		}
	}
}
