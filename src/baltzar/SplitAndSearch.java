package baltzar;

import baltzar.CompareAndCleanThreads.Holder;
import java.util.*;
import java.io.*;

public class SplitAndSearch {


    private static MyArrayList<SearcherThread> list;
    private static boolean threadsInitiated = false;
    private static String partsDir;

    public static Object[] splitAndSearch(String fileURL, String dir, String searchQuery, int numberOfColumns) {

        final int NUMBER_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();
        split(fileURL, dir, NUMBER_OF_PROCESSORS, numberOfColumns);
        return search(fileURL, dir, searchQuery, NUMBER_OF_PROCESSORS);
    }

    public static void split(String fileURL, String dir, final int NUMBER_OF_PROCESSORS, int numberOfColumns) {

        BufferedWriter bw = null;
        BufferedReader br = null;

        try {

            partsDir = dir + "parts/";
            Runtime.getRuntime().exec("mkdir " + partsDir);

            LineNumberReader lnr = new LineNumberReader(new FileReader(new File(fileURL)));
            lnr.skip(Long.MAX_VALUE);
            lnr.close();

            int lines = lnr.getLineNumber();

            br = new BufferedReader(new FileReader(new File(fileURL)));
            br.readLine(); // to get rid of column headers

            bw = new BufferedWriter(new FileWriter(new File(dir + "/parts/" + 1)));

            int linesPerFile = lines / NUMBER_OF_PROCESSORS;

            int switchToNextFileThreshold = linesPerFile;
            int writeToFileNumber = 1; // begins at 1 --> n
            boolean lastFileReached = false;
            String s = "";
            StringBuilder builder = new StringBuilder();

            for (int i = 0; (i <= lines && s != null); i++) {
                if (i >= switchToNextFileThreshold) {
                    writeToFileNumber += (writeToFileNumber < NUMBER_OF_PROCESSORS) ? 1 : 0; // last file longer
                    switchToNextFileThreshold = linesPerFile * writeToFileNumber;

                    if (writeToFileNumber < NUMBER_OF_PROCESSORS) {
                        bw.close();
                        bw = new BufferedWriter(new FileWriter(new File(dir + "/parts/" + writeToFileNumber)));
                    } else if (writeToFileNumber == NUMBER_OF_PROCESSORS && lastFileReached == false) {
                        lastFileReached = true;
                        bw.close();
                        bw = new BufferedWriter(new FileWriter(new File(dir + "/parts/" + writeToFileNumber)));
                    }
                }
                if ((s = br.readLine()) != null) {
                    while (s.equals("")) s = br.readLine(); // to get rid of blanks
                    bw.write(s + "\n");
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (bw != null) bw.close();
                if (br != null) br.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }


    private static void initiateThreads(int NUMBER_OF_PROCESSORS, String dir) {

        if (threadsInitiated == false) {
            list = new MyArrayList<SearcherThread>(NUMBER_OF_PROCESSORS);
            for (int i = 1; (i <= NUMBER_OF_PROCESSORS); i++) {
                SearcherThread st = new SearcherThread(partsDir + i+"", list); // i is the filename of the parts
                list.add(st);
            }
            threadsInitiated = true;
        }

    }

    private static Object[] search(String fileURL, String dir, String searchQuery, final int NUMBER_OF_PROCESSORS) {

        if (!threadsInitiated) initiateThreads(NUMBER_OF_PROCESSORS, dir);

        list.setExistsFully(false);
        list.setExistsPartially(false);
        list.setSearchResult("");
        list.setSearchQuery(searchQuery);
        for (SearcherThread st : list) {
            st.run();
        }

        boolean found = list.isFound();
        boolean completed = list.isCompleted();
        while (!found && !completed) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (found) {
            for (SearcherThread st : list) {
                st.interrupt();
            }
        }
        if (completed && !found) {
            list.setSearchResult("not_found");
            list.setExistsPartially(false);
            list.setExistsFully(false);
        }


        Object[] ret = new Object[3];
        ret[0] = list.getSearchResult();
        ret[1] = list.getExistsFully();
        ret[2] = list.getExistsPartially();

        return ret;
    }

    // public static void main(String[] args) {
    //
    // String fileURL = "/Users/baltzarmattsson/txt/nyaservicescleaned.txt";
    // String dir = "/Users/baltzarmattsson/txt/";
    // //String searchQuery = "cybercom-mmaweb2 Up time service-desk 3 0h 5m 0s
    // 0h 1m 0s check_nrpe!check_uptime 24x7 Yes Yes Yes Yes 0h 10m 0s 0h 0m 0s
    // 24x7 Yes Yes Program-wide value Program-wide value Yes ";
    // String searchQuery = "hejsan";
    //
    // splitAndSearch(fileURL, dir, searchQuery);
    //
    // //splitAndSearch("/home/studentpoolen/Documents/Baltzar/tmp/nyaservicescleaned.txt",
    // "/home/studentpoolen/Documents/Baltzar/tmp", "surchqueryhurhr");
    // }

}

// 1. Dela upp nya filen i [number_of_processors] filer - utan columnheaders
// 2. Gör [number_of_processors] st trådar som går igenom respektive textfil
// 3. Ifall en tråd hittar searchQuery säger den till de andra trådarna att
// sluta leta
// 3.2 Samt returnerar String, själva jämförelsen gör vi i huvudprogrammet,
// trådarna letar bara upp respektive grej