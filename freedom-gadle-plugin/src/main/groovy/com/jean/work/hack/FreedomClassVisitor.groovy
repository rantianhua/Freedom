package com.jean.work.hack

import jdk.internal.org.objectweb.asm.ClassVisitor
import jdk.internal.org.objectweb.asm.MethodVisitor
import jdk.internal.org.objectweb.asm.Opcodes
import jdk.internal.org.objectweb.asm.Type



/**
 * Created by rantianhua on 17/5/13.
 */
class FreedomClassVisitor extends ClassVisitor implements Opcodes{

    private boolean canHack = true


    FreedomClassVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor)
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if ("android/app/Application".equals(superName)) {
            canHack = false
        }
        if (canHack) {
            System.out.println "start to visit class " + name + " canHack:" + canHack
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
        return new MethodVisitor(ASM4, mv) {
            @Override
            void visitInsn(int opcode) {
                if (canHack) {
                    if ("<init>".equals(name) && opcode == RETURN) {
                        System.out.println "start to visit method " + name
                        super.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                        super.visitLdcInsn(Type.getType("Lfreedom/mini/hack/FreedomHack;"));
                        super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/Object;)V", false);
                    }
                }
                super.visitInsn(opcode)
            }

            @Override
            void visitMaxs(int i, int i1) {
                if (canHack && "<init>".equals(name)) {
                    super.visitMaxs(i+1, i1)
                }else {
                    super.visitMaxs(i, i1)
                }
            }
        }
    }
}
