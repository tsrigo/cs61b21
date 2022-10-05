package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author Tsrigo
 */
public class Repository implements Serializable {
    /**
     * The length of common SHA-1.
     */
    static final int SHALENGTH = 40;
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * The commit directory.
     */
    public static final File COMIT_DIR = join(GITLET_DIR, "commits");
    /**
     * The staging area.
     */
    public static final File STAGING_DIR = join(GITLET_DIR, ".staging");
    /**
     * Current branch of commit tree.
     */
    private String currentBranch;
    /**
     * Each branch is a pointer to a commit, each commit has parent.
     */
    private HashMap<String, Commit> branches;
    /**
     * The files in staging area.
     */
    private HashSet<File> stagingArea;
    /**
     * The tracking area that has the files being tracked with its SHA-1.
     */
    private HashMap<String, String> trackingArea;
    /**
     * Treat sha-1 as a reference.
     */
    private HashMap<String, File> sha2file;
    /**
     * SHA-1 to commits
     */
    private HashMap<String, Commit> sha2commit;
    /**
     * Commits to SHA-1;
     */
    private HashMap<Commit, String> commit2Sha = new HashMap<>();
    /**
     * Set used to record what files have been staged for removal.
     */
    private HashSet<String> removingArea;


    public Repository() {
        //LinkedList<Commit> commits = new LinkedList<>();
        Commit initialCommit = new Commit("initial commit", "Wed Dec 31 16:00:00 1969 -0800");

        branches = new HashMap<>();
        branches.put("master", initialCommit);
        currentBranch = "master";
        stagingArea = new HashSet<>();
        sha2file = new HashMap<>();
        sha2commit = new HashMap<>();
        trackingArea = new HashMap<>();
        removingArea = new HashSet<>();

        commit2sha(initialCommit);
        //commits.addFirst(initialCommit);
    }

