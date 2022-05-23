# Documentation

In this directory, you will find documentation files about ISBPL.

## Language design

ISBPL is a concatenative language, meaning non-keyword instructions are executed in-order. 

Example: `42 27 +`
- First, 42 is executed, which pushes ISBPLObject{type=ISBPLType{id=0, name='int', superTypes=[]}, object=***__42__***} onto the stack
- Then, 27 is executed, which pushes ISBPLObject{type=ISBPLType{id=0, name='int', superTypes=[]}, object=***__27__***} onto the stack
- In the end, + is executed from the stdlib, which does the following (stack pseudocode): `a b -> c WHERE c=math(a + b)`

Here is the full example in stack pseudocode:
- `42 :::     -> a WHERE a=42         ` ::: *42*
- `27 :::     -> a WHERE a=27         ` ::: 42 *27*
- `+  ::: a b -> c WHERE c=math(a + b)` ::: ~~42 27~~ *69*

Something is truthy, when it is not null AND not 0

## Function resolving

When a function is to be resolved, the resolver checks the following where WORD is the function name:
- Hand WORD over to the keyword resolver, if it finds one, execute and RETURN
- If the stack is NOT empty:
  - Check, if the topmost object on the stack has a method of the name WORD, if it does, execute and RETURN
- Check for a function of the name WORD in the current frame, if it find it, execute and RETURN
- Check, if WORD is a number, if so, push and RETURN
- (nothing is found) Throw an InvalidWord error

## Types

A type is defined by its numeric ID and its name. A type can be redefined, but will have a different numeric ID
despite the name being the same. 
An object always has a type, there are no primitives.

A type has the following properties:
- A list of variables
- A list of methods (this includes automatically generated ones to get/set variable values)
- Supertypes

To define a type, the **`construct`** keyword is used. `-> a WHERE a=createdType.id`
```ìsbpl
construct name [names of supertypes separated by spaces] {
    [variables separated by spaces or newlines]
    ;
    [methods separated by spaces and newlines]
}
```

Example:
```isbpl
construct call {
    date duration
    num
    outbound
    ;
    setnum {
        [method body redacted for simplicity]
    }
}
```
Here, the setnum method is custom. The automatically defined one is `=num ::: a -> WHERE a=newValue`.

Methods named `construct ::: a -> b WHERE a=(an empty object), b=(the instance the constructor has made)` (yes, the same name as the keyword) are constructors. 
Usually, a and b are the same, but for some types, the automatically generated empty object is not fitting, and a different object can be used. 
This can store one value of data (useful for quick typecasting) and does not have to be unique as the fields are associated with an instance ID.

To forcefully set the type of an object (this can cause errors if the instance objects aren't compatible), use `settype ::: a b -> a WHERE a=object, b=typeID`.

## Keywords

- `native <name>`: Used to import a native function from the interpreter
- `func <name> <block>`: Used to define a function
- `def <name>`: Used to define a variable. To use it, the functions `<name> ::: -> a WHERE a=value` and `=<name> ::: a -> WHERE a=newValue` are generated.
- `if <block>`: Used to run block only if the (popped) value from the top of the stack is truthy
- `while <condition block> <code block>`: Runs condition block and checks if popped value is truthy. If so, code block is run. This process is repeated until it is no longer truthy.
- `stop`: Pops a value, and exits as many blocks as the popped value.
- `try <block> <catch block>`: Pops a string or array of strings, and catches any of the errors that these strings match, pushing the error and its message when running the catch block. The catch block is only run if there is an error.
- `do <block> <finally block>`: Runs block, and, no matter what, runs the finally block aswell, regardless of any errors in the main block. This is similar to try, but does not need error types to be on the stack, and does neither catch nor push the error if one is thrown.
- `{` (yes, this is a keyword name): Reads code until it hits the matching close bracket, pushes an anonymous function onto the stack, able to be called with `fcall ::: a -> WHERE a=function`.
- `fork <block>`: Runs <block> in a new thread.
- `construct <...>`: Explained above.
- `with <names separated by spaces> ;`: Pops values, and puts them into variables. The closest name to the semicolon is filled first, then the 2nd closest, etc.
- `string! { <anything> }`: Pushes a string containing the raw words inside of the block. The words are not executed, but the block ends on the matching curly brace, meaning the number of curly braces inside of the block have to be even. Newlines are handled like spaces, any number of consecutive spaces will only be one space in the final string.
