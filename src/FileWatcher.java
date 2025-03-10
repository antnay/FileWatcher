import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import controller.FileListController;
import controller.FileWatcherController;
import model.SystemWatch;
import view.MainFrame;

/**
 * Main class for FileWatcher.
 */
public class FileWatcher {
    /**
     * Main entry point to FileWatcher.
     * @param theArgs is the user arguments.
     */
    public static void main(String[] theArgs) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
            }
        } catch (final UnsupportedLookAndFeelException | ClassNotFoundException
                | IllegalAccessException | InstantiationException exception) {
            exception.printStackTrace();
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SystemWatch sysWatch = new SystemWatch();
                MainFrame view = new MainFrame();
                new FileWatcherController(view, sysWatch);
                new FileListController(view);
                view.setVisible(true);
            }
        });
    }
}