    public static Repository init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system " +
                    "already exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        COMIT_DIR.mkdir();
        STAGING_DIR.mkdir();
        return new Repository();
    }

    private Commit getCurrentCommit() {
        return branches.get(currentBranch);
    }

    public void add(String filename) {
        //System.out.println("Current commit is: " + getCurrentCommit().toString());
        File augend = join(CWD, filename);
        File stagingFile = join(STAGING_DIR, filename);
        if (!augend.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        byte[] contents = readContents(augend);
        String fileSha = sha1((Object) contents);
        String trackedSha = trackingArea.get(filename);
        String existSha = getCurrentCommit().getFilesha(filename);
        if (removingArea.contains(filename)) {
            //System.out.println("DEBUG: The file will no longer be staged for removal");
            removingArea.remove(filename);
        }
        if (existSha != null && existSha.equals(fileSha)) {
            //System.out.println("DEBUG: File is already committed.");
            if (stagingFile.exists()) {
                removeStage(stagingFile);
                stagingFile.delete();
            }
            trackingArea.put(filename, fileSha);
            return; // 上面还有需要保存的状态，不能直接exit(0)
        }
        stagingArea.add(stagingFile);
        trackingArea.put(filename, fileSha);
        writeContents(stagingFile, (Object) contents);
        //System.out.println("The files you have staged are: " + stagingArea.toString());
    }

    public void commit(String message, String givenBranch) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("E LLL dd hh:mm:ss yyyy -0800");
        Commit newCommit = new Commit(trackingArea, message, dtf.format(LocalDateTime.now()));
        if (stagingArea.isEmpty() && removingArea.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        removingArea.clear();
        for (File X : stagingArea) {
            String S = sha1((Object) readContents(X));
            newCommit.addFile(X.getName(), S);
        }
        // Add a commit to the commit tree.
        newCommit.setFirstParent(commit2sha(getCurrentCommit()));
        if (givenBranch != null) {
            newCommit.setSecondParent(commit2sha(branches.get(givenBranch)));
        }
        branches.put(currentBranch, newCommit);
        String commitSha = commit2sha(newCommit);
        //System.out.println("DEBUG: The new commit sha is "+commitSha);
        //System.out.println("DEBUG: New currentCommit is: " + newCommit + '\n');

        File D = join(COMIT_DIR, commitSha);
        D.mkdir();
        HashSet<File> tep = new HashSet<>(stagingArea);
        for (File X : tep) {
            File file = join(D, X.getName());
            byte[] contents = readContents(X);
            String S = sha1((Object) contents);
            writeContents(file, (Object) contents);
            sha2file.put(S, file);
            removeStage(X);
            X.delete();
        }
    }

    public void rm(String filename) {
        //System.out.println("DEBUG: Notice: " + filename + " will be rm");
        File file = join(STAGING_DIR, filename);
        if (stagingArea.isEmpty() && trackingArea.isEmpty()) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (stagingArea.contains(file)) {
            removeStage(file);
        }
        if (getCurrentCommit().getFilesha(filename) != null) {
            // tracking area will always be same with currentCommit.ids
            //System.out.println("Notice: " + filename + " will be deleted");
            removeTrack(filename);
            removingArea.add(filename);
        }
    }

    public void log() {
        //QUESTION: log may not start from the very first of the current branch.
        Commit tep = branches.get(currentBranch);
        while (tep != null) {
            System.out.println(tep);
            tep = sha2commit.get(tep.getFirstParent());
        }
    }

    public void globalLog() {
        for (Commit x : commit2Sha.keySet()) {
            System.out.println(x);
        }
    }

    public void find(String message) {
        boolean flag = false;
        for (Commit x : commit2Sha.keySet()) {
            if (x.getMessage().equals(message)) {
                System.out.println(commit2sha(x));
                flag = true;
            }
        }
        if (!flag) {
            System.out.println("Found no commit with that message.");
        }
        System.exit(0);
    }

    public void status() {
        System.out.println("=== Branches ===");
        for (String x : new TreeSet<>(branches.keySet())) {
            if (x.equals(currentBranch)) {
                System.out.print('*');
            }
            System.out.println(x);
        }

        // UPDATE: This method is too ugly. Improve it.
        System.out.println("\n=== Staged Files ===");
        List<String> tep = new ArrayList<>();
        for (File f : stagingArea) {
            tep.add(f.getName());
        }
        Collections.sort(tep);
        for (String s : tep) {
            System.out.println(s);
        }

        System.out.println("\n=== Removed Files ===");
        for (String f : new TreeSet<>(removingArea)) {
            System.out.println(f);
        }

        System.out.println("\n=== Modifications Not Staged For Commit ===");
        TreeSet<String> allFiles = new TreeSet<>();
        TreeSet<String> cwdFiles = new TreeSet<>(Objects.requireNonNull(plainFilenamesIn(CWD)));
        allFiles.addAll(cwdFiles);
        allFiles.addAll(trackingArea.keySet());
        for (File f : stagingArea) {
            allFiles.add(f.getName());
        }

        for (String f : allFiles) {
            File stagingFile = join(STAGING_DIR, f);
            File cwdFile = join(CWD, f);
            // trackingSha refers to the f that was tracked in the last commit(current commit)
            // stagingSha refers to the f that is staged in the present commit(newCommit)
            String trackingSha = getCurrentCommit().getFilesha(f);
            String stagingSha = trackingArea.get(f);
            // QUESTION: Is stagingArea can changed to type of String?
            boolean isStaging = stagingArea.contains(stagingFile);
            boolean isTracking = (trackingSha != null);
            boolean inCwd = cwdFiles.contains(f);

            if (inCwd) {
                // if there is a modified file that is not staged, it will be marked modified.
                // if there is a file that is staged but not same with the tracked version,
                // it will be marked modified.
                String cwdSha = sha1((Object) readContents(cwdFile));
                if (isTracking && !isStaging && !cwdSha.equals(trackingSha)
                        || isStaging && !cwdSha.equals(stagingSha)) {
                    System.out.println(f + "(modified)");
                }
            } else {
                if (isTracking || isStaging) {
                    System.out.println(f + "(deleted)");
                }
            }
        }

        System.out.println("\n=== Untracked Files ===");
        for (String f : cwdFiles) {
            boolean isTracking = (trackingArea.get(f) != null);
            if (!isTracking) {
                System.out.println(f + "(untracked)");
            }
        }
    }

    /**
     * Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory,
     * overwriting the version of the file that’s already there if there is one.
     *
     * @param commitId The checkout commit.
     * @param filename The checkout file.
     */
    public void checkoutFile(String commitId, String filename) {
        if (commitId != null && commitId.length() < SHALENGTH) {
            commitId = findId(commitId);
        }
        Commit previousCommit = (commitId == null) ? getCurrentCommit() : sha2commit.get(commitId);
        if (commitId != null && sha2commit.get(commitId) == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        String sourceSha = previousCommit.getFilesha(filename);
        File sourceFile = sha2file.get(sourceSha);
        if (sourceFile == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        checkUntrack(filename, commitId);
        File cwdFile = join(CWD, filename); // 安全措施要做牢！
        String cwdSha = cwdFile.exists() ? sha1((Object) readContents(cwdFile)) : null;
        if (!sourceSha.equals(cwdSha)) {
            writeContents(cwdFile, (Object) readContents(sourceFile));
        }
        removeStage(join(STAGING_DIR, filename));
        trackingArea.put(filename, sourceSha);
    }

    public void checkoutBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        Commit checkoutCommit = branches.get(branchName);
        Commit currentCommit = getCurrentCommit();
        //System.out.println("checkoutCommit is: " + checkoutCommit + '\n');
        //System.out.println("currentCommit is " + currentCommit+ '\n');
        Set<String> checkoutFiles = checkoutCommit.getFiles();
        Set<String> currentFiles = getCurrentCommit().getFiles();
        //System.out.println("checkout files: " + checkoutFiles.toString()+ '\n');
        //System.out.println("current files: " + currentFiles.toString()+ '\n'    );
        for (String f : checkoutFiles) {
            checkUntrack(f, commit2sha(checkoutCommit));
        }
        for (String f : checkoutFiles) {
            checkoutFile(commit2sha(checkoutCommit), f);
        }
        for (String f : currentFiles) {
            if (!checkoutFiles.contains(f)) {
                File file = join(CWD, f);
                //System.out.println("DEBUG: Warning: " + file + " will be deleted");
                file.delete();
                trackingArea.remove(f);
            }
        }
        currentBranch = branchName;
        clearStagingArea();
    }

    public void branch(String branchName) {
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        Commit newBranch = branches.get(currentBranch);
        branches.put(branchName, newBranch);
    }

    public void removeBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        }
        if (branchName.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
        }
        branches.remove(branchName);
    }

    public void reset(String id) {
        Commit previousCommit = sha2commit.get(id);
        if (previousCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        String backup = currentBranch;
        branches.put("tepBranch", previousCommit);
        checkoutBranch("tepBranch");
        currentBranch = backup;
        removeBranch("tepBranch");
        branches.put(currentBranch, previousCommit);
    }

    public void merge(String givenBranch) {
        Commit givenCommit = branches.get(givenBranch);
        Commit headCommit = getCurrentCommit();
        if (givenCommit == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        Commit splitCommit = getSplitCommit(givenCommit, headCommit);
        String givenSha = commit2sha(givenCommit);
        String headSha = commit2sha(headCommit);
        String splitSha = commit2sha(splitCommit);
        if (givenBranch.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        if (splitSha.equals(givenSha)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitSha.equals(headSha)) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(givenBranch);
            System.exit(0);
        }
        if (!stagingArea.isEmpty() || !removingArea.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        Set<String> givenFiles = givenCommit.getFiles();
        for (String f : givenFiles) {
            checkUntrack(f, givenSha);
        }
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(headCommit.getFiles());
        allFiles.addAll(givenFiles);
        allFiles.addAll(splitCommit.getFiles());
        for (String f : allFiles) {
            String splitfilesha = splitCommit.getFilesha(f);
            String headfilesha = headCommit.getFilesha(f);
            String givenfilesha = givenCommit.getFilesha(f);
            if (splitfilesha != null) {
                if (givenfilesha == null) {
                    if (headfilesha == null || splitfilesha.equals(headfilesha)) {
                        removeTrack(f);
                        continue;
                    }
                    sloveConflict(headSha, givenSha, f);
                    add(f);
                } else if (headfilesha == null) {
                    if (splitfilesha.equals(givenfilesha)) {
                        removeTrack(f);
                        continue;
                    }
                    sloveConflict(headSha, givenSha, f);
                    add(f);
                } else {
                    boolean headModified = !splitfilesha.equals(headfilesha);
                    boolean givenModified = !splitfilesha.equals(givenfilesha);
                    if (!headModified && givenModified) {
                        checkoutFile(givenSha, f);
                        add(f);
                    } else if (headModified && givenModified && !headfilesha.equals(givenfilesha)) {
                        sloveConflict(headSha, givenSha, f);
                        add(f);
                    }
                }
            } else {
                if (headfilesha == null) { // givenSha 不会为null，因为allfiles包含的是这三者的文件。
                    checkoutFile(givenSha, f);
                } else if (givenfilesha == null) {
                    checkoutFile(headSha, f);
                } else if (!headfilesha.equals(givenfilesha)) {
                    sloveConflict(headSha, givenSha, f);
                }
                add(f);
            }
        }
        commit("Merged " + givenBranch + " into " + currentBranch + ".", givenBranch);
    }

    // Bellowed are some helper functions.
    private void sloveConflict(String currentSha, String givenSha, String filename) {
        System.out.println("Encountered a merge conflict.");
        String currentFileSha = sha2commit.get(currentSha).getFilesha(filename);
        File currentFile = sha2file.get(currentFileSha);
        String givenFileSha = sha2commit.get(givenSha).getFilesha(filename);
        File givenFile = sha2file.get(givenFileSha);

        byte[] currentContent, givenContent;
        if (currentFile == null || !currentFile.exists()) {
            currentContent = "".getBytes();
        } else {
            currentContent = readContents(currentFile);
        }
        if (givenFile == null || !givenFile.exists()) {
            givenContent = "".getBytes();
        } else {
            givenContent = readContents(givenFile);
        }
        currentFile = join(CWD, filename);
        String contents = "<<<<<<< HEAD\n" + new String(currentContent) + "=======\n"
                + new String(givenContent) + ">>>>>>>\n";
        writeContents(currentFile, contents);
    }

    private Commit getSplitCommit(Commit x, Commit y) {
        HashSet<String> xAncestor = new HashSet<>();
        Queue<String> tep = new LinkedBlockingQueue<>();
        tep.add(commit2sha(x));
        while (!tep.isEmpty()) {
            String t = tep.poll();
            xAncestor.add(t);
            String p1 = sha2commit.get(t).getFirstParent();
            String p2 = sha2commit.get(t).getSecondParent();
            if (p1 != null) {
                tep.add(p1);
            }
            if (p2 != null) {
                tep.add(p2);
            }
            if (p1 == null && p2 == null) {
                break;
            }
        }
        tep.clear();
        tep.add(commit2sha(y));
        while (!tep.isEmpty()) {
            String t = tep.poll();
            if (xAncestor.contains(t)) {
                return sha2commit.get(t);
            }
            xAncestor.add(t);
            String p1 = sha2commit.get(t).getFirstParent();
            String p2 = sha2commit.get(t).getSecondParent();
            if (p1 != null) {
                tep.add(p1);
            }
            if (p2 != null) {
                tep.add(p2);
            }
            if (p1 == null && p2 == null) {
                break;
            }
        }
        return null;
    }

    /**
     * Untrack has several condition:
     * 1. Files present in the working directory but neither staged for addition nor tracked.
     * 2. Files that have been staged for removal, but then re-created without Gitlet’s knowledge.
     *
     * @param filename
     * @param commitId
     * @return
     */
    private boolean checkUntrack(String filename, String commitId) {
        File cwdFile = join(CWD, filename);
        if (commitId == null || !cwdFile.exists()) {
            return true;
        }
        String cwdFilesha = sha1((Object) readContents(cwdFile));
        String checkoutFile = sha2commit.get(commitId).getFilesha(filename);
        boolean isTracking = (trackingArea.get(filename) != null);
        if (cwdFile.exists() && !isTracking && !cwdFilesha.equals(checkoutFile)) {
            System.out.println("There is an untracked file in the way; " +
                    "delete it, or add and commit it first.");
            System.exit(0);
        }
        return true;
    }

    private void clearStagingArea() {
        stagingArea.clear();
    }

    /**
     * Removes the file from the stagingArea.
     *
     * @param filename the file to be removed
     */
    private void removeStage(File filename) {
        if (!stagingArea.contains(filename)) {
            //System.out.println("DEBUG: "filename + " was already removed.");
            return;
        }
        stagingArea.remove(filename);
    }

    /**
     * Removes the file from the trackingArea and the workingArea.
     *
     * @param filename the file to be removed
     */
    private void removeTrack(String filename) {
        File workingFile = join(CWD, filename);
        trackingArea.remove(filename);
        workingFile.delete();
    }

    private String findId(String id) {
        for (String f : sha2commit.keySet()) {
            if (id.equals(f.substring(0, id.length()))) {
                return f;
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }

    private String commit2sha(Commit commit) {
        String t = commit2Sha.get(commit);
        if (t == null) {
            t = commit.getSha();
            commit2Sha.put(commit, t);
            sha2commit.put(t, commit);
        }
        return t;
    }
    private static void printError(String info) {
        System.out.println(info);
        System.exit(0);
    }
}
