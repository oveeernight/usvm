namespace samples;

public class Classes
{
    [SvmTest(81)]
    public int SymbolicClassSet(User u)
    {
        u.Id = 1337;
        if (u.Id == 1337)
        {
            return 1;
        }
        return -1;
    }

    [SvmTest(100)]
    public int SymbolicCyclicListNode(ListNode n)
    {
        if (n.next == n)
        {
            return 1;
        }
        return 0;
    }
    public int ConcreteObjectDefaultValue()
    {
        var user = new User();
        var id = user.Id;
        if (id != 0)
        {
            return -1;
        }

        return 0;
    }
}

public class ListNode
{
    public ListNode next;
    public int value;
}

public class User
{
    public int Id;
} 