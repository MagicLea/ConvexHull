import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JFrame;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ConvexHull {

	private JFrame frame;
	private MyComponent component = new MyComponent();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ConvexHull window = new ConvexHull();

					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ConvexHull() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 900, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				MyComponent src = (MyComponent) e.getSource();
				if (e.getButton() == MouseEvent.BUTTON1) {
					System.out.println("LmouseClicked");
					component.shuffle();
					src.setBonus(0);
				}
				else if (e.getButton() == MouseEvent.BUTTON2) {
					System.out.println("MmouseClicked");
					component.switchStep();
					src.setBonus(0);
				}

				else if (e.getButton() == MouseEvent.BUTTON3) {
					System.out.println("RmouseClicked");
					src.switchCoordinate();
					src.setBonus(src.getBonus() + 1);
					if (src.getBonus() >= 4) {
						src.setBonus(0);
						System.out.println("Bonus!");
						new BounsThread(src).start();
					}

				}
			}

		});
		component.addMouseWheelListener(new MouseAdapter() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				MyComponent src = (MyComponent) e.getSource();
				if (e.getWheelRotation() > 0) {
					System.out.println("mouseWheelUp");
					src.incPointNumber();
				}
				else {
					System.out.println("mouseWheelDown");
					src.decPointNumber();
				}
			}
		});
		frame.getContentPane().add(component, BorderLayout.CENTER);
	}

}

@SuppressWarnings("serial")
class MyComponent extends JComponent {
	public static final int PRECISION = 5000;
	public static final int SPACE = 10;
	public static final int P_D = 6; // Point Diameter 點的直徑
	public static final int P_NUM_MAX = 6553600;
	public static final int P_NUM_MIN = 4;
	public static final int P_NUM_DRAW_MAX = 12800;
	public static final int P_NUM_POLYGON_DRAW_MAX = 409600;
	private int pointNumber = 128;
	private Point[] points;
	private Queue<List<List<Point>>> polygonsQueue;
	private List<List<Point>> paintPolygons; // 現在畫的Polygon組合
	private boolean coordinate = false;
	private boolean step = false;
	private int bonus = 0;
	private Color color = Color.MAGENTA;
	private ComputeThread computeThread = null;
	private String hintString;

