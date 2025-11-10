using System.Security.AccessControl;
using System.Text;

namespace DsaPrep;

public static class DsaProblems
{
    public static void Main(string[] args)
    {
        // 1. Merge strings together
        // Console.WriteLine(MergeStrings.merge("abc", "pqr"));
        // Console.WriteLine(MergeStrings.merge("abcdef", "pqr"));
        // Console.WriteLine(MergeStrings.merge("abc", "pqrst"));

        /* 3. GreateNumberOfCandies
            Input -
                candies - empty array, same value, null
                extraCandies - 0. extra candies > max
            data transformations
        */
        // PrintArray(GreatestNumberOfCandies.KidsWithCandies(new[] { 2, 3, 5, 1, 3 }, 3).ToArray());

        // can place flower
        // Console.WriteLine(Flowerbed.CanPlaceFlowers(new[] { 1, 0, 0, 0, 1 }, 1));
        // Console.WriteLine(Flowerbed.CanPlaceFlowers(new[] { 1, 0, 0, 0, 1 }, 2));
        // Console.WriteLine(Flowerbed.CanPlaceFlowers(new[] { 0, 0, 1 }, 1));
        // Console.WriteLine(Flowerbed.CanPlaceFlowers(new[] { 0, 1, 0 }, 1));


        // 5. Reverse Vowels
        // Console.WriteLine(ReverseVowelsQ.ReverseVowels("IceCreAm"));
        // Console.WriteLine(ReverseVowelsQ.ReverseVowels(null));
        // Console.WriteLine(ReverseVowelsQ.ReverseVowels("I"));
        // Console.WriteLine(ReverseVowelsQ.ReverseVowels("Ii"));

        // 6. Reverse words
        // Console.WriteLine(ReverseWordsQ.ReverseWords("hellO worlD"));
        // Console.WriteLine(ReverseWordsQ.ReverseWords("the sky is blue"));

        // 7. product of array except self
        // PrintArray(ProductExceptSelfQ.ProductExceptSelf(new[] { 1, 2, 3, 4 }));

        // 8. Increasing triplet subsequence
        // Console.WriteLine("=== Example 1: Your confusing case ===");
        // IncreasingTripletSeq.TraceAlgorithm(new[] { 5, 3, 0, 3, 1, -1, 2 });
        //
        // Console.WriteLine("=== Example 2: Simple case ===");
        // IncreasingTripletSeq.TraceAlgorithm(new[] { 1, 2, 3 });
        //
        // Console.WriteLine("=== Example 3: No triplet ===");
        // IncreasingTripletSeq.TraceAlgorithm(new[] { 5, 4, 3, 2, 1 });
        //
        // Console.WriteLine("=== Example 4: First gets updated after second is set ===");
        // IncreasingTripletSeq.TraceAlgorithm(new[] { 4, 5, 1, 6 });

        // 9. String compression
        // char[] input = new[] { 'a', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'c', 'c' };
        // char[] input = new[] { 'a', 'b', 'b', 'c', 'c' };
        // char[] input = new[] { 'a', 'a', 'a', 'a', 'b', 'a' };
        // PrintArray(input);
        // Console.WriteLine(StringCompression.Compress(input));
        // Console.WriteLine("Compressed");
        // PrintArray(input);

        // SlidingWindowProblems();

        // DynamicProgrammingProblems();

        // TwoPointerQuestions();

        // HashMapProblems();
        
        // LinkedListProblems.SolveProblems();
        // BinaryTreeProblems.SloveBinaryTree();
        
        GraphProblems.SolveProblems();
    }

    private static void HashMapProblems()
    {
        // Console.WriteLine(HashMapQ.EqualPairs([[3, 2, 1], [1, 7, 6], [2, 7, 7]]));

        Console.WriteLine(HashMapQ.EqualPairs([[3, 1, 2, 2], [1, 4, 4, 5], [2, 4, 2, 2], [2, 4, 2, 2]]));
    }

    private static void SlidingWindowProblems()
    {
        // 1. Max avg subarray
        // Console.WriteLine(SlidingWindow.FindMaxAverage(new[] { 1, 12, -5, -6, 50, 3 }, 4));

        // 2. Longest subarray
        // Console.WriteLine(SlidingWindow.LongestSubarray(new[] { 1, 1, 1, 2 }));

        // 3. Max Vowels
        // Console.WriteLine(SlidingWindow.MaxVowels("abciiidef", 3));

        // 4. Longest ones
        // Console.WriteLine(SlidingWindow.LongestOnes([0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1], 3));
        Console.WriteLine(SlidingWindow.LongestOnes([0, 0, 0, 0], 0));
    }


