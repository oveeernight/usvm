namespace samples;

public class Boxing
{
    [SvmTest(100)]
    public int BoxInt(int i)
    {
        if (i < 100)
        {
            return Foo(i);
        }

        return Foo(null);
    }
    
    private int Foo(object n)
    {
        if (n is int i) return i;
        return 0;
    }
    
    public interface IMyInterface
    {
        public int X { get; }
        public void Incr();
    }

    public struct Impl : IMyInterface
    {
        public int X { get => x; }
        public void Incr()
        {
            x++;
        }

        private int x;
    }

    public void IntfFun(IMyInterface o)
    {
        o.Incr();
    }
    
    [SvmTest(100)]
    public int BoxStruct()
    {
        var s = new Impl();
        IntfFun(s);
        if (s.X != 0)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(100)]
    public int InterfacesArray()
    {
        var a = new IMyInterface[10];
        var impl = new Impl();
        a[0] = impl;
        a[0].Incr();
        if (impl.X != 0)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(100)]
    public int UnboxInterface(IMyInterface o)
    {
        var obj = (Impl)o;
        return 0;
    }
}