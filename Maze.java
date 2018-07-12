import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * A class that randomly generates, displays, and traverses an m x n maze with no cycles.
 * Generation is done with Kruskal's algorithm, where the weight of each edge is generated randomly.
 * 
 * Some GUI Code borrowed from or heavily inspired by online resources: avaliable from:
 * https://www.dyclassroom.com/image-processing-project/how-to-create-a-random-pixel-image-in-java
 * 
 * @author Davis C. Railsback
 * @version 05.30.2018.1
 * 
 */
public class Maze {
	
	private GraphNode[][] maze;					// nodes of graph 
	private Map<Set<GraphNode>, Edge> edgeMap;	// edges between nodes
	private int width;							
	private int depth;
	
	/**
	 * A Graph node that contains its name, printable symbol, knowledge of whether it has been visited,
	 * and a list of its neighbors.
	 */
	private class GraphNode {


		private boolean visited;
		private Integer name;				// name is an integer to work with Kruskal's algorithm
		private GraphNode[] adjacencyList;	// only 4 possible adjacent nodes
		private String symbol;
		
		/**
		 * constructs a new unvisited node without a symbol
		 * @param name - the name of the node
		 */
		private GraphNode(Integer name) {
			 visited = false;
			 this.name = name;
			 symbol = "  ";
			 adjacencyList = new GraphNode[4];
		}
		
		/**
		 * Displayable graph node - name and adjacency list are shown.
		 */
		public String toString() {

			String ret = "Node: " + name;
			ret+= "\nAdjacencyList: ";
			if(adjacencyList[0] != null)
				ret += adjacencyList[0].name + ", ";
			else ret += "null, ";
			if(adjacencyList[1] != null)
				ret += adjacencyList[1].name + ", ";
			else ret += "null, ";
			if(adjacencyList[2] != null)
				ret += adjacencyList[2].name + ", ";
			else ret += "null, ";
			if(adjacencyList[3] != null)
				ret += adjacencyList[3].name;
			else ret += "null";
			
			return ret;
		}
		
	}

	/**
	 * An Edge is a set of two graph nodes (the endpoints), a weight (to be generated randomly) , and a printable symbol
	 */
	private class Edge implements Comparable<Edge>{

		
		Set<GraphNode> endpoints; // nodes connected by this edge
		Integer weight;			  // priority of the edge for adding
		String symbol;				
		
		/**
		 * Constructs a new edge with the symbol X, two given endpoints, and a weight
		 * @param endpoints
		 * @param weight
		 */
		private Edge(Set<GraphNode> endpoints, Integer weight) {
			
			symbol = "X ";
			this.endpoints = endpoints;
			this.weight = weight;
		}
		
		/**
		 * An edge is greater than another edge if it has a higher weight
		 */
		public int compareTo(Edge other) {
			if(this.weight > other.weight) return 1;
			if(this.weight < other.weight) return -1;
			return 0;
		}
		
		/** 
		 * Displayable version of an edge -- the names of the endpoints and the weight are shown
		 */
		public String toString() {
			return "Edge: " + ((GraphNode) endpoints.toArray()[0]).name + 
					" to "  + ((GraphNode) endpoints.toArray()[1]).name + 
					" ; Weight: " + weight + "\n";
		}
	}

	/**
	 * A Root Pointing Node is a node with a single connection. By default, a root pointing node points to itself.
	 */
	private class RootPointingNode {
		
		private Integer name;				// the graphNode the root pointing node is meant to reference
		private RootPointingNode parent;	// the next member of the set the root pointing node belongs to
		
		/**
		 * Constructs a new root pointing node with a name and a pointer to itself.
		 * @param name
		 */
		private RootPointingNode(Integer name) {
			this.name = name;
			parent = this;
		}
	}

	/**
	 * Constructs a new maze through a randomized version of Kruskal's algorithm, finds the shortest traversal path, and
	 * displays the maze with the path traced through it. If debug is true, displays the maze at each phase of generation. 
	 * @param width
	 * @param depth
	 * @param debug
	 */
	public Maze(int width, int depth, boolean debug, boolean runGUI) {
	
		this.width = width;		// maze dimensions
		this.depth = depth;

		maze = initMazeAsGraph();		// instantiates m*n graph nodes and establishes their adjacency lists
		generateSpanningTree(debug);	// randomly generates a list of edges that span all nodes			 

		// starting from the first node, finds the minimal path to the final node
		Stack<GraphNode> shortestPath = new Stack<GraphNode>();	
		GraphNode current = maze[0][0];
		findPath( shortestPath, current );

		if(runGUI) 
			mazeGUI();
		else
			display();

	}
	
