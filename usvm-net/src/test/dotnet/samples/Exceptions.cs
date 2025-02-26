namespace samples;

public class Exceptions
{
    [SvmTest(100)]
    public int IndexOutOfBounds(int[] a)
    {
        try
        {
            return a[10];
        }
        catch (IndexOutOfRangeException e)
        {
            return -1;
        }
    }
}