package gitlet;


import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;

/**
 * Represents a gitlet commit object.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author Tsrigo
 */
public class Commit implements Serializable {
    /**
     * The message of this Commit.
     */
    private final String message;
    /**
     * The timestamp of the Commit.
     */
    private final String timestamp;

    /**
     * The reference to the files SHA-1.
     */
    private HashMap<String, String> ids;
    /**
     * The reference to parent. SHA-1 of the parent commit are stored in it.
     */
    private String firstParent, secondParent;
    /**
     * The SHA-1 of the commit.
     */
    private String sha;

    public Commit(String message, String timestamp) {
        this.message = message;
        this.timestamp = timestamp;
        ids = new HashMap<>();
        firstParent =  null;
        secondParent = null;
    }

    public Commit(HashMap<String, String> ids, String message, String timestamp) {
        this.ids = new HashMap<>(ids);
        this.message = message;
        this.timestamp = timestamp;
    }

    public Commit(String message) {
        this.message = message;
        this.timestamp = "hhh";
    }

    public static void main(String[] args) {

    }

    /**
     * Gets the files that the commit has snapshot.
     *
     * @return the files that the commit has snapshot
     */
    public Set<String> getFiles() {
        return ids.keySet();
    }

    /**
     * Gets the message of this commit.
     *
     * @return the message of this commit.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Gets the sha-1 of the given filename of this commit has snapshot.
     *
     * @param filename The file we want to know.
     * @return Null if the file does not exist. Otherwise, its sha-1.
     */
    public String getFilesha(String filename) {
        return ids.get(filename);
    }

    /**
     * Adds a file to the snapshot.
     *
     * @param filename The file we want to save.
     * @param fileSha      The sha-1 mapping with the content of the file.
     */
    public void addFile(String filename, String fileSha) {
        ids.put(filename, fileSha);
    }

    /**
     * Set the parents.
     */
    public void setFirstParent(String firstParent) {
        this.firstParent = firstParent;
    }

    public String getFirstParent() {
        return firstParent;
    }

    public String getSecondParent() {
        return secondParent;
    }

    public void setSecondParent(String secondParent) {
        this.secondParent = secondParent;
    }


    public String getSha() {
        String idsString;
        idsString = (ids == null)
                ? ""
                : ids.toString();
        if (sha == null) {
            this.sha = sha1(idsString + message + timestamp + firstParent + secondParent);
        }
        return sha;
    }

    @Override
    public String toString() {
        return "===\n"
                + "commit " + this.getSha() + '\n'
                + "Date: " + this.timestamp + '\n'
                + this.message + '\n';
        //  + "Tracking Files: " + this.ids.keySet();
    }
}
