# 2+2=5 Using bytecode engineering

The Java language doesn't provide a way of overloading operators. However, with a bit of persistence, and the insight provided to Java Agents, (mostly) everything is possible.

You can see the effects of running with the agent and without it by running:

```
./run.sh
```
## Basics of a Java Agent

Java Agents are components that can provide "instrumentation" to existing code. The main use case for agents are to provide runtime information to an external monitoring system. This information is used to troubleshoot or debug problems in the JVM.
Normally agents are loaded by passing a parameter to JVM:

```
java -javaagent:myagent.jar -jar myapp.jar
```

Most agents work by tweaking classes are they are being loaded by the ClassLoader. This opens the gate to make serious modifications to the classes, and that's what we're abusing to make 2+2=5.

## Bytecode engineering

Java source files are compiled into Java bytecode.  Java bytecode is the "machine language" used by the JVM.
Several libraries provide facilities to modify this bytecode, without having to drop down to use a hex editor.
For this example I use Javassist, one of the newer and more user friendly bytecode engineering libraries.

### The basic case: overriding the plus operator

The most obvious case is changing the plus operator to suit our needs. We achieved this by iterating over all ops in the bytecode and changing the addition ops:

```java
if (op == Opcode.IADD) {
  ci.writeByte(Opcode.NOP, index);//Change the current OP to NOP to leave the parameters on the stack
  Bytecode code = new Bytecode(constPool);
  code.addInvokestatic("com/andresolarte/minitrue/agent/DoubleSpeak", "add", "(II)I");
  ci.insertAt(index, code.get()); //Insert a call to our function
}
```

We actually change the addition opcode to NOP (which means do nothing). Before this operation we add a static call to our method.


### Dealing with compiler optimizations

The compiler tries to be smart, and thwarts our efforts somewhat. The following code:
```java
int x=2+2;
```
will be optimized by the compiler into bytecode that looks exactly as if it was generated by something like:

```java
int x=4;
```

To get around this we change the opcode for loading a 4 into the stack for the equivalent instruct to load a 5 in the stack:

```java
if (op == Opcode.ICONST_4) {
  ci.writeByte(Opcode.ICONST_5, index);
}
```

### Dealing with the String pool

Strings in Java bytecode are stored inside the Constant Pool. The compiler will also try to optimize them. The String stored in the pool from this code:

```java
System.out.println("2 + 2: " + (2+2) );
```

Will look like this:

```
"2 + 2 : 4"
```

This makes things harder. One possible solution? Tweak any String that is loaded into the stack:
```java
if (op == Opcode.LDC) {
    int poolIndex = ci.byteAt(index+1);
    int tag=constPool.getTag(poolIndex);
    if (tag==8) { //Constant is a String
        Bytecode code = new Bytecode(constPool);
        code.addInvokestatic("com/andresolarte/minitrue/agent/DoubleSpeak", "memoryHoleString", "(Ljava/lang/String;)Ljava/lang/String;");
        //Insert a call to our function. This call will happen after the string is loaded in the stack.
        //Our function will use the string from the stack, and replace it with the modified version.
        ci.insertAt(ci.lookAhead(), code.get());
    }
}
```

## Conclusion
While using Java Agents to break basic arithmetic in Java is a Very Bad Idea(tm), it's important for any serious Java developer to understand the basics of Java bytecode, and what the compiler is doing.

The other obvious thing, is the value of Java Agents, which can of course also be used for more constructive purposes.

## Limitations
Plenty of things still missing to make this watertight:
* We only "overloaded" the integer operator. Floats, Doubles, Longs and so on will still have the normal behavior.
* Due to lack of access to the source code, we assumed that String containing 4 should have their 4 replaced with 5.  We don't know why the compiler placed that 4 in the String
* We only cover one of the operations that can load a String from the pool, there are few more to take into account.
