import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class HashSearch {
	
	private static boolean initialized = false;
	private static HashMap<String, String> newFileMap;

	public static Object[] SearchHashmap(String fileURL, String dir, String searchQuery, int numberOfColumns) {
		
		if (initialized == false) initialize(fileURL, dir, searchQuery, numberOfColumns);
		
		return search(searchQuery);
	}
	
	private static void initialize(String fileURL, String dir, String searchQuery, int numberOfColumns) {

		/** 	Entries are saved as key: "host + {tab} + description"	**/
		newFileMap = new HashMap<String, String>();

		BufferedReader br;

		try {
			br = new BufferedReader(new FileReader(new File(fileURL)));
			br.readLine(); //get rid of column names
			
			String s = "";
			while ((s = br.readLine()) != null) {
				String[] sSplit = s.replaceAll(" --BL--", "").split("\t");
				String colOne = sSplit[0].trim();
				String colTwo = sSplit[1].trim();
				newFileMap.put(colOne + "\t" + colTwo, s);
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		initialized = true;
	}
	
	private static Object[] search(String searchQuery) {
		
		String[] sqSplit = searchQuery.replaceAll(" --BL--", "").split("\t");
		String hostQuery = sqSplit[0].trim();
		String descQuery = sqSplit[1].trim();
		String key = hostQuery + "\t" + descQuery;

		boolean existsFully = false;
		boolean existsPartially = false;
		
		String value = "";
		if (newFileMap.get(key) != null) {
			value = newFileMap.get(key);
			if (searchQuery.equals(value)) {
				existsFully = true;
			} else {
				existsPartially = true; // since only the host + description is equal in both files, the rest must be investigated
			}			
		}
		
		Object[] ret = new Object[3];
		
		ret[0] = value;
		ret[1] = existsFully;
		ret[2] = existsPartially;
		
		return ret;
	}

}
