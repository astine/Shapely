package net.theatticlight.Shapely;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.theatticlight.Shapely.Record.Point;
import net.theatticlight.Shapely.Record.Polygon;

public class GraphicalTest {
	
	static class MyPanel extends JPanel {
		ShapeFile shapeFile;
		public AtomicInteger X, Y;
		boolean firstWrite = true;
		String field;

	    public MyPanel(ShapeFile shapeFile, String field) {
	    	this.shapeFile = shapeFile;
	    	this.X = new AtomicInteger(0);
	    	this.Y = new AtomicInteger(0);
	        setBorder(BorderFactory.createLineBorder(Color.black));
	        this.field = field;
	    }

	    public Dimension getPreferredSize() {
	        return new Dimension(600,640);
	    }

	    public void paintComponent(Graphics g) {
//	        super.paintComponent(g);       
	
			double xRatio = 1000 / (shapeFile.getBounds()[2] - shapeFile.getBounds()[0]);
			double yRatio = 700 / (shapeFile.getBounds()[3] - shapeFile.getBounds()[1]); 
        
			// Draw Text
			int downshift = 40;
			g.clearRect(0, 0, 1000, downshift);
			double x = X.doubleValue() / xRatio + shapeFile.getBounds()[0];
			double y = (700-(Y.doubleValue()-downshift)) / yRatio + shapeFile.getBounds()[1];

			String TZs = "";
			try
			{
				for(Object tz: shapeFile.getInfoAtPoint(new Record.Vect(x,y), field))
					TZs = TZs + tz + ", ";
			} catch (Exception e) {
				System.out.println("Error looking up records: " + e.getMessage());
				e.printStackTrace();
			}
			g.drawString("X: " + x + " Y: " + y + " Time Zone: " + TZs,10,20);
			
			if(firstWrite)
			{
				for(int i = 1; i < shapeFile.getRecordCount()+1;i++)
				{
					Record record;
					try
					{
		        		record = shapeFile.getRecord(i);
		        	} catch (Exception e) {
		        		System.out.println("Failed to load record: " + i);
		        		System.out.println("Skipping.");
		        		break;
		        	}
					Polygon polygon = ((Polygon)record.shape);
					
					for(int j = 1; j < polygon.points.length; j++)
					{
						Point point1 = polygon.points[j-1];
						Point point2 = polygon.points[j];
						g.drawLine(	(int)((point1.X - shapeFile.getBounds()[0]) * xRatio),
	        						(700-(int)((point1.Y - shapeFile.getBounds()[1]) * yRatio)) + downshift,
	        						(int)((point2.X - shapeFile.getBounds()[0]) * xRatio),
	        						(700-(int)((point2.Y - shapeFile.getBounds()[1]) * yRatio)) + downshift);
					}
				}
			}

			firstWrite = false;
	    }  
	}
	
	static MyPanel panel;
	
	static void showPolygons(ShapeFile shapeFile, String field)
	{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel = new MyPanel(shapeFile, field);
		panel.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent evt) {
				panel.X.set(evt.getPoint().x);
				panel.Y.set(evt.getPoint().y);
				panel.paint(panel.getGraphics());
			}
		});
		frame.setSize(1000, 1000);
		frame.add(panel);
		frame.setVisible(true);
	}

	
	public static void main(String[] args)
	{
		try
		{
			ShapeFile shapeFile = new ShapeFile(args[0]);
			
			showPolygons(shapeFile, args[1]);
			
			System.out.println("Done.");
		}
		catch (Exception e)
		{
			System.out.println("Error: " + e.getMessage());
			for(StackTraceElement element : e.getStackTrace())
				System.out.println(element);
		}
	}
}
