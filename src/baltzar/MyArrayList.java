package baltzar;

import java.util.ArrayList;

@SuppressWarnings({ "hiding", "serial" })
public class MyArrayList<SearcherThread> extends ArrayList<SearcherThread> {

    private boolean found;
    private String searchResult;
    private String searchQuery;
    private int threadsCompleted;
    private int totalThreads;
    private boolean existsFully;
    private boolean existsPartially;


    public MyArrayList(int threads) {
        found = false;
        totalThreads = threads;

    }

    public boolean isCompleted() {
        return threadsCompleted == totalThreads;
    }

    public void tellListImFinished() {
        threadsCompleted++;
    }


    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(String searchResult) {
        this.searchResult = searchResult;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String sq) {
        this.searchQuery = sq;
    }

    public boolean getExistsPartially() {
        return existsPartially;
    }

    public void setExistsPartially(boolean existsPartially) {
        this.existsPartially = existsPartially;
    }

    public boolean getExistsFully() {
        return existsFully;
    }

    public void setExistsFully(boolean existsFully) {
        this.existsFully = existsFully;
    }
}