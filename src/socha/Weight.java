package socha;

import org.jgrapht.graph.DefaultWeightedEdge;

@SuppressWarnings("serial")
public class Weight extends DefaultWeightedEdge {
	public Weight() {
		super();
	}
	
	public String toString() {
		return "" + this.getWeight();
	}
	
	public double weight() {
		return this.getWeight();
	}
	
	public Block getSource() {
		return (Block) super.getSource();
	}
	
	public Block getTarget() {
		return (Block) super.getTarget();
	}
}
