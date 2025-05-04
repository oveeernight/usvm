namespace samples;

public class Arrays
{
    [SvmTest(100)]
    public int ArrayStore(int[] a, int i) {
        a[i] = 5;
        return a[i];
    }

    [SvmTest(100)]
    public int ArraySimpleBranch(int[] a, int i) {
        if (a[i] != 42) {
            return 1;
        }
        return 2;
    }

    [SvmTest(100)]
    public int StringIndex(string a, int i) {
        if (a[i] == 'h') {
            return 1;
        }
        return 2;
    }

    [SvmTest(100)]
    public int ClassesArray(MyClass[] array, int i)
    {
        if (array[i].x == 42)
        {
            return 1;
        }
        return 2;
    }

    [SvmTest(96)]
    public int ConcreteArraySymbolicIndex(int i)
    {
        var a = new int [10];
        a[0] = 0;
        a[1] = 1;
        a[2] = 2;
        a[3] = 3;
        a[4] = 4;
        a[5] = 5;
        a[6] = 6;
        a[7] = 7;
        a[8] = 8;
        a[9] = 9;
        var reading = a[i];
        if (reading == 9 && i != 9)
        {
            return -1;
        }

        return 0;
    }

    public byte Rofl() {
        Int16 xx = 5;
        Console.WriteLine(xx);
        sbyte asd = -1;
        Console.WriteLine(asd);
        short t = 5;
        Console.WriteLine(t);
        ushort a = 6;
        Console.WriteLine(a);
        byte b = 7;
                Console.WriteLine(b);
        sbyte u = -5;
                Console.WriteLine(t);
        char fd = 'a';
                Console.WriteLine(fd);
        int x = 5;
                Console.WriteLine(x);
        uint c = 6;
                Console.WriteLine(c);
        long z = 6;
                Console.WriteLine(z);
        var list = new List<String>();
                        Console.WriteLine(list);
        var typ = typeof(List<Int16>);
        Console.WriteLine(typ);
        throw new NullReferenceException();
    }

    public void F() {
        throw new IndexOutOfRangeException();
    }
 }

public class Point
{
    public int x;
    public int y;
}
