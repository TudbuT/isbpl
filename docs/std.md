# Documentation: std.isbpl

The standard library. Everything here is (supposed to be) loaded before ANY other code is executed. It is responsible for things ranging from array literals and comments,
over type definitions, to string processing.

## Functions

### `ftos ::: float -> string`

    [Unfinished]
    Converts float to string


### `dtos ::: double -> string`

    [Unfinished]
    Converts double to string


### `stof ::: string -> float`

    [Unfinished]
    Converts string to float


### `stod ::: string -> double`

    [Unfinished]
    Converts string to double


### `strsplit ::: string separator -> array`

    Splits string by separator, if the separator is not found anywhere, the entire string 
    will end up being wrapped in an array. REGEX **not** supported


### `inc ::: integer -> `

    Uses _last_words to find the variable of the integer variable mentioned previously, 
    takes its value, and increments it, then stores the value back.
    The integer argument is discarded as it is not useful.


### `dec ::: integer -> `

    Uses _last_words to find the variable of the integer variable mentioned previously, 
    takes its value, and decrements it, then stores the value back.
    The integer argument is discarded as it is not useful.


### `gettype ::: object -> object.type.id`

    Returns the type ID of the object's type as integer


### `isfloat ::: object -> result`

    Returns 1 if the object is a float, otherwise 0


### `islong ::: object -> result`

    Returns 1 if the object is a long, otherwise 0


### `isstring ::: object -> result`

    Returns 1 if the object is a string, otherwise 0


### `ischar ::: object -> result`

    Returns 1 if the object is a char, otherwise 0


### `isarray ::: object -> result`

    Returns 1 if the object is an array, otherwise 0


### `isint ::: object -> result`

    Returns 1 if the object is an int, otherwise 0


### `isbyte ::: object -> result`

    Returns 1 if the object is a byte, otherwise 0


### `isdouble ::: object -> result`

    Returns 1 if the object is a double, otherwise 0


### `strconcat ::: string1 string2 -> string`

    Concatenates string1 and string2, and returns the result.


### `strjoin ::: array joiner -> string`

    Joins all elements of the array with the joiner string. 
    Example: `"foo" "bar" -> "foobar"`


### `fcall ::: callable -> `

    Calls the provided callable


### `asub ::: array begin end -> subarray`

    Returns a portion of array starting at index begin, and ending with index end.


### `strsub ::: string begin end -> substring`

    Returns a portion of string starting at index begin, and ending with index end.


### `putchar ::: char -> `

    Puts the character into stdout


### `typeid ::: name -> id`

    Returns the name of the type with the id


### `_int ::: number -> int`

    Casts a number to int


### `_char ::: number -> char`

    Casts a number to char


### `_long ::: number -> long`

    Casts a number to long


### `_string ::: any -> string`

    Tries to cast an object to a string.


### `_byte ::: number -> byte`

    Casts a number to byte


### `_double ::: number -> double`

    Casts a number to double


### `_float ::: number -> float`

    Casts a number to float


### `_array ::: any -> array`

    Tries to cast an object to an array


### `! ::: a -> a a`

    Duplicates the topmost value. This is synonymous with dup, and should be used when
    multiple actions are done on one object.


### `# ::: comment -> `

    Comment indicator. Synonymous with pop.


### `aput ::: array idx value -> `

    Puts value into array at index idx.


### `% ::: a b -> math(a % b)`

    Calculates the modulo. This **does** work with floats.


### `( ::: -> `

    Does nothing, purely for visuals.


### `) ::: -> `

    Does nothing, purely for visuals.


### `* ::: a b -> math(a * b)`

    Multiplies a and b


### `subprocess ::: commandline -> `

    Starts a subprocess with commandline


### `+ ::: a b -> math(a + b)`

    Adds a and b


### `- ::: a b -> math(a - b)`

    Subtracts a with b


### `/ ::: a b -> math(a / b)`

    Divides a by b


### `exit ::: value -> (throw)`

    Exits the program. Can be catched.


### `defmethod ::: name callable type -> `

    Defines a method in type executing callable named name


### `defsuper ::: type1 type2 -> `

    Makes type2 extend type1.


### `alen ::: array -> length`

    Returns the length of array, the last possible idx + 1.


### `** ::: a b -> math(pow(a, b))`

    Calculates a to the power of b


### `astartswith ::: array1 array2 -> result`

    Returns 1 if array1 starts with array2, otherwise 0


### `pop ::: object -> `

    Drops an object from the stack.


### `[ ::: -> shadow`

    Pushes an array shadow to the stack.
    This indicates the start of an array literal.


### `] ::: shadow [DYN] -> array`

    Pops from stack until array shadow is reached, and puts the popped elements into an 
    array.
    This indicates the end of an array literal.


### `^ ::: a b -> math(xor(a, b))`

    Calculates XOR of a and b


### `deffunc ::: name callable -> `

    Defines a function by a callable.


### `++ ::: a -> math(a + 1)`

    Returns input + 1


### `include ::: file -> `

    Loads and executes ISBPL code from file.
    The argument is parsed differently if:
        - it starts with a hash: this loads a global library
        - it starts with a /: this indicates that the file path is absolute
    Otherwise, it is relative to the directory that the included file is in.


### `reload ::: file -> `

    Acts like include, but is able to load a file multiple times.


