package net.theatticlight.Shapely;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpatialIndex {
	final int maxDepth;
    final int totalCount;
    final TreeNode rootNode;
    
    public static class SpatialIndexException extends Exception {
    	SpatialIndexException(String message) {
    		super(message);
    	}
    }
    
    public class TreeNode {
    	final double minX;
    	final double minY;
    	final double maxX;
    	final double maxY;
    	final int recordCount;
    	final int[] recordIDs;
    	
    	final int subNodeCount;
    	final TreeNode[] subNodes;
    	
    	TreeNode(RandomAccessFile file) throws IOException {
    		int offset = file.readInt();
    		
    		minX = file.readDouble();
    		minY = file.readDouble();
    		maxX = file.readDouble();
    		maxY = file.readDouble();
    		recordCount = file.readInt();
    		recordIDs = new int[recordCount];
    		for (int i = 0; i < recordCount; i++)
    			recordIDs[i] = file.readInt() + 1;

    		subNodeCount = file.readInt();
    		subNodes = new TreeNode[subNodeCount];
    		for (int i = 0; i < subNodeCount; i++)
    			subNodes[i] = new TreeNode(file);
    	}
    	
    	public boolean inBoundry(double X, double Y) {
    		return 	X > minX &&
    				X < maxX &&
    				Y > minY &&
    				Y < maxY;
    	}
    	
    	public List<Integer> getRecordsAtPoint(double X, double Y) {

    		if(!inBoundry(X,Y))
    			return new ArrayList<Integer>();
    		
    		List<Integer> records = new ArrayList<Integer>(recordCount);

    		for(int rec: recordIDs)
    			records.add(rec);
    		
    		for(TreeNode node: subNodes)
    			records.addAll(node.getRecordsAtPoint(X, Y));
    		
    		return records;
    	}
    }
    
    SpatialIndex (String fileBaseName) throws IOException, SpatialIndexException {
    	RandomAccessFile file = new RandomAccessFile(fileBaseName + ".qix", "r");
    	
    	file.seek(0);
    	byte[] sig = new byte[3];
    	file.read(sig);
    	if(!new String(sig).equals("SQT"))
    		throw new SpatialIndexException("Wrong file signature.");
    	
    	int endianness = (int)file.readByte();
    	
    	if(!(file.readByte() == 1))
    		throw new SpatialIndexException("Wrong file version number");
    	
    	file.seek(8);
    	totalCount = file.readInt();
    	maxDepth = file.readInt();
    	
    	rootNode = new TreeNode(file);
    }
    
    public List<Integer> getRecordsAtPoint(double X, double Y) {
    	return rootNode.getRecordsAtPoint(X, Y);
    }
  
    public static void main (String[] args)
    {
    	try
    	{
    		SpatialIndex si = new SpatialIndex("resources/tz_world");
    		System.out.println("Max Depth: " + si.maxDepth);
    		System.out.println("Total Count: " + si.totalCount);
    		System.out.println("Bounds: " + si.rootNode.minX + ", " + si.rootNode.minY + ", " + si.rootNode.maxX + ", " + si.rootNode.maxY);
    		List<Integer> records = si.getRecordsAtPoint(0.0106, 51.4788);
    		for(Integer record: records)
    			System.out.println("record: " + record);
    	}
    	catch (Exception e)
    	{
    		System.out.println(e.getMessage());
    	}
    	
    }
}
