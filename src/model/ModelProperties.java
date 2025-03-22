package model;

/**
 * An interface that holds constant String values to be used when firing property changes related to the model.
 */
public interface ModelProperties {
    /**
     * A constant to be used when starting the watcher system.
     */
    String START = "start";

    /**
     * A constant to be used when stopping the watcher system.
     */
    String STOP = "stop";

    /**
     * A constant to be used when an event is caught and needs to be passed.
     */
    String EVENT = "event";

    /**
     * A constant to be used when updating the FileListModel table.
     */
    String FILE_LIST_MODEL_UPDATED = "fileListModelUpdated";

    /**
     * A constant to be used to let other parts of the program know when
     * the watcher system is in the process of starting.
     */
    String REGISTER_START = "registerStart";

    /**
     * A constant to be used to let other parts of the program know
     * when the watcher system has completed the starting process.
     */
    String REGISTER_DONE = "registerDone";

    /**
     * A constant to be used when updating the LogListModel table.
     */
    String LOG_LIST_MODEL_UPDATED = "logListModelUpdated";

    /**
     * A constant to be used when updating the DBFriend table.
     */
    String TABLE_MODEL_QUERY = "dbQueryModel";
}
