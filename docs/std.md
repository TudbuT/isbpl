# Documentation: std.isbpl

The standard library. Everything here is (supposed to be) loaded before ANY other code is executed. It is responsible for things ranging from array literals and comments,
over type definitions, to string processing.

## Functions

### `ftos ::: float -> string`

    [Unfinished]
    Converts float to string


### `strsplit ::: string separator -> array`

    Splits string by separator, if the separator is not found anywhere, the entire string will end up being
    wrapped in an array. REGEX **not** supported


### `dec ::: integer -> `

    Uses _last_words to find the variable of the integer variable mentioned previously, takes its value, and increments it, then stores the value back.
    The integer argument is discarded as it is not useful.


### `gettype ::: object -> object.type.id`

    Returns the type ID of the object's type as integer


### `isfloat ::: object -> result`

    Returns 1 if the object is a float, otherwise 0


### `strconcat ::: string1 string2 -> string`

    Concatenates string1 and string2, and returns the result.


### `strjoin ::: array joiner -> string`

    Joins all elements of the array with the joiner string. Example: `"foo" "bar" -> "foobar"`


### `fcall ::: -> `

    -


### `islong ::: -> `

    -


### `asub ::: -> `

    -


### `putchar ::: -> `

    -


### `typeid ::: -> `

    -


### `_int ::: -> `

    -


### `! ::: -> `

    -


### `# ::: -> `

    -


### `aput ::: -> `

    -


### `% ::: -> `

    -


### `isstring ::: -> `

    -


### `( ::: -> `

    -


### `) ::: -> `

    -


### `* ::: -> `

    -


### `subprocess ::: -> `

    -


### `+ ::: -> `

    -


### `- ::: -> `

    -


### `ischar ::: -> `

    -


### `/ ::: -> `

    -


### `exit ::: -> `

    -


### `defmethod ::: -> `

    -


### `_long ::: -> `

    -


### `alen ::: -> `

    -


### `** ::: -> `

    -


### `astartswith ::: -> `

    -


### `pop ::: -> `

    -


### `isarray ::: -> `

    -


### `dtos ::: -> `

    -


### `[ ::: -> `

    -


### `_char ::: -> `

    -


### `] ::: -> `

    -


### `^ ::: -> `

    -


### `ltos ::: -> `

    -


### `getfile ::: -> `

    -


### `deffunc ::: -> `

    -


### `++ ::: -> `

    -


### `include ::: -> `

    -


### `getos ::: -> `

    -


### `astartswith_old ::: -> `

    -


### `call ::: -> `

    -


### `puts ::: -> `

    -


### `throw ::: -> `

    -


### `_string ::: -> `

    -


### `aget ::: -> `

    -


### `isint ::: -> `

    -


### `char ::: -> `

    -


### `typename ::: -> `

    -


### `itos ::: -> `

    -


### `areverse ::: -> `

    -


### `callmethod ::: -> `

    -


### `lt ::: -> `

    -


### `main ::: -> `

    -


### `aadd ::: -> `

    -


### `_byte ::: -> `

    -


### `reference ::: -> `

    -


### `neg ::: -> `

    -


### `_double ::: -> `

    -


### `acontains ::: -> `

    -


### `isbyte ::: -> `

    -


### `-- ::: -> `

    -


### `swap ::: -> `

    -


### `anewput ::: -> `

    -


### `eq ::: -> `

    -


### `_float ::: -> `

    -


### `anew ::: -> `

    -


### `deffield ::: -> `

    -


### `extends ::: -> `

    -


### `null ::: -> `

    -


### `struppercase ::: -> `

    -


### `strsub ::: -> `

    -


### `stod ::: -> `

    -


### `mktype ::: -> `

    -


### `settype ::: -> `

    -


### `isdouble ::: -> `

    -


### `mkinstance ::: -> `

    -


### `stol ::: -> `

    -


### `delete ::: -> `

    -


### `stoi ::: -> `

    -


### `stof ::: -> `

    -


### `not ::: -> `

    -


### `reload ::: -> `

    -


### `acopy ::: -> `

    -


### `_layer_call ::: -> `

    -


### `and ::: -> `

    -


### `_last_word ::: -> `

    -


### `strlowercase ::: -> `

    -


### `inc ::: -> `

    -


### `or ::: -> `

    -


### `gt ::: -> `

    -


### `_array ::: -> `

    -


### `defsuper ::: -> `

    -


### `strcontains ::: -> `

    -


### `dup ::: -> `

    -


## Variables

### TYPE_JIO

    -


### String

    -


### TYPE_SHADOW

    -


### TYPE_INT

    -


### TYPE_BYTE

    -


### TYPE_FUNCTION

    -


### TYPE_FLOAT

    -


### TYPE_NULL

    -


### TYPE_LONG

    -


### TYPE_STRING

    -


### TYPE_REFERENCE

    -


### Function

    -


### TYPE_ARRAY

    -


### TYPE_DOUBLE

    -


### Array

    -


### JIO

    -


### TYPE_CHAR

    -