    private static void DynamicProgrammingProblems()
    {
        // 1. Maximize robbed 
        // Console.WriteLine(Dynamic1D.Rob(new[] { 1, 1, 1, 2 }));

        // 2. Stair cost
        // Console.WriteLine(Dynamic1D.MinCostClimbingStairs(new[] { 10, 15, 20 }));

        // 3. Unique paths
        // Console.WriteLine(Dynamic2D.UniquePaths(3, 2));

        // 4. Longest common subsequence
        Console.WriteLine(Dynamic2D.LongestCommonSubsequence("bsbininm", "jmjkbkjkv"));
    }

    private static void TwoPointerQuestions()
    {
        // TwoPointers.MoveZeroes([0, 1, 0, 3, 12]);
        // Console.WriteLine(TwoPointers.IsSubsequence("abc", "ahbgdc"));

        // Console.WriteLine(TwoPointers.MaxArea([1, 8, 6, 2, 5, 4, 8, 3, 7]));
        Console.WriteLine(TwoPointers.MaxOperations([2, 5, 4, 4, 1, 3, 4, 4, 1, 4, 4, 1, 2, 1, 2, 2, 3, 2, 4, 2], 3));
        // -,5,4,4,-,3,4,4,*,4,4,!,*,@,!,@,3,2,4,2
    }


    private static void PrintMatrix<T>(T[,] matrix)
    {
        for (int i = 0; i < matrix.GetLength(0); i++)
        {
            for (int j = 0; j < matrix.GetLength(1); j++)
            {
                Console.Write($"{matrix[i, j]}, ");
            }

            Console.WriteLine();
        }
    }

    public static void PrintArray<T>(T[] array, int[] indexes = null)

    {
        for (int i = 0; i < array.Length; i++)
        {
            if (indexes != null && indexes.Contains(i)) Console.Write("\u001b[7m"); // Inverse on
            Console.Write($"{array[i]}");
            if (indexes != null && indexes.Contains(i)) Console.Write("\u001b[0m"); // Reset
            Console.Write(", ");
        }

        Console.WriteLine();
    }

    public static void PrintStringWithHighlight(string input, int[] indexes, bool newline = true)
    {
        if (string.IsNullOrEmpty(input))
        {
            Console.WriteLine(input);
            return;
        }

        for (int i = 0; i < input.Length; i++)
        {
            if (indexes.Contains(i))
            {
                Console.Write("\u001b[7m"); // Inverse on
                Console.Write(input[i]);
                Console.Write("\u001b[0m"); // Reset 
            }
            else
            {
                Console.Write(input[i]);
            }
        }

        if (newline) Console.WriteLine();

        else Console.Write(" ");
    }

    public static class HashMapQ
    {
        private class TrieNode
        {
            public Dictionary<int, TrieNode> nextNodes;
            public int number;
            public int count;

            public bool isRoot;

            public TrieNode(int num)
            {
                nextNodes = new Dictionary<int, TrieNode>();
                number = num;
                isRoot = false;
            }

            public TrieNode()
            {
                nextNodes = new Dictionary<int, TrieNode>();
                isRoot = true;
            }

            public TrieNode? GetNodeWithValue(int num)
            {
                if (nextNodes != null && nextNodes.ContainsKey(num))
                {
                    return nextNodes[num];
                }

                return null;
            }
        }

        public static int EqualPairs(int[][] grid)
        {
            TrieNode root = new TrieNode();
            int pairs = 0;

            for (int i = 0; i < grid.Length; i++)
            {
                AddNode(root, grid[i].AsSpan());
            }

            for (int j = 0; j < grid.Length; j++)
            {
                TrieNode? node = root;
                int[] path = new int[grid.Length];
                for (int i = 0; i < grid.Length; i++)
                {
                    path[i] = grid[i][j];
                    node = node.GetNodeWithValue(grid[i][j]);
                    if (node == null) break;
                }

                if (node != null)
                {
                    PrintTriePath(root, path);
                    pairs += node.count;
                }
            }

            PrintTrie(root);


            return pairs;
        }

