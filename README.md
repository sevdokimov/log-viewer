# LogViewer

LogViewer is a simple web UI for viewing logs on a server or a local machine. LogViewer can show huge log files
without significant resource consumption because it reads only the part of the file that a user is watching. 

### Features

1. Highlighting fields, lines, parent brackets. Highlighting makes the log much more readable.
1. Event filtering by a level, logger, or a custom condition written on Groovy.
1. Merging events from several log files and showing its as one log. If log files are located on different
machines, all machines must have run LogViewer.
1. Collapsing secondary information like unmeaning parts of exception stacktraces, full name of logger.
1. A permanent link to a log position. A user can copy a link to the current position and send it to another user. 

[Demo video](https://www.youtube.com/watch?v=1ukLMIqN0i0)

### Usage

LogViewer can be run as a standalone application and can be embedded to java web application.

[Configuration standalone application](_docs/standalone.md)

[Embedding to Spring Boot application](_docs/embadded-spring-boot.md)

[Embedding to java web application](_docs/embadded.md)

### Screenshot

![](_docs/screenshot.png)