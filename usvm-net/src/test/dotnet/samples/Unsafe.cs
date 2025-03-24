namespace samples;

public class Unsafe
{
    // [SvmTest(90)]
    // public int ConcreteUnsafe1()
    // {
    //     var a = 20;
    //     var b = 10;
    //     unsafe
    //     {
    //         *&a = *&b;
    //     }
    //
    //     if (a != 10)
    //     {
    //         return -1;
    //     }    
    //
    //     return 0;
    // }

    [SvmTest(90)]
    public unsafe int SymbolicUnsafe1(int a, int i)
    {
        var initValue = a;
        var ptr = &a;
        var casted = (byte*)ptr;
        *(int*)(casted + i) = 322;
        if (i == 2 && initValue == 5 && a != 21102597)
        {
            return -1;
        }
        return 0;
    }

    public unsafe int RefField(MyClass o)
    {
        fixed (int* r = &o.x)
        {
            *r = 322;
        }

        if (o.x == 322)
        {
            return 0;
        }
        return -1;
    }

    // public unsafe int RefArray(int i)
    // {
    //     var a = new int[] { 1, 2, 3, 4, 5 };
    //     fixed (int* p = &a[1])
    //     {
    //         
    //     }
    // }
}

public class  MyClass
{
    public int x;
    public int y;
}