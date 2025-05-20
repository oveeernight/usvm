namespace samples;

public class Calls
{
    public int Add(int x, int y) => x + y;
    public int Sub(int x, int y) => x - y;
    public int Mul(int x, int y) => x * y;
    public int Div(int x, int y) => x / y;

    [SvmTest(100)]
    public int MulOrAdd(bool flag, int x, int y)
    {
        var add = 10l + x;
        if (flag)
        {
            return Add(x, y);
        }
        return Sub(x, y);
    }

    [SvmTest(77)]
    public int NonVirtualCall1()
    {
        var a = new A();
        var result = a.Foo(42);
        if (result != 42)
        {
            return -1;
        }
        return 0;
    }

    [SvmTest(77)]
    public int VirtualCall1()
    {
        A obj = new B();
        var result = obj.Foo(42);
        if (result != 43)
        {
            return -1;
        }
        return 0;
    }

    [SvmTest(83)]
    public int VirtualCall2(A obj)
    {
        var result = obj.Foo(42);
        if (result == 43 && obj is not B)
        {
            return -1;
        }
        return 0;
    }

    [SvmTest(91)]
    public int VirtualCallOnSymbolicReading(A[] a, int i)
    {
        var elem = a[i];
        var call = elem.Foo(42);
        if (call == 42)
        {
            return 1;
        }
        if (call == 43 && elem is B)
        {
            return 2;
        }
        return -1;
    }
        
    
    public class A
    {
        public virtual int Foo(int x)
        {
            return x;
        }
    }

    public class B : A
    {
        public override int Foo(int x) => x + 1;
    }
}