### `getos ::: -> osname`

    Returns OS name. This is identical to System.getProperty("os.name") in Java.


### `call ::: name -> `

    [Deprecated]
    Calls function by name. Deprecated in favor of &<name> callable getters.


### `puts ::: string-> `

    Prints a string. No newline is appended.


### `throw ::: id description -> (throw)`

    Throws an error.


### `aget ::: array idx -> value`

    Returns value in array at idx


### `char ::: string -> char`

    Returns first character of a string


### `typename ::: id -> name`

    Returns name of the type identified by id


### `itos ::: int -> string`

    Converts an integer to a string


### `ltos ::: long -> string`

    Converts a long to a string


### `stol ::: string -> long`

    Converts string to long, assumes base 10 string, anything else will not throw an error,
    and bug out to wrong results instead.


### `stoi ::: string -> int`

    Converts string to int, assumes base 10 string, anything else will not throw an error,
    and bug out to wrong results instead.


### `getfile ::: -> file`

    Gets the filename of the calling file.


### `areverse ::: array1 -> array2`

    Reverses the order of array1. array1 is not changed.


### `callmethod ::: name object -> `

    Calls method identified by name on the object.


### `gt ::: a b -> cond(a > b)`

    Returns 1 if a is greater than b, otherwise 0


### `lt ::: a b -> cond(a < b)`

    Returns 1 if a is less than b, otherwise 0


### `main ::: args -> 0`

    Placeholder main function. Does nothing.


### `aadd ::: array1 array2 -> array3`

    Adds array1 and array2 together:
    Makes array3 with the length of array1 and array2 combined;
    Adds values from array1;
    Adds values from array2, offset by the length of array1.


### `reference ::: object -> reference`

    Makes a reference to object


### `neg ::: a -> math(-a)`

    Returns the negative of a


### `acontains ::: array1 array2 -> result`

    Returns 1 if array1 contains all elements from array2, in the same order, and without
    any mismatches inbetween. Example: `12345 23 -> 1`, `12345 24 -> 0`


### `strcontains ::: string1 string2 -> result`

    Returns 1 if string1 contains all elements from string2, in the same order, and without
    any mismatches inbetween. Example: `"12345" "23" -> 1`, `"12345" "24" -> 0`


### `-- ::: a -> math(a - 1)`

    Returns input - 1


### `swap ::: a b -> b a`

    Swaps two values on the stack


### `anewput ::: [DYN] amount -> array[amount]<-DYN`

    Returns array with length amount and contents DYN. Exmaple: `a b c 3 -> [ a b c ]`


### `eq ::: a b -> equal(a, b)`

    Returns 1 if a and b are equal, otherwise 0


### `anew ::: length -> array[length]`

    Creates array.


### `deffield ::: name type -> `

    Defines field called name on type


### `extends ::: a b -> result`

    Returns 1 if a extends b, meaning a declared b as supertype, otherwise 0.


### `null ::: -> null`

    Returns null object


### `struppercase ::: string -> UPPERCASESTRING`

    Flips [a-z] to their uppercase variants.


### `strlowercase ::: string -> lowercasestring`

    Flips [A-Z] to their lowercase variants.


### `mktype ::: name -> id`

    Makes a type named name, and returns its id. Generally replaced by construct keyword.


### `settype ::: object1 typeid -> object2`

    Copies object, and sets the copy's type to typeid


### `mkinstance ::: typeid -> instance`

    Makes an instance of typeid, calling constructor if necessary.


### `delete ::: object -> `

    Makes the object and all of its copies lose their identity, resetting them and freeing
    the occupied storage.


### `not ::: a -> cond(!a)`

    If a is truthy, return 0, otherwise, return 1.


### `acopy ::: a b idxa idxb amount -> b`

    Copies array a into b, and returns b. The first element of a that is copied is idxa,
    and it is copied from idxb. The amount of copied elements is amount. There is a 
    faster native version of this, but it is not used here.


### `_layer_call ::: name idx -> `

    Calls a function called name on the idx'th frame from the top of the framestack.


### `and ::: a b -> cond(a && b)`

    Returns if both a and b are truthy.


### `or ::: a b -> cond(a || b)`

    Returns if either a or b are truthy.


### `_last_word ::: idx -> word`

    Returns the idx'th last word in the code. 
    Example `"foobar" 2 _last_word -> "\"foobar"`. Keep in mind that string words 
    internally do not end with a quote!


### `dup ::: a -> a a`

    Duplicates the topmost value on the stack.


## Variables

### TYPE_JIO

    The typeid of the JIO type


### String

    The typeid of a string


### TYPE_SHADOW

    The typeid of a shadow, used for array literals.


### TYPE_INT

    The typeid of an integer


### TYPE_BYTE

    The typeid of a byte


### TYPE_FUNCTION

    [Deprecated]
    The typeid of a callable


### TYPE_FLOAT

    The typeid of a float


### TYPE_NULL

    The typeid of null


### TYPE_LONG

    The typeid of a long


### TYPE_STRING

    [Deprecated]
    The typeid of string


### TYPE_REFERENCE

    The typeid of a reference


### Function

    The typeid of a callable


### TYPE_ARRAY

    [Deprecated]
    The typeid of an array


### TYPE_DOUBLE

    The typeid of a double


### Array

    The typeid of an array


### JIO

    The JIO instance.


### TYPE_CHAR

    The typeid of chars