	/**
	 * Searches through maze until it finds the shortest path.
	 * @param shortestPath		-- a stack of sequential nodes
	 * @param current			-- the current node being observed
	 */
	private void findPath( Stack<GraphNode> shortestPath, GraphNode current ) {
		
		current.visited = true;			// marks the current node as visited
		shortestPath.push(current);		// adds current node to the stack (recorded traversal path)
		
		if(current.equals( maze[depth-1][width-1]) ) {	// when the end of the maze has been found,
			while(!shortestPath.isEmpty()) {			// marks all nodes in the current recorded traversal path  
				shortestPath.pop().symbol = "@ ";		// with a symbol that shows they are essential nodes in the solution path
			}
			return;
		}

		for(GraphNode g : current.adjacencyList) {		// for each unvisited direction that is currently available:
			if(g != null && !g.visited) {				
				
				findPath( shortestPath, g );	// check if it leads to the end of the maze
				
				if(!shortestPath.isEmpty()) shortestPath.pop(); // if it doesn't, remove it from the recorded path and try the next one. 
			} 
		}
		
	}
	
	/**
	 * Makes a priority queue for of edges with random weights and adds them to the spanning tree
	 * as long as they don't create a cycle.
	 * @param debugging
	 */
	private void generateSpanningTree(boolean debugging) {

		PriorityQueue<Edge> edgeList = new PriorityQueue<Edge>(); // temporary structure contains the edges before adding
		Map<Integer, RootPointingNode> rootPointingForest = new HashMap<Integer, RootPointingNode>();	// root pointing forest for union find
		ArrayList<Edge> spanningTree = new ArrayList<Edge>();	// the edges of the final maze
		Random ran = new Random();	// for generating random weights
		
		edgeMap = new HashMap<Set<GraphNode>, Edge>(); // map to locate an edge based on its end points
	
		int randomWeight = 0;
		
		for(int i=0; i<depth; i++) {		// for each graph node
			for(int j=0; j<width; j++) {
				
				// add a root pointing node to the forest referenced by its name (setup for union find)
				rootPointingForest.put(width*i+j, new RootPointingNode(width*i+j));	
				
				for(int k=0; k<4; k++) {		// for every slot in each node's adjacency list
					
					// in a pattern of tesselated crosses (to skip adding duplicate edges) ... 
					if(maze[i][j].adjacencyList[k] != null && (j%2==0 && i%2==1 || j%2==1 && i%2==0)) { 
						
						Set<GraphNode> temp = new HashSet<GraphNode>();	// make a set of two nodes
						temp.add(maze[i][j]);
						temp.add(maze[i][j].adjacencyList[k] );
						randomWeight = ran.nextInt(1000);				// generate a random weight
						edgeList.add( new Edge( temp , randomWeight ));	// make a new edge and add it to the priority queue
						edgeMap.put(temp , new Edge( temp , randomWeight ));	// and the map
					}
				}
			}
		}
		if(debugging) display(); // if debugging is on, shows maze before construction
			
		while(!edgeList.isEmpty()) {	
			
			Edge current = edgeList.poll();									// removes an edge from priority queue
			GraphNode node1 = (GraphNode) current.endpoints.toArray()[0];	// saves its endpoints in temp variables 
			GraphNode node2 = (GraphNode) current.endpoints.toArray()[1];	
			
			Integer root1 = getRootNode(rootPointingForest.get(node1.name));	// finds the root of each node in the
			Integer root2 = getRootNode(rootPointingForest.get(node2.name));	// root-pointing forest
			
			if(root1 != root2) {					// as long as the nodes are not in the same tree in the root-pointing forest:
				
				spanningTree.add(current);			// adds the edge between the two nodes to the maze
				
				rootPointingForest.get(root1).parent = rootPointingForest.get(root2);	// joins the trees in the root-pointing forest
			
				edgeMap.get(current.endpoints).symbol = "  ";	// marks the edge between the two nodes as blank (for printing)
				node1.symbol = "V ";							
				node2.symbol = "V ";		// labels the two nodes as 'visited' - for display in debugging
				
				if(debugging) display();	// shows the new maze after each edge is added (during debugging only)
				
			} else {
				
				// if two nodes are already part of the tree in the root pointing forest, removes them from each other's 
				// adjacency lists ( because there is guaranteed to be a wall between these two nodes )
				for(int i=0; i<4; i++) {
					if(node1.adjacencyList[i] != null && node1.adjacencyList[i].equals(node2))
						node1.adjacencyList[i] = null;
					if(node2.adjacencyList[i] != null && node2.adjacencyList[i].equals(node1))
						node2.adjacencyList[i] = null;
				}
			}
		}
		
		// resets the symbol of all nodes in the maze to blank for later display
		for(int i=0; i<depth; i++)
			for(int j=0; j<width; j++)
				maze[i][j].symbol = "  ";
	}

