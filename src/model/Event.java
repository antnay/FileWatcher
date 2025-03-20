package model;

import java.time.LocalDateTime;

/**
 * Represents a file event with details about the file and what happened to it.
 */
public final class Event {
    private final String myExtension;
    private final String myFileName;
    private final String myPath;
    private final String myEventKind;
    private final LocalDateTime myTimeStamp;

    /**
     * Creates an Event with file details and the type of change.
     *
     * @param theExtension The file extension.
     * @param theFileName The name of the file.
     * @param thePath The file's location.
     * @param theEventKind The type of event (e.g., "created", "modified").
     */
    public Event(final String theExtension, final String theFileName, final String thePath,
            final String theEventKind) {
        myExtension = theExtension;
        myFileName = theFileName;
        myPath = thePath;
        myEventKind = theEventKind;
        myTimeStamp =  LocalDateTime.now();
    }

    public final String getExtension() {
        return myExtension;
    }

    public final String getFileName() {
        return myFileName;
    }

    public final String getPath() {
        return myPath;
    }

    public final String geEventKind() {
        return myEventKind;
    }

    public final LocalDateTime getTimeStamp() {
        return myTimeStamp;
    }

    public final Object[] toArray() {
        return new Object[] { myExtension, myFileName, myPath, myEventKind, myTimeStamp };
    }

    /**
     * Returns a formatted string with event details.
     *
     * @return A string representation of the event.
     */
    @Override
    public final String toString() {
        StringBuilder sB = new StringBuilder();
        sB.append("file: ").append(myFileName)
                .append("\nextension: ").append(myExtension)
                .append("\npath: ").append(myPath)
                .append("\nkind: ").append(myEventKind)
                .append("\ntime: ").append(myTimeStamp);
        return sB.toString();
    }
}