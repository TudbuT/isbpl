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
( ( ( 2 1 * ) 1 + ) print )
```
in both languages, this will print 3.

These examples used the print function, which does not exist by default, instead, puts should be used in combination with \_string.
