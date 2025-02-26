namespace samples;

public class Arithmetic
{
    [SvmTest(100)]
    public int Add(int a, int b)
    {
        return a + b;
    }

    [SvmTest(100)]
    public int Subtract(int a, int b)
    {
        return a - b;
    }

    [SvmTest(100)]
    public int Multiply(int a, int b)
    {
        return a * b;
    }

    [SvmTest(100)]
    public int Divide(int a, int b)
    {
        if (b == 0)
        {
            return 0;
        }
        return a / b;
    }

    [SvmTest(100)]
    public int Modulo(int a, int b)
    {
        if (b != 0)
        {
            return a % b;
        }

        return 0;
    }

    [SvmTest(100)]
    public bool Gt(int a, int b)
    {
        return a > b;
    }

    [SvmTest(100)]
    public bool Ge(int a, int b, bool flag)
    {
        if (flag) return false;
        return a >= b;
    }

    [SvmTest(100)]
    public bool Lt(int a, int b)
    {
        return a < b;
    }

    [SvmTest(100)]
    public bool Le(int a, int b)
    {
        return a <= b;
    }

    [SvmTest(100)]
    public int Shl(int a, int b)
    {
        // b >= 0 <=> !(b < 0))
        if (a != 0 || b >= 0)
        {
            return a << b;
        }
        return 0;
    }
    
    [SvmTest(100)]
    public int Shr(int a, int b)
    {
        if (a != 0 && b > 0)
        {
            return a >> b;
        }
        return 0;
    }
}