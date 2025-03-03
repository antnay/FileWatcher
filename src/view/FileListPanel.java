package view;

import controller.FileListController;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeSupport;

public class FileListPanel extends JPanel {
    public FileListPanel() {
        setLayout(new BorderLayout());
        JLabel fileListLabel = new JLabel("Files and directories being watched");
        add(fileListLabel, BorderLayout.NORTH);
        initFileList();
    }

    private void initFileList() {
        JTable fileTable = FileListController.getFileListTable();
        JScrollPane listScrollPane = new JScrollPane(fileTable);
        listScrollPane.setPreferredSize(new Dimension(400, 100));
        add(listScrollPane, BorderLayout.CENTER);
    }
}
