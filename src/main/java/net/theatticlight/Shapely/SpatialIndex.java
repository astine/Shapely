package net.theatticlight.Shapely;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
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
    	
    	TreeNode(DataInputStream file) throws IOException {
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
    	DataInputStream file = new DataInputStream(new FileInputStream(fileBaseName + ".qix"));
    	
    	byte[] sig = new byte[3];
    	file.read(sig);
    	if(!new String(sig).equals("SQT")) {
    		file.close();
    		throw new SpatialIndexException("Wrong file signature.");
    	}
    	
    	int endianness = (int)file.readByte();
    	
    	if(!(file.readByte() == 1)) {
    		file.close();
    		throw new SpatialIndexException("Wrong file version number");
    	}
    	
    	file.skip(3);
    	totalCount = file.readInt();
    	maxDepth = file.readInt();

    	rootNode = new TreeNode(file);
    	file.close();
    }
    
    public List<Integer> getRecordsAtPoint(double X, double Y) {
    	return rootNode.getRecordsAtPoint(X, Y);
    }
}
