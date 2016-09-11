package socha;

import java.util.ArrayList;

import javassist.bytecode.analysis.ControlFlow;

public class Block {
	final ControlFlow.Block block;
	boolean catcher;
	int outgoing;
	double weight;

	final ArrayList<Integer> bytecode = new ArrayList<>();

	Block(Blocks family, javassist.bytecode.analysis.ControlFlow.Block block) {
		this.block = block;
		this.outgoing = block.exits() + block.catchers().length;

		for (int position = block.position(); position < block.position() + block.length(); position++) {
			int instruction = family.iterator.byteAt(position);
			int transformed = instruction;
			if (family.heuristics != null)
				transformed = family.heuristics.transform(position, instruction, family.method, family.iterator);
			this.bytecode.add(transformed);
		}

		if (family.heuristics != null) {
			this.weight = this.bytecode.stream().map(x -> family.heuristics.measure(x)).mapToDouble(Double::doubleValue)
					.sum();
		} else {
			this.weight = this.bytecode.size();
		}

		this.catcher = false;
	}

	Block(Blocks family, javassist.bytecode.analysis.ControlFlow.Catcher catcher) {
		this(family, catcher.block());
		this.catcher = true;
	}

	public String toString() {
		if (this.catcher)
			return "Catcher #" + this.block.index();
		else
			return "Block #" + this.block.index();
	}
}