	/**
	 * Finds the root node in a root-pointing tree.
	 * @param p
	 * @return - the name of the root node
	 */
	private Integer getRootNode(RootPointingNode p) {
		
		while(p.parent != p) {		// traverses upward through the tree until it finds a node pointing to itself
			p = p.parent;
		}
		return p.name;		// returns the name of that node
	}
	
	/**
	 * Makes a 2-D array of graph nodes connected to each other if they are directly adjacent
	 * @return
	 */
	private GraphNode[][] initMazeAsGraph() {


		GraphNode[][] ret = new GraphNode[depth][width];
		
		// makes preliminary graph with maze dimensions (depth x width)
		for(int i=0; i<depth; i++) {
			for(int j=0; j<width; j++) {
				ret[i][j] = new GraphNode(width*i+j);	// names based on their row+column position
			}
		}
		
		// connects all vertices to adjacent vertices
		for(int i=0; i<depth; i++) {
			for(int j=0; j<width; j++) {
				if(j>0) ret[i][j].adjacencyList[0] = ret[i][j-1];
				if(i>0) ret[i][j].adjacencyList[1] = ret[i-1][j];
				if(j<width-1) ret[i][j].adjacencyList[2] = ret[i][j+1];
				if(i<depth-1) ret[i][j].adjacencyList[3] = ret[i+1][j];
			}
		}
		return ret;
	}

	/**
	 * Displays graphical version of maze in a JFrame
	 */
	public void mazeGUI() {
		
		char[][] colors = mazeToColorArray();	// converts maze data into characters denoting how to color pixels
		int w = colors.length;
		int h = colors[0].length;
	
		int greyScale = 0;
		int[] flattenedData = new int[3*w*h];	// one-D arrray to store image data
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);	// RGB image template
		int ind = 0;	// row index
		for (int i=0; i<w; i++) {
		    for (int j=0; j<h; j++)
		    {
		    	
		    	if(colors[i][j] == 'W' || colors[i][j] == 'B') {	
		    		greyScale = colors[i][j] == 'W' ? 255 : 0;
		    		
		    		// assigns walls and corridors white & black pixels
		        	flattenedData[ind + j*3] = greyScale;
		        	flattenedData[ind + j*3+1] = greyScale;
		        	flattenedData[ind + j*3+2] = greyScale;
		    	}
		    	
		    	else {
		    	
		    		// assigns the solution path with colored pixels
		    		flattenedData[ind + j*3] = 255;
		        	flattenedData[ind + j*3+1] = 0;
		        	flattenedData[ind + j*3+2] = 255;
		    	}
		      }
		    ind += 3*h;
		}       

		// constructs image with image data and dimensions w, h
		img.getRaster().setPixels(0, 0, w, h, flattenedData);

		// configures a JPanel to display image
		JLabel jLabel = new JLabel(new ImageIcon(img));
		JPanel jPanel = new JPanel();
		jPanel.add(jLabel);
		
