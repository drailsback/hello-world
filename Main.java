
/**
 * Driver for the Maze class. Instantiates a maze with specified dimensions. 
 * 
 * Debug mode created a 5x5 maze and shows all steps of generation.
 * 
 * Run GUI can be toggles when debug is false to display a width x depth maze on the console or as an image.
 * Note: GUI only works with square mazes.

 * @author Davis C. Railsback
 * @version 05.30.2018.1
 * 
 */

public class Main {

	public static final int WIDTH = 300;
	public static final int DEPTH = 300;
	public static final boolean DEBUG = false; 
	public static final boolean RUN_GUI = true;
	
	public static void main(String[] args) {
		
		if(DEBUG) 								// 5x5 with debugging console output
			new Maze(5, 5, true, false);
		else if(RUN_GUI) {
			new Maze(WIDTH, WIDTH, false, true);	// width x width with GUI output
		} else 
			new Maze(WIDTH, DEPTH, false, false);	// width x depth with console output
	}
}

