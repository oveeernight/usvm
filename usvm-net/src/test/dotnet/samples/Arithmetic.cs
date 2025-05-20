namespace samples;

public class Arithmetic
{
    [SvmTest(100)]
    public int AddInts(int a, int b)
    {
        return a + b;
    }

    [SvmTest(100)]
    public int SubInts(int a, int b)
    {
        return a - b;
    }

    [SvmTest(100)]
    public int MultiplyInts(int a, int b)
    {
        return a * b;
    }

    [SvmTest(100)]
    public int DivideInts(int a, int b)
    {
        if (b == 0)
        {
            return 0;
        }

        return a / b;
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
        if (a != 0 && b >= 0)
        {
            return a << b;
        }

        return 0;
    }

    [SvmTest(100)]
    public static bool MultiplicationOfFloatsIsNotAssociative()
    {
        float a = 0.825402526103613f;
        float b = 0.909231618470155f;
        float c = 0.654626872695343f;
        float d = (a * b) * c;
        float e = a * (b * c);
        return d != e;
    }

    [SvmTest(100)]
    public static bool MultiplicationOfDoublesIsNotAssociative()
    {
        double a = 0.825402526103613;
        double b = 0.909231618470155;
        double c = 0.654626872695343;
        double d = (a * b) * c;
        double e = a * (b * c);
        return d != e;
    }

    [SvmTest(100)]
    public static uint DivideWithoutOverflow(uint a)
    {
        int x = -1;
        uint y = (uint)x;
        return a / y;
    }

    [SvmTest(100)]
    public static float DivideFloatOnZero(float a)
    {
        return a / 0;
    }

    [SvmTest(100)]
    public static double DivideDoubleOnZero(double a)
    {
        return a / 0;
    }

    [SvmTest(0)]
    public static float DivideOnZero1()
    {
        int x = 8;
        int y = 0;
        return x / y;
    }

    [SvmTest(0)]
    public static float DivideOnZero2()
    {
        uint x = 8;
        uint y = 0;
        return x / y;
    }

    [SvmTest(100)]
    public static float AddFloats(float a, float b)
    {
        return a + b;
    }

    [SvmTest(100)]
    public static double AddDoubles(double a, double b)
    {
        return a + b;
    }

    // overflow exceptions possible
    [SvmTest(100)]
    public static int AddChecked(int a, int b)
    {
        return checked(a + b);
    }

    // no exceptions
    [SvmTest(100)]
    public static uint AddUnsigned(uint a, uint b)
    {
        return a + b;
    }

    [SvmTest(100)]
    public static uint AddOvfUn(uint a, uint b)
    {
        return checked(a + b);
    }

    [SvmTest(100)]
    public static float MulFloats(float a, float b)
    {
        return a * b;
    }

    // no exceptions
    [SvmTest(100)]
    public static double MulDoubles(double a, double b)
    {
        return a * b;
    }

    // overflow exceptions possible
    [SvmTest(100)]
    public static uint MulOvfUn(uint a, uint b)
    {
        return checked(a * b);
    }

    // overflow exceptions possible
    [SvmTest(100)]
    public static int MulOvf(int a, int b)
    {
        return checked(a * b);
    }

    [SvmTest(100)]
    public static Int64 MulOvf64(Int64 a, Int64 b)
    {
        return checked(a * b);
    }

    [SvmTest(100)]
    public static UInt64 MulOvfU64(UInt64 a, UInt64 b)
    {
        return checked(a * b);
    }

    // no exceptions
    [SvmTest(100)]
    public static float SubFloats(float a, float b)
    {
        return a - b;
    }

    // no exceptions
    [SvmTest(100)]
    public static double SubDoubles(double a, double b)
    {
        return a - b;
    }

    [SvmTest(100)]
    public static int SubOvf(int a, int b)
    {
        return checked(a - b);
    }

    [SvmTest(100)]
    public static uint SubOvfUn(uint a, uint b)
    {
        return checked(a - b);
    }

    [SvmTest(100)]
    public static int AddSbyteShort(sbyte a, short b)
    {
        return checked(a + b);
    }

