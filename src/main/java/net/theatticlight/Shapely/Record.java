package net.theatticlight.Shapely;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import net.theatticlight.Shapely.Record.XY;

public class Record {
	
	public static class ShapeException extends Exception {
		ShapeException (String message) {
			super(message);
		}
	}
	
	public enum ShapeType {
		NULLSHAPE,
		POINT,
		POLYLINE,
		POLYGON,
		MULTIPOINT,
		POINTZ,
		POLYLINEZ,
		POLYGONZ,
		MULTIPOINTZ,
		POINTM,
		POLYLINEM,
		POLYGONM,
		MULTIPOINTM,
		MULTIPATCH;
			
		public static ShapeType getShape (int type) throws ShapeException
		{
			switch (type)
			{
			case 0: 	
				return NULLSHAPE;
			case 1: 
				return POINT;
			case 3: 
				return POLYLINE;
			case 5: 
				return POLYGON;
			case 8: 
				return MULTIPOINT;
			case 11: 
				return POINTZ;
			case 13: 
				return POLYLINEZ;
			case 15: 
				return POLYGONZ;
			case 18: 
				return MULTIPOINTZ;
			case 21: 
				return POINTM;
			case 23: 
				return POLYLINEM;
			case 25: 
				return POLYGONM;
			case 28: 
				return MULTIPOINTM;
			case 31: 
				return MULTIPATCH;
			default:
				throw new ShapeException("Bad shape type: " + type);
			}
		}
	}
	
	public static class ShapeHeader {
		final int fileSize;
		final int version;
		final ShapeType shapeType;
		
		final double minX;
		final double minY;
		final double maxX;
		final double maxY;

		final double minZ;
		final double maxZ;
		final double minM;
		final double maxM;
		
		ShapeHeader(RandomAccessFile file) throws IOException, ShapeException
		{
			file.seek(0);
			byte[] barray = new byte[100];
			ByteBuffer bb = ByteBuffer.wrap(barray);
			file.read(barray);
			
			if(bb.getInt() != 9994)
				throw new ShapeException("Bad ShapeFile");
			
			bb.position(24);
			fileSize = bb.getInt();
			version = Integer.reverseBytes(bb.getInt());
			if(version != 1000)
				throw new ShapeException("Wrong file version");
			
			shapeType = ShapeType.getShape(Integer.reverseBytes(bb.getInt()));
			minX = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			minY = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			maxX = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			maxY = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			minZ = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			maxZ = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			minM = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			maxM = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
		}
		
		public boolean equals(ShapeHeader header)
		{
			return 	this.fileSize == header.fileSize &&
					this.version == header.version &&
					this.shapeType == header.shapeType &&
					this.minX == header.minX &&
					this.minY == header.minY &&
					this.maxX == header.maxX &&
					this.maxY == header.maxY &&
					this.minZ == header.minZ &&
					this.maxZ == header.maxZ &&
					this.minM == header.minM &&
					this.maxM == header.maxM;
		}
	}
	
	public static interface XY {
		public double getX();
		public double getY();
	}
	
	public static class Vect implements XY {
		final double X, Y;

		public double getX() { return X; }
		public double getY() { return Y; }
		
		public Vect(double X, double Y) {
			this.X = X;
			this.Y = Y;
		}
		
		Vect(XY xy) {
			this(xy.getX(), xy.getY());
		}
		
		public Vect add(XY xy){
			return new Vect(xy.getX() + X, xy.getY() + Y);
		}
		
		public Vect sub(XY xy){
			return new Vect(xy.getX() - X, xy.getY() - Y);
		}
		
		public double dotProd(XY xy){
			return this.X*xy.getX()+this.Y*xy.getY();
		}
		
		public Vect scale(double s){
			return new Vect(X*s,Y*s);
		}
		
		public double distance(XY point)
		{
			return 	Math.sqrt(	Math.pow(this.X - point.getX(), 2) +
								Math.pow(this.Y - point.getY(), 2));
		}
		
//		public double distanceToLine(XY a, XY b)
//		{
//			return 	Math.abs((b.X-a.X)*(a.Y-point.Y)-(a.X-point.X)*(b.Y-a.Y)) / 
//					Math.sqrt(Math.pow((b.X-a.X), 2)+ Math.pow((b.Y-a.Y),2));
//		}
		
		public double distanceToLineSegment(XY a, XY b)
		{
			Vect v = new Vect(b).sub(b); 
			Vect x = this.sub(a);
			double s = x.dotProd(v) / v.dotProd(v);
			if(s < 1 && s > 0)
				return this.distance(v.scale(s).add(a));
			else if ( s >= 1)
				return this.distance(b);
			else
				return this.distance(a);
		}
		
		public double perpendicularity(XY a, XY b)
		{
			Vect v = new Vect(b).sub(b); 
			Vect x = this.sub(a);
			double s = x.dotProd(v) / v.dotProd(v);
			return Math.abs(s-0.5);
		}

	}
	
	
	public abstract class Shape {
		Shape (ByteBuffer bb)
		{			
		}
		Shape ()
		{
		}
		