		JFrame r = new JFrame();
		r.setSize(3*width, 5*depth/2);
		r.add(jPanel);
		r.setVisible(true);	
	}
	
	/**
	 * Turns maze into an array of color information.
	 * White = ground
	 * Black = wall
	 * Color = solution path
	 * @return
	 */
	public char[][] mazeToColorArray() {
		
		char[][] colorArray = new char[2*depth+1][2*width+1];
		
		// temporary set for finding the symbol of an edge given two adjacent nodes
		HashSet<GraphNode> temp= new HashSet<GraphNode>(); 
		
		for(int i=0; i<2*depth+1; i++) {
			for(int j=0; j<2*width+1; j++) {
				if(i==0) {								// adds B's on first row, except for the entrance
					colorArray[i][j] = j==1 ? 'R' : 'B';
				} else if(i==2*depth) {					// adds B's on last row, except for the exit
					colorArray[i][j] = j==2*width-1 ? 'R' : 'B';
				} else if(i%2==0 && j%2==0 || j==0 || j==2*width) {	// adds B's on side of the perimeter and in between edges
					colorArray[i][j] = 'B';
				} else if(i%2==1 && j%2==1) {				// adds the node's symbol in the center of each 'cell'
					colorArray[i][j] = maze[(i-1)/2][(j-1)/2].symbol.equals("  ") ? 'W' : 'R';
				} else if(i%2==1) {							// adds the symbol of horizontal edges between nodes
					temp.add(maze[(i-1)/2][(j-1)/2]);
					temp.add(maze[(i-1)/2][(j+1)/2]);
					if(maze[(i-1)/2][(j-1)/2].symbol.equals("@ ") && 
					   maze[(i-1)/2][(j+1)/2].symbol.equals("@ ") &&
					   edgeMap.get(temp).symbol.equals("  ")) {
						colorArray[i][j] = 'R';
					} else {
						colorArray[i][j] = edgeMap.get(temp).symbol.equals("  ") ? 'W' : 'B';
						
					}
					temp = new HashSet<GraphNode>();
				} else {									// adds the symbol of vertical edges between nodes
					temp.add(maze[(i-1)/2][(j-1)/2]);
					temp.add(maze[(i+1)/2][(j-1)/2]);
					if(maze[(i-1)/2][(j-1)/2].symbol.equals("@ ") && 
					   maze[(i+1)/2][(j-1)/2].symbol.equals("@ ") &&
					   edgeMap.get(temp).symbol.equals("  ")) {
						colorArray[i][j] = 'R';
					} else {						
						colorArray[i][j] = edgeMap.get(temp).symbol.equals("  ") ? 'W' : 'B';
						
					}
					temp = new HashSet<GraphNode>();
				}
			}
		}
		
		return colorArray;
	}
	
	/**
	 * Converts a maze into a string for display
	 */
	public String toString() {

		// temporary set for finding the symbol of an edge given two adjacent nodes
		HashSet<GraphNode> temp= new HashSet<GraphNode>(); 
		
		String ret = "";
		for(int i=0; i<2*depth+1; i++) {
			for(int j=0; j<2*width+1; j++) {
				if(i==0) {								// prints X's on first row, except for the entrance
					ret += j==1 ? "  " : "X ";
				} else if(i==2*depth) {					// prints X's on last row, except for the exit
					ret += j==2*width-1 ? "  " : "X ";
				} else if(i%2==0 && j%2==0 || j==0 || j==2*width) {	// prints X's on side of the perimeter and in between edges
					ret += "X ";
				} else if(i%2==1 && j%2==1) {				// prints the node's symbol in the center of each 'cell'
					ret += maze[(i-1)/2][(j-1)/2].symbol;
				} else if(i%2==1) {							// prints the symbol of horizontal edges between nodes
					temp.add(maze[(i-1)/2][(j-1)/2]);
					temp.add(maze[(i-1)/2][(j+1)/2]);
					ret += edgeMap.get(temp).symbol;
					temp = new HashSet<GraphNode>();
				} else {									// prints the symbol of vertical edges between nodes
					temp.add(maze[(i-1)/2][(j-1)/2]);
					temp.add(maze[(i+1)/2][(j-1)/2]);
					ret += edgeMap.get(temp).symbol;
					temp = new HashSet<GraphNode>();
				}
			}
			ret += "\n";	// adds a newline after each row
		}
		return ret;
	}
	
	/**
	 * displays the maze object
	 */
	public void display() {

		System.out.println(this); // calls toString
	}
}