    [SvmTest(100)]
    public static float RemFloats(float a, float b)
    {
        return a % b;
    }

    [SvmTest(100)]
    public static double RemConcreteFloats()
    {
        return RemFloats(10.0f, 6.0f);
    }

    [SvmTest(100)]
    public static double RemDoubles(double a, double b)
    {
        return a % b;
    }

    [SvmTest(100)]
    public static double RemConcreteDoubles()
    {
        return RemDoubles(10.0, 6.0);
    }

    [SvmTest(100)]
    public static int RemInts(int a, int b)
    {
        return a % b;
    }

    [SvmTest(0)]
    public static int RemIntsDivideOnZero(int a)
    {
        return RemInts(a, 0);
    }

    [SvmTest(100)]
    public static uint RemUnInts(uint a, uint b)
    {
        return a % b;
    }

    [SvmTest(0)]
    public static uint RemUnIntsDivideOnZero(uint a)
    {
        return RemUnInts(a, 0);
    }

    // 7 + n
    [SvmTest(100)]
    public static int ArithmeticsMethod1(int n, int m)
    {
        return -((n - m) + (m - n) + (1 + m + 2 + 0 - m + 4 + m) - (m + n)) + 14 +
               (n * (5 - 4) + (5 - 7 + m / m) * n) / m;
    }

    // 0
    [SvmTest(100)]
    public static int ArithmeticsMethod2(int a, int b)
    {
        return a + b - a - b;
    }

    // c - 11
    [SvmTest(100)]
    public static int ArithmeticsMethod3(int a, int b, int c)
    {
        return (a + b + c) - 1 * (a + b + 8) - 3;
    }

    // 6*n - 126826
    [SvmTest(100)]
    public static int ArithmeticsMethod4(int n, int m)
    {
        return (n + n + n + n + n + n - 2312) + m * m * m / (2 * n - n + 3 * n - 4 * n + m * m * m) - 124515;
    }

    // private unsafe int Asd()
    // {
    //     var a = new int[5];
    //     SomeFunction(ref a[3]);
    // }
    private unsafe int SomeFunction(ref int a)
    {
        fixed (int* p = &a)
        {
            
        }
        return 0;
    }

    // Expecting true
    [SvmTest(100)]
    public static bool IncrementsWorkCorrect(int x)
    {
        int xorig = x;
        x = x++;
        int x1 = x;
        x++;
        int x2 = x;
        x = ++x;
        int x3 = x;
        ++x;
        int x4 = x;
        return x1 == xorig & x2 == xorig + 1 & x3 == xorig + 2 & x4 == xorig + 3;
    }

    [SvmTest(100)]
    public static int BigSum(int x)
    {
        return x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x +
               x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x +
               x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x +
               x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x +
               x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x;
    }

    [SvmTest(100)]
    public static int SmallBigSum(int x)
    {
        return x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x;
    }

    [SvmTest(100)]
    public static int BigSumCycle(int x)
    {
        int res = 0;
        for (int i = 0; i < 9; i++)
        {
            res += x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x;
        }

        return res;
    }

    // Expecting true
    [SvmTest(100)]
    public static bool Decreasing(int x)
    {
        int x1 = x + 1;
        int x2 = x + 2;
        return x2 - x1 == 1;
    }

    [SvmTest(100)]
    public static int CheckedUnchecked(int x0, int x1, int x2, int x3, int x4, int x5, int x6, int x7, int x8, int x9)
    {
        return checked(x0 + unchecked(x1 + checked(x2 + x3 + x4)) + unchecked(x5 - x6 * x7));
    }

    private static int CheckOverflow0(int x0, int x1)
    {
        return checked(2147483620 + x0 + 2147483620) + x1;
    }

    // Expecting overflow error
    [SvmTest(0)]
    public static int CheckOverflow1(int x1)
    {
        return CheckOverflow0(2147483620, 2147483620 + x1);
    }

    // Expecting overflow error
    [SvmTest(0)]
    public static int CheckOverflow2(int x1)
    {
        int x = 1000 * 1000 * 1000;
        int y = x;
        return checked((x + y) * 2);
    }