		abstract public boolean inBoundry(XY point);
	}
	
	public class Point extends Shape implements XY{
		final double X, Y;
		
		public double getX() { return X; }
		public double getY() { return Y; }
		
		Point(XY xy) {
			this(xy.getX(), xy.getY());
		}
		
		Point (ByteBuffer bb) throws IOException
		{
			super(bb);
			X = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			Y = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
		}
		Point (double X, double Y)
		{
			this.X = X;
			this.Y = Y;
		}
		
		public boolean inBoundry(XY point)
		{
			return this.X == point.getX() && this.Y == point.getY();
		}
	}
	
	public abstract class CompoundShape extends Shape {
		final double minX, minY, maxX, maxY;

		CompoundShape (ByteBuffer bb) throws IOException
		{
			super(bb);
			minX = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			minY = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			maxX = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
			maxY = Double.longBitsToDouble(Long.reverseBytes(bb.getLong()));
		}
		
		protected boolean inBoundingBox(XY point)
		{
			return 
				maxX >= point.getX() &&
//				minX <= point.getX() && 
				maxY >= point.getY() && 
				minY <= point.getY();
		}
	}
	
	public class MultiPoint extends CompoundShape {
		final Point[] points;
		
		MultiPoint (ByteBuffer bb) throws IOException
		{
			super(bb);
			int count = Integer.reverseBytes(bb.getInt());
			Point[] points = new Point[count];
			for(int i = 0; i < count; i++)
				points[i] = new Point(bb);
			this.points = points;
		}
		
		public boolean inBoundry(XY point)
		{
			if(!inBoundingBox(point))
				return false;
			
			for(Point lPoint : points)
				if(lPoint.inBoundry(point))
					return true;
			
			return false;
		}
	}
	
	public class PolyLine extends CompoundShape {
		final int[] parts;
		final Point[] points;
		
		PolyLine (ByteBuffer bb) throws IOException
		{
			super(bb);
			
			int countParts = Integer.reverseBytes(bb.getInt());
			int countPoints = Integer.reverseBytes(bb.getInt());
			
			int[] parts = new int[countParts];
			for(int i = 0; i < countParts; i++)
				parts[i] = Integer.reverseBytes(bb.getInt());
			
			Point[] points = new Point[countPoints];
			for(int i = 0; i < countPoints; i++)
				points[i] = new Point(bb);
			
			this.parts = parts;
			this.points = points;
		}

		public boolean inBoundry(XY point)
		{
			if(!inBoundingBox(point))
				return false;
			
			return true;
		}
	}
	

	public class Polygon extends PolyLine {
		Polygon (ByteBuffer bb) throws IOException
		{
			super(bb);
		}

		/**
		 * Detects whether given point is within the polygon
		 */
		public boolean inBoundry(XY point)
		{
			if(!inBoundingBox(point))
				return false;

	        double xRatio = 1000 / (maxX - minX);
	        double yRatio = 1000 / (maxY - minY); 
			
			Vect ps[] = new Vect[points.length];
			for(int i = 0; i < points.length; i++)
				ps[i] = new Vect(	(points[i].X-minX)*xRatio,
									(points[i].Y-minY)*yRatio);
			
			Vect p = new Vect(	(point.getX()-minX)*xRatio,
								(point.getY()-minY)*yRatio);
			
			int closest = 1;
			int part = 1;
			for(int i = 2; i < ps.length; i++)
			{
				if(	(part < parts.length) &&
					parts[part] == i)
					part++;
				else if(!ps[i].equals(ps[i-1]))
				{
					double mag = (	p.distanceToLineSegment(ps[i-1],ps[i]) -
									p.distanceToLineSegment(ps[closest-1],ps[closest]));
					if (mag < 0)
					{
						closest = i;
					}
					else if(mag == 0)
					{
						if(	p.perpendicularity(ps[i-1],ps[i]) < 
							p.perpendicularity(ps[closest-1],ps[closest]))
							closest = i;
					}
				}
			}
			
			Vect first = ps[closest-1];
			Vect second = ps[closest];
				
			return ((first.X - second.X)*(p.Y -second.Y) - ((first.Y - second.Y)*(p.X-second.X))) > 0;
			}
	}
	
		final int recordNumber;
		final int shapeSize;
		final ShapeType shapeType;
		final Shape shape;
		
		Record (RandomAccessFile file) throws IOException, ShapeException
		{
			recordNumber = file.readInt();
			shapeSize = file.readInt();
			
			byte[] barray = new byte[shapeSize*2];
			ByteBuffer bb = ByteBuffer.wrap(barray);
			file.read(barray);
			
			shapeType = ShapeType.getShape(Integer.reverseBytes(bb.getInt()));
			
			switch (shapeType)
			{
			case POINT:
				shape = new Point(bb);
				break;
			case POLYLINE:
				shape = new PolyLine(bb);
				break;
			case MULTIPOINT:
				shape = new MultiPoint(bb);
				break;
			case POLYGON:
				shape = new Polygon(bb);
				break;
			default:
				throw new ShapeException("Unhandled shape type: " + shapeType);
			}
			bb.clear();
		}
}