        private static void AddNode(TrieNode root, Span<int> nums)
        {
            if (!root.nextNodes.ContainsKey(nums[0]))
            {
                root.nextNodes.Add(nums[0], new TrieNode(nums[0]));
            }

            if (nums.Length > 1)
            {
                AddNode(root.nextNodes[nums[0]], nums.Slice(1));
            }
            else
            {
                root.nextNodes[nums[0]].count++;
            }
        }

        private static void PrintTrie(TrieNode node, string prefix = "", bool isLast = true)
        {
            // Print current node
            Console.Write(prefix);
            Console.Write(isLast ? "└── " : "├── ");

            if (node.isRoot)
            {
                Console.WriteLine("Root");
            }
            else
            {
                Console.WriteLine($"{node.number} ({node.count})");
            }

            // Prepare prefix for children
            prefix += isLast ? "    " : "│   ";

            // Print children
            var children = node.nextNodes.Values.ToList();
            for (int i = 0; i < children.Count; i++)
            {
                PrintTrie(children[i], prefix, i == children.Count - 1);
            }
        }

        // Helper method to print a specific path in the trie
        private static void PrintTriePath(TrieNode root, int[] path)
        {
            Console.WriteLine($"Path: [{string.Join(", ", path)}]");
            TrieNode current = root;
            string prefix = "";

            Console.WriteLine("Root");
            foreach (int num in path)
            {
                prefix += "  ";
                if (current.nextNodes.ContainsKey(num))
                {
                    current = current.nextNodes[num];
                    Console.WriteLine($"{prefix}└── {num} (count: {current.count})");
                }
                else
                {
                    Console.WriteLine($"{prefix}└── {num} (not found)");
                    break;
                }
            }
        }
    }

    public static class TwoPointers
    {
        public static int MaxOperations(int[] nums, int k)
        {
            List<int> neededNums = new List<int>();

            // List<int> indexesToDelete = new List<int>();
            int numOfOps = 0;
            for (int i = 0; i < nums.Length; i++)
            {
                if (neededNums.Contains(nums[i]))
                {
                    numOfOps++;
                    neededNums.Remove(nums[i]);
                }
                else
                {
                    neededNums.Add(k - nums[i]);
                }
            }

            return numOfOps;
        }

        public static int MaxArea(int[] height)
        {
            int l = 0, r = height.Length - 1;

            int maxArea = 0;
            while (l < r)
            {
                int area = 0;
                if (height[l] < height[r])
                {
                    area = height[l] * (r - l);
                    l++;
                }
                else
                {
                    area = height[r] * (r - l);
                    r--;
                }

                if (area > maxArea)
                {
                    maxArea = area;
                }
            }

            return maxArea;
        }

        public static bool IsSubsequence(string s, string t)
        {
            if (s.Length == 0) return true;
            if (t.Length < s.Length) return false;
            if (t.Length == s.Length) return s.Equals(t);

            int l = 0, r = 0;

            for (; l < s.Length && r < t.Length; r++)
            {
                PrintStringWithHighlight(s, [l, r], false);
                if (s[l] == t[r]) l++;
            }

            return l == s.Length;
        }

        public static void MoveZeroes(int[] nums)
        {
            int l = 0, r = 0;
            // find the first zero with l
            for (; l < nums.Length; l++)
            {
                if (nums[l] == 0) break;
            }

            // find the first non-zero after l with r
            for (r = l; r < nums.Length; r++)
            {
                if (nums[r] != 0) break;
            }

            Console.WriteLine($"l: {l}, r: {r}");
            PrintArray(nums);
            while (r < nums.Length)
            {
                if (nums[r] != 0)
                {
                    (nums[l], nums[r]) = (nums[r], nums[l]);
                    l++; // move l forwad since it will be pointing to a non-zero now
                }

                r++;
                PrintArray(nums);
            }
        }
    }

