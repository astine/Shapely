Shapely
=======

Shapely is a small minimal Java library for querying ESRI [shapefiles](http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf).
It's not a full shapefile, editing and manipulation tool and it's intended purpose is merely to see whether a given
point is with a given shape. I wrote it with the intent of computing the timezones for a given IP address using the 
[tz_world](http://efele.net/maps/tz/world/) shape files.

*Usage*

Usage is simple. Include the jar and import the class ShapeFile.

    import net.theatticlight.Shapely.ShapeFile;

Then you can load a shapefile and query it for info:

	ShapeFile shapeFile = new ShapeFile("/path/to/shapefile/base/"); 
	List<Object> value = shapeFile.getInfoAtPoint(new Record.Vect(x,y), "field name");
	System.out.println(value.get(0));
	
*Swing App*

If run as an application with a base path and a field name as arguments, the ShapeFile class will run a simple
swing application which will display a map of the entire shapefile. This application is meant primarily for sanity
checks.

*Limitations*

Shapely is not a full shapefile processing library. [GeoTools](http://www.geotools.org) is a better choice if you need a complete
shapefile API. Shapely is only meant for read only queries against extant shapefiles. 

In addition, these limitations apply:
* Shapely currently does not support more than two dimensions.
* Shapely might not work correctly on little endian systems.
* Shapely only reads the .shp .shx .dbf and .qix files associated with a given shapefile

*Author/Contact*
The author of the code is Andrew Stine. 
He can be reached at stine dot drew at theatticlight dot net or at his [website](http://www.theatticlight.net). 
	
