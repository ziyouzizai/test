package test.others;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class TaskQueueWorker implements Watcher {
	private ZooKeeper zk;
	// the znode to receive assignments
	private String assignPath;
	private LinkedBlockingQueue<QueuedAssignment> assignments = new LinkedBlockingQueue<QueuedAssignment>();
	private static class QueuedAssignment {
		QueuedAssignment(String taskPath, String args[]) {
			this.taskPath = taskPath;
			this.args = args;
		}
		// the znode that represents the task
		String taskPath;
		// the cmd and args to run
		String args[];
	}

	public static void main(String[] args) throws KeeperException, InterruptedException, IOException {
		if (args.length != 1) {
			System.err.println("USAGE: zkhostports");
			System.exit(2);
		}
		String hostPort = args[0];
		TaskQueueWorker tq = new TaskQueueWorker();
		ZooKeeper zk = new ZooKeeper(hostPort, 10000, tq);
		tq.zk = zk;
		tq.assignPath = ""; // TODO: create a znode to receive assignments and let the master know we are available (hit use SEQUENTIAL_EPHEMERAL)
		tq.checkAssignment();
		while(true) {
			QueuedAssignment assignment = tq.assignments.take();
			try {
				System.out.println("Executing " + assignment.taskPath + ": " + Arrays.toString(assignment.args));
				Process p = Runtime.getRuntime().exec(assignment.args);
				p.waitFor();
				System.out.println("Finished execution");
			} catch(IOException e) {}
			// TODO: let everyone know we are done
		}
	}

	private void checkAssignment() {
		// TODO: get an assignment (if there is one) and queue it
	}
	
	private String[] stringArrayFromBytes(byte[] argBytes) {
		return new String(argBytes).split("\n");
	}

	public void process(WatchedEvent event) {
		switch(event.getType()) {
		case None: // This is a session event
			if (event.getState() == KeeperState.Expired) {
				System.exit(1);
			}
			break;
		}
	}
}
