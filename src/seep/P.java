package seep;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class P {
	//Properties object
	private static Properties globals = new Properties();
	
	//Method to get value doing: Main.valueFor(key) instead of Main.globals.getProperty(key)
	public static String valueFor(String key){
		return globals.getProperty(key);
	}
	
	//Load properties from file
	public boolean loadProperties(){
		
		boolean success = false;
		try {
			globals.load(new FileInputStream("config.properties"));
			success = true;
		}
		catch (FileNotFoundException e1) {
			System.out.println("Properties file not found "+e1.getMessage());
			e1.printStackTrace();
		}
		catch (IOException e1) {
			System.out.println("While loading properties file "+e1.getMessage());
			e1.printStackTrace();
		}
		//LOAD RUNTIME VAR GLOBALS FROM FILE HERE
		//#######################################
		return success;
	}
}
