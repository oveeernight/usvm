using System.Runtime.InteropServices;

namespace samples;

public class Unsafe
{
    [SvmTest(83)]
    public int ByrefVar()
    {
        var v = 5;
        Increment(ref v);
        if (v != 6)
        {
            return -1;
        }

        return 0;
    }

    private void Increment(ref int x)
    {
        x++;
    }
    
    [SvmTest(88)]
    public int ConcreteStackUnsafe()
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
    
    [SvmTest(88)]
    public unsafe int SymbolicStackUnsafe1(int a, int i)
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
    public unsafe int SymbolicStackUnsafe2(int a, int i)
    {
        var initValue = a;
        var ptr = &a;
        var casted = (byte*)ptr;
        *(short*)(casted + i) = 322;
        if (i == 2 && initValue == 5 && a != 21102597)
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
    public unsafe int ConcreteArrayWrite1()
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
    public unsafe int ConcreteArrayWrite2()
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
    
    [SvmTest(95)]
    public static unsafe bool ConcreteArrayWrite3()
    {
        var array = new int[5];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        array[4] = 5;
        fixed (int* ptr = &array[0])
        {
            var ptr2 = (long*) ptr;
            *ptr2 = 17179869187L;
        }

        if (array[0] == 3)
            return true;
        return false;
    }

    [SvmTest(94)]
    public unsafe int SymbolicArrayWrite1(int i)
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

    [SvmTest(96)]
    public static unsafe bool SymbolicArrayWrite2(int i, int j)
    {
        var array = new long[5];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        array[4] = 5;
        fixed (long* ptr = &array[0])
        {
            var ptr2 = (byte*) ptr;
            var ptr3 = ptr2 + i;
            *ptr3 = 67;
            var ptr4 = (int*)(ptr2 + j);
            *ptr4 = 23181328;
        }

        if (i == 1 && j == 2 && array[0] != 1519211528961L)
            return false;

        return true;
    }
        
    [SvmTest(96)]
    public static unsafe bool SymbolicArrayWrite3(int i)
    {
        var array = new long[5];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        array[4] = 5;
        fixed (long* ptr = &array[0])
        {
            var ptr1 = (byte*) ptr;
            var ptr3 = (int*)(ptr1 + i);
            *ptr3 = 67;
            var ptr4 = (byte*)(ptr1 + i);
            *ptr4 = 87;   
        }

        if (i == 2 && array[0] != 5701633)
            return false;

        return true;
    }
    
    [SvmTest(96)]
    public static unsafe bool SymbolicArrayWriteAffectingTwoElements(int i)
    {
        var array = new long[5];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        array[4] = 5;
        fixed (long* ptr = &array[0])
        {
            var ptr1 = (byte*) ptr;
            var ptr3 = (short*)(ptr1 + i);
            *ptr3 = 67;
        }

        if (i == 7 && array[1] == 2)
            return false;

        return true;
    }
    
    [SvmTest(97)]
    public static unsafe bool SymbolicArrayWriteAffectingThreeElements(int i, int j)
    {
        var array = new long[5];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        array[4] = 5;
        fixed (long* ptr = &array[0])
        {
            var ptr1 = (byte*) ptr;
            var ptr3 = (S*)(ptr1 + i);
            var value = new S() {x1 = 4455, x2 = 4143, x3 = 31323, x4 = 44, x5 = 3243};
            *ptr3 = value;
        }

        // array[3] is affected by writing struct
        if (i == 6 && array[3] == 4)
            return false;

        return true;
    }
    
    [SvmTest(98)]
    public static unsafe bool SymbolicArrayWriteOfSameBytes(int i)
    {
        var array = new int[5];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        array[4] = 5;
        var test = new int[5];
        test[0] = 1;
        test[1] = 2;
        test[2] = 3;
        test[3] = 4;
        test[4] = 5;
        fixed (int* ptr = &array[0])
        {
            var ptr2 = (byte*) ptr;
            var ptr3 = (long*) (ptr2 + i);
            *ptr3 = 216172782147338240L;
        }

        if ((array[0] != test[0] || array[1] != test[1] || array[2] != test[2] || array[3] != test[3] || array[4] != test[4]) && i == 1)
            return false;
        return true;
    }
    
    [SvmTest(95)]
    public static unsafe bool SymbolicArrayRead1(int i)
    {
        var array = new int[5];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        array[4] = 5;
        long result;
        fixed (int* ptr = &array[0])
        {
            var ptr2 = (long*) ptr;
            result = *(ptr2 + i);
        }

        if (result != 17179869187L && i == 1)
            return false;
        return true;
    }
    
    [SvmTest(95)]
    public static unsafe bool SymbolicArrayRead2(int i)
    {
        var array = new int[5];
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;
        array[3] = 4;
        array[4] = 5;
        long result;
        fixed (int* ptr = &array[0])
        {
            var ptr2 = (byte*) ptr;
            var ptr3 = (long*) (ptr2 + i);
            result = *ptr3;
        }

        if (result != 216172782147338240L && i == 1)
            return false;
        else
            return true;
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

    [SvmTest(95)]
    public unsafe int SymbolicWriteInStructsArray1(int i)
    {
        var array = new SomeStruct[2];
        array[0] =  new SomeStruct() { x = 5 };
        array[1] = new SomeStruct() { y = 1 };
        fixed (SomeStruct* ptr = &array[0])
        {
            var casted = (byte*)ptr;
            *(int*)(casted + i) = 500;
            if (array[0].y != 500 && i == 4)
            {
                return -1;
            }

            return 0;
        }
    }
    
    [SvmTest(95)]
    public unsafe int SymbolicWriteInStructsArray2(int i)
    {
        var array = new SomeStruct[2];
        array[0] =  new SomeStruct() { x = 5, y = 3 }; // 1.x = 5, 1.y = 3
        array[1] = new SomeStruct() { y = 1 }; // 2.x = 0, 2.y = 1
        // a[i / 8].x -> ite(i / 8 = 0, 5, 0)
        // a[i / 8].y - >ite(i / 8 = 0, 3, 0)
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
    
    [SvmTest(96)]
    public static unsafe bool SymbolicWriteInStructsArray3(int i, SequentialStruct v)
    {
        var array = new SequentialStruct[3];
        array[0] = new SequentialStruct(i, i);
        array[1] = new SequentialStruct(i, i);
        array[2] = new SequentialStruct(i, i);
        fixed (SequentialStruct* ptr = &array[0])
        {
            var ptr2 = (int*) ptr;
            var ptr3 = ptr2 + i;
            var ptr4 = (SequentialStruct*) ptr3;
            *ptr4 = v;
        }

        if (i == 1 && (array[0].y != v.x || array[1].x != v.y))
            return false;
        return true;
    }
    
    [StructLayout(LayoutKind.Explicit)]
    struct ExplicitStruct
    {
        [FieldOffset(0)]
        public int x;
        [FieldOffset(1)]
        public int y;

        public ExplicitStruct(int x, int y)
        {
            this.y = y;
            this.x = x;
        }
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct SequentialStruct
    {
        public int x;
        public int y;

        public SequentialStruct(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }
    
    [SvmTest(95)]
    public static unsafe bool SymbolicReadInStructsArray1(int i)
    {
        var array = new SequentialStruct[3];
        array[0] = new SequentialStruct(1, 2);
        array[1] = new SequentialStruct(3, 4);
        array[2] = new SequentialStruct(5, 6);
        long result;
        fixed (SequentialStruct* ptr = &array[0])
        {
            var ptr2 = (byte*) ptr;
            var ptr3 = (long*) (ptr2 + i);
            result = *ptr3;
        }

        if (i == 1 && result != 216172782147338240L)
            return false;
        return true;
    }
    
    [SvmTest(95)]
    public static unsafe bool SymbolicReadInStructsArray2(int i)
    {
        var array = new ExplicitStruct[3];
        array[0] = new ExplicitStruct(1, 2);
        array[1] = new ExplicitStruct(3, 4);
        array[2]  = new ExplicitStruct(5, 6);
        long result;
        fixed (ExplicitStruct* ptr = &array[0])
        {
            var ptr2 = (byte*) ptr;
            var ptr3 = (long*) (ptr2 + i);
            result = *ptr3;
        }

        if (i == 1 && result != 216172782113783808L)
            return false;
        else
            return true;
    }
    
    [StructLayout(LayoutKind.Explicit)]
    class ExplicitClassWithStructsInside
    {
        [FieldOffset(3)]
        public ExplicitStruct x;
        [FieldOffset(5)]
        public SequentialStruct y;

        public ExplicitClassWithStructsInside(ExplicitStruct x, SequentialStruct y)
        {
            this.x = x;
            this.y = y;
        }
    }
    
    [StructLayout(LayoutKind.Sequential)]
    class SequentialClassWithStructsInside
    {
        public SequentialStruct x;
        public SequentialStruct y;

        public SequentialClassWithStructsInside(SequentialStruct x, SequentialStruct y)
        {
            this.x = x;
            this.y = y;
        }
    }
    
    [SvmTest(94)]
    public static unsafe bool ClassSymbolicUnsafeRead1(int i)
    {
        var x = new ExplicitStruct(1, 2);
        var y = new SequentialStruct(3, 4);
        var c = new ExplicitClassWithStructsInside(x, y);
        long result;
        fixed (ExplicitStruct* ptr = &c.x)
        {
            var ptr2 = (byte*) ptr;
            var ptr3 = (long*) (ptr2 + i);
            result = *ptr3;
        }

        if (i == 1 && result != 4398046511872L)
            return false;
        else
            return true;
    }

    [SvmTest(94)]
    public static unsafe bool ClassSymbolicUnsafeRead2(int i)
    {
        var x = new SequentialStruct(1, 2);
        var y = new SequentialStruct(3, 4);
        var c = new SequentialClassWithStructsInside(x, y);
        long result;
        fixed (SequentialStruct* ptr = &c.x)
        {
            var ptr2 = (byte*) ptr;
            var ptr3 = ptr2 + i;
            var ptr4 = (long*) ptr3;
            result = *ptr4;
        }

        if (i == 1 && result != 216172782147338240L)
            return false;
        else
            return true;
    }
    
    [StructLayout(LayoutKind.Explicit)]
    public class ExplicitClass
    {
        [FieldOffset(3)]
        public int x;
        [FieldOffset(4)]
        public int y;

        public ExplicitClass(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }
    
    [SvmTest(92)]
    public static unsafe bool ClassSymbolicReadZeroBetweenFields(int i)
    {
        var c = new ExplicitClass(1, 2);
        byte result;
        fixed (int* ptr = &c.x)
        {
            var ptr2 = (byte*) ptr;
            var ptr3 = ptr2 + i;
            result = *ptr3;
        }
        if (i == -1 && result != 0)
            return false;
        return true;
    }
    
    [SvmTest(87)]
    // TODO: minimize combine term #do
    public static bool ClassWriteSafeOverlappingFields(int i, int j)
    {
        var c = new ExplicitClass(i, j);
        c.y = 42;
        c.x = -1000;
        if (c.y == 16777212)
            return true;
        return false;
    }
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

public struct S
{
    public int x1;
    public int x2;
    public int x3;
    public int x4;
    public int x5;
}

