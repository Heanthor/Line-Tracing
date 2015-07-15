package src;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.*;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class LineFinder {
	/**
	 * Represents each side of a square image
	 * @author Reed
	 */
	private enum Side {TOP, BOTTOM, LEFT, RIGHT};

	private ArrayList<Integer> foundIndexes = new ArrayList<Integer>();
	
	/**
	 * Current image being displayed
	 */
	private Component currentImage;

	public static void main(String[] args) {
		new LineFinder().init();
	}

	/**
	 * Launches UI, gets and draws points
	 */
	private void init() {
		URL imageURL = LineFinder.class.getResource("images/square8.jpg");

		if (imageURL != null) {
			ImageIcon i = new ImageIcon(imageURL);
			BufferedImage br = toBufferedImage(i.getImage());

			Raster ri = br.getRaster();

			final int[] data = calculateLine(ri);

			//Display original image
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JFrame frame = new JFrame();
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.setSize(br.getWidth() + 100, br.getHeight() + 100);

					frame.getContentPane().setLayout(new BorderLayout(0, 0));

					JPanel panel = new JPanel();
					panel.setBackground(SystemColor.control);
					frame.getContentPane().add(panel, BorderLayout.SOUTH);

					JButton btnToggleLine = new JButton("Toggle Line");
					panel.add(btnToggleLine);

					/* Toggle between original and traced images */
					btnToggleLine.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							if (currentImage instanceof LineLayer) {
								frame.getContentPane().remove(currentImage);
								currentImage = frame.getContentPane().add(new PicLayer(br));
								frame.revalidate();
							} else {
								frame.getContentPane().remove(currentImage);
								currentImage = frame.getContentPane().add(new LineLayer(br, data[0], data[1], data[2], data[3], Color.red));
								frame.revalidate();
							}
						}
					});

					//Display line
					JComponent l = new LineLayer(br, data[0], data[1], data[2], data[3], Color.red);

					currentImage = frame.getContentPane().add(l);
					
					frame.setLocationByPlatform(true);
					frame.setVisible(true);
				}
			});

			//statistics(ri);
		} else {
			System.out.println("File read failed");
			System.exit(1);
		}
	}

	/**
	 * Finds the line present in the provided solid-background raster.
	 * @param ri Image raster
	 * @return {x1, y1, x2, y2} of line
	 */
	public int[] calculateLine(Raster ri) {
		/* Order: top, right, left, bot */ 
		int top = -1, right = -1, left = -1, bot = -1; //-1 for not set
		int foundCount = 0;

		for (int j = 0; j < 4; j++) {
			switch (j) {
			case 0:
				if ((top = getPoint(ri, Side.TOP)) != -1) {
					foundCount++;
				}
				break;
			case 1:
				if ((right = getPoint(ri, Side.RIGHT)) != -1) {
					foundCount++;
				}
				break;
			case 2:
				if ((left = getPoint(ri, Side.LEFT)) != -1) {
					foundCount++;
				}
				break;
			case 3:
				if ((bot = getPoint(ri, Side.BOTTOM)) != -1) {
					foundCount++;
				}
				break;
			default:
			}

			//Find 2 sides at max
			if (foundCount == 2) {
				break;
			}
		}

		//Map points to x1, y1, x2, y2
		//Direction is irrelevant
		final int x1, y1, x2, y2;

		if (top != -1) { //top is starting point
			x1 = top;
			y1 = 0;

			if (left != -1) {
				x2 = 0;
				y2 = left;
			} else if (right != -1) {
				x2 = ri.getWidth() - 1;
				y2 = right;
			} else { //bottom != -1
				x2 = bot;
				y2 = ri.getHeight() - 1;
			}
		} else if (left != -1) { //left is starting point
			x1 = 0;
			y1 = left;

			//top cannot be ending point since it was already checked
			if (right != -1) {
				x2 = ri.getWidth() - 1;
				y2 = right;
			} else { //bottom != -1
				x2 = bot;
				y2 = ri.getHeight() - 1;
			}
		} else { //No other combination but right-bot
			x1 = ri.getWidth() - 1;
			y1 = right;

			x2 = bot;
			y2 = ri.getHeight() - 1;
		}

		System.out.println("(" + x1 + ", " + y1 + "), (" + x2 + ", " + y2 + ")");
		final int[] data = {x1, y1, x2, y2};

		return data;
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

	/**
	 * Finds the minimum value in array a.
	 * @param a The array to search
	 * @return {minimum value, index of found value}, a value of -1 if not found.
	 */
	private int[] getMinInfo(int[] a) {
		int min = Integer.MAX_VALUE;
		final int FUZZING = 30; //deviation from mean
		boolean found = false;
		int minIndex = -1;

		for (int i = 0; i < a.length; i++) {
			if (a[i] < min - FUZZING) { //eliminate false positives
				min = a[i];
				minIndex = i;

				//If a point is found after the initial point (255), min is found
				if (i > 0) {
					found = true;
				}
			}
		}

		return new int[] {min, found? minIndex : -1};
	}

	private int[][] scanRow(Raster ri, int row) {
		int width = ri.getWidth();
		int[] r = new int[width];
		int[] g = new int[width];
		int[] b = new int[width];

		ri.getSamples(0, row, width, 1, 0, r);
		ri.getSamples(0, row, width, 1, 1, g);
		ri.getSamples(0, row, width, 1, 2, b);

		return new int[][] {r, g, b};
	}

	private int[][] scanCol(Raster ri, int col) {
		int height = ri.getHeight();
		int[] r = new int[height];
		int[] g = new int[height];
		int[] b = new int[height];

		ri.getSamples(col, 0, 1, height, 0, r);
		ri.getSamples(col, 0, 1, height, 1, g);
		ri.getSamples(col, 0, 1, height, 2, b);

		return new int[][] {r, g, b};
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

	/**
	 * Find point on given side.
	 * @param ri
	 * @param s
	 * @return
	 */
	private int getPoint(Raster ri, Side s) {
		switch (s) {
		case TOP:
			int [][] containerTop = scanRow(ri, 0);

			int[] r1 = containerTop[0];
			int[] g1 = containerTop[1];
			int[] b1 = containerTop[2];

			int[] avgTop = averageArrays(new int[][] {r1, g1, b1});

			return getMinInfo(avgTop)[1];
		case BOTTOM:
			int[][] containerBot = scanRow(ri, ri.getHeight() - 1);

			int[] r2 = containerBot[0];
			int[] g2 = containerBot[1];
			int[] b2 = containerBot[2];

			int[] avgBot = averageArrays(new int[][] {r2, g2, b2});
			return getMinInfo(avgBot)[1];
		case LEFT:
			int [][] containerLeft = scanCol(ri, 0);
			int[] r3 = containerLeft[0];
			int[] g3 = containerLeft[1];
			int[] b3 = containerLeft[2];

			int[] avgLeft = averageArrays(new int[][] {r3, g3, b3});
			return getMinInfo(avgLeft)[1];
		case RIGHT:
			int[][] containerRight = scanCol(ri, ri.getWidth() - 1);

			int[] r4 = containerRight[0];
			int[] g4 = containerRight[1];
			int[] b4 = containerRight[2];

			int[] avgRight = averageArrays(new int[][] {r4, g4, b4});
			return getMinInfo(avgRight)[1];
		default:
			return 0;
		}
	}
	/**
	 * Used for drawing a line on top of an image
	 * @author Reed
	 *
	 */
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

	private class PicLayer extends JComponent {
		private static final long serialVersionUID = 1L;
		final BufferedImage bgImage;

		public PicLayer(BufferedImage bgImage) {
			this.bgImage = bgImage;
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			g2.drawImage(bgImage, 0, 0, this);
		}
	}
}
