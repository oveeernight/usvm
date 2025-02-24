namespace samples;

public class Calls
{
    public int Add(int x, int y) => x + y;
    public int Sub(int x, int y) => x - y;
    public int Mul(int x, int y) => x * y;
    public int Div(int x, int y) => x / y;

    [SvmTest(100)]
    public int Test1(bool flag, int x, int y)
    {
        var add = 10l + x;
        if (flag)
        {
            return Add(x, y);
        }
        return Sub(x, y);
    }
}