    [SvmTest(100)]
    public static long SumOfIntAndUint(int a, uint b)
    {
        return b + a;
    }

    [SvmTest(100)]
    public static long SumOfIntAndShort(int a, short b)
    {
        return b + a;
    }

    [SvmTest(0)]
    public static int CheckDivideByZeroException0(int x1)
    {
        int x = 255;
        int y = 0;
        return (x / y + x1);
    }

    // Expecting 2000000000 + x1 + 2000000000
    [SvmTest(100)]
    public static int CheckOrder(int x1)
    {
        int x = 2000000000;
        int y = x;
        return checked(x + x1 + y);
    }

    // Expecting a
    [SvmTest(100)]
    public static int ShiftLeftOnZero(int a)
    {
        return (a << 0) >> 0;
    }

    // Expecting 0.0
    [SvmTest(100)]
    public static double ZeroShift(int a)
    {
        return 0 << a >> a;
    }

    // Expecting a << b
    [SvmTest(100)]
    public static int DefaultShift(int a, int b)
    {
        return a << b;
    }

    // Expecting a << 32
    [SvmTest(100)]
    public static Int64 SumShifts(Int64 a)
    {
        return (a << 31) + (a << 31);
    }

    // Expecting 0
    [SvmTest(100)]
    public static Int32 ShiftSum(Int32 a)
    {
        return (a + a) << 31;
    }

    // Expecting a << 19
    [SvmTest(100)]
    public static Int32 MultiplyOnShift1(Int16 a)
    {
        return (a << 17) * 4;
    }

    // Expecting a << 16
    [SvmTest(100)]
    public static Int32 MultiplyOnShift2(Int16 a)
    {
        return (a << 14) * 4;
    }

    // Expecting 0
    [SvmTest(100)]
    public static Int32 ShiftMultiplication(Int16 a)
    {
        return (a * 512) << 23;
    }

    // Expecting (a >> 20) / 1024
    [SvmTest(100)]
    public static int ShiftDivision1(byte a)
    {
        return (a >> 20) / 1024;
    }

    // Expecting (a / 512) >> 12
    [SvmTest(100)]
    public static int ShiftDivision2(int a)
    {
        return (a / 512) >> 12;
    }

    // Expecting 0
    [SvmTest(100)]
    public static uint ShiftDivision3(uint a)
    {
        return (a >> 22) / 1024;
    }

    // Expecting a >> 41
    [SvmTest(100)]
    public static ulong ShiftDivision4(ulong a)
    {
        return (a >> 31) / 1024;
    }

    [SvmTest(100)]
    public static uint ShrUn(int a)
    {
        uint b = (uint)a;
        return b >> 1;
    }

    [SvmTest(100)]
    public static uint Shr(int a)
    {
        return (uint)(a >> 1);
    }

    // expecting 4294967295
    [SvmTest(100)]
    public static uint ShrTest()
    {
        int a = -1;
        return Shr(a);
    }

    // Expecting 0
    [SvmTest(100)]
    public static int ShiftSumOfShifts1(int a)
    {
        return ((a << 30) + (a << 30)) << 2;
    }

    // Expecting a << 34
    [SvmTest(100)]
    public static long ShiftSumOfShifts2(long a)
    {
        return ((a << 31) + (a << 31)) << 2;
    }

    // Expecting -2147483648
    [SvmTest(100)]
    public static int ConcreteShift()
    {
        return 2 << 30;
    }

    // Expecting 0
    [SvmTest(100)]
    public static int MultiplyShifts1(int a, int c)
    {
        return ((a + a) << 14) * (c << 17);
    }

    // Expecting (a * c) << 29
    [SvmTest(100)]
    public static int MultiplyShifts2(int a, int c)
    {
        return ((a + a) << 11) * (c << 17);
    }

    // Expecting (a << 6) / 4
    [SvmTest(100)]
    public static int ShiftWithDivAndMul(int a)
    {
        return ((a * 16) << 2) / 4;
    }

