package view;

public interface InputErrorProperties {
    String EXTENSION = "Invalid extension. Valid extensions start with '.' followed by at least 1 non-whitespace character";
    String DIRECTORY = "Directory at the given path does not exist, was not a directory, or was a root directory";
    String BOTH_INPUTS = "Invalid extension and directory does not exist. Valid extensions start with '.'";
}
