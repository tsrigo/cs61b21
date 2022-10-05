package gitlet;

import java.io.File;
import java.util.HashMap;

import static gitlet.Utils.*;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Tsrigo
 */
public class Main {
    private static final HashMap<String, Integer> ARG_LENGTH;
    private static final int THREE_ARG = 3, FOUR_ARG = 4;
    static {
        ARG_LENGTH = new HashMap<>();
        ARG_LENGTH.put("init", 1);
        ARG_LENGTH.put("add", 2);
        ARG_LENGTH.put("commit", 2);
        ARG_LENGTH.put("rm", 2);
        ARG_LENGTH.put("log", 1);
        ARG_LENGTH.put("global-log", 1);
        ARG_LENGTH.put("find", 2);
        ARG_LENGTH.put("status", 1);
        ARG_LENGTH.put("checkout", FOUR_ARG);
        ARG_LENGTH.put("branch", 2);
        ARG_LENGTH.put("rm-branch", 2);
        ARG_LENGTH.put("reset", 2);
        ARG_LENGTH.put("merge", 2);
    }

    private static void checkArglength(String[] args) {
        if (args == null || args.length == 0) {
            printError("Please enter a command.");
        }
        int givenNum = args.length;
        String command = args[0];
        int std = ARG_LENGTH.get(command);

        if (std < givenNum) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        switch (command) {
            case "add" -> {
                if (givenNum < std) {
                    printError("Please input the file to be added.");
                }
            }
            case "commit" -> {
                if (givenNum < std || args[1].isEmpty()) {
                    printError("Please enter a commit message.");
                }
            }
            case "rm" -> {
                if (givenNum < std) {
                    printError("Please enter a file to be removed.");
                }
            }
            case "checkout" -> {
                if (givenNum == THREE_ARG && !args[1].equals("--")) {
                    printError("Incorrect operands.");
                }
                if (givenNum == FOUR_ARG && !args[2].equals("--")) {
                    printError("Incorrect operands.");
                }
            }
            default -> printError("No command with that name exists.");
        }
    }

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */

    public static final File REPO_DIR = join(Repository.GITLET_DIR, ".repo");

    public static void main(String[] args) {
        checkArglength(args);
        String firstArg = args[0];
        Repository repo = REPO_DIR.exists() ? readRepo() : null;
        if (repo == null) {
            printError("Not in an initialized Gitlet directory.");
        }
        assert repo != null;
        switch (firstArg) {
            case "init" -> {
                repo = Repository.init();
            }
            case "add" -> repo.add(args[1]);
            case "commit" -> repo.commit(args[1], null);
            case "rm" -> repo.rm(args[1]);
            case "log" -> repo.log();
            case "global-log" -> repo.globalLog();
            case "find" -> repo.find(args[1]);
            case "status" -> repo.status();
            case "checkout" -> {
                switch (args.length) {
                    case 2 -> repo.checkoutBranch(args[1]);
                    case THREE_ARG -> repo.checkoutFile(null, args[2]);
                    case FOUR_ARG -> repo.checkoutFile(args[1], args[THREE_ARG]);
                    default -> printError("Incorrect operands.");
                }
            }
            case "branch" -> repo.branch(args[1]);
            case "rm-branch" -> repo.removeBranch(args[1]);
            case "reset" -> repo.reset(args[1]);
            case "merge" -> repo.merge(args[1]);
            default -> printError("No command with that name exists.");
        }
        saveRepo(repo);
    }


    private static void printError(String info) {
        System.out.println(info);
        System.exit(0);
    }

    private static void saveRepo(Repository repo) {
        writeObject(REPO_DIR, repo);
    }

    private static Repository readRepo() {
        return readObject(REPO_DIR, Repository.class);
    }
}
