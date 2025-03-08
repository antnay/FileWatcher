package view;

public interface InputErrorProperties {
    String EXTENSION = "Invalid extension. Valid extensions start with '.' followed by at least 1 non-whitespace character";
    String DIRECTORY = "Directory does not exist or given path was not a directory";
    String BOTH_INPUTS = "Invalid extension and directory does not exist. Valid extensions start with '.'";
}
