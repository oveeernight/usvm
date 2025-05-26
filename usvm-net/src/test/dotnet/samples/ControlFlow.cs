namespace samples;

public class ControlFlow
{
    [SvmTest(100)]
    public static int SwitchWithSequentialCases(int x)
    {
        switch (x)
        {
            case 0:
                return 1;
            case 1:
                return 11;
            case 2:
                return 101;
            case 3:
                return 1001;
            case 4:
                return 10001;
            default:
                return -1;
        }
    }
    
    // x > 10 && x >= 95 => x + 5
    // x > 10 && x < 95 && (x + 3) != 0 (mod 5) => x + 3
    // x > 10 && x < 95 && (x + 3) == 0 (mod 5) && (x + 8) >= 100 => x + 8
    // x > 10 && x < 95 && (x + 3) == 0 (mod 5) && (x + 8) < 100 => x + 6
    // x <= 10 && (x - 2) != 0 (mod 5) => x - 2
    // x <= 10 && (x - 2) == 0 (mod 5) => x + 1  //(x + 3) < 100 && (x + 1) != 0 (mod 5)
    [SvmTest(100)]
    public static int Gotos1(int x)
    {
        if (x <= 10)
        {
            goto labelB;
        }

        labelA:
        x += 5;
        if (x >= 100)
        {
            goto exit;
        }

        labelB:
        x -= 2;
        if (x % 5 == 0)
        {
            goto labelA;
        }

        exit:
        return x;
    }
    
    [SvmTest(100)]
    public static int Gotos2(int x)
    {
        if (x <= 10)
        {
            goto labelB;
        }

        labelA:
        x += 5;
        if (x >= 100)
        {
            goto exit;
        }

        if (x > 50)
        {
            x *= 3;
            goto labelA;
        }

        labelB:
        x -= 2;
        if (x % 5 == 0)
        {
            x *= 2;
            goto labelA;
        }

        if (x % 5 == 1)
        {
            goto labelB;
        }

        exit:
        return x;
    }
    
    // [SvmTest(100)]
    // public static int GotosWithinSwitch(int x)
    // {
    //     switch (x)
    //     {
    //         case 0:
    //             x += 2;
    //             x *= 3;
    //             goto case 2;
    //
    //         case 1:
    //             x *= 10;
    //             goto case 5;
    //
    //         case 2:
    //             x %= 50;
    //             goto case 4;
    //
    //         case 4:
    //             x += 34;
    //             if (x > 50)
    //             {
    //                 goto case 5;
    //             }
    //             else
    //             {
    //                 goto case 2;
    //             }
    //
    //         case 5:
    //             x -= 15;
    //             goto default;
    //
    //         default:
    //             if (x == 28)
    //             {
    //                 break;
    //             }
    //             else
    //             {
    //                 x += 100;
    //                 goto case 0;
    //             }
    //     }
    //
    //     return x;
    // }
    
    [SvmTest(100)]
    public static int AcyclicGotos(int x)
    {
        if (x > 100)
        {
            if (x < 1000)
            {
                goto l2;
            }
            goto l3;
        }

        if (x < 42)
        {
            goto l1;
        }

        goto l2;

        l1:
        x *= 10;
        goto exit;

        l2:
        x += 100;
        goto  exit;

        l3:
        x /= 1000;

        exit:
        return x;
    }
    
    [SvmTest(100)]
    public static int SequentialIfsHard(int x)
    {
        if (2 * x == 50)
        {
            x += 100;
        }

        if (x % 7 == 5)
        {
            x *= 2;
        }

        if (x - 12 >= 0)
        {
            x++;
        }

        return x;
    }
    
    [SvmTest(100)]
    public static int SequentialIfsSimple(int x)
    {
        int res =  0;
        if (2 * x == 50)
        {
            res += 100;
        }

        if (x % 7 == 5)
        {
            res *= 2;
        }

        if (x - 12 >= 0)
        {
            res++;
        }

        return res;
    }
    
    // [SvmTest(100)]
    // public static int BinarySearch(int[] a, int x, int lo, int hi)
    // {
    //     if (a == null) throw new ArgumentException("a == null");
    //
    //     if (lo < 0) throw new ArgumentException("lo < 0");
    //     if (lo > hi) throw new ArgumentException("lo > hi");
    //
    //     var m = lo + (hi - lo) / 2;
    //
    //     while (lo < hi)
    //         if (a[m] == x)
    //             return m;
    //         else if (a[m] > x)
    //             hi = m;
    //         else
    //             lo = m + 1;
    //
    //     return -1;
    // }
    
    [SvmTest(100)]
    public static int CycleWith3EntryPoints(int x)
    {
        if (x == 1)
            goto lab1;
        if (x == 2)
            goto lab2;
        goto lab3;

        lab1:
        x += 100500;
        goto lab4;

        lab2:
        x *= 10;
        goto lab4;

        lab3:
        x++;

        lab4:
        if (x == 123)
            goto lab1;
        if (x == 234)
            goto lab2;
        if (x == 345)
            goto lab3;

        return x;
    }
    
    
}