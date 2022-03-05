import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author TudbuT
 * @since 04 Mar 2022
 */

public class ISBPL {
    ArrayList<ISBPLType> types = new ArrayList<>();
    Stack<HashMap<String, ISBPLCallable>> functionStack = new Stack<>();
    HashMap<Object, ISBPLObject> vars = new HashMap<>();
    ArrayList<String> lastWords = new ArrayList<>(16);
    int exitCode;
    
    public ISBPL() {
        functionStack.push(new HashMap<>());
    }
    
    public ISBPLKeyword getKeyword(String word) {
        switch (word) {
            case "native":
                return (idx, words, file, stack) -> {
                    idx++;
                    addNative(words[idx], file, stack);
                    return idx;
                };
            case "func":
                return this::createFunction;
            case "def":
                return (idx, words, file, stack) -> {
                    idx++;
                    Object var = new Object();
                    functionStack.peek().put(words[idx], () -> stack.push(vars.get(var)));
                    functionStack.peek().put("=" + words[idx], () -> vars.put(var, stack.pop()));
                    return idx;
                };
            case "if":
                return (idx, words, file, stack) -> {
                    idx++;
                    AtomicInteger i = new AtomicInteger(idx);
                    ISBPLCallable callable = readBlock(i, words, file, stack, false);
                    if(stack.pop().isTruthy()) {
                        callable.call();
                    }
                    return i.get();
                };
            case "while":
                return (idx, words, file, stack) -> {
                    idx++;
                    AtomicInteger i = new AtomicInteger(idx);
                    ISBPLCallable cond = readBlock(i, words, file, stack, false);
                    i.getAndIncrement();
                    ISBPLCallable block = readBlock(i, words, file, stack, false);
                    cond.call();
                    while (stack.pop().isTruthy()) {
                        block.call();
                        cond.call();
                    }
                    return i.get();
                };
            case "stop":
                return (idx, words, file, stack) -> {
                    ISBPLObject o = stack.pop();
                    o.checkType(getType("int"));
                    throw new ISBPLStop((int) o.object);
                };
            case "try":
                return (idx, words, file, stack) -> {
                    idx++;
                    ISBPLObject array = stack.pop();
                    array.checkTypeMulti(getType("array"), getType("string"));
                    String[] allowed;
                    if(array.type.name.equals("string")) {
                        allowed = new String[] { toJavaString(array) };
                    }
                    else {
                        ISBPLObject[] arr = ((ISBPLObject[]) array.object);
                        allowed = new String[arr.length];
                        for (int i = 0 ; i < arr.length ; i++) {
                            allowed[i] = toJavaString(arr[i]);
                        }
                    }
                    AtomicInteger i = new AtomicInteger(idx);
                    ISBPLCallable block = readBlock(i, words, file, stack, false);
                    i.getAndIncrement();
                    ISBPLCallable catcher = readBlock(i, words, file, stack, false);
                    try {
                        block.call();
                    } catch (ISBPLError error) {
                        if (Arrays.asList(allowed).contains(error.type) || allowed.length != 1 && allowed[0].equals("all")) {
                            stack.push(toISBPLString(error.type));
                            stack.push(toISBPLString(error.message));
                            catcher.call();
                        }
                        else {
                            throw error;
                        }
                    }
                    return i.get();
                };
            case "do":
                return (idx, words, file, stack) -> {
                    idx++;
                    AtomicInteger i = new AtomicInteger(idx);
                    ISBPLCallable block = readBlock(i, words, file, stack, false);
                    i.getAndIncrement();
                    ISBPLCallable catcher = readBlock(i, words, file, stack, false);
                    try {
                        block.call();
                    } finally {
                        catcher.call();
                    }
                    return i.get();
                };
            default:
                return null;
        }
    }
    
