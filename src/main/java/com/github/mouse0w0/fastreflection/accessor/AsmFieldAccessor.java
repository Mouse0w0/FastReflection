package com.github.mouse0w0.fastreflection.accessor;

import com.github.mouse0w0.fastreflection.FieldAccessor;
import com.github.mouse0w0.fastreflection.util.AsmUtils;
import com.github.mouse0w0.fastreflection.util.SafeClassDefiner;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Opcodes.*;

public final class AsmFieldAccessor {

	private AsmFieldAccessor() {
	}

	private static int id = 0;

	public static FieldAccessor create(Field field) throws Exception {
		requireNonNull(field);
		
		Class<?> declaringClass = field.getDeclaringClass();
		String className = String.format("AsmFieldAccessor_%d_%s_%s", id++, declaringClass.getSimpleName(),
				field.getName());
		boolean isStatic = Modifier.isStatic(field.getModifiers());
		Type declaringClassType = Type.getType(declaringClass);
		String fieldName = field.getName();
		Type fieldType = Type.getType(field.getType());

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		FieldVisitor fv;
		MethodVisitor mv;

		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "com/github/mouse0w0/fastreflection/FieldAccessor" });

		cw.visitSource(".dynamic", null);

		{
			fv = cw.visitField(ACC_PRIVATE + ACC_FINAL, "field", "Ljava/lang/reflect/Field;", null, null);
			fv.visitEnd();
		}

		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/reflect/Field;)V", null, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, className, "field", "Ljava/lang/reflect/Field;");
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "getField", "()Ljava/lang/reflect/Field;", null, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, "field", "Ljava/lang/reflect/Field;");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		createGetMethod(cw, "get", AsmUtils.OBJECT_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createGetMethod(cw, "getByte", Type.BYTE_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createGetMethod(cw, "getShort", Type.SHORT_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createGetMethod(cw, "getInt", Type.INT_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createGetMethod(cw, "getLong", Type.LONG_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createGetMethod(cw, "getFloat", Type.FLOAT_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createGetMethod(cw, "getDouble", Type.DOUBLE_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createGetMethod(cw, "getBoolean", Type.BOOLEAN_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createGetMethod(cw, "getChar", Type.CHAR_TYPE, declaringClassType, fieldName, fieldType, isStatic);

		createSetMethod(cw, "set", AsmUtils.OBJECT_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createSetMethod(cw, "set", Type.BYTE_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createSetMethod(cw, "set", Type.SHORT_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createSetMethod(cw, "set", Type.INT_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createSetMethod(cw, "set", Type.LONG_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createSetMethod(cw, "set", Type.FLOAT_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createSetMethod(cw, "set", Type.DOUBLE_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createSetMethod(cw, "set", Type.BOOLEAN_TYPE, declaringClassType, fieldName, fieldType, isStatic);
		createSetMethod(cw, "set", Type.CHAR_TYPE, declaringClassType, fieldName, fieldType, isStatic);

		cw.visitEnd();

		return (FieldAccessor) SafeClassDefiner.INSTANCE
				.defineClass(field.getDeclaringClass().getClassLoader(), className, cw.toByteArray())
				.getConstructor(Field.class).newInstance(field);
	}

	private static void createGetMethod(ClassWriter cw, String name, Type type, Type classType, String fieldName,
			Type fieldType, boolean isStatic) {
		String desc = "(Ljava/lang/Object;)" + type.getDescriptor();
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, name, desc, null, new String[] { "java/lang/Exception" });
		GeneratorAdapter ga = new GeneratorAdapter(mv, ACC_PUBLIC, name, desc);
		int sSort = fieldType.getSort(), tSort = type.getSort();
		if (tSort == Type.OBJECT || isCastable(sSort, tSort) ) {
			if (isStatic) {
				ga.getStatic(classType, fieldName, fieldType);
			} else {
				ga.loadArg(0);
				ga.checkCast(classType);
				ga.getField(classType, fieldName, fieldType);
			}
			if(tSort == Type.OBJECT)
				ga.box(fieldType);
			ga.returnValue();
		} else {
			ga.throwException(Type.getType(IllegalArgumentException.class),"");
		}
		ga.visitMaxs(0, 0);
		ga.endMethod();
	}

	private static void createSetMethod(ClassWriter cw, String name, Type type, Type classType, String fieldName,
			Type fieldType, boolean isStatic) {
		String desc = "(Ljava/lang/Object;" + type.getDescriptor() + ")V";
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, name, desc, null, new String[] { "java/lang/Exception" });
		GeneratorAdapter ga = new GeneratorAdapter(mv, ACC_PUBLIC, name, desc);
		int sSort = type.getSort(), tSort = fieldType.getSort();
		if (sSort == Type.OBJECT || isCastable(sSort, tSort)) {
			if (!isStatic) {
				ga.loadArg(0);
				ga.checkCast(classType);
			}
			ga.loadArg(1);
			if (sSort == Type.OBJECT) {
				if (tSort <= Type.DOUBLE)
					ga.unbox(fieldType);
				else
					ga.checkCast(fieldType);
			} else if (Type.BYTE <= sSort && sSort <= Type.DOUBLE && Type.BYTE <= tSort && tSort <= Type.DOUBLE) {
				ga.cast(type, fieldType);
			}
			if (isStatic)
				ga.putStatic(classType, fieldName, fieldType);
			else
				ga.putField(classType, fieldName, fieldType);
			ga.returnValue();
		} else {
			ga.throwException(Type.getType(IllegalArgumentException.class),"");
		}
		ga.visitMaxs(0, 0);
		ga.endMethod();
	}

	private static boolean isCastable(int sSort, int tSort) {
		if(sSort == tSort)
			return true;
		if (Type.BYTE <= sSort && sSort <= Type.DOUBLE && Type.BYTE <= tSort && tSort <= Type.DOUBLE && tSort < sSort)
			return true;
		return false;
	}
}
