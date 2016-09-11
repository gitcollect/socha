package socha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import socha.heuristics.BytecodeWeightHeuristics;

public class Blocks {
	public final BytecodeWeightHeuristics heuristics;

	public final HashMap<javassist.bytecode.analysis.ControlFlow.Block, Block> blocks = new HashMap<>();
	public final ArrayList<Block> listed = new ArrayList<>();

	public final CodeAttribute code;
	public final CodeIterator iterator;
	public final CtMethod method;

	public Blocks(CtMethod method, BytecodeWeightHeuristics heuristics) {
		this.method = method;
		this.heuristics = heuristics;

		code = this.method.getMethodInfo().getCodeAttribute();
		iterator = code.iterator();
	}

	public Blocks(CtMethod method) {
		this(method, null);
	}

	public Block getFirst() {
		Block first = listed.get(0);
		return first;
	}

	public List<Block> getLast() {
		return blocks.values().stream().filter(((Block b) -> {
			return b.outgoing == 0;
		})).collect(Collectors.toList());
	}

	public Set<javassist.bytecode.analysis.ControlFlow.Block> all() {
		return blocks.keySet();
	}

	public Block get(javassist.bytecode.analysis.ControlFlow.Block block) {
		if (!blocks.containsKey(block)) {
			final Block pack = new Block(this, block);
			blocks.put(block, pack);
			listed.add(pack);
		}

		return blocks.get(block);
	}

	public Block get(javassist.bytecode.analysis.ControlFlow.Catcher catcher) {
		final javassist.bytecode.analysis.ControlFlow.Block block = catcher.block();

		if (!blocks.containsKey(block)) {
			final Block pack = new Block(this, catcher);
			blocks.put(block, pack);
			listed.add(pack);
		} else {
			blocks.get(block).catcher = true;
		}

		return blocks.get(block);
	}
}