    public static class Dynamic2D
    {
        public static int LongestCommonSubsequence(string text1, string text2)
        {
            if (text1.Length == 0 || text2.Length == 0) return 0;
            if (text1.Length == 1 && text2.Length == 1) return (text1 == text2) ? 1 : 0;
            if (text1.Length == 1) return (text2.Contains(text1)) ? 1 : 0;
            if (text2.Length == 1) return (text1.Contains(text2)) ? 1 : 0;

            int[,] lcs = new int[text1.Length, text2.Length];

            for (int i = 0; i < text1.Length; i++)
            {
                for (int j = 0; j < text2.Length; j++)
                {
                    if (i == 0 && j == 0) lcs[i, j] = (text1[0] == text2[0]) ? 1 : 0;
                    else if (i == 0) lcs[i, j] = (text1[0] == text2[j]) ? 1 : lcs[0, j - 1];
                    else if (j == 0) lcs[i, j] = (text1[i] == text2[0]) ? 1 : lcs[i - 1, 0];
                    else
                    {
                        lcs[i, j] = Math.Max(lcs[i, j - 1], lcs[i - 1, j]);
                        if (text1[i] == text2[j]) lcs[i, j] = lcs[i - 1, j - 1] + 1;
                    }
                }

                Console.WriteLine($"{i}");
                PrintMatrix(lcs);
            }

            return lcs[text1.Length - 1, text2.Length - 1];
        }

        public static int UniquePaths(int m, int n)
        {
            if (m == 1 && n == 1) return 0;
            if (m == 1 || n == 1) return 1;

            int[,] grid = new int[m, n];
            grid[0, 0] = 1;

            for (int i = 0; i < m; i++)
            {
                for (int j = 0; j < n; j++)
                {
                    if (i == 0 && j == 0) continue;
                    else if (i == 0) grid[0, j] = 1;
                    else if (j == 0) grid[i, 0] = 1;
                    else
                    {
                        grid[i, j] = grid[i, j - 1] + grid[i - 1, j];
                    }
                }
            }

            return grid[m - 1, n - 1];
        }
    }

    public static class Dynamic1D
    {
        public static int MinCostClimbingStairs(int[] cost)
        {
            if (cost.Length == 0) return 0;
            if (cost.Length == 1) return cost[0];
            if (cost.Length == 2) return Math.Min(cost[0], cost[1]);

            int[] totalCost = new int[cost.Length];
            totalCost[0] = cost[0];
            totalCost[1] = Math.Min(cost[0] + cost[1], cost[1]);

            for (int i = 2; i < cost.Length; i++)
            {
                totalCost[i] = Math.Min(totalCost[i - 1], totalCost[i - 2]) + cost[i];
            }

            PrintArray(totalCost);
            return Math.Min(totalCost[^1], totalCost[^2]);
        }

        public static int Rob(int[] nums)
        {
            if (nums.Length == 0) return 0;
            if (nums.Length == 1) return nums[0];
            if (nums.Length == 2) return Math.Max(nums[0], nums[1]);
            if (nums.Length == 3) return Math.Max(nums[1], nums[0] + nums[2]);


            int[] robbedAmount = new int[nums.Length];
            robbedAmount[0] = nums[0];
            robbedAmount[1] = Math.Max(nums[0], nums[1]);
            robbedAmount[2] = Math.Max(nums[1], nums[0] + nums[2]);
            int maxAmount = Math.Max(robbedAmount[0], Math.Max(robbedAmount[1], robbedAmount[2]));

            for (int i = 3; i < nums.Length; i++)
            {
                robbedAmount[i] = Math.Max(robbedAmount[i - 2], robbedAmount[i - 3]) + nums[i];
                if (robbedAmount[i] > maxAmount) maxAmount = robbedAmount[i];
                PrintArray(robbedAmount);
            }

            return maxAmount;
        }
    }

    public static class SlidingWindow
    {
        public static int LongestOnes(int[] nums, int k)
        {
            int l = 0, r = 0;

            // Use r to find last 1 after k zeroes.
            int numOfZeroes = 0;
            int maxContinousOnes = 0;

            for (int i = 0; i < nums.Length; i++)
            {
                if (nums[i] == 0) numOfZeroes++;

                if (numOfZeroes > k)
                {
                    Console.Write($"{i} zeroes: {numOfZeroes} | ones: {maxContinousOnes} > ");
                    PrintArray(nums, [l, i]);
                    for (; l <= i; l++)
                    {
                        if (nums[l] == 0)
                        {
                            numOfZeroes--;
                            l++;
                            break;
                        }
                    }

                    Console.WriteLine($"z{numOfZeroes} l{l}");
                }

                if (maxContinousOnes < i - l + 1) maxContinousOnes = i - l + 1;
                Console.Write($"{i} zeroes: {numOfZeroes} | ones: {maxContinousOnes} | ");
                PrintArray(nums, [l, i]);
            }


            return maxContinousOnes;
        }

