using System.Collections.Generic;
using System.ComponentModel.Design.Serialization;
using System.Linq;
using System.Runtime.InteropServices;

namespace DsaPrep;

public static class BinaryTreeProblems
{
    public class TreeNode
    {
        public int val;
        public TreeNode? left;
        public TreeNode? right;

        public TreeNode(int val = 0, TreeNode? left = null, TreeNode? right = null)
        {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    public static void SloveBinaryTree()
    {
        // Console.WriteLine(MaxLevelSum(CreateTree([1, 7, 0, 7, -8, null, null ])));
        // PrintBinaryTree(SearchBST(CreateTree([4, 2, 7, 1, 3]), 2));
        // TreeNode root = CreateTree([
        //     10, 5, 15, 2, 7, 13, 17, 1, 3, 6, 9, 12, 14, 16, 18, null, null, null, 4, null, null, 8
        // ]);

        TreeNode rootOnly = CreateTree([3, 2, 4, 1]);
        PrintBinaryTree(rootOnly);
        PrintBinaryTree(DeleteNode(rootOnly, 1));

        // PrintBinaryTree(DeleteNode(
        //     CreateTree([5, 3, 6, 2, 4, null, 7]), 3));
    }

    public static TreeNode DeleteNode(TreeNode root, int key)
    {
        if (root == null) return null;
        if (hasChildren(root) == 0)
        {
            if (root.val == key) return null;
            return root;
        }

        TreeNode parent = root;

        return DeleteNode(parent, root, 0, key);
    }

    private static TreeNode DeleteNode(TreeNode parent, TreeNode node, int child, int key)
    {
        if (node.val == key)
        {
            if (node.left != null)
            {
                if (node.left.right != null)
                {
                    TreeNode nodeToShift = RightmostNode(node, node.left);
                    node.val = nodeToShift.val;
                }
                else
                {
                    node.val = node.left.val;
                    node.left = node.left.left;
                }
            }
            else if (node.right != null)
            {
                if (node.right.left != null)
                {
                    TreeNode nodeToShift = LeftmostNode(node, node.right);
                    node.val = nodeToShift.val;
                }
                else
                {
                    node.val = node.right.val;
                    node.right = node.right.right;
                }
            }
            else
            {
                if (child == 0) // root
                {
                    return null;
                }

                if (child == 1) // right
                {
                    parent.right = null;
                }
                else
                {
                    parent.left = null;
                }
            }
        }
        else if (key > node.val && node.right != null) DeleteNode(node, node.right, 1, key);
        else if (key < node.val && node.left != null) DeleteNode(node, node.left, 2, key);

        return parent;
    }

    private static TreeNode RightmostNode(TreeNode parent, TreeNode node)
    {
        if (node.right == null)
        {
            parent.right = node.left;
            return node;
        }

        return RightmostNode(node, node.right);
    }

    private static TreeNode LeftmostNode(TreeNode parent, TreeNode node)
    {
        if (node.left == null)
        {
            parent.left = node.right;
            return node;
        }

        return LeftmostNode(node, node.left);
    }

    private static int hasChildren(TreeNode node)
    {
        if (node.left != null && node.right != null) return 3;
        if (node.left == null && node.right != null) return 2;
        if (node.left != null && node.right == null) return 1;
        return 0;
    }

    public static TreeNode SearchBST(TreeNode root, int val)
    {
        if (root == null) return null;

        if (val < root.val) return SearchBST(root.left, val);
        if (val > root.val) return SearchBST(root.right, val);

        return root;
    }

    public static int MaxLevelSum(TreeNode root)
    {
        int currentLevel = 1;
        int level = currentLevel;
        TreeNode node = root;
        int maxSum = int.MinValue;
        int maxSumLevel = 1;
        int levelSum = 0;

        Queue<(int, TreeNode)> queue = new Queue<(int, TreeNode)>();
        queue.Enqueue((currentLevel, root));

        while (queue.Count > 0)
        {
            (level, node) = queue.Peek();
            // for all the elements at the same level
            while (currentLevel == level && queue.Count > 0)
            {
                levelSum += node.val;
                queue.Dequeue();
                if (node.left != null) queue.Enqueue((level + 1, node.left));
                if (node.right != null) queue.Enqueue((level + 1, node.right));

                if (queue.Count > 0) (level, node) = queue.Peek();
                else
                {
                    level++;
                    break;
                }
            }

            if (currentLevel != level)
            {
                if (levelSum > maxSum)
                {
                    maxSum = levelSum;
                    maxSumLevel = currentLevel;
                }

                currentLevel = level;
                levelSum = 0;
            }
        }


        return maxSumLevel;
    }

    public static void PrintBinaryTree(TreeNode? root, string indent = "", bool isRight = true)
    {
        if (root == null)
        {
            return;
        }

        Console.Write(indent);
        if (isRight)
        {
            Console.Write("└── ");
            indent += "    ";
        }
        else
        {
            Console.Write("├── ");
            indent += "|   ";
        }

        Console.WriteLine(root.val);

        PrintBinaryTree(root.right, indent, false);
        PrintBinaryTree(root.left, indent, true);
    }

    public static TreeNode? CreateTree(int?[] arr)
    {
        if (arr == null || arr.Length == 0 || arr[0] == null)
        {
            return null;
        }

        TreeNode root = new TreeNode(arr[0].Value);
        Queue<TreeNode> queue = new Queue<TreeNode>();
        queue.Enqueue(root);

        int i = 1;
        while (queue.Any() && i < arr.Length)
        {
            TreeNode current = queue.Dequeue();

            // Left child
            if (i < arr.Length && arr[i] != null)
            {
                current.left = new TreeNode(arr[i].Value);
                queue.Enqueue(current.left);
            }

            i++;

            // Right child
            if (i < arr.Length && arr[i] != null)
            {
                current.right = new TreeNode(arr[i].Value);
                queue.Enqueue(current.right);
            }

            i++;
        }

        return root;
    }
}