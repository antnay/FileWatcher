package model;

import java.time.LocalDateTime;

class Event {
    private final String myExtension;
    private final String myFileName;
    private final String myPath;
    private final String myEventKind;
    private final LocalDateTime myTimeStamp;

    Event(final String theExtension, final String theFileName, final String thePath,
            final String theEventKind, final LocalDateTime theTimestamp) {
        // TODO: Clean up fields to prevent malicous attacks
        myExtension = theExtension;
        myFileName = theFileName;
        myPath = thePath;
        myEventKind = theEventKind;
        myTimeStamp = theTimestamp;
    }

    final String getMyExtension() {
        return myExtension;
    }

    final String getFileName() {
        return myFileName;
    }

    final String getPath() {
        return myPath;
    }

    final String geEventKind() {
        return myEventKind;
    }

    final String getTimeStamp() {
        return myTimeStamp.toString();
    }

    @Override
    public String toString() {
        StringBuilder sB = new StringBuilder();
        sB.append("file: ").append(myFileName)
                .append("\npath: ").append(myPath)
                .append("\nextension: ").append(myExtension)
                .append("\nkind: ").append(myEventKind)
                .append("\ntime: ").append(myTimeStamp);
        return sB.toString();
    }
}