        public static int MaxVowels(string s, int k)
        {
            if (s.Length == 0) return 0;
            if (s.Length < k) return 0;

            HashSet<char> vowels = new HashSet<char> { 'a', 'e', 'i', 'o', 'u' };
            int vowelsInCurrentWindow = 0;
            int maxVowels = 0;
            for (int i = 0; i < s.Length; i++)
            {
                if (i < k)
                {
                    // PrintStringWithHighlight(s, [i]);
                    if (vowels.Contains(s[i])) vowelsInCurrentWindow++;
                    if (vowelsInCurrentWindow > maxVowels) maxVowels = vowelsInCurrentWindow;
                    continue;
                }

                // PrintStringWithHighlight(s, [i, i - k]);
                // Console.WriteLine($" {vowelsInCurrentWindow} {maxVowels}");
                if (vowels.Contains(s[i])) vowelsInCurrentWindow++;
                if (vowels.Contains(s[i - k])) vowelsInCurrentWindow--;

                if (vowelsInCurrentWindow > maxVowels) maxVowels = vowelsInCurrentWindow;
            }

            return maxVowels;
        }

        public static int LongestSubarray(int[] nums)
        {
            if (nums.Length < 2) return 0;

            int[] prefix = new int[nums.Length];
            prefix[0] = (nums[0] == 0) ? 0 : 1;
            for (int i = 1; i < nums.Length; i++)
            {
                if (nums[i] == 0) prefix[i] = 0;
                else prefix[i] = prefix[i - 1] + 1;
            }

            int[] postfix = new int[nums.Length];
            postfix[^1] = (nums[^1] == 0) ? 0 : 1;
            for (int i = postfix.Length - 2; i >= 0; i--)
            {
                if (nums[i] == 0) postfix[i] = 0;
                else postfix[i] = postfix[i + 1] + 1;
            }

            int[] ifDeleted = new int[nums.Length];
            int maxlen = 0;
            int indexToBeDeleted = -1;

            ifDeleted[0] = postfix[1];
            ifDeleted[^1] = prefix[^2];

            if (ifDeleted[0] >= ifDeleted[^1])
            {
                maxlen = ifDeleted[0];
                indexToBeDeleted = 0;
            }
            else
            {
                maxlen = ifDeleted[^1];
                indexToBeDeleted = ifDeleted.Length - 1;
            }

            for (int i = 1; i < ifDeleted.Length - 1; i++)
            {
                ifDeleted[i] = prefix[i - 1] + postfix[i + 1];
                if (maxlen < ifDeleted[i])
                {
                    maxlen = ifDeleted[i];
                    indexToBeDeleted = i;
                }
            }

            return maxlen;
        }

        public static double FindMaxAverage(int[] nums, int k)
        {
            if (nums.Length == 0) return 0.0;
            if (nums.Length == 1) return nums[0];

            long sum = nums[0];
            long maxSum = nums[0];
            for (int i = 1; i < nums.Length; i++)
            {
                if (i < k)
                {
                    sum += nums[i];
                    maxSum += nums[i];
                    continue;
                }

                sum = sum - nums[i - k] + nums[i];
                if (sum > maxSum)
                {
                    maxSum = sum;
                }
            }

            return ((double)maxSum) / k;
        }
    }

    public static class StringCompression
    {
        public static int Compress(char[] chars)
        {
            if (chars.Length == 1) return 1;

            char currentGroupChar = chars[0];
            int currentGroupCount = 1;
            int compressionTracker = -1; // index value which denotes till which char compression has been done

            for (int i = 1; i < chars.Length; i++)
            {
                if (chars[i] == currentGroupChar)
                {
                    currentGroupCount++;
                }
                else if (currentGroupCount == 1)
                {
                    chars[++compressionTracker] = currentGroupChar;
                    currentGroupChar = chars[i];
                }
                else
                {
                    chars[++compressionTracker] = currentGroupChar;
                    char[] countChars = currentGroupCount.ToString().ToCharArray();
                    foreach (var countChar in countChars)
                    {
                        chars[++compressionTracker] = countChar;
                    }

                    currentGroupChar = chars[i];
                    currentGroupCount = 1;
                }
            }

            if (currentGroupCount == 1)
            {
                chars[++compressionTracker] = currentGroupChar;
            }
            else if (currentGroupCount > 1)
            {
                chars[++compressionTracker] = currentGroupChar;
                char[] countChars = currentGroupCount.ToString().ToCharArray();
                foreach (var countChar in countChars)
                {
                    chars[++compressionTracker] = countChar;
                }
            }

            for (int i = compressionTracker + 1; i < chars.Length; i++)
            {
                chars[i] = '\0';
            }

            return compressionTracker + 1;
        }
    }

