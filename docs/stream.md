# Documentation: stream.isbpl



## Functions

### `STREAM.stdin ::: -> path`

    Returns the path to the os-dependant unix-/dev/stdin equivalent


### `stream.write ::: data id -> `

    Writes data to a stream


### `stream ::: [DYN]`

    Native for actions related to streams


### `stream.readline ::: id -> string`

    Returns the line that was read as a string. -1 means EOF.


## Variables

### STREAM.create.file.out

    Constant to indicate create.file.out action: `filename STREAM.create.file.out stream -> id`.
    Creates an output stream.


### STREAM.create.file.in

    Constant to indicate create.file.in action: `filename STREAM.create.file.in stream -> id`.
    Creates an input stream.


### STREAM.close

    Constant to indicate close action: `id STREAM.close stream -> `.
    Closes a stream.


### STREAM.create.socket

    Constant to indicate create.socket action: `ip port STREAM.create.socket stream -> id`.
    Creates an IO stream to a TCP server.


### STREAM.create.server

    Constant to indicate create.server action: `port STREAM.create.server stream -> id`.
    Creates an input stream. Each int read from it is a new IO socket stream id.


### STREAM.read

    Constant to indicate read action: `id STREAM.read stream -> int`.
    Reads a value from an input stream.


### STREAM.write

    Constant to indicate write action: `int id STREAM.write stream -> `.
    Writes a value to an output stream.


