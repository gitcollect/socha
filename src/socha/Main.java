package socha;

import socha.heuristics.BytecodeWeightHeuristics;
import socha.heuristics.MethodCallsHeuristics;

public class Main {
	public static void main(String[] args) {
		final BytecodeWeightHeuristics heuristics = new MethodCallsHeuristics();
		final Socha analysis = new Socha("examples/lawdb/DistributedStore.jar", "server.UDPServerHandler", heuristics);
		analysis.addJar("libs/netty-all-4.0.29.Final.jar");
		analysis.analyze("channelRead0");
		analysis.show();
	}
}