    @SuppressWarnings("RedundantCast")
    private void addNative(String name, File file, Stack<ISBPLObject> stack) {
        ISBPLCallable func = null;
        switch (name) {
            case "alen":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    o.checkType(getType("array"));
                    stack.push(new ISBPLObject(getType("int"), ((ISBPLObject[]) o.object).length));
                };
                break;
            case "aget":
                func = () -> {
                    ISBPLObject i = stack.pop();
                    ISBPLObject o = stack.pop();
                    i.checkType(getType("int"));
                    o.checkType(getType("array"));
                    stack.push(((ISBPLObject[]) o.object)[((int) i.object)]);
                };
                break;
            case "aput":
                func = () -> {
                    ISBPLObject toPut = stack.pop();
                    ISBPLObject i = stack.pop();
                    ISBPLObject o = stack.pop();
                    i.checkType(getType("int"));
                    o.checkType(getType("array"));
                    ((ISBPLObject[]) o.object)[((int) i.object)] = toPut;
                };
                break;
            case "anew":
                func = () -> {
                    ISBPLObject i = stack.pop();
                    i.checkType(getType("int"));
                    stack.push(new ISBPLObject(getType("array"), new ISBPLObject[((int) i.object)]));
                };
                break;
            case "_array":
                func = () -> {
                    ISBPLObject a = stack.pop();
                    if(a.type.equals(getType("array")))
                        stack.push(a);
                    else if(a.object instanceof ISBPLObject[])
                        stack.push(new ISBPLObject(getType("array"), a.object));
                    else
                        typeError(a.type.name, "array");
                };
                break;
            case "_char":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("char"), ((char) o.toLong())));
                };
                break;
            case "_byte":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("byte"), ((byte) o.toLong())));
                };
                break;
            case "_int":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("int"), ((int) o.toLong())));
                };
                break;
            case "_float":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("float"), ((float) o.toDouble())));
                };
                break;
            case "_long":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("long"), o.toLong()));
                };
                break;
            case "_double":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("double"), o.toDouble()));
                };
                break;
            case "ischar":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("char")) ? 1 : 0));
                };
                break;
            case "isbyte":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("byte")) ? 1 : 0));
                };
                break;
            case "isint":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("int")) ? 1 : 0));
                };
                break;
            case "isfloat":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("float")) ? 1 : 0));
                };
                break;
            case "islong":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("long")) ? 1 : 0));
                };
                break;
            case "isdouble":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("double")) ? 1 : 0));
                };
                break;
            case "isarray":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("array")) ? 1 : 0));
                };
                break;
            case "_layer_call":
                func = () -> {
                    ISBPLObject i = stack.pop();
                    ISBPLObject s = stack.pop();
                    i.checkType(getType("int"));
                    functionStack.get(functionStack.size() - 1 - ((int) i.object)).get(toJavaString(s)).call();
                };
                break;
            case "include":
                func = () -> {
                    ISBPLObject s = stack.pop();
                    String filepath = toJavaString(s);
                    processPath:
                    {
                        if (filepath.startsWith("/"))
                            break processPath;
                        if (filepath.startsWith("#")) {
                            filepath = System.getenv().getOrDefault("ISBPL_PATH", "/usr/lib/isbpl") + "/" + filepath.substring(1);
                            break processPath;
                        }
                        filepath = file.getParentFile().getAbsolutePath() + "/" + filepath;
                    }
                    File f = new File(filepath).getAbsoluteFile();
                    try {
                        interpret(f, readFile(f), stack);
                    }
                    catch (IOException e) {
                        throw new ISBPLError("IO", "Couldn't find file " + filepath + " required by include keyword.");
                    }
                };
                break;
            case "putchar":
                func = () -> {
                    ISBPLObject c = stack.pop();
                    c.checkType(getType("char"));
                    System.out.print(((char) c.object));
                };
                break;
            case "eputchar":
                func = () -> {
                    ISBPLObject c = stack.pop();
                    c.checkType(getType("char"));
                    System.err.print(((char) c.object));
                };
                break;
            case "_file":
                func = () -> {
                    ISBPLObject s = stack.pop();
                    File f = new File(toJavaString(s));
                    stack.push(new ISBPLObject(getType("file"), f));
                };
                break;
            case "read":
                func = () -> {
                    ISBPLObject end = stack.pop();
                    ISBPLObject begin = stack.pop();
                    ISBPLObject fileToRead = stack.pop();
                    end.checkType(getType("int"));
                    begin.checkType(getType("int"));
                    fileToRead.checkType(getType("file"));
                    try {
                        FileInputStream f = new FileInputStream((File) fileToRead.object);
                        int b = ((int) begin.object);
                        int e = ((int) end.object);
                        byte[] bytes = new byte[e - b];
                        f.read(bytes, b, e);
                        ISBPLObject[] arr = new ISBPLObject[bytes.length];
                        for (int i = 0 ; i < arr.length ; i++) {
                            arr[i] = new ISBPLObject(getType("byte"), bytes[i]);
                        }
                        stack.push(new ISBPLObject(getType("array"), arr));
                    }
                    catch (FileNotFoundException e) {
                        throw new ISBPLError("FileNotFound", "File not found.");
                    }
                    catch (IOException e) {
                        throw new ISBPLError("IO", "File couldn't be read from" + (e.getMessage() != null ? ": " + e.getMessage() : "."));
                    }
                };
                break;
            case "flength":
                func = () -> {
                    ISBPLObject f = stack.pop();
                    f.checkType(getType("file"));
                    stack.push(new ISBPLObject(getType("int"), ((int) ((File) f.object).length())));
                };
                break;
            case "write":
                func = () -> {
                    ISBPLObject content = stack.pop();
                    ISBPLObject fileToWrite = stack.pop();
                    content.checkType(getType("array"));
                    fileToWrite.checkType(getType("file"));
                    throw new ISBPLError("NotImplemented", "_file write is not implemented");
                };
                break;
            case "getos":
                func = () -> {
                    // TODO: This is not done yet, and it's horrible so far.
                    stack.push(toISBPLString("linux"));
                };
                break;
            case "mktype":
                func = () -> {
                    ISBPLObject s = stack.pop();
                    ISBPLType type = registerType(toJavaString(s));
                    stack.push(new ISBPLObject(getType("int"), type));
                };
                break;
            case "typename":
                func = () -> {
                    ISBPLObject i = stack.pop();
                    i.checkType(getType("int"));
                    stack.push(toISBPLString(types.get(((int) i.object)).name));
                };
                break;
            case "gettype":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.id));
                };
                break;
            case "throw":
                func = () -> {
                    ISBPLObject message = stack.pop();
                    ISBPLObject type = stack.pop();
                    String msg = toJavaString(message);
                    String t = toJavaString(type);
                    throw new ISBPLError(t, msg);
                };
                break;
            case "exit":
                func = () -> {
                    ISBPLObject code = stack.pop();
                    code.checkType(getType("int"));
                    exitCode = ((int) code.object);
                    throw new ISBPLStop(0);
                };
                break;
            case "eq":
                func = () -> {
                    ISBPLObject o1 = stack.pop();
                    ISBPLObject o2 = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o1.equals(o2) ? 1 : 0));
                };
                break;
            case "gt":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("int"), o1.toDouble() > o2.toDouble() ? 1 : 0));
                };
                break;
            case "lt":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("int"), o1.toDouble() < o2.toDouble() ? 1 : 0));
                };
                break;
            case "not":
                func = () -> {
                    stack.push(new ISBPLObject(getType("int"), stack.pop().isTruthy() ? 0 : 1));
                };
                break;
            case "neg":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(o.type, o.negative()));
                };
                break;
            case "or":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    if(o1.isTruthy())
                        stack.push(o1);
                    else
                        stack.push(o2);
                };
                break;
            case "and":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    // Pushes either 1 or the failed object
                    if (o1.isTruthy()) {
                        if (o2.isTruthy())
                            stack.push(new ISBPLObject(getType("int"), 1));
                        else
                            stack.push(o2);
                    }
                    else
                        stack.push(o1);
                };
                break;
            case "+":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) (Integer) object1 + (int) (Integer) object2);
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) (Long) object1 + (long) (Long) object2);
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) (Character) object1 + (char) (Character) object2);
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), Byte.toUnsignedInt((Byte) object1) + Byte.toUnsignedInt((Byte) object2));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) (Float) object1 + (float) (Float) object2);
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) (Double) object1 + (double) (Double) object2);
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "-":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) (Integer) object1 - (int) (Integer) object2);
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) (Long) object1 - (long) (Long) object2);
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) (Character) object1 - (char) (Character) object2);
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), Byte.toUnsignedInt((Byte) object1) - Byte.toUnsignedInt((Byte) object2));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) (Float) object1 - (float) (Float) object2);
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) (Double) object1 - (double) (Double) object2);
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "/":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) (Integer) object1 / (int) (Integer) object2);
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) (Long) object1 / (long) (Long) object2);
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) (Character) object1 / (char) (Character) object2);
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), Byte.toUnsignedInt((Byte) object1) / Byte.toUnsignedInt((Byte) object2));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) (Float) object1 / (float) (Float) object2);
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) (Double) object1 / (double) (Double) object2);
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "*":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) (Integer) object1 * (int) (Integer) object2);
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) (Long) object1 * (long) (Long) object2);
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) (Character) object1 * (char) (Character) object2);
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), Byte.toUnsignedInt((Byte) object1) * Byte.toUnsignedInt((Byte) object2));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) (Float) object1 * (float) (Float) object2);
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) (Double) object1 * (double) (Double) object2);
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "**":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), Math.pow((int) (Integer) object1, (int) (Integer) object2));
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), Math.pow((long) (Long) object1, (long) (Long) object2));
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), Math.pow((char) (Character) object1, (char) (Character) object2));
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), Math.pow(Byte.toUnsignedInt((Byte) object1), Byte.toUnsignedInt((Byte) object2)));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), Math.pow((float) (Float) object1, (float) (Float) object2));
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), Math.pow((double) (Double) object1, (double) (Double) object2));
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "%":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) (Integer) object1 % (int) (Integer) object2);
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) (Long) object1 % (long) (Long) object2);
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) (Character) object1 % (char) (Character) object2);
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), Byte.toUnsignedInt((Byte) object1) % Byte.toUnsignedInt((Byte) object2));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) (Float) object1 % (float) (Float) object2);
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) (Double) object1 % (double) (Double) object2);
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "^":
                func = () -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("long"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("long"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) (Integer) object1 ^ (int) (Integer) object2);
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) (Long) object1 ^ (long) (Long) object2);
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) (Character) object1 ^ (char) (Character) object2);
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), Byte.toUnsignedInt((Byte) object1) ^ Byte.toUnsignedInt((Byte) object2));
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "dup":
                func = () -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(o.type, o.object));
                    stack.push(new ISBPLObject(o.type, o.object));
                };
                break;
            case "pop":
                func = stack::pop;
                break;
            case "swap":
                func = () -> {
                    ISBPLObject o1 = stack.pop();
                    ISBPLObject o2 = stack.pop();
                    stack.push(o2);
                    stack.push(o1);
                };
                break;
        }
        functionStack.peek().put(name, func);
    }
    
    private int createFunction(int i, String[] words, File file, Stack<ISBPLObject> stack) {
        i++;
        String name = words[i];
        AtomicInteger integer = new AtomicInteger(++i);
        ISBPLCallable callable = readBlock(integer, words, file, stack, true);
        i = integer.get();
        functionStack.peek().put(name, callable);
        return i;
    }
    
    private ISBPLCallable readBlock(AtomicInteger idx, String[] words, File file, Stack<ISBPLObject> stack, boolean isFunction) {
        ArrayList<String> newWords = new ArrayList<>();
        int i = idx.get();
        i++;
        int lvl = 1;
        for (; i < words.length && lvl > 0 ; i++) {
            String word = words[i];
            if(word.equals("{"))
                lvl++;
            if(word.equals("}")) {
                if(--lvl == 0)
                    break;
            }
            newWords.add(word);
        }
        idx.set(i);
        String[] theWords = newWords.toArray(new String[0]);
        return () -> interpretRaw(file, theWords, stack, isFunction);
    }
    
    public String toJavaString(ISBPLObject string) {
        string.checkType(getType("string"));
        ISBPLObject[] array = ((ISBPLObject[]) string.object);
        char[] chars = new char[array.length];
        for (int i = 0 ; i < array.length ; i++) {
            chars[i] = ((char) array[i].object);
        }
        return new String(chars);
    }
    
    public ISBPLObject toISBPLString(String s) {
        char[] chars = s.toCharArray();
        ISBPLObject[] objects = new ISBPLObject[chars.length];
        ISBPLType type = getType("char");
        for (int i = 0 ; i < chars.length ; i++) {
            objects[i] = new ISBPLObject(type, chars[i]);
        }
        return new ISBPLObject(getType("string"), objects);
    }
    
    public ISBPLType registerType(String name) {
        ISBPLType type = new ISBPLType(name);
        types.add(type);
        return type;
    }
    
    // These will die as soon as std creates the real types and any types created before these are replaced become invalid.
    static final ISBPLType defaultTypeInt = new ISBPLType("int");
    static final ISBPLType defaultTypeString = new ISBPLType("string");
    
    public ISBPLType getType(String name) {
        for (int i = 0 ; i < types.size() ; i++) {
            if(types.get(i).name.equals(name))
                return types.get(i);
        }
        if(name.equals("int"))
            return defaultTypeInt;
        if(name.equals("string"))
            return defaultTypeString;
        return null;
    }
    public void typeError(String got, String wanted) {
        throw new ISBPLError("IncompatibleTypes", "Incompatible types: " + got + " - " + wanted);
    }
    
    public void interpret(File file, String code, Stack<ISBPLObject> stack) {
        code = cleanCode(code);
        String[] words = splitWords(code);
        interpretRaw(file, words, stack, false);
    }
    
    private void interpretRaw(File file, String[] words, Stack<ISBPLObject> stack, boolean isFunction) {
        if(isFunction)
            functionStack.push(new HashMap<>());
        try {
            for (int i = 0 ; i < words.length ; i++) {
                String word = words[i];
                if (word.length() == 0)
                    continue;
                lastWords.add(0, word);
                while(lastWords.size() > 16)
                    lastWords.remove(lastWords.size() - 1);
                ISBPLKeyword keyword = getKeyword(word);
                if (keyword != null) {
                    i = keyword.call(i, words, file, stack);
                    continue;
                }
                ISBPLCallable func = functionStack.peek().get(word);
                if(func != null) {
                    func.call();
                    continue;
                }
                func = functionStack.get(0).get(word);
                if(func != null) {
                    func.call();
                    continue;
                }
                if (word.startsWith("\"")) {
                    stack.push(toISBPLString(word.substring(1)));
                    continue;
                }
                try {
                    stack.push(new ISBPLObject(getType("int"), Integer.parseInt(word)));
                    continue;
                } catch (Exception ignore) {}
                try {
                    stack.push(new ISBPLObject(getType("long"), Long.parseLong(word)));
                    continue;
                } catch (Exception ignore) {}
                try {
                    stack.push(new ISBPLObject(getType("float"), Float.parseFloat(word)));
                    continue;
                } catch (Exception ignore) {}
                try {
                    stack.push(new ISBPLObject(getType("double"), Double.parseDouble(word)));
                    continue;
                } catch (Exception ignore) {}
                throw new ISBPLError("InvalidWord", word + " is not a function, object, or keyword.");
            }
        } catch (ISBPLStop stop) {
            if(stop.amount == 0)
                return;
            throw new ISBPLStop(stop.amount);
        } finally {
            if(isFunction)
                functionStack.pop();
        }
    }
    
    // Magic, please test before pushing changes!
    private String[] splitWords(String code) {
        ArrayList<String> words = new ArrayList<>();
        char[] chars = code.toCharArray();
        boolean isInString = false;
        boolean escaping = false;
        String word = "";
        for (int i = 0 ; i < chars.length ; i++) {
            char c = chars[i];
            if(isInString) {
                if(c == '\\') {
                    escaping = !escaping;
                    if(escaping)
                        continue;
                }
                if(c == 'n' && escaping) {
                    word += '\n';
                    escaping = false;
                    continue;
                }
                if(c == 'r' && escaping) {
                    escaping = false;
                    word += '\r';
                    continue;
                }
                if(c == '"') {
                    if (escaping) {
                        escaping = false;
                    }
                    else {
                        isInString = false;
                        continue;
                    }
                }
                word += c;
                if(escaping)
                    throw new RuntimeException("Error parsing code: Invalid Escape.");
            }
            else if(c == '"' && word.length() == 0) {
                word += '"';
                isInString = true;
            }
            else if(c == ' ') {
                words.add(word);
                word = "";
            }
            else {
                word += c;
            }
        }
        words.add(word);
        return words.toArray(new String[0]);
    }
    
    private String cleanCode(String code) {
        return code
                .replaceAll("\r", "\n")
                .replaceAll("\n", " ")
                ;
    }
    
    public static void main(String[] args) throws IOException {
        Stack<ISBPLObject> stack = new Stack<>();
        ISBPL isbpl = new ISBPL();
        try {
            File std = new File(System.getenv().getOrDefault("ISBPL_PATH", "/usr/lib/isbpl") + "/std.isbpl");
            isbpl.interpret(std, readFile(std), stack);
            File file = new File(args[0]).getAbsoluteFile();
            isbpl.interpret(file, readFile(file), stack);
            stack.push(argarray(isbpl, args));
            isbpl.interpret(file, "main exit", stack);
        } catch (ISBPLStop stop) {
            System.exit(isbpl.exitCode);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(stack);
        }
    }
    
    private static ISBPLObject argarray(ISBPL isbpl, String[] args) {
        ISBPLObject[] array = new ISBPLObject[args.length - 1];
        for (int i = 1 ; i < args.length ; i++) {
            array[i - 1] = new ISBPLObject(isbpl.getType("string"), isbpl.toISBPLString(args[i]));
        }
        return new ISBPLObject(isbpl.getType("array"), array);
    }
    
    private static String readFile(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] currentBytes = new byte[4096];
        int len;
        while ((len = fis.read(currentBytes)) > 0) {
            bytes.write(currentBytes, 0, len);
        }
        return bytes.toString();
    }
}

