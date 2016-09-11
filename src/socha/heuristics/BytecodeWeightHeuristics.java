package socha.heuristics;

import javassist.CtMethod;
import javassist.bytecode.CodeIterator;

public interface BytecodeWeightHeuristics {
	public int transform(int position, int instruction, CtMethod method, CodeIterator iterator);
	public double measure(int instruction);
}
