# Documentation: time.isbpl



## Functions

### `sleep ::: delay -> `

    Delay in millis


### `delay ::: delay -> `

    Delay in millis


### `delays ::: delay -> `

    Delay in seconds


### `getms ::: -> ms`

    Returns current unix millis


### `time ::: delay -> currentUnixMillis`

    Native to assist with time. Sleeps delay and returns current unix 
    millis.


## Variables

### Timer

    The Timer type.


---

# Types

## Timer

    Type to time the duration of operations

### Methods

#### `start ::: -> `

    Starts the timer


#### `timeTaken ::: -> ms`

    Returns the number of millis between the last start & end calls


#### `end ::: -> `

    Stops the timer


### Variables

#### startMS

    unix timestamp of last start call


#### timeTakenTMP

    Temporary to store a cached value of the time taken


#### stopMS

    unix timestamp of last stop call


