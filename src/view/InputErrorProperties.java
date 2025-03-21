package view;

/**
 * An interface that holds constant String values to be used when showing input error messages.
 */
public interface InputErrorProperties {
    /**
     * A constant to be used when the extension is invalid.
     */
    String EXTENSION = "Invalid extension. Valid extensions start with '.' followed by at least 1 non-whitespace character";

    /**
     * A constant to be used when the directory is invalid.
     */
    String DIRECTORY = "Directory at the given path does not exist, was not a directory, or was a root directory";

    /**
     * A constant to be used when the extension and directory are invalid.
     */
    String BOTH_INPUTS = "Invalid extension and directory does not exist. Valid extensions start with '.'";
}
