namespace samples;

public class Unsafe
{
    [SvmTest(90)]
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

    public unsafe int SymbolicUnsafe1(int a, int i)
    {
        var ptr = &a;
        
            var casted = (byte*)ptr;
            *(int*)(casted + i) = 322;
        return 0;
    }
}