    // Expecting (int64)(a >> 15 >> 18)
    [SvmTest(100)]
    public static Int64 DoubleShiftRight(int a)
    {
        return (a >> 15) >> 18;
    }

    [SvmTest(100)]
    public static double EncodeDoubleTest(double x)
    {
        if (x > 0) return x;
        return -x;
    }

    [SvmTest(100)]
    public static double EncodeDoubleTest1(double x)
    {
        if (198.1234 == x) return 1;
        return 0;
    }

    [SvmTest(100)]
    public static double EncodeFloatTest(float x)
    {
        if (x > 0) return x;
        return -x;
    }

    [SvmTest(75)]
    public static double EncodeFloatTest1(float x)
    {
        if (198.1234 == x) return 1;
        return 0;
    }

    [SvmTest(100)]
    public static double CompareDoubleAndFloatTest1(double x, float y)
    {
        if (x > y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareDoubleAndFloatTest2(double x, float y)
    {
        if (x >= y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareDoubleAndFloatTest3(double x, float y)
    {
        if (x < y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareDoubleAndFloatTest4(double x, float y)
    {
        if (x <= y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareDoublesTest1(double x, double y)
    {
        if (x > y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareDoublesTest2(double x, double y)
    {
        if (x >= y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareDoublesTest3(double x, double y)
    {
        if (x < y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareDoublesTest4(double x, double y)
    {
        if (x <= y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareFloatsTest1(float x, float y)
    {
        if (x > y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareFloatsTest2(float x, float y)
    {
        if (x >= y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareFloatsTest3(float x, float y)
    {
        if (x < y) return x;
        return y;
    }

    [SvmTest(100)]
    public static double CompareFloatsTest4(float x, float y)
    {
        if (x <= y) return x;
        return y;
    }

    [SvmTest(85)]
    public static int CastRealToIntegral(float x)
    {
        int i = (int)x;
        if (x == 123.0 && i != 123)
            return -1;
        return i;
    }

    [SvmTest(85)]
    public static byte CastRealToIntegral1(float x)
    {
        byte i = (byte)x;
        if (x == 123.0 && i != 123)
            throw new ArgumentException();
        return i;
    }

    [SvmTest(81)]
    public static long CastRealToIntegral2(float x)
    {
        long i = (long)x;
        if (x == 123.0 && i != 123)
            return -1;
        return i;
    }

    [SvmTest(84)]
    public static int CastRealToIntegral3(double x)
    {
        int i = (int)x;
        if (x == 123.0 && i != 123)
            return -1;
        return i;
    }

    [SvmTest(84)]
    public static byte CastRealToIntegral4(double x)
    {
        byte i = (byte)x;
        if (x == 123.0 && i != 123)
            throw new ArgumentException();
        return i;
    }

    [SvmTest(80)]
    public static long CastRealToIntegral5(double x)
    {
        long i = (long)x;
        if (x == 123.0 && i != 123)
            return -1;
        return i;
    }

    [SvmTest(85)]
    public static int CastIntegralToReal(int i)
    {
        float x = i;
        if (i == 123 && x != 123.0)
            return -1;
        return i;
    }

    [SvmTest(85)]
    public static byte CastIntegralToReal1(byte i)
    {
        float x = i;
        if (i == 123 && x != 123.0)
            throw new ArgumentException();
        return i;
    }

    [SvmTest(81)]
    public static long CastIntegralToReal2(long i)
    {
        float x = i;
        if (i == 123 && x != 123.0)
            return -1;
        return i;
    }

    [SvmTest(84)]
    public static int CastIntegralToReal3(int i)
    {
        double x = i;
        if (i == 123 && x != 123.0)
            return -1;
        return i;
    }

    [SvmTest(84)]
    public static byte CastIntegralToReal4(byte i)
    {
        double x = i;
        if (i == 123 && x != 123.0)
            throw new ArgumentException();
        return i;
    }

    [SvmTest(80)]
    public static long CastIntegralToReal5(long i)
    {
        double x = i;
        if (i == 123 && x != 123.0)
            return -1;
        return i;
    }
}