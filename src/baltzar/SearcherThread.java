package baltzar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Created by studentpoolen on 2016-07-26.
 */
public class SearcherThread extends Thread {

    private String fileURL;
    private MyArrayList<SearcherThread> list;

    public SearcherThread(String fileURL, MyArrayList<SearcherThread> list) { // String searchQuery,
        this.fileURL = fileURL;
        this.list = list;
    }

    public String search() {

        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(fileURL)));
            String s = "";
            String searchQuery = list.getSearchQuery();

            while ((s = br.readLine()) != null) {
                if (s.equals(searchQuery)) {
                    list.setSearchResult(s);
                    return "found";
                } else  {
                    //s.replaceAll("--BL--", "").equals(searchQuery.replaceAll("--BL--", "")

                    String[] ssplit = s.split("\t");
                    String sColValue1 = ssplit[0].replaceAll("--BL--", "");
                    String sColValue2 = ssplit[1].replaceAll("--BL--", "");

                    String[] searchQuerySplit = searchQuery.split("\t");
                    String sqColValue1 = searchQuerySplit[0].replaceAll("--BL--", "");
                    String sqCoLValue2 = searchQuerySplit[1].replaceAll("--BL--", "");

                    if (sColValue1.equals(sqColValue1) && sColValue2.equals(sqCoLValue2)) {
                        list.setSearchResult(s);
                        return "partial_match";
                    }
                }
;            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return "not_found";
    }

    public void run() {
        String s = search();
        if (s.equals("found")) {
            list.setFound(true);
            list.setExistsFully(true);
        } else if (s.equals("not_found")){
            list.tellListImFinished();
            interrupt();
        } else if (s.equals("partial_match")) {
            list.setFound(true);
            list.setExistsPartially(true);
        }
    }

}