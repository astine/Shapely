package net.theatticlight.Shapely;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.theatticlight.Shapely.Record.CompoundShape;
import net.theatticlight.Shapely.Record.Point;
import net.theatticlight.Shapely.Record.Polygon;
import net.theatticlight.Shapely.Record.ShapeException;
import net.theatticlight.Shapely.Record.ShapeHeader;
import net.theatticlight.Shapely.SpatialIndex.SpatialIndexException;

//import org.joda.time.DateTime;

import com.hexiong.jdbf.DBFReader;
import com.hexiong.jdbf.JDBFException;
import com.hexiong.jdbf.JDBField;

public class ShapeFile {
	
	
	public static class Index {
		final int[] index; 
		Map<Integer,Record> cache = new WeakHashMap<Integer,Record>();
		
		Index (String filePath) throws FileNotFoundException, IOException
		{
			RandomAccessFile indexFile = new RandomAccessFile(filePath, "r");
			indexFile.seek(100); //Skip header
			
			int numRecords = (int)((indexFile.length()-100)/8);
			index = new int[numRecords];
			
			for(int i = 0; i < index.length; i++)
			{
				index[i] = indexFile.readInt() * 2;	//Keep the offset
				indexFile.readInt();				//Toss the length
			}
			indexFile.close();
		}
		
		public int getOffset(int index)
		{
			return this.index[index-1];
		}
		
		public Record getRecord(RandomAccessFile file, int recordNumber) throws IOException, ShapeException
		{
			Integer key = new Integer(recordNumber);
			if(cache.containsKey(key))
				return cache.get(new Integer(recordNumber));
			
			file.seek(getOffset(recordNumber));
			Record record = new Record(file);
			cache.put(key, record);
			return record;
		}
	}
	
	public static class GridIndex {
		private class GridKey {
			final int x, y;
			GridKey (double x, double y)
			{
				this.x = (int)Math.round(x);
				this.y = (int)Math.round(y);
			}
		}
		
		final Map<GridKey,Set<Integer>> gridIndex;
		
		void addMember(GridKey key, Integer number)
		{
			if(!gridIndex.containsKey(key))
				gridIndex.put(key, new HashSet<Integer>());

			gridIndex.get(key).add(number);
		}
		
		GridIndex(ShapeFile shapeFile) throws ShapeException, IOException
		{
			gridIndex = new HashMap<GridKey,Set<Integer>>();
			for(int i = 1;i < shapeFile.getRecordCount()+1; i++)
			{
				Record record = shapeFile.getRecord(i);
				Integer recordNumber = new Integer(record.recordNumber);
				switch(record.shapeType)
				{
				case POINT:
					Point point = (Point)record.shape;
					addMember(new GridKey(point.X, point.Y), recordNumber);
					break;
				case POLYLINE:
				case POLYGON:
				case MULTIPOINT:
					CompoundShape shape = (CompoundShape)record.shape;
					addMember(new GridKey(shape.minX, shape.minY), recordNumber);
					addMember(new GridKey(shape.maxX, shape.minY), recordNumber);
					addMember(new GridKey(shape.minX, shape.maxY), recordNumber);
					addMember(new GridKey(shape.maxX, shape.maxY), recordNumber);
				}
			}
		}
		
		public Set<Integer> shapesForGrid(double x, double y)
		{
			return gridIndex.get(new GridKey(x, y));
		}
	}
	
	public static class Descriptors {
		final List<Object[]> descriptors;
		final Map<String,Integer> fields;
		
		Descriptors (String filePath) throws FileNotFoundException, IOException, JDBFException
		{
			DBFReader dbfr = new DBFReader(filePath);
			
			descriptors = new ArrayList<Object[]>();
			while(dbfr.hasNextRecord())
				descriptors.add(dbfr.nextRecord());
			
			fields = new HashMap<String,Integer>();
			for(int i = 0; i < dbfr.getFieldCount(); i++)
			{
				JDBField field = dbfr.getField(i);
				fields.put(field.getName(), i);
			}
			dbfr.close();
		}
		
		public Object getShapeInfo(int recordNumber, String fieldName)
		{
			return descriptors.get(recordNumber-1)[fields.get(fieldName).intValue()];
		}
	}

	private final RandomAccessFile shapeFile;
	private final Index index;
	private final Descriptors shapeInfo;
	private final ShapeHeader header;
	private final SpatialIndex spatialIndex;
	
	public ShapeFile(String filePath) throws FileNotFoundException, IOException, JDBFException, ShapeException, SpatialIndexException {
		shapeFile = new RandomAccessFile(filePath + ".shp", "r");
		header = new ShapeHeader(shapeFile);
		index = new Index(filePath + ".shx");
		shapeInfo = new Descriptors(filePath + ".dbf");
		spatialIndex = new SpatialIndex(filePath);
	}
	
	public int getRecordCount() {
		return index.index.length;
	}
	
	public Record getRecord(int recordNumber) throws IOException, ShapeException {
		return index.getRecord(shapeFile, recordNumber);
	}
	
	public Object getInfo(int recordNumber, String field){
		return shapeInfo.getShapeInfo(recordNumber, field);
	}
	
	public List<Record> getRecordsAtPoint(Record.XY xy) throws IOException, ShapeException {
		List<Integer> ids = spatialIndex.getRecordsAtPoint(xy.getX(), xy.getY());
		List<Record> records = new ArrayList<Record>();
		
		for(Integer id: ids) {
			Record record = getRecord(id);
			if(record.shape.inBoundry(xy))
				records.add(record);
		}

		return records;
	}
	
	public List<Object> getInfoAtPoint(Record.XY xy, String field) throws IOException, ShapeException {
		List<Record> records = getRecordsAtPoint(xy);
		List<Object> infos = new ArrayList<Object>();
		for(Record record: records)
			infos.add(getInfo(record.recordNumber, field));
		
		return infos;
	}
	
	public int getFileSize() {
		return header.fileSize;
	}
	
	public int getVersion() {
		return header.version;
	}
	
//	final ShapeType shapeType;
	
	public double[] getBounds() {
		return new double[]{header.minX,header.minY,header.maxX,header.maxY};
	}

	public double[] getZBounds() {
		return new double[]{header.minZ,header.maxZ};
	}
	
	public double[] getMBounds() {
		return new double[]{header.minM,header.maxM};
	}
}