	public Point[] getPoints() {
		return points;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public int getBonus() {
		return bonus;
	}

	public void setBonus(int bonus) {
		this.bonus = bonus;
	}

	public Queue<List<List<Point>>> getPolygonsQueue() {
		return polygonsQueue;
	}

	public synchronized List<List<Point>> getPaintPolygons() {
		return paintPolygons;
	}

	public synchronized void setPaintPolygons(List<List<Point>> paintPolygons) {
		this.paintPolygons = paintPolygons;
	}

	public synchronized ComputeThread getComputeThread() {
		return computeThread;
	}

	public synchronized void setComputeThread(ComputeThread computeThread) {
		this.computeThread = computeThread;
	}

	public void setHintString(String hintString) {
		this.hintString = hintString;
	}

	public void incPointNumber() {
		int newPointNumber = (int) (pointNumber * 2);
		if (newPointNumber <= P_NUM_MAX) {
			pointNumber = newPointNumber;
			shuffle();
		}
		else if (pointNumber != P_NUM_MAX && newPointNumber > P_NUM_MAX) {
			pointNumber = P_NUM_MAX;
			shuffle();
		}
	}

	public void decPointNumber() {
		int newPointNumber = (int) (pointNumber / 2);
		if (newPointNumber >= P_NUM_MIN) {
			pointNumber = newPointNumber;
			shuffle();
		}
		else if (pointNumber != P_NUM_MIN && newPointNumber < P_NUM_MIN) {
			pointNumber = P_NUM_MIN;
			shuffle();
		}
	}

	public boolean isCoordinate() {
		return coordinate;
	}

	public void switchCoordinate() {
		this.coordinate = !coordinate;
		repaint();
	}

	public boolean isStep() {
		return step;
	}

	public void switchStep() {
		this.step = !step;
		compute();
	}

	public void compute() {
		new GiftWrappingThread(this).start();
	}

	public MyComponent() {
		hintString = "";
		polygonsQueue = new LinkedBlockingQueue<List<List<Point>>>();
		shuffle();
		new MyThread(this).start();
	}

	public void shuffle() {

		points = new Point[pointNumber];
		for (int i = 0; i < points.length; i++) {
			points[i] = new MyPoint((int) (Math.random() * PRECISION), (int) (Math.random() * PRECISION));
		}
		polygonsQueue.clear();
		setPaintPolygons(null);
		repaint();

		if (getComputeThread() != null) {
			getComputeThread().terminate();
			while (getComputeThread() != null) {
				; // nothing
			}
		}
		if (!getPolygonsQueue().isEmpty()) {
			getPolygonsQueue().clear();
		}
		compute();
	}

	@Override
	public void paint(Graphics g) {
		int vHeight = getHeight() - SPACE * 2;
		int vWidth = getWidth() - SPACE * 2;
		Graphics2D g2 = (Graphics2D) g;
		RenderingHints rh = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHints(rh);
		g2.setColor(Color.BLACK);
		if (pointNumber <= P_NUM_DRAW_MAX) {
			for (Point p : points) {
				g2.fillOval((int) (p.x * (double) vWidth / PRECISION) + SPACE,
						getHeight() - (int) (p.y * (double) vHeight / PRECISION + SPACE), P_D, P_D);
				if (coordinate) {
					g2.drawString("(" + p.x + "," + p.y + ")", (int) (p.x * (double)
							vWidth / PRECISION) + SPACE, getHeight() - (int) (p.y * (double)
							vHeight / PRECISION + SPACE));
				}
			}
		}
		else {
			String s = "Too many to draw.";
			g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 96));
			int stringLen = (int) g2.getFontMetrics().getStringBounds(s, g2).getWidth();
			int start = getWidth() / 2 - stringLen / 2;
			g2.drawString(s, start, getHeight() / 2);
		}

		g2.setColor(color);
		g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
		g2.drawString("POINT NUMBER:" + pointNumber + " " + hintString, SPACE, this.getHeight() - SPACE);

		if (pointNumber <= P_NUM_POLYGON_DRAW_MAX && getPaintPolygons() != null) {
			for (List<Point> paintPolygon : getPaintPolygons()) {
				draw(g2, paintPolygon);
			}
		}

	}

	public void draw(Graphics2D g2, List<Point> polygon) {
		if (polygon == null) {
			return;
		}
		int size = polygon.size();
		for (int i = 0; i < size - 1; i++) {
			drawLine(g2, polygon.get(i), polygon.get(i + 1));
		}
	}

	public void drawLine(Graphics2D g2, Point p1, Point p2) {
		int vHeight = getHeight() - SPACE * 2;
		int vWidth = getWidth() - SPACE * 2;
		g2.setColor(Color.RED);
		g2.drawLine((int) (p1.x * (double) vWidth / PRECISION) + SPACE + P_D / 2,
				getHeight() - (int) (p1.y * (double) vHeight / PRECISION + SPACE) + P_D / 2,
				(int) (p2.x * (double) vWidth / PRECISION) + SPACE + P_D / 2,
				getHeight() - (int) (p2.y * (double) vHeight / PRECISION + SPACE) + P_D / 2);
	}

}

class MyThread extends Thread {
	private MyComponent c;

	public MyThread(MyComponent c) {
		this.c = c;
	}

	@Override
	public void run() {
		try {
			while (true) {
				if (!c.getPolygonsQueue().isEmpty()) {
					List<List<Point>> polygons = c.getPolygonsQueue().remove();
					System.out.println("Get new polygons. " + polygons);
					c.setPaintPolygons(polygons);
					c.repaint();
					sleep(100);
				}
			}
		} catch (InterruptedException e) {
			System.out.println("Exception");
			new MyThread(c).start(); // 重新開一個Thread繼續管理
		}
	}
}

