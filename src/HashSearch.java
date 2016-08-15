import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HashSearch {
	
	private static boolean initialized = false;
	private static boolean previousInitialized = false;
	private static HashMap<String, String> newFileMap;
	private static ArrayList<HashMap<String, String>> prevDiffs = new ArrayList<HashMap<String, String>>();

	public static Object[] SearchHashmap(String fileURL, String dir, String searchQuery, int numberOfColumns, ArrayList<String> nagiosDOCgetURL) {
		
		if (initialized == false) initialize(fileURL, dir, searchQuery, numberOfColumns);
		if (previousInitialized == false && nagiosDOCgetURL.size() > 0) initializePrevious(nagiosDOCgetURL);
		
		return search(searchQuery);
	}

	private static void initializePrevious(ArrayList<String> urls) {

		EraserThread et = new EraserThread("[?] Enter password for confluence: ");
		Thread mask = new Thread(et);
		mask.start();

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String password = "";

		try {
			password = in.readLine();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		// stop masking
		et.stopMasking();

		String tempFile = "/tmp/nag/temp.doc";
		
		for (String url : urls) {
			try {
				Runtime.getRuntime().exec("curl --user bamat1:" + password + " " + url + " -o " + tempFile);
				Thread.sleep(2000);
				
				// Checking if comments exist in previous diffs
				prevDiffs.add(GetDiffComments.getComment(tempFile));
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		previousInitialized = true;
		
		//test TODO
//		HashMap<String, String> m = prevDiffs.get(0);
//		for (Map.Entry<String, String> map : m.entrySet()) {
//			System.out.println(map.getKey() + " _ " + map.getValue());
//		}
//		
		//test
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
		String previousComment = "";
		
		if (newFileMap.get(key) != null) {
			value = newFileMap.get(key);
			if (searchQuery.equals(value)) {
				existsFully = true;
			} else {
				existsPartially = true; // since only the host + description is equal in both files, the rest must be investigated
			}			
		}
		for (HashMap<String, String> map : prevDiffs) {
			if (key.contains("mobill-sql2")) {
				String s = map.get(key);
				key.length();
			}
			if (map.get(key) != null && !map.get(key).equals("")) {
				System.out.println(map.get(key));
				previousComment += map.get(key) + ", ";
			}
		}
		
		Object[] ret = new Object[4];
		
		ret[0] = value;
		ret[1] = existsFully;
		ret[2] = existsPartially;
		if (previousComment.length() > 2) previousComment = previousComment.substring(0, previousComment.length()-2); // to get rid of ", "
		ret[3] = previousComment;
		
		return ret;
	}

}
