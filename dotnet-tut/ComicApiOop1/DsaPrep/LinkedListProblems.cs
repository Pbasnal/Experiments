namespace DsaPrep;

public static class LinkedListProblems
{
    public class ListNode
    {
        public int val;
        public ListNode next;

        public ListNode(int val = 0, ListNode next = null)
        {
            this.val = val;
            this.next = next;
        }
    }

    public static ListNode CreateLinkedList(int[] arr)
    {
        if (arr == null || arr.Length == 0) return null;

        ListNode head = new ListNode(arr[0]);
        ListNode current = head;

        for (int i = 1; i < arr.Length; i++)
        {
            current.next = new ListNode(arr[i]);
            current = current.next;
        }

        return head;
    }

    public static void PrintList(ListNode node)
    {
        if (node == null) Console.WriteLine("NULL");
        else
        {
            while (node != null)
            {
                Console.Write("{0}", node.val);
                if (node.next != null)
                {
                    Console.Write("->");
                }

                node = node.next;
            }

            Console.WriteLine();
        }
    }


    public static void SolveProblems()
    {
        Console.WriteLine(PairSum(CreateLinkedList([5, 4, 2, 3])));

        // PrintList(ReverseList(CreateLinkedList([1, 2, 3, 4, 5, 6,])));

        // PrintList(OddEvenList(CreateLinkedList([1, 2, 3])));

        // PrintList(null);
        // PrintList(DeleteMiddle(null));
        // Console.WriteLine();
        //
        //
        // PrintList(CreateLinkedList([1]));
        // PrintList(DeleteMiddle(CreateLinkedList([1])));
        // Console.WriteLine();
        //
        //
        // PrintList(CreateLinkedList([1, 3]));
        // PrintList(DeleteMiddle(CreateLinkedList([1, 3])));
        // Console.WriteLine();
        //
        // PrintList(CreateLinkedList([1, 3, 4]));
        // PrintList(DeleteMiddle(CreateLinkedList([1, 3, 4])));
        // Console.WriteLine();
        //
        // PrintList(CreateLinkedList([1, 3, 4, 7]));
        // PrintList(DeleteMiddle(CreateLinkedList([1, 3, 4, 7])));
        // Console.WriteLine();
        //
        // PrintList(CreateLinkedList([1, 3, 4, 7, 1, 2, 6]));
        // PrintList(DeleteMiddle(CreateLinkedList([1, 3, 4, 7, 1, 2, 6])));
        // Console.WriteLine();
    }

    public static int PairSum(ListNode head)
    {
        if (head == null) return 0;
        if (head.next == null) return head.val;
        if (head.next.next == null) return head.val + head.next.val;

        int maxSum = Int32.MinValue;
        IDictionary<int, int> pairSumMap = new Dictionary<int, int>();

        ListNode slowPtr = head;
        ListNode fastPtr = head;

        int i = 0;
        while (fastPtr != null && fastPtr.next != null)
        {
            fastPtr = fastPtr.next.next;
            pairSumMap.Add(i, slowPtr.val);
            slowPtr = slowPtr.next;
            i++;
        }

        int listLength = (i ) * 2;
        while (slowPtr != null)
        {
            int key = listLength - i - 1;
            pairSumMap[key] += slowPtr.val;
            slowPtr = slowPtr.next;

            if (maxSum < pairSumMap[key])
            {
                maxSum = pairSumMap[key];
            }

            i++;
        }

        return maxSum;
    }

    public static ListNode ReverseList(ListNode head)
    {
        if (head == null || head.next == null) return head;

        ListNode node = head;
        ListNode next = head.next;

        head = null;
        while (next != null)
        {
            node.next = head;
            head = node;
            node = next;
            next = next.next;
        }

        node.next = head;
        head = node;

        return head;
    }

    public static ListNode DeleteMiddle(ListNode head)
    {
        if (head == null || head.next == null) return null;

        if (head.next.next == null)
        {
            head.next = null;
            return head;
        }

        ListNode nodeToUpdate = head;
        ListNode slowPtr = head.next;
        ListNode fastPtr = head.next;

        while (fastPtr.next != null && fastPtr.next.next != null)
        {
            fastPtr = fastPtr.next.next;
            nodeToUpdate = slowPtr;
            slowPtr = slowPtr.next;
        }

        nodeToUpdate.next = slowPtr.next;

        return head;
    }

    public static ListNode OddEvenList(ListNode head)
    {
        if (head == null || head.next == null) return head;

        ListNode oddHead = head;
        ListNode evenHead = head.next;

        ListNode oddPtr = head;
        ListNode evenPtr = head.next;

        ListNode listPtr = head.next.next;

        bool isEven = false;
        while (listPtr != null)
        {
            if (isEven)
            {
                evenPtr.next = listPtr;
                evenPtr = listPtr;
            }
            else
            {
                oddPtr.next = listPtr;
                oddPtr = listPtr;
            }

            listPtr = listPtr.next;
            isEven = !isEven;
        }

        oddPtr.next = evenHead;
        evenPtr.next = null;

        return oddHead;
    }
}