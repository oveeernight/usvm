namespace samples;

public class Unsafe
{
    [SvmTest(88)]
    public int ConcreteUnsafe1()
    {
        var a = 20;
        var b = 10;
        unsafe
        {
            *&a = *&b;
        }

        if (a != 10)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(81)]
    public unsafe int ArgumentWrite(int a)
    {
        var ptr = &a;
        *ptr = 442;
        if (a != 442)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(88)]
    public unsafe int StackUnsafe1(int a, int i)
    {
        var initValue = a;
        var ptr = &a;
        var casted = (byte*)ptr;
        *(int*)(casted + i) = 322;
        if (i == 0 && a != 322)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(91)]
    public unsafe int StackUnsafe2(int a, int i)
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


    [SvmTest(90)]
    public unsafe int DetachedPtr()
    {
        var array = new int[4];
        fixed (int* ptr = &array[0])
        {
            *ptr = 10;
        }

        if (array[0] != 10)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(94)]
    public unsafe int ConcreteArrayUnsafe1()
    {
        var array = new int[4];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        fixed (int* ptr = &array[0])
        {
            *ptr = 10;
            if (array[0] != 10)
            {
                return -1;
            }

            return 0;
        }
    }

    [SvmTest(93)]
    public unsafe int ConcreteArrayUnsafe2()
    {
        var array = new int[4];
        fixed (int* ptr = &array[0])
        {
            var casted = (byte*)ptr;
            var shifted = (int*)(casted + 3);
            *shifted = 322;
            if (array[0] != 1107296256 || array[1] != 1)
            {
                return -1;
            }

            return 0;
        }
    }

    [SvmTest(94)]
    public unsafe int SymbolicArrayUnsafe1(int i)
    {
        var array = new int[4];
        fixed (int* ptr = &array[0])
        {
            var casted = (byte*)ptr;
            var shifted = (int*)(casted + i);
            *shifted = 322;
            if (array[0] == 1107296256 && array[1] == 1 && i != 3)
            {
                return -1;
            }

            return 0;
        }
    }


    [SvmTest(86)]
    public unsafe int RefField(MyClass o)
    {
        fixed (int* r = &o.x)
        {
            *r = 322;
            if (o.x == 322)
            {
                return 0;
            }

            return -1;
        }
    }

    [SvmTest(90)]
    public unsafe int ConcreteStructWrite()
    {
        var s = new SomeStruct();
        var ptr = &s;
        var casted = (byte*)ptr;
        *(int*)(casted + 3) = 322;
        if (s.x != 1107296256 || s.y != 1)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(90)]
    public unsafe int SymbolicStructWrite(int i)
    {
        var array = new SomeStruct[2];
        array[0] =  new SomeStruct() { x = 5 };
        array[1] = new SomeStruct() { y = 1 };
        fixed (SomeStruct* ptr = &array[0])
        {
            var casted = (byte*)ptr;
            *(int*)(casted + i) = 500;
            if (array[0].y == 500 && i != 4)
            {
                return -1;
            }

            return 0;
        }
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

public struct SomeStruct
{
    public int x;
    public int y;
}
