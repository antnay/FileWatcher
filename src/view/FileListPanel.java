package view;

import controller.FileListController;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeSupport;

public class FileListPanel extends JPanel {
    private PropertyChangeSupport myPCS;

    public FileListPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        setLayout(new BorderLayout());
        JLabel fileListLabel = new JLabel("Files and directories being watched");
        add(fileListLabel, BorderLayout.NORTH);
        initFileList();
    }

    private void initFileList() {
        JTable fileTable = FileListController.getFileListModel();
        JScrollPane listScrollPane = new JScrollPane(fileTable);
        listScrollPane.setPreferredSize(new Dimension(400, 100));
        // set File Extension column width
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        // set Directory column width
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        add(listScrollPane, BorderLayout.CENTER);
    }
}