interface ISBPLKeyword {
    int call(int idx, String[] words, File file, Stack<ISBPLObject> stack);
}

interface ISBPLCallable {
    void call();
}

class ISBPLType {
    static int gid = -2;
    int id = gid++;
    String name;
    
    public ISBPLType(String name) {
        this.name = name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISBPLType)) return false;
        ISBPLType type = (ISBPLType) o;
        return id == type.id;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
}

class ISBPLObject {
    final ISBPLType type;
    final Object object;
    
    public ISBPLObject(ISBPLType type, Object object) {
        this.type = type;
        this.object = object;
    }
    
    public boolean isTruthy() {
        return object != null && object != Integer.valueOf(0);
    }
    
    // This has heavy optimizations, please do not change unless necessary
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof ISBPLObject)) return false;
        ISBPLObject object = (ISBPLObject) o;
        if(this.object == object.object)
            return true;
        if(this.object == null)
            return false;
        if(object.object == null)
            return false;
        if(this.object.getClass().isArray() || object.object.getClass().isArray()) {
            if(this.object.getClass().isArray() && object.object.getClass().isArray()) {
                return Arrays.equals((Object[]) this.object, (Object[]) object.object);
            }
            else {
                return false;
            }
        }
        return this.object.equals(object.object);
    }
    
    public void checkType(ISBPLType wanted) {
        if(wanted.id != type.id) {
            throw new ISBPLError("IncompatibleTypes", "Incompatible types: " + type.name + " - " + wanted.name);
        }
    }
    
    public int checkTypeMulti(ISBPLType... wanted) {
        int f = -1;
        String wantedNames = "";
        for (int i = 0 ; i < wanted.length ; i++) {
            wantedNames += " " + wanted[i].name;
            if(wanted[i].id == type.id) {
                f = i;
                break;
            }
        }
        if(f == -1) {
            throw new ISBPLError("IncompatibleTypes", "Incompatible types: " + type.name + " - " + wantedNames.substring(1));
        }
        return f;
    }
    
    public double toDouble() {
        if(object instanceof Integer) {
            return (double) (int) (Integer) object;
        }
        if(object instanceof Long) {
            return (double) (long) (Long) object;
        }
        if(object instanceof Character) {
            return (double) (char) (Character) object;
        }
        if(object instanceof Byte) {
            return Byte.toUnsignedInt((Byte) object);
        }
        if(object instanceof Float) {
            return (double) (float) (Float) object;
        }
        if(object instanceof Double) {
            return (double) (Double) object;
        }
        throw new ISBPLError("InvalidArgument", "The argument is not a number.");
    }
    
    public long toLong() {
        if(object instanceof Integer) {
            return (long) (int) (Integer) object;
        }
        if(object instanceof Long) {
            return (long) (Long) object;
        }
        if(object instanceof Character) {
            return (long) (char) (Character) object;
        }
        if(object instanceof Byte) {
            return Byte.toUnsignedInt((Byte) object);
        }
        if(object instanceof Float) {
            return (long) (float) (Float) object;
        }
        if(object instanceof Double) {
            return (long) (double) (Double) object;
        }
        throw new ISBPLError("InvalidArgument", "The argument is not a number.");
    }
    
    public Object negative() {
        if(object instanceof Integer) {
            return -(int) (Integer) object;
        }
        if(object instanceof Long) {
            return -(long) (Long) object;
        }
        if(object instanceof Float) {
            return -(float) (Float) object;
        }
        if(object instanceof Double) {
            return -(double) (Double) object;
        }
        throw new ISBPLError("InvalidArgument", "This type of number can't be negated!");
    }
}

class ISBPLError extends RuntimeException {
    final String type;
    final String message;
    
    public ISBPLError(String type, String message) {
        this.type = type;
        this.message = message;
    }
    
    @Override
    public String getMessage() {
        return type + ": " + message;
    }
}

class ISBPLStop extends RuntimeException {
    
    int amount;

    public ISBPLStop(int amount) {
        this.amount = amount - 1;
    }
}
