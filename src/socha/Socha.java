package socha;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.ListenableDirectedWeightedGraph;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.tree.JGraphTreeLayout;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.analysis.ControlFlow;
import socha.heuristics.BytecodeWeightHeuristics;

public class Socha {
	Blocks blocks;

	double shortest = Double.MAX_VALUE;
	List<Weight> shortestPath;

	double longest = Double.MIN_VALUE;
	List<Weight> longestPath;

	double leak = 0.0;
	BytecodeWeightHeuristics heuristics;

	String className;
	CtMethod method;

	ListenableDirectedWeightedGraph<Block, Weight> cfg = null;

	public ListenableDirectedWeightedGraph<Block, Weight> getControlFlowGraph() {
		return cfg;
	}

	public double getLeak() {
		return leak;
	}

	public double getShortestPathLength() {
		return shortest;
	}

	public double getLongestPathLength() {
		return longest;
	}

	public static ClassPool classPool;

	public Socha(final String jarName, final String className) {
		this(jarName, className, null);
	}

	public Socha(final String jarName, final String className, BytecodeWeightHeuristics heuristics) {
		this.className = className;
		Socha.classPool = new ClassPool(ClassPool.getDefault());

		this.heuristics = heuristics;

		try {
			classPool.appendClassPath(jarName);
		} catch (NotFoundException e) {
			System.out.println("File not found: " + jarName);
		}
	}

	public Socha analyze(String methodName) {
		try {
			final CtClass klass = classPool.get(className);
			method = klass.getDeclaredMethod(methodName);

			blocks = new Blocks(method, heuristics);

			final ListenableDirectedWeightedGraph<Block, Weight> cfg = constructControlFlowGraph(method, heuristics);

			for (final Block last : blocks.getLast()) {
				final DijkstraShortestPath<Block, Weight> shortestPathProcess = new DijkstraShortestPath<Block, Weight>(
						cfg, blocks.getFirst(), last);

				double shorter = shortestPathProcess.getPathLength();
				if (shorter < shortest) {
					shortest = shorter;
					shortestPath = shortestPathProcess.getPath().getEdgeList();
				}

				cfg.edgeSet().forEach((Weight w) -> cfg.setEdgeWeight(w, -w.weight()));

				final BellmanFordShortestPath<Block, Weight> longestPathProcess = new BellmanFordShortestPath<Block, Weight>(
						cfg, blocks.getFirst());

				double longer = -longestPathProcess.getCost(last);
				if (longer > longest) {
					longest = longer;
					longestPath = longestPathProcess.getPathEdgeList(last);
				}

				cfg.edgeSet().forEach((Weight w) -> cfg.setEdgeWeight(w, -w.weight()));

				this.leak = 1 - shortest / longest;
			}
		} catch (NotFoundException e) {
			System.out.println("Class/method not found: " + className + "." + methodName);
		}

		return this;
	}

	private ListenableDirectedWeightedGraph<Block, Weight> constructControlFlowGraph(CtMethod method,
			BytecodeWeightHeuristics heuristics) {
		ControlFlow flow;
		try {
			flow = new ControlFlow(method);
			blocks = new Blocks(method, heuristics);

		} catch (BadBytecode e1) {
			e1.printStackTrace();
			return null;
		}

		cfg = new ListenableDirectedWeightedGraph<Block, Weight>(Weight.class);

		for (javassist.bytecode.analysis.ControlFlow.Block b : flow.basicBlocks()) {
			cfg.addVertex(blocks.get(b));
			for (int i = 0; i < b.catchers().length; i++)
				cfg.addVertex(blocks.get(b.catchers()[i]));
		}

		for (javassist.bytecode.analysis.ControlFlow.Block b : blocks.all()) {
			for (int i = 0; i < b.exits(); i++) {
				final Weight edge = cfg.addEdge(blocks.get(b), blocks.get(b.exit(i)));
				if (edge == null)
					continue;

				final Block bb = blocks.get(b);
				cfg.setEdgeWeight(edge, bb.weight);
			}

			for (int i = 0; i < b.catchers().length; i++) {
				final Weight edge = cfg.addEdge(blocks.get(b), blocks.get(b.catchers()[i]));
				cfg.setEdgeWeight(edge, blocks.get(b).weight);
			}
		}

		return cfg;
	}

	public static final Color YES = new Color(47, 52, 204);
	public static final Color NO = new Color(230, 25, 25);
	public static final Color BOTH = new Color(129, 80, 153);
	public static final Color WHATEVER = new Color(152, 160, 179);

	public JGraph show() {
		final JGraph jgraph = new JGraph(new JGraphModelAdapter<>(cfg));
		// final JGraphHierarchicalLayout layout = new
		// JGraphHierarchicalLayout();
		final JGraphTreeLayout layout = new JGraphTreeLayout();
		final JGraphFacade facade = new JGraphFacade(jgraph);
		layout.run(facade);

		for (Object v : facade.getVertices()) {
			Map attribs = facade.getAttributes(v);
			attribs.put("border", BorderFactory.createLineBorder(Color.BLACK));
			attribs.put("backgroundColor", WHATEVER);
			facade.setAttributes(v, attribs);
		}

		final HashMap<Block, DefaultGraphCell> cells = new HashMap<>();
		final HashMap<Weight, DefaultEdge> weights = new HashMap<>();

		for (Object o : facade.getVertices()) {
			final DefaultGraphCell cell = (DefaultGraphCell) o;
			final Block block = (Block) (cell.getUserObject());

			cells.put(block, cell);
		}

		for (Object o : facade.getEdges()) {
			final DefaultEdge edge = (DefaultEdge) o;
			final Weight weight = (Weight) (edge.getUserObject());
			Map attribs = facade.getAttributes(edge);
			attribs.put("endFill", false);
			attribs.put("linecolor", WHATEVER);

			weights.put(weight, edge);
		}

		for (int i = 0; i < longestPath.size(); i++) {
			Weight weight = longestPath.get(i);
			Map attribs = facade.getAttributes(weights.get(weight));
			attribs.put("endFill", true);
			attribs.put("linecolor", NO);
			attribs.put("linewidth", 2.0f);
			facade.setAttributes(weights.get(weight), attribs);
		}

		for (int i = 0; i < shortestPath.size(); i++) {
			Weight weight = shortestPath.get(i);
			Map attribs = facade.getAttributes(weights.get(weight));
			if (((Color) attribs.get("linecolor")).getRed() == 230) {
				attribs.put("linecolor", BOTH);
			} else {
				attribs.put("linecolor", YES);
			}
			attribs.put("endFill", true);
			attribs.put("linewidth", 2.0f);
			facade.setAttributes(weights.get(weight), attribs);
		}

		final Map<?, ?> nestedMap = facade.createNestedMap(true, true);
		jgraph.getGraphLayoutCache().edit(nestedMap);

		jgraph.setEdgeLabelsMovable(false);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		{
			final JFrame frame = new JFrame();
			final DecimalFormat format = new DecimalFormat("0.000");
			frame.setTitle(method.getDeclaringClass().getName() + "." + method.getName() + " lo: "
					+ format.format(shortest) + ", hi: " + format.format(longest) + ", leak: " + format.format(leak));

			frame.setContentPane(new JScrollPane(jgraph));
			frame.pack();

			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
		}

		return jgraph;
	}

	public void addJar(String path) {
		try {
			classPool.appendClassPath(path);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
