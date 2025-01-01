package pokemon.panel.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import pokemon.event.EventManager;
import pokemon.event.ui.TreeFileOpened;

public class TreeFilePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5305053754146010493L;

	private JTree fileTree;

	public TreeFilePanel(File baseFile) throws IOException {
		// White background
		this.setBackground(Color.white);

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeObject(baseFile.toPath(), "/"), true);
		Files.walkFileTree(baseFile.toPath(), new NDSFileVisitor(root));

		fileTree = new JTree(root);
		fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		fileTree.addMouseListener(new DoubleClickNode(fileTree));
		this.add(fileTree, BorderLayout.WEST);
	}

	private static class NodeObject {

		private Path nodePath;
		private String nodeName;

		public NodeObject(Path nodePath, String nodeName) {
			this.nodePath = nodePath;
			this.nodeName = nodeName;
		}

		public Path getNodePath() {
			return nodePath;
		}

		public String getNodeName() {
			return nodeName;
		}

		@Override
		public String toString() {
			return nodeName;
		}
	}

	private static class NDSFileVisitor extends SimpleFileVisitor<Path> {

		private DefaultMutableTreeNode rootNode;

		public NDSFileVisitor(DefaultMutableTreeNode rootNode) {
			this.rootNode = rootNode;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			super.visitFile(file, attrs);
			NodeObject leaf = new NodeObject(file, file.getFileName().toString());
			rootNode.add(new DefaultMutableTreeNode(leaf, false));
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			super.preVisitDirectory(dir, attrs);

			// Create a new node that will allow children and create a new visitor
			NodeObject subtree = new NodeObject(dir, dir.getFileName().toString());
			DefaultMutableTreeNode subtreeNode = new DefaultMutableTreeNode(subtree, true);

			// If it is the root node, we continue
			if (((NodeObject) (rootNode.getUserObject())).getNodePath().equals(subtree.getNodePath())) {
				return FileVisitResult.CONTINUE;
			}

			Files.walkFileTree(dir.toAbsolutePath(), new NDSFileVisitor(subtreeNode));
			rootNode.add(subtreeNode);

			// Do not go into the subtree, already done with another visitor
			return FileVisitResult.SKIP_SUBTREE;
		}
	}

	private static class DoubleClickNode extends MouseAdapter {

		private JTree fileTree;

		public DoubleClickNode(JTree fileTree) {
			this.fileTree = fileTree;
		}

		public void mousePressed(MouseEvent e) {
			int selRow = fileTree.getRowForLocation(e.getX(), e.getY());
			TreePath selPath = fileTree.getPathForLocation(e.getX(), e.getY());

			if (selRow != -1) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
				
				if (treeNode.isLeaf() && e.getClickCount() == 2) {
					NodeObject node = (NodeObject) treeNode.getUserObject();
					TreeFileOpened event = new TreeFileOpened(node.getNodePath(), node.getNodeName());
					EventManager.getInstance().throwEvent(event);
				}
			}
		}
	}
}
