#!/bin/node

const fs = require('fs');
const process = require('process');

const stack = [];
const vars = [{}];
const keywords = [
    "native",
    "func",
    "if",
    "def",
    "stop",
];
let insns;
let die = 0;

const natives = {
    "alen": function () {
        stack.push(stack.pop().length);
    },
    "aget": function () {
        let idx = stack.pop();
        let array = stack.pop();
        stack.push(array[idx]);
    },
    "aput": function () {
        let val = stack.pop();
        let idx = stack.pop();
        let array = stack.pop();
        array[idx] = val;
    },
    "anew": function () {
        let length = stack.pop();
        let val = stack.pop();
        stack.push(Array.apply(val, Array(length));
    },
    "_char": function () {
        stack.push(String.fromCharCode(stack.pop()));
    },
    "_int": function () {
        stack.push(Number(stack.pop()));
    },
    "_file": function () {
        stack.push(String(stack.pop()));
    },
    "_float": function () {
        stack.push(Number(stack.pop()));
    },
    "_long": function () {
        stack.push(Number(stack.pop()));
    },
    "_double": function () {
        stack.push(Number(stack.pop()));
    },
    "ischar": function () {
        let x = stack.pop();
        stack.push(x instanceof String && x.length == 1)
    },
    "isint": function () {
        let x = stack.pop();
        stack.push(x instanceof Number)
    },
    "isfloat": function () {
        let x = stack.pop();
        stack.push(x instanceof Number)
    },
    "islong": function () {
        let x = stack.pop();
        stack.push(x instanceof Number)
    },
    "isdouble": function () {
        let x = stack.pop();
        stack.push(x instanceof Number)
    },
    "putchar": function () {
        if(x instanceof String && x.length == 1)
            process.stdout.write(x);
        else
            process.stdout.write(Number(x))
        process.stdout.flush();
    },
    "read": function () {
        let filename = stack.pop();
        stack.push(fs.readFileSync(filename, "iso-8859-1"));
    },
    "write": function () {
        let content = stack.pop();
        let filename = stack.pop();
        fs.writeFileSync(filename, content, { encoding: "iso-8859-1" });
    },
    "flength": function () {
        let filename = stack.pop();
        stack.push(fs.statSync(filename).size);
    },
    "mktype": function () {
    },
    "type": function () {
    },
    "call": function () {
        resolve(stack.pop(), 0);
    },
    "typename": function () {
    }
}

function beginBlock(i) {
    if(insns[i++] !== "{") {
        console.err("Exoected { at " + insns[i] + " at beginning of block!")
        process.exit(1);
    }
    const finsns = [];
    let n = 1;
    while(n > 0) {
        finsns.push(insns[i]);
        if(insns[i] === "{") n++;
        if(insns[i] === "}") n--;
        i++;
    }
    finsns.pop();
    return [
        function call () {
            for(let j = 0; j < finsns.length; ) {
                j = resolve(finsns[j], j);
                if(die > 0) {
                    die--;
                    return;
                }
            }
        },
        i
    ]
}

function handleKeyword(id, i) {
    if(id === "def") {
        i++;
        vars.peek()[insns[i]] = 0
        vars.peek()["=" + insns[i]] = function set() {
            vars.peek()[insns[i]] = stack.pop();
        };
    }
    if(id === "native") {
        i++;
        vars.peek()[insns[i]] = natives[insns[i]];
    }
    if(id === "if") {
        i++;
        const block = beginBlock();
        if(stack.pop()) {
            block[0]();
        }
        i = block[1];
    }
    if(id === "func") {
        i++;
        const block = beginBlock(i);
        vars.peek()[insns[i]] = block[0];
        i = block[1];
    }
    if(id === "while") {
        i++;
        const n = i;
        // This is needed to set the insn pointer to the right location after the block
        let block;
        while((function check() {
            while(insns[i] !== "{") {
                i = resolve(insns[i], i);
            }
            i--;
            return stack.pop()
        })()) {
            block = block || beginBlock(i);
            block[0]();
            i = n;
        }
        block = block || beginBlock(i);
        i = block[1];
    }
    if(id === "stop") {
        die = stack.pop()
    }
    return i + 1;
}

function resolve(id, i) {
    if(keywords.contains(keywords)) {
        return handleKeyword(id, i);
    }
    let toRun = vars.peek()[id] || vars[0][id];
    if(!toRun) {
        console.err("Could not find keyword " + id + "!");
        process.exit(1);
    }
    else {
        toRun();
    }
    return ++i;
}


for(let i = 0; i < isns.length; ) {
    i = resolve(insns[i]);
}
