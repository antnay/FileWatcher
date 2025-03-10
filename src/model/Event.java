package model;

import java.time.LocalDateTime;

public final class Event {
    private final String myExtension;
    private final String myFileName;
    private final String myPath;
    private final String myEventKind;
    private final LocalDateTime myTimeStamp;

    public Event(final String theExtension, final String theFileName, final String thePath,
            final String theEventKind) {
        myExtension = theExtension;
        myFileName = theFileName;
        myPath = thePath;
        myEventKind = theEventKind;
        myTimeStamp =  LocalDateTime.now();
    }

    public final String getMyExtension() {
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