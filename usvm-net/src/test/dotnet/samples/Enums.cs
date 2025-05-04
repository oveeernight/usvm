namespace samples;

public class Enums
{
    [SvmTest(100)]
    public int EnumToInt(MyEnum e)
    {
        if (e == MyEnum.One) return 1;
        if (e == MyEnum.Two) return 2;
        if (e == MyEnum.Three) return 3;
        return -1;
    }
}

public enum MyEnum
{
    One = 1,
    Two = 2,
    Three = 3
}