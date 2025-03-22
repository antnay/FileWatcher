package view;

/**
 * An interface that holds constant String values to be used when firing property changes related to the view.
 */
public interface ViewProperties {
    /**
     * A constant to be used for adding to the file watch system.
     */
    String ADD_BUTTON = "addExtensionAndDirectory";

    /**
     * A constant to be used for removing from the file watch system.
     */
    String REMOVE_BUTTON = "removeExtensionAndDirectory";

    /**
     * A constant to be used for clearing the log table.
     */
    String CLEAR_LOG = "clearLogView";

    /**
     * A constant to be used for clearing the database.
     */
    String CLEAR_DATABASE = "clearDatabase";

    /**
     * A constant to be used for saving the log table to the database.
     */
    String SAVE_LOG = "saveLog";

    /**
     * A constant to be used for updating the list of watched files when something gets added.
     */
    String ADDED_TO_FILE_LIST_MODEL = "addedToFileListModel";

    /**
     * A constant to be used for updating the list of watched files when something gets removed.
     */
    String REMOVED_FROM_FILE_LIST_MODEL = "removedFromFileListModel";

    /**
     * A constant to be used for querying the database.
     */
    String DB_QUERY = "dbQueryView";

    /**
     * A constant to be used for sending emails.
     */
    String EMAIL = "emailView";
}
