package model;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.HashSet;

public class Trie {

    private class Node {
        private HashSet<Node> myParent;
        private Path myPath;
        private WatchKey myKey;
    }

    public Trie() {
    }

}