    public static class IncreasingTripletSeq
    {
        public static bool IncreasingTriplet(int[] nums)
        {
            if (nums == null || nums.Length < 3) return false;

            int first = int.MaxValue;
            int second = int.MaxValue;

            foreach (int num in nums)
            {
                if (num <= first)
                {
                    first = num;
                }
                else if (num <= second)
                {
                    second = num;
                }
                else
                {
                    return true;
                }
            }

            return false;
        }

        // Helper method to trace the algorithm step by step for understanding
        public static void TraceAlgorithm(int[] nums)
        {
            if (nums == null || nums.Length < 3)
            {
                Console.WriteLine("Array too small");
                return;
            }

            int first = int.MaxValue;
            int second = int.MaxValue;

            Console.WriteLine($"Processing array: [{string.Join(", ", nums)}]\n");

            for (int i = 0; i < nums.Length; i++)
            {
                int num = nums[i];
                string action;

                if (num <= first)
                {
                    first = num;
                    action = $"Update FIRST to {num}";
                }
                else if (num <= second)
                {
                    second = num;
                    action = $"Update SECOND to {num} (we now have {first} < {second})";
                }
                else
                {
                    Console.WriteLine($"Index {i}: num={num}, first={first}, second={second}");
                    Console.WriteLine(
                        $"Found triplet! {num} > {second}, and second was set when a smaller value existed before it.");
                    Console.WriteLine($"Result: TRUE\n");
                    return;
                }

                Console.WriteLine($"Index {i}: num={num}, first={first}, second={second} - {action}");
            }

            Console.WriteLine($"\nFinal: first={first}, second={second}");
            Console.WriteLine("Result: FALSE - No triplet found\n");
        }
    }

    public static class ProductExceptSelfQ
    {
        public static int[] ProductExceptSelf(int[] nums)
        {
            if (nums == null || nums.Length < 2) return nums;
            if (nums.Length == 2) return new[] { nums[1], nums[0] };

            int[] prefixArr = new int[nums.Length];
            prefixArr[0] = nums[0];
            for (int i = 1; i < nums.Length; i++)
            {
                prefixArr[i] = nums[i] * prefixArr[i - 1];
            }

            // Console.WriteLine("Prefix array");
            // PrintArray(prefixArr);

            int[] postfixArr = new int[nums.Length];
            postfixArr[nums.Length - 1] = nums[nums.Length - 1];
            for (int i = nums.Length - 2; i >= 0; i--)
            {
                postfixArr[i] = nums[i] * postfixArr[i + 1];
            }
            // Console.WriteLine("Postfix array");
            // PrintArray(postfixArr);

            int[] productArr = new int[nums.Length];
            productArr[0] = postfixArr[1];
            productArr[nums.Length - 1] = prefixArr[prefixArr.Length - 2];
            for (int i = 1; i < productArr.Length - 1; i++)
            {
                productArr[i] = prefixArr[i - 1] * postfixArr[i + 1];
            }

            return productArr;
        }
    }

    public static class ReverseWordsQ
    {
        public static string ReverseWords(string s)
        {
            if (s == null || s.Length == 0) return string.Empty;

            StringBuilder sb = new StringBuilder(s.Length);

            ReadOnlySpan<char> span = s.AsSpan();

            int wordLength = 0;
            bool isFirstWord = true;

            for (int i = span.Length - 1; i >= 0; i--)
            {
                if (span[i] != ' ')
                {
                    wordLength++;
                }
                else if (span[i] == ' ' && wordLength > 0)
                {
                    // found a word
                    if (!isFirstWord)
                    {
                        sb.Append(' ');
                    }

                    sb.Append(span.Slice(i + 1, wordLength));
                    isFirstWord = false;
                    wordLength = 0;
                }
            }

            if (wordLength > 0)
            {
                if (!isFirstWord)
                {
                    sb.Append(' ');
                }

                sb.Append(span.Slice(0, wordLength));
            }

            return sb.ToString();
        }
    }

