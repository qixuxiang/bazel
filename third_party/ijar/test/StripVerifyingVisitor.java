// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A {@code ClassVisitor} that checks that ijar did its job.
 */
final class StripVerifyingVisitor extends ClassVisitor {
  public List<String> errors = new ArrayList<String>();
  private String className;

  public StripVerifyingVisitor() {
    super(Opcodes.ASM6);
  }

  @Override public void visit(
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {
    className = name;
    if ((access & Opcodes.ACC_PRIVATE) != 0) {
      errors.add(String.format("Private class found %s", className));
    }
  }

  @Override public void visitSource(String source, String debug) {
    errors.add(String.format("Source entry found %s %s", source, debug));
  }

  @Override public void visitOuterClass(String owner, String name, String desc) {
    // Always okay.
  }

  @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    // Always okay.
    return null;
  }

  @Override public void visitAttribute(Attribute attr) {
    // Just assume this is okay.
  }

  @Override public void visitInnerClass(
      String name,
      String outerName,
      String innerName,
      int access) {
    // Always okay
    // TODO(bazel-team): verify removal of leftover inner classes once implemented
    // TODO(bazel-team): verify removal of private inner classes once implemented
  }

  @Override public FieldVisitor visitField(
      int access,
      String name,
      String desc,
      String signature,
      Object value) {
    if ((access & Opcodes.ACC_PRIVATE) != 0) {
      errors.add(String.format("Private field %s found in %s", name, className));
    }
    // We need to go deeper.
    return new StripVerifyingFieldVisitor(errors, className, name);
  }

  @Override public MethodVisitor visitMethod(
      int access,
      String name,
      String desc,
      String signature,
      String[] exceptions) {
    if ((access & Opcodes.ACC_PRIVATE) != 0) {
      errors.add(String.format("Private method %s found in %s", name, className));
    }
    // We need to go deeper.
    return new StripVerifyingMethodVisitor(errors, className, name);
  }

  @Override public void visitEnd() {
    // Always okay.
  }

  private static class StripVerifyingFieldVisitor extends FieldVisitor {
    private final List<String> errors;
    private final String className;
    private final String fieldName;

    StripVerifyingFieldVisitor(List<String> errors, String className, String fieldName) {
      super(Opcodes.ASM6);
      this.errors = errors;
      this.className = className;
      this.fieldName = fieldName;
    }

    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      // Always okay.
      return null;
    }

    @Override public void visitAttribute(Attribute attr) {
      // Just assume this is okay.
    }

    @Override public void visitEnd() {
      // Always okay.
    }
  }

  private static class StripVerifyingMethodVisitor extends MethodVisitor {
    private final List<String> errors;
    private final String className;
    private final String methodName;

    StripVerifyingMethodVisitor(List<String> errors, String className, String methodName) {
      super(Opcodes.ASM6);
      this.errors = errors;
      this.className = className;
      this.methodName = methodName;
    }

    @Override public AnnotationVisitor visitAnnotationDefault() {
      // Always okay.
      return null;
    }

    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      // Always okay.
      return null;
    }

    @Override public AnnotationVisitor visitParameterAnnotation(
        int parameter,
        String desc,
        boolean visible) {
      // Always okay.
      return null;
    }

    @Override public void visitAttribute(Attribute attr) {
      // Just assume this is okay.
    }

    @Override public void visitCode() {
      errors.add(String.format("Code found for method %s of %s", methodName, className));
    }

    @Override public void visitEnd() {}

    // We ignore all details about code segments. Presumably we already logged an error about code
    // existing at all.

    @Override public void visitFrame(
        int type,
        int nLocal,
        Object[] local,
        int nStack,
        Object[] stack) {}

    @Override public void visitInsn(int opcode) {}

    @Override public void visitIntInsn(int opcode, int operand) {}

    @Override public void visitVarInsn(int opcode, int var) {}

    @Override public void visitTypeInsn(int opcode, String type) {}

    @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) {}

    @Override
    public void visitMethodInsn(
        int opcode, String owner, String name, String desc, boolean isInterface) {}

    @Override public void visitJumpInsn(int opcode, Label label) {}

    @Override public void visitLabel(Label label) {}

    @Override public void visitLdcInsn(Object cst) {}

    @Override public void visitIincInsn(int var, int increment) {}

    @Override public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {}

    @Override public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {}

    @Override public void visitMultiANewArrayInsn(String desc, int dims) {}

    @Override public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {}

    @Override public void visitLocalVariable(
        String name,
        String desc,
        String signature,
        Label start,
        Label end,
        int index) {}

    @Override public void visitLineNumber(int line, Label start) {}

    @Override public void visitMaxs(int maxStack, int maxLocals) {}
  }
}
