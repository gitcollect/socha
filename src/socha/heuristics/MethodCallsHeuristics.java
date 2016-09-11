package socha.heuristics;

import java.lang.reflect.Modifier;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import socha.Socha;
import socha.utils.SpecialOpcode;

public class MethodCallsHeuristics implements BytecodeWeightHeuristics {

	@Override
	public int transform(int position, int instruction, CtMethod method, CodeIterator iterator) {
		if (instruction == Opcode.INVOKEVIRTUAL || instruction == Opcode.INVOKESTATIC
				|| instruction == Opcode.INVOKEINTERFACE || instruction == Opcode.INVOKESPECIAL
				|| instruction == Opcode.INVOKEDYNAMIC) {

			// find method info here...
			int methodInfoPos = iterator.u16bitAt(position + 1);

			final ConstPool pool = method.getDeclaringClass().getClassFile().getConstPool();

			final String className = pool.getMethodrefClassName(methodInfoPos);
			final String methodName = pool.getMethodrefName(methodInfoPos);
			final String methodType = pool.getMethodrefType(methodInfoPos);

			if (!methodName.startsWith("<")) {
				CtClass invokedClass = null;
				try {
					invokedClass = Socha.classPool.get(className);
				} catch (NotFoundException e) {
					e.printStackTrace();
				}

				CtMethod invokedMethod = null;
				try {
					invokedMethod = invokedClass.getMethod(methodName, methodType);
				} catch (NotFoundException e) {
					e.printStackTrace();
				}

				if (Modifier.isNative(invokedMethod.getModifiers())) {
					instruction = SpecialOpcode.NATIVE_CALL;
				}

				if ((className + "#" + methodName + methodType).equals("java.lang.Thread#sleep(J)V")) {
					instruction = SpecialOpcode.THREAD_SLEEP;
				}
			}	
		}

		return instruction;
	}
	@Override
	public double measure(int instruction) {
		switch (instruction) {
		case Opcode.NOP:
			return 0;
		case Opcode.INVOKESTATIC:
			return 1.85;
		case Opcode.INVOKEDYNAMIC:
			return 2.1;
		case Opcode.INVOKEINTERFACE:
			return 2.61;
		case SpecialOpcode.NATIVE_CALL:
			return 10;
		case SpecialOpcode.THREAD_SLEEP:
			return 20;
		default:
			return 1;
		}
	}

}
