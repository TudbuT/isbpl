# Documentation: multi.isbpl

Library to create multiple contexts.

## >Variables

### Context

    Context type


### ISBPLStack

    ISBPLStack type (java)


### File

    File type (java)


### ISBPL

    ISBPL context type (java)


---

# Types

## >Context

    ISBPL context wrapper

### Methods

#### `copyFunc ::: callable name -> `

    Copies a function to the context


#### `construct ::: -> context`

    Makes a new context


#### `makeStack ::: -> stack`

    Creates a new ISBPLStack


#### `eval ::: string -> `

    Evaluates a string, the stack of this context is used in the new one aswell.


#### `evalNewStack ::: string -> stack`

    Evaluates a string, the stack used is fresh and returned afterwards.


#### `evalCustom ::: string stack -> stack`

    Evaluates a string using the specified stack object, and returns the same object again.


### Variables

#### jContext

    The real context that is being wrapped.