class ComputeThread extends Thread {
	protected MyComponent c;
	protected boolean isContinue = true;

	public ComputeThread(MyComponent c) {
		this.c = c;
	}

	public void terminate() {
		isContinue = false;
	}

}

class GiftWrappingThread extends ComputeThread {
	public GiftWrappingThread(MyComponent c) {
		super(c);
	}

	@Override
	public void run() {
		try { // 確保發生意外可以取消此thread
			c.setComputeThread(this);
			List<List<Point>> polygons = new ArrayList<List<Point>>();
			List<Point> polygon = new ArrayList<Point>();
			Point[] points = c.getPoints().clone();
			Point firstPoint;
			Point nextPoint;
			Point tempPoint;
			int index;
			c.setHintString("(Gift Wrapping...)");
			c.repaint();
			long StartTime = System.currentTimeMillis(); // 取出目前時間
			// 尋找最左下點當第一個點
			firstPoint = points[0];
			index = 0;

			for (int i = 0; i < points.length && isContinue; i++) {
				if (points[i].y < firstPoint.y || (points[i].y == firstPoint.y && points[i].x < firstPoint.x)) {
					index = i;
					firstPoint = points[i];
				}
			}
			// 最左下點放到索引0
			tempPoint = points[0];
			points[0] = points[index];
			points[index] = tempPoint;
			polygon.add(points[0]);
			// 逆時針尋找
			do {
				nextPoint = points[1]; // 選point[1]當第一個點
				index = 1;
				for (int i = 2; i < points.length && isContinue; i++) {
					double cross = (points[i].x - points[0].x) * (nextPoint.y - points[0].y)
							- (points[i].y - points[0].y) * (nextPoint.x - points[0].x);
					if (cross > 0) {
						index = i;
						nextPoint = points[i];
					}
				}
				// 這次使用的放到索引0
				tempPoint = points[0];
				points[0] = points[index];
				points[index] = tempPoint;
				polygon.add(points[0]);

				// 顯示每個步驟
				if (c.isStep() && c.getPoints().length <= MyComponent.P_NUM_POLYGON_DRAW_MAX) {
					polygons.clear();
					@SuppressWarnings("unchecked")
					List<Point> polygonClone = (List<Point>) ((ArrayList<Point>) polygon).clone();
					polygons.add(polygonClone);
					@SuppressWarnings("unchecked")
					List<List<Point>> polygonsClone = (List<List<Point>>) ((ArrayList<List<Point>>) polygons).clone();
					c.getPolygonsQueue().offer(polygonsClone);
					polygons.clear();
				}

			} while (nextPoint != firstPoint && isContinue);
			long ProcessTime = System.currentTimeMillis() - StartTime; // 計算處理時間
			c.setHintString("(Gift Wrapping : " + ProcessTime + " milliseconds)");
			polygon.add(firstPoint);
			polygons.add(polygon);
			c.getPolygonsQueue().offer(polygons);
		} catch (Exception e) {
			e.printStackTrace();
			c.getPolygonsQueue().clear();
		} finally {
			if (c.isStep()) {
				c.switchStep();
			}
			c.setComputeThread(null);
		}
	}
}

class MyPoint extends Point {
	public MyPoint(int x, int y) {
		super(x, y);
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}
}

class BounsThread extends Thread {
	private MyComponent c;

	public BounsThread(MyComponent c) {
		this.c = c;
	}

	@Override
	public void run() {
		try {
			c.setColor(Color.BLUE);
			c.repaint();
			sleep(500);

			c.setColor(Color.GREEN);
			c.repaint();
			sleep(500);

			c.setColor(Color.lightGray);
			c.repaint();
			sleep(500);

			c.setColor(Color.ORANGE);
			c.repaint();
			sleep(500);

			c.setColor(Color.PINK);
			c.repaint();
			sleep(500);

			c.setColor(Color.MAGENTA);
			c.repaint();
		} catch (InterruptedException e) {
			c.setColor(Color.MAGENTA);
			c.repaint();
		}
	}
}
