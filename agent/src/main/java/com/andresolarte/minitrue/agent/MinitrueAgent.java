package com.andresolarte.minitrue.agent;


import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.*;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;


/**
 * Created by aolarte on 12/30/15.
 */
public class MinitrueAgent {
    public static void premain(String agentArguments,
                               Instrumentation instrumentation){
        // And if the party says that it is not four but five--then how many?
        ClassFileTransformer transformer=new MiniTrueTransformer();
        instrumentation.addTransformer(transformer);
    }

    public static class MiniTrueTransformer  implements ClassFileTransformer {

        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            byte[] ret=classfileBuffer;

            if (shouldTransform(className)) {
                ClassPool pool = ClassPool.getDefault();

                try {
                    CtClass ctClass = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    ConstPool constPool=ctClass.getClassFile().getConstPool();
                    List<MethodInfo> methodList=ctClass.getClassFile().getMethods();
                    for (MethodInfo info : methodList) {
                        CodeAttribute ca = info.getCodeAttribute();
                        if (ca!=null) {
                            CodeIterator ci = ca.iterator();
                            while (ci.hasNext()) {
                                int index = ci.next();
                                int op = ci.byteAt(index);

                                //Replace the + op with our function
                                //This will cover the case: int z=x + y, where x=2 and y=2
                                if (op == Opcode.IADD) {
                                    ci.writeByte(Opcode.NOP, index);//Change the current OP to NOP to leave the parameters on the stack
                                    Bytecode code = new Bytecode(constPool);
                                    code.addInvokestatic("com/andresolarte/minitrue/agent/DoubleSpeak", "add", "(II)I");
                                    ci.insertAt(index, code.get()); //Insert a call to our function
                                }

                                //The compiler will actually optimize a literal 2+2 into a constant (ICONST_4), change it to ICONST_5
                                //This will cover the case: int x=2+2;
                                if (op == Opcode.ICONST_4) {
                                    ci.writeByte(Opcode.ICONST_5, index);
                                }

                                //If the arithmetic is done and then result is concatenated into a string, the compiler will
                                //optimize the result, and just store the result into the constant pool
                                //We will manipulate the String after it has been loaded into the stack
                                //This will cover this case: System.out.println("Testing 2 + 2: " + (2+2));
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

                            }
                        }
                    }
                    ret=ctClass.toBytecode();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return ret;
        }



        private boolean shouldTransform(String className) {
            //Make sure we don't instrument TOO much
            if (className.startsWith("java/") || className.startsWith("sun/") || className.startsWith("javassist/") || className.startsWith("com/andresolarte/minitrue/agent")){
                return false;
            }
            return true;
        }
    }



}