// See https://aka.ms/new-console-template for more information

Console.WriteLine("Hello, World!");


// Database style
// Query Support
// creating tables 
// forming relations


/*
 * database
 * Tables
 * queries
 * Columns and rows
 *
 */

/*
 * Data - homogeneous data in arrays
 * queries - on arrays
 *
 */


public struct Node
{
    public int Id { get; set; }
    public int Value { get; set; }
    
}

public struct NodeRegistry // table
{
    public Node[] nodes { get; set; }
}


/*
 * How to generalize queries and general algos?
 */

public class Queries
{
    public static int FindNodeIndex(NodeRegistry nodeRegistry, int value)
    {
        for (int i = 0; i < nodeRegistry.nodes.Length; i++)
        {
            if (nodeRegistry.nodes[i].Value.Equals(value))
            {
                return i;
            }
        }

        return -1;
    }
}











