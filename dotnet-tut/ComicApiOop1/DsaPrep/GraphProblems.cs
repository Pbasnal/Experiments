namespace DsaPrep;

public static class GraphProblems
{
    public static void SolveProblems()
    {
        // Console.WriteLine($"Can visit? {CanVisitAllRooms([[1],[2],[3],[]])}");
        // Console.WriteLine($"Can visit? {CanVisitAllRooms([[1,3],[3,0,1],[2],[0]])}");
        // Console.WriteLine($"Province count {FindCircleNum([[1, 1, 0], [1, 1, 0], [0, 0, 1]])}");
        // Console.WriteLine($"Province count {FindCircleNum([[1,0,0,1],[0,1,1,0],[0,1,1,1],[1,0,1,1]])}");

        Console.WriteLine($"Reorders: {MinReorder(6, [[0, 1], [1, 3], [2, 3], [4, 0], [4, 5]])}");
    }


    private static int MinReorder(int n, int[][] connections)
    {
        int numOfReorders = 0;
        
        
        
        return 0;
    }

    private static int FindCircleNum(int[][] isConnected)
    {
        if (isConnected == null || isConnected.Length == 0) return 0;
        if (isConnected.Length == 1) return 1;

        int provinceCount = 0;
        // at the end of the loop, this number will be incremented

        bool[] isVisited = new bool[isConnected.Length];
        Stack<int> frontier = new Stack<int>();

        for (int i = 0; i < isConnected.Length; i++)
        {
            if (isVisited[i]) continue;

            frontier.Push(i);

            while (frontier.Count > 0)
            {
                int provinceToVisit = frontier.Pop();

                if (isVisited[provinceToVisit]) continue;

                for (int connectedProvince = 0;
                     connectedProvince < isConnected[provinceToVisit].Length;
                     connectedProvince++)
                {
                    if (isConnected[provinceToVisit][connectedProvince] == 1)
                    {
                        frontier.Push(connectedProvince);
                    }
                }

                isVisited[provinceToVisit] = true;
            }

            provinceCount++;
        }

        return provinceCount == 0 ? 1 : provinceCount;
    }

    public static bool CanVisitAllRooms(IList<IList<int>> rooms)
    {
        // Structure
        // 2 lists -
        // visited - if the node has been visited: HashSet
        // frontier - the next room to visit. Stack for DFS

        // Flow
        // start - pick the first room
        //
        // Process -    move all keys to frontier  
        //              Mark room as visited
        //              pick the room from the frontier list
        // 
        // End -    If all rooms are visited, then true, else false
        // 
        // Cons -   performance? frequent traversals between nodes 
        //          Need More Memory - memory for frontier and stack
        //              recursive calls, pass the visited hashSet to each call. Reduces frontier stack.
        //              Not that good since callstack will be used.
        //

        if (rooms == null || rooms.Count < 2) return true;

        // rooms are 0 based indexed
        // contains index of the room and the room itself
        bool[] visitedRooms = new bool[rooms.Count];
        int numberOfVisitedRooms = 0;

        Stack<int> frontierStack = new Stack<int>();
        frontierStack.Push(0);

        while (frontierStack.Count > 0)
        {
            int roomToVisit = frontierStack.Pop();

            if (visitedRooms[roomToVisit]) continue;

            IList<int> keysToNextRooms = rooms[roomToVisit];
            foreach (var key in keysToNextRooms)
            {
                frontierStack.Push(key);
            }

            visitedRooms[roomToVisit] = true;
            numberOfVisitedRooms++;
        }

        return numberOfVisitedRooms == rooms.Count;
    }
}