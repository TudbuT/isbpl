import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
    static boolean debug = false;
    public ISBPLDebugger.IPC debuggerIPC = new ISBPLDebugger.IPC();
    ArrayList<ISBPLType> types = new ArrayList<>();
    Stack<HashMap<String, ISBPLCallable>> functionStack = new Stack<>();
    HashMap<Object, ISBPLObject> vars = new HashMap<>();
    ArrayList<String> lastWords = new ArrayList<>(16);
    int exitCode;
    private ISBPLStreamer streamer = new ISBPLStreamer(this);
    
    public ISBPL() {
        functionStack.push(new HashMap<>());
    }
    
    public ISBPLKeyword getKeyword(String word) {
        switch (word) {
            case "native":
                return (idx, words, file, stack) -> {
                    idx++;
                    addNative(words[idx], stack);
                    return idx;
                };
            case "func":
                return (i1, words1, stack1, stack12) -> createFunction(i1, words1, stack12);
            case "def":
                return (idx, words, file, stack) -> {
                    idx++;
                    Object var = new Object();
                    functionStack.peek().put(words[idx], (file1) -> stack.push(vars.get(var)));
                    functionStack.peek().put("=" + words[idx], (file1) -> vars.put(var, stack.pop()));
                    return idx;
                };
            case "if":
                return (idx, words, file, stack) -> {
                    idx++;
                    AtomicInteger i = new AtomicInteger(idx);
                    ISBPLCallable callable = readBlock(i, words, stack, false);
                    if(stack.pop().isTruthy()) {
                        callable.call(file);
                    }
                    return i.get();
                };
            case "while":
                return (idx, words, file, stack) -> {
                    idx++;
                    AtomicInteger i = new AtomicInteger(idx);
                    ISBPLCallable cond = readBlock(i, words, stack, false);
                    i.getAndIncrement();
                    ISBPLCallable block = readBlock(i, words, stack, false);
                    cond.call(file);
                    while (stack.pop().isTruthy()) {
                        block.call(file);
                        cond.call(file);
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
                    ISBPLCallable block = readBlock(i, words, stack, false);
                    i.getAndIncrement();
                    ISBPLCallable catcher = readBlock(i, words, stack, false);
                    try {
                        block.call(file);
                    } catch (ISBPLError error) {
                        if (Arrays.asList(allowed).contains(error.type) || allowed.length != 1 && allowed[0].equals("all")) {
                            stack.push(toISBPLString(error.message));
                            stack.push(toISBPLString(error.type));
                            catcher.call(file);
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
                    ISBPLCallable block = readBlock(i, words, stack, false);
                    i.getAndIncrement();
                    ISBPLCallable catcher = readBlock(i, words, stack, false);
                    try {
                        block.call(file);
                    } finally {
                        catcher.call(file);
                    }
                    return i.get();
                };
            default:
                return null;
        }
    }
    
    @SuppressWarnings("RedundantCast")
    private void addNative(String name, Stack<ISBPLObject> stack) {
        ISBPLCallable func = null;
        switch (name) {
            case "alen":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    o.checkType(getType("array"));
                    stack.push(new ISBPLObject(getType("int"), ((ISBPLObject[]) o.object).length));
                };
                break;
            case "aget":
                func = (File file) -> {
                    ISBPLObject i = stack.pop();
                    ISBPLObject o = stack.pop();
                    i.checkType(getType("int"));
                    o.checkType(getType("array"));
                    stack.push(((ISBPLObject[]) o.object)[((int) i.object)]);
                };
                break;
            case "aput":
                func = (File file) -> {
                    ISBPLObject toPut = stack.pop();
                    ISBPLObject i = stack.pop();
                    ISBPLObject o = stack.pop();
                    i.checkType(getType("int"));
                    o.checkType(getType("array"));
                    ((ISBPLObject[]) o.object)[((int) i.object)] = toPut;
                };
                break;
            case "anew":
                func = (File file) -> {
                    ISBPLObject i = stack.pop();
                    i.checkType(getType("int"));
                    stack.push(new ISBPLObject(getType("array"), new ISBPLObject[((int) i.object)]));
                };
                break;
            case "_array":
                func = (File file) -> {
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
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("char"), ((char) o.toLong())));
                };
                break;
            case "_byte":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("byte"), ((byte) o.toLong())));
                };
                break;
            case "_int":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("int"), ((int) o.toLong())));
                };
                break;
            case "_float":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("float"), ((float) o.toDouble())));
                };
                break;
            case "_long":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("long"), o.toLong()));
                };
                break;
            case "_double":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("double"), o.toDouble()));
                };
                break;
            case "ischar":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("char")) ? 1 : 0));
                };
                break;
            case "isbyte":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("byte")) ? 1 : 0));
                };
                break;
            case "isint":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("int")) ? 1 : 0));
                };
                break;
            case "isfloat":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("float")) ? 1 : 0));
                };
                break;
            case "islong":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("long")) ? 1 : 0));
                };
                break;
            case "isdouble":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("double")) ? 1 : 0));
                };
                break;
            case "isarray":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.equals(getType("array")) ? 1 : 0));
                };
                break;
            case "_layer_call":
                func = (File file) -> {
                    ISBPLObject i = stack.pop();
                    ISBPLObject s = stack.pop();
                    i.checkType(getType("int"));
                    functionStack.get(functionStack.size() - 1 - ((int) i.object)).get(toJavaString(s)).call(file);
                };
                break;
            case "include":
                func = (File file) -> {
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
                func = (File file) -> {
                    ISBPLObject c = stack.pop();
                    c.checkType(getType("char"));
                    System.out.print(((char) c.object));
                };
                break;
            case "eputchar":
                func = (File file) -> {
                    ISBPLObject c = stack.pop();
                    c.checkType(getType("char"));
                    System.err.print(((char) c.object));
                };
                break;
            case "_file":
                func = (File file) -> {
                    ISBPLObject s = stack.pop();
                    File f = new File(toJavaString(s));
                    stack.push(new ISBPLObject(getType("file"), f));
                };
                break;
            case "read":
                func = (File file) -> {
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
                func = (File file) -> {
                    ISBPLObject f = stack.pop();
                    f.checkType(getType("file"));
                    stack.push(new ISBPLObject(getType("int"), ((int) ((File) f.object).length())));
                };
                break;
            case "write":
                func = (File file) -> {
                    ISBPLObject content = stack.pop();
                    ISBPLObject fileToWrite = stack.pop();
                    content.checkType(getType("array"));
                    fileToWrite.checkType(getType("file"));
                    throw new ISBPLError("NotImplemented", "_file write is not implemented");
                };
                break;
            case "getos":
                func = (File file) -> {
                    // TODO: This is not done yet, and it's horrible so far.
                    stack.push(toISBPLString("linux"));
                };
                break;
            case "mktype":
                func = (File file) -> {
                    ISBPLObject s = stack.pop();
                    ISBPLType type = registerType(toJavaString(s));
                    stack.push(new ISBPLObject(getType("int"), type.id));
                };
                break;
            case "typename":
                func = (File file) -> {
                    ISBPLObject i = stack.pop();
                    i.checkType(getType("int"));
                    stack.push(toISBPLString(types.get(((int) i.object)).name));
                };
                break;
            case "gettype":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o.type.id));
                };
                break;
            case "settype":
                func = (File file) -> {
                    ISBPLObject i = stack.pop();
                    ISBPLObject o = stack.pop();
                    i.checkType(getType("int"));
                    stack.push(new ISBPLObject(types.get(((int) i.object)), o.object));
                };
                break;
            case "throw":
                func = (File file) -> {
                    ISBPLObject message = stack.pop();
                    ISBPLObject type = stack.pop();
                    String msg = toJavaString(message);
                    String t = toJavaString(type);
                    throw new ISBPLError(t, msg);
                };
                break;
            case "exit":
                func = (File file) -> {
                    ISBPLObject code = stack.pop();
                    code.checkType(getType("int"));
                    exitCode = ((int) code.object);
                    throw new ISBPLStop(0);
                };
                break;
            case "eq":
                func = (File file) -> {
                    ISBPLObject o1 = stack.pop();
                    ISBPLObject o2 = stack.pop();
                    stack.push(new ISBPLObject(getType("int"), o1.equals(o2) ? 1 : 0));
                };
                break;
            case "gt":
                func = (File file) -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("int"), o1.toDouble() > o2.toDouble() ? 1 : 0));
                };
                break;
            case "lt":
                func = (File file) -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(getType("int"), o1.toDouble() < o2.toDouble() ? 1 : 0));
                };
                break;
            case "not":
                func = (File file) -> {
                    stack.push(new ISBPLObject(getType("int"), stack.pop().isTruthy() ? 0 : 1));
                };
                break;
            case "neg":
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    o.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    stack.push(new ISBPLObject(o.type, o.negative()));
                };
                break;
            case "or":
                func = (File file) -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    if(o1.isTruthy())
                        stack.push(o1);
                    else
                        stack.push(o2);
                };
                break;
            case "and":
                func = (File file) -> {
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
                func = (File file) -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) ((int) (Integer) object1 + (int) (Integer) object2));
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) ((long) (Long) object1 + (long) (Long) object2));
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) ((char) (Character) object1 + (char) (Character) object2));
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), (byte) (Byte.toUnsignedInt((Byte) object1) + Byte.toUnsignedInt((Byte) object2)));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) ((float) (Float) object1 + (float) (Float) object2));
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) ((double) (Double) object1 + (double) (Double) object2));
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "-":
                func = (File file) -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) ((int) (Integer) object1 - (int) (Integer) object2));
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) ((long) (Long) object1 - (long) (Long) object2));
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) ((char) (Character) object1 - (char) (Character) object2));
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), (byte) (Byte.toUnsignedInt((Byte) object1) - Byte.toUnsignedInt((Byte) object2)));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) ((float) (Float) object1 - (float) (Float) object2));
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) ((double) (Double) object1 - (double) (Double) object2));
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "/":
                func = (File file) -> {
                    try {
                        ISBPLObject o2 = stack.pop();
                        ISBPLObject o1 = stack.pop();
                        o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                        o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                        Object object1 = o1.object;
                        Object object2 = o2.object;
                        ISBPLObject r = null;
                        if (object1 instanceof Integer && object2 instanceof Integer) {
                            r = new ISBPLObject(getType("int"), (int) ((int) (Integer) object1 / (int) (Integer) object2));
                        }
                        if (object1 instanceof Long && object2 instanceof Long) {
                            r = new ISBPLObject(getType("long"), (long) ((long) (Long) object1 / (long) (Long) object2));
                        }
                        if (object1 instanceof Character && object2 instanceof Character) {
                            r = new ISBPLObject(getType("char"), (char) ((char) (Character) object1 / (char) (Character) object2));
                        }
                        if (object1 instanceof Byte && object2 instanceof Byte) {
                            r = new ISBPLObject(getType("byte"), (byte) (Byte.toUnsignedInt((Byte) object1) / Byte.toUnsignedInt((Byte) object2)));
                        }
                        if (object1 instanceof Float && object2 instanceof Float) {
                            r = new ISBPLObject(getType("float"), (float) ((float) (Float) object1 / (float) (Float) object2));
                        }
                        if (object1 instanceof Double && object2 instanceof Double) {
                            r = new ISBPLObject(getType("double"), (double) ((double) (Double) object1 / (double) (Double) object2));
                        }
                        if (r != null)
                            stack.push(r);
                        else
                            typeError(o1.type.name, o2.type.name);
                    } catch (ArithmeticException ex) {
                        throw new ISBPLError("Arithmetic", "Division by 0");
                    }
                };
                break;
            case "*":
                func = (File file) -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) ((int) (Integer) object1 * (int) (Integer) object2));
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) ((long) (Long) object1 * (long) (Long) object2));
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) ((char) (Character) object1 * (char) (Character) object2));
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), (byte) (Byte.toUnsignedInt((Byte) object1) * Byte.toUnsignedInt((Byte) object2)));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) ((float) (Float) object1 * (float) (Float) object2));
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) ((double) (Double) object1 * (double) (Double) object2));
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "**":
                func = (File file) -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) Math.pow((int) (Integer) object1, (int) (Integer) object2));
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) Math.pow((long) (Long) object1, (long) (Long) object2));
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) Math.pow((char) (Character) object1, (char) (Character) object2));
                    }
                    if(object1 instanceof Byte && object2 instanceof Byte) {
                        r = new ISBPLObject(getType("byte"), (byte) Math.pow(Byte.toUnsignedInt((Byte) object1), Byte.toUnsignedInt((Byte) object2)));
                    }
                    if(object1 instanceof Float && object2 instanceof Float) {
                        r = new ISBPLObject(getType("float"), (float) Math.pow((float) (Float) object1, (float) (Float) object2));
                    }
                    if(object1 instanceof Double && object2 instanceof Double) {
                        r = new ISBPLObject(getType("double"), (double) Math.pow((double) (Double) object1, (double) (Double) object2));
                    }
                    if(r != null)
                        stack.push(r);
                    else
                        typeError(o1.type.name, o2.type.name);
                };
                break;
            case "%":
                func = (File file) -> {
                    try {
                        ISBPLObject o2 = stack.pop();
                        ISBPLObject o1 = stack.pop();
                        o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                        o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("float"), getType("long"), getType("double"));
                        Object object1 = o1.object;
                        Object object2 = o2.object;
                        ISBPLObject r = null;
                        if (object1 instanceof Integer && object2 instanceof Integer) {
                            r = new ISBPLObject(getType("int"), (int) ((int) (Integer) object1 % (int) (Integer) object2));
                        }
                        if (object1 instanceof Long && object2 instanceof Long) {
                            r = new ISBPLObject(getType("long"), (long) ((long) (Long) object1 % (long) (Long) object2));
                        }
                        if (object1 instanceof Character && object2 instanceof Character) {
                            r = new ISBPLObject(getType("char"), (char) ((char) (Character) object1 % (char) (Character) object2));
                        }
                        if (object1 instanceof Byte && object2 instanceof Byte) {
                            r = new ISBPLObject(getType("byte"), (byte) (Byte.toUnsignedInt((Byte) object1) % Byte.toUnsignedInt((Byte) object2)));
                        }
                        if (object1 instanceof Float && object2 instanceof Float) {
                            r = new ISBPLObject(getType("float"), (float) ((float) (Float) object1 % (float) (Float) object2));
                        }
                        if (object1 instanceof Double && object2 instanceof Double) {
                            r = new ISBPLObject(getType("double"), (double) ((double) (Double) object1 % (double) (Double) object2));
                        }
                        if (r != null)
                            stack.push(r);
                        else
                            typeError(o1.type.name, o2.type.name);
                    } catch (ArithmeticException ex) {
                        throw new ISBPLError("Arithmetic", "Division by 0");
                    }
                };
                break;
            case "^":
                func = (File file) -> {
                    ISBPLObject o2 = stack.pop();
                    ISBPLObject o1 = stack.pop();
                    o1.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("long"));
                    o2.checkTypeMulti(getType("int"), getType("byte"), getType("char"), getType("long"));
                    Object object1 = o1.object;
                    Object object2 = o2.object;
                    ISBPLObject r = null;
                    if(object1 instanceof Integer && object2 instanceof Integer) {
                        r = new ISBPLObject(getType("int"), (int) ((int) (Integer) object1 ^ (int) (Integer) object2));
                    }
                    if(object1 instanceof Long && object2 instanceof Long) {
                        r = new ISBPLObject(getType("long"), (long) ((long) (Long) object1 ^ (long) (Long) object2));
                    }
                    if(object1 instanceof Character && object2 instanceof Character) {
                        r = new ISBPLObject(getType("char"), (char) ((char) (Character) object1 ^ (char) (Character) object2));
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
                func = (File file) -> {
                    ISBPLObject o = stack.pop();
                    stack.push(new ISBPLObject(o.type, o.object));
                    stack.push(new ISBPLObject(o.type, o.object));
                };
                break;
            case "pop":
                func = (File file) -> stack.pop();
                break;
            case "swap":
                func = (File file) -> {
                    ISBPLObject o1 = stack.pop();
                    ISBPLObject o2 = stack.pop();
                    stack.push(o2);
                    stack.push(o1);
                };
                break;
            case "_last_word":
                func = (File file) -> {
                    ISBPLObject i = stack.pop();
                    i.checkType(getType("int"));
                    int n = (int) i.object;
                    if(n >= lastWords.size())
                        throw new ISBPLError("IllegalArgument", "_last_words called with wrong argument");
                    stack.push(toISBPLString(lastWords.get(n)));
                };
                break;
            case "time":
                func = (File file) -> {
                    ISBPLObject i = stack.pop();
                    long n = (long) i.toLong();
                    try {
                        Thread.sleep(n);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    stack.push(new ISBPLObject(getType("long"), System.currentTimeMillis()));
                };
                break;
            case "stream":
                func = (File file) -> {
                    ISBPLObject action = stack.pop();
                    action.checkType(getType("int"));
                    int n = ((int) action.object);
                    try {
                        streamer.action(stack, n);
                    }
                    catch (IOException e) {
                        throw new ISBPLError("IO", e.getMessage());
                    }
                };
        }
        functionStack.peek().put(name, func);
    }
    
    private int createFunction(int i, String[] words, Stack<ISBPLObject> stack) {
        i++;
        String name = words[i];
        AtomicInteger integer = new AtomicInteger(++i);
        ISBPLCallable callable = readBlock(integer, words, stack, true);
        i = integer.get();
        functionStack.peek().put(name, callable);
        return i;
    }
    
    private ISBPLCallable readBlock(AtomicInteger idx, String[] words, Stack<ISBPLObject> stack, boolean isFunction) {
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
        return (file) -> interpretRaw(file, theWords, stack, isFunction);
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
                if(debug) {
                    String s = "";
                    for (int x = 0 ; x < functionStack.size() ; x++) {
                        s += "\t";
                    }
                    System.err.println(s + word + "\t\t" + stack);
                }
                while (debuggerIPC.run == 0) Thread.sleep(1);
                if(debuggerIPC.run < 0) {
                    if(debuggerIPC.run < -1) {
                        if (debuggerIPC.run == -2) {
                            if (word.equals(debuggerIPC.until)) {
                                debuggerIPC.run = 0;
                                while (debuggerIPC.run == 0) Thread.sleep(1);
                            }
                        }
                        if (debuggerIPC.run == -3 && Thread.currentThread().getId() != debuggerIPC.threadID) {
                            while (debuggerIPC.run == -3) Thread.sleep(1);
                        }
                    }
                } else
                    debuggerIPC.run--;
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
                    func.call(file);
                    continue;
                }
                func = functionStack.get(0).get(word);
                if(func != null) {
                    func.call(file);
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
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
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
        isbpl.debuggerIPC.stack = stack;
        debug = !System.getenv().getOrDefault("DEBUG", "").equals("");
        if(debug) {
            new ISBPLDebugger(isbpl).start();
        }
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
            array[i - 1] = isbpl.toISBPLString(args[i]);
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
    void call(File file);
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
    
    @Override
    public String toString() {
        return "ISBPLType{" +
               "id=" + id +
               ", name='" + name + '\'' +
               '}';
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
    
    @Override
    public String toString() {
        if(type != null && object instanceof ISBPLObject[]) {
            try {
                return "ISBPLObject{" +
                       "type=" + type +
                       ", object=" + Arrays.toString(((ISBPLObject[]) object)) +
                       '}';
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "ISBPLObject{" +
               "type=" + type +
               ", object=" + object +
               '}';
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

class ISBPLDebugger extends Thread {
    private ISBPL isbpl;
    
    public ISBPLDebugger(ISBPL isbpl) {
        this.isbpl = isbpl;
        isbpl.debuggerIPC.run = 0;
    }
    
    @Override
    public void run() {
        try {
            ServerSocket socket = new ServerSocket(Integer.parseInt(System.getenv("DEBUG")));
            while (true) {
                Socket s = socket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        switch (line.split(" ")[0]) {
                            case "continue":
                            case "cont":
                            case "c":
                                isbpl.debuggerIPC.run = -1;
                                break;
                            case "stop":
                                isbpl.debuggerIPC.run = 0;
                                break;
                            case "next":
                            case "n":
                                isbpl.debuggerIPC.run = 1;
                                break;
                            case "do":
                            case "d":
                                isbpl.debuggerIPC.run = Integer.parseInt(line.split(" ")[1]);
                                break;
                            case "until":
                            case "u":
                                isbpl.debuggerIPC.until = line.split(" ")[1];
                                isbpl.debuggerIPC.run = -2;
                                break;
                            case "eval":
                                isbpl.debuggerIPC.run = -3;
                                isbpl.debuggerIPC.threadID = Thread.currentThread().getId();
                                try {
                                    isbpl.interpret(new File("_debug").getAbsoluteFile(), line.substring(5), isbpl.debuggerIPC.stack);
                                } catch (ISBPLStop stop) {
                                    System.exit(isbpl.exitCode);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    boolean fixed = false;
                                    while (!fixed) {
                                        try {
                                            System.err.println("Stack recovered: " + isbpl.debuggerIPC.stack);
                                            fixed = true;
                                        }
                                        catch (Exception e1) {
                                            e.printStackTrace();
                                            System.err.println("!!! STACK CORRUPTED!");
                                            isbpl.debuggerIPC.stack.pop();
                                            System.err.println("Popped. Trying again.");
                                        }
                                    }
                                }
                                break;
                            case "dump":
                                try {
                                    System.err.println("VAR DUMP\n----------------");
                                    for (HashMap<String, ISBPLCallable> map : isbpl.functionStack) {
                                        for (String key : map.keySet()) {
                                            if(key.startsWith("=")) {
                                                map.get(key.substring(1)).call(new File("_debug").getAbsoluteFile());
                                                System.err.println("\t" + key.substring(1) + ": \t" + isbpl.debuggerIPC.stack.pop());
                                            }
                                        }
                                        System.err.println("----------------");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.err.println("!!! VARS CORRUPTED! CANNOT FIX AUTOMATICALLY.");
                                }
                            case "stack":
                                boolean fixed = false;
                                while (!fixed) {
                                    try {
                                        System.err.println("STACK DUMP");
                                        for (ISBPLObject object : isbpl.debuggerIPC.stack) {
                                            System.err.println("\t" + object);
                                        }
                                        fixed = true;
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                        System.err.println("!!! STACK CORRUPTED!");
                                        isbpl.debuggerIPC.stack.pop();
                                        System.err.println("Popped. Trying again.");
                                    }
                                }
                                break;
                            case "son":
                                ISBPL.debug = true;
                                break;
                            case "soff":
                                ISBPL.debug = false;
                                break;
                            case "exit":
                                System.exit(255);
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace(new PrintStream(s.getOutputStream()));
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static class IPC {
        long threadID;
        String until = null;
        int run = -1;
        Stack<ISBPLObject> stack = null;
        
    }
}

class ISBPLStreamer {
    public static final int CREATE_FILE =   1;
    public static final int CREATE_SOCKET = 2;
    public static final int READ =          3;
    public static final int WRITE =         4;
    public static final int AREAD =         5;
    public static final int AWRITE =        6;
    
    static class ISBPLStream {
        final InputStream in;
        final OutputStream out;
        static int gid = 0;
        final int id = gid++;
    
        public ISBPLStream(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }
    }
    
    final ISBPL isbpl;
    
    public ISBPLStreamer(ISBPL isbpl) {
        this.isbpl = isbpl;
    }
    
    public ArrayList<ISBPLStream> streams = new ArrayList<>();
    
    public void action(Stack<ISBPLObject> stack, int action) throws IOException {
        ISBPLStream stream;
        ISBPLObject s, i;
        switch (action) {
            case CREATE_FILE:
                s = stack.pop();
                s.checkType(isbpl.getType("string"));
                File f = new File(isbpl.toJavaString(s));
                stream = new ISBPLStream(new FileInputStream(f), new FileOutputStream(f));
                streams.add(stream);
                stack.push(new ISBPLObject(isbpl.getType("int"), stream.id));
                break;
            case CREATE_SOCKET:
                i = stack.pop();
                s = stack.pop();
                i.checkType(isbpl.getType("int"));
                s.checkType(isbpl.getType("string"));
                Socket socket = new Socket(isbpl.toJavaString(s), ((int) i.object));
                stream = new ISBPLStream(socket.getInputStream(), socket.getOutputStream());
                streams.add(stream);
                stack.push(new ISBPLObject(isbpl.getType("int"), stream.id));
                break;
            case READ:
                i = stack.pop();
                i.checkType(isbpl.getType("int"));
                try {
                    stack.push(new ISBPLObject(isbpl.getType("int"), streams.get(((int) i.object)).in.read()));
                } catch (IndexOutOfBoundsException e) {
                    throw new ISBPLError("IllegalArgument", "streamid STREAM_READ stream called with non-existing stream argument");
                }
                break;
            case WRITE:
                i = stack.pop();
                i.checkType(isbpl.getType("int"));
                ISBPLObject bte = stack.pop();
                bte.checkTypeMulti(isbpl.getType("int"), isbpl.getType("char"), isbpl.getType("byte"));
                try {
                    streams.get(((int) i.object)).out.write(((int) bte.toLong()));
                } catch (IndexOutOfBoundsException e) {
                    throw new ISBPLError("IllegalArgument", "streamid STREAM_READ stream called with non-existing stream argument");
                }
                break;
            default:
                throw new ISBPLError("NotImplemented", "Not implemented");
        }
    }
}
