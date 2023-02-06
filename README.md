# isbpl
Improved Stack-Based Programming Language

Incomplete, not currently compilable, only interpretable.

Stuff: [TudbuT/isbpl-random-stuff](https://github.com/TudbuT/isbpl-random-stuff)

---

## ISBPL is similar to Lisp:

```lisp
(print (+ 1 (* 1 2)))
```
is the same as
```isbpl
2 1 * 1 + print
```
or
```isbpl
(((2 1 *) 1 +) print)
```
in both languages, this will print 3.

These examples used the print function, which does not exist by default, instead, puts should be used in combination with \_string.

---

## Objects, Functions, and Variables in ISBPL
OOP works like this:

- There are three separate function resolvers:
  - Object
  - Local
  - (Multiple more levels determined by the frame height)
  - Global
- They are executed in the order shown above
- The object resolver peeks onto the stack, gets the type of the object, and checks for methods on the type, if it finds one, it executes it
- The local resolver checks for functions defined in the current function, but not in any other function or area
- The global resolver checks for top-level functions, meaning ones that arent in any other function.
- Because variables are native functions under the hood, they are also called by the function resolvers
- Object-local variables are in a Table: `Type?->Instance?->ID?->Value`

### To call a method of an object:

```isbpl
parameter1 parameter2 etc object method
```
As explained above, methods are resolved separately, and it is therefore not required to define them in any other way.