    public static class ReverseVowelsQ
    {
        public static string ReverseVowels(string s)
        {
            if (s == null || s.Length < 2)
            {
                return s;
            }

            StringBuilder sb = new StringBuilder(s);

            HashSet<char> vowels = new HashSet<char>
            {
                'a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U'
            };


            int i = 0, j = sb.Length - 1;
            while (i < j)
            {
                while (i < j && !vowels.Contains(sb[i])) i++; // find the vowels from front
                while (i < j && !vowels.Contains(sb[j])) j--; // find the vowels from back

                (sb[i], sb[j]) = (sb[j], sb[i]);
                i++;
                j--;
            }

            return sb.ToString();
        }
    }

    public static class Flowerbed
    {
        public static bool CanPlaceFlowers(int[] flowerbed, int n)
        {
            int continuousEmptySpaces = (flowerbed[0] == 1) ? 0 : 1;

            for (int i = 0; i < flowerbed.Length; i++)
            {
                if (flowerbed[i] == 1)
                {
                    n -= (continuousEmptySpaces - 1) / 2;
                    continuousEmptySpaces = 0;
                }
                else
                {
                    continuousEmptySpaces++;
                }
            }

            n -= continuousEmptySpaces / 2;
            return n <= 0;
        }
    }

    public static class GreatestNumberOfCandies
    {
        public static IList<bool> KidsWithCandies(int[] candies, int extraCandies)
        {
            if (candies == null)
            {
                return new List<bool>();
            }

            int max = candies.Max();

            bool[] isGreatest = new bool[candies.Length];

            for (int i = 0; i < candies.Length; i++)
            {
                isGreatest[i] = (candies[i] + extraCandies >= max);
            }

            return isGreatest;
        }
    }

    public class GreatestCommonDivisorOfStrings
    {
        public static string GcdOfStrings(string str1, string str2)
        {
            int minLength = Math.Min(str1.Length, str2.Length);

            string commonDivisor = string.Empty;
            StringBuilder sb = new StringBuilder();
            for (int i = minLength; i > 0; i--)
            {
                if (str1.Length % i == 0 && str2.Length % i == 0 && IsDivisible(str1, str2, i, sb))
                {
                    return str1.Substring(0, i);
                }
            }

            return commonDivisor;
        }

        private static bool IsDivisible(string str1, string str2, int i, StringBuilder sb)
        {
            ReadOnlySpan<char> candidateDivisorString = str1.AsSpan(0, i);

            int str1Factor = str1.Length / i;
            int str2Factor = str2.Length / i;

            for (int j = 0; j < str1Factor; j++)
            {
                sb.Append(candidateDivisorString);
            }

            bool str1IsDivisible = sb.Equals(str1.AsSpan());

            if (!str1IsDivisible) return false;

            sb.Clear();
            for (int j = 0; j < str2Factor; j++)
            {
                sb.Append(candidateDivisorString);
            }

            bool str2IsDivisible = sb.Equals(str2.AsSpan());
            sb.Clear();

            if (!str2IsDivisible) return false;

            return true;
        }
    }

    public class MergeStrings
    {
        public static string merge(string word1, string word2)
        {
            // 1. Core logic
            // 2. Edge cases - input validation
            //  null values
            // 3. logging for debugging
            // 4. exceptions

            if (word1 == null)
            {
                word1 = "";
            }

            if (word2 == null)
            {
                word2 = "";
            }

            try
            {
                StringBuilder word3Builder = new(word1.Length + word2.Length);
                int minLength = Math.Min(word1.Length, word2.Length);

                for (int i = 0; i < minLength; i++)
                {
                    word3Builder.Append(word1[i]).Append(word2[i]);
                }

                word3Builder.Append(word1.AsSpan(minLength))
                    .Append(word2.AsSpan(minLength));

                string word3 = word3Builder.ToString();
                Console.WriteLine($" {word1} + {word2} => {word3}");
                return word3;
            }
            catch (Exception ex)
            {
                Console.WriteLine($" {word1} + {word2} threw Error: " + ex.Message);
                return "";
            }
        }
    }
}