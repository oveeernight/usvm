namespace samples;

public class Classes
{
    [SvmTest(83)]
    public int FieldsInequalityImpliesObjectsInequality(User a, User b)
    {
        if (a.Id != b.Id)
        {
            if (a == b)
            {
                return -1;
            }
        }
        return 0;
    }
    
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
    
    [SvmTest(71)]
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
    
    
    [SvmTest(77)]
    public int StaticCtorTest1()
    {
        var user = new User();
        if (User.staticField != 42)
        {
            return -1;
        }

        return 0;
    }
    
    [SvmTest(71)]
    public int StaticCtorTest2()
    {
        if (User.staticField != 42)
        {
            return -1;
        }

        return 0;
    }
    
    [SvmTest(81)]
    public int StaticCtorTest3()
    {
        User.staticField++;
        if (User.staticField != 43)
        {
            return -1;
        }
        return 0;
    }
    
    [SvmTest(100)]
    public static A RecObject(int n)
    {
        if (n > 0)
        {
            return null;
        }

        var a = new A();
        var b = new B();
        a.SetB(b);
        b.SetA(a);
        return a;
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

    static User()
    {
        staticField = 42;
    }

    public static int staticField;
} 

public class A
{
    private B _b;

    public void SetB(B b)
    {
        _b = b;
    }
}

public class B
{
    private A _a;

    public void SetA(A a)
    {
        _a = a;
    }
}