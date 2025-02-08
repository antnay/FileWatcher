import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import controller.FileWatcherController;
import view.MainView;

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
                FileWatcherController fileWatcherController = new FileWatcherController();
                MainView view = new MainView();
                view.setVisible(true);
            }
        });
    }
}
