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
    
    
    [SvmTest(90)]
    public int BoxNullable1()
    {
        int? i = null;
        int? j = 5;
        var ifoo = Foo(i);
        var jfoo = Foo(j);
        if (ifoo != 0 || jfoo != 5)
        {
            return -1;
        }
        return 0;
    }

    [SvmTest(88)]
    public int UnboxNullable()
    {
        object b = 5;
        var unboxed = (int?)b;
        if (unboxed != 5)
        {
            return -1;
        }
        return 0;
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
    
    [SvmTest(84)]
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
        return obj.X;
    }
    
    public class A
    {
        private int x;

        public A()
        {
            x = 123;
        }
    }

    public struct B<T>
    {
        private T x;

        public B(T z)
        {
            x = z;
        }
    }

    private static bool IsCast<T>(object o)
    {
        return o is T;
    }

    private static T AsCast<T> (object o) where T : class
    {
        return o as T;
    }

    private static T Cast<T> (object o)
    {
        return (T) o;
    }

    // [SvmTest(100)]
    // public static B UnboxAny1()
    // {
    //     var b = new B<int>(5);
    //     return Cast<B>(b);
    // }

    [SvmTest(40)]
    public static object UnboxAny2()
    {
        var b = new B<double>(5);
        return Cast<A>(b);
    }

    [SvmTest(33)]
    public static object UnboxAny3()
    {
        var a = new A();
        return Cast<B>(a);
    }

    [SvmTest(100)]
    public static object UnboxAny4()
    {
        var a = new A();
        return Cast<A>(a);
    }

    [SvmTest(100)]
    public static uint[] UnboxAny5()
    {
        var a = new int[] {1, 2, 3};
        return Cast<uint[]>(a);
    }

    [SvmTest(100)]
    public static int[] UnboxAny6()
    {
        var a = new uint[] {1, 2, 3};
        return Cast<int[]>(a);
    }
    
    [SvmTest(100)]
    public static object TrickyBox(int x)
    {
        if (x == 5)
        {
            return x;
        }
        return 42;
    }

    [SvmTest(100)]
    public static object Box7()
    {
        int? x = 7;
        return x;
    }
    
    [SvmTest(100)]
    public static object BoxNullable(int? x)
    {
        return x;
    }

    [SvmTest(100)]
    public static bool AlwaysNull()
    {
        return BoxNullable(null) == null;
    }

    private static bool AlwaysTrueForNullable(int? x)
    {
        object obj = x;
        int? y = (int?) obj;
        return x == y;
    }

    [SvmTest(100)]
    public static bool True1()
    {
        return AlwaysTrueForNullable(null);
    }

    [SvmTest(100)]
    public static bool True2()
    {
        int? x = 55;

        return AlwaysTrueForNullable(x);
    }

    [SvmTest(100)]
    public static bool True3()
    {
        int x = 42;
        object obj = x;
        int y = (int) obj;
        return x == y;
    }

}