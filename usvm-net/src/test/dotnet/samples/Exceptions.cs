namespace samples;

public class Exceptions
{
    [SvmTest(100)]
    public int ArrayIndexReading(int[] a, int i)
    {
        var x = 0;
        try
        {
            return a[10];
        }
        catch (IndexOutOfRangeException e)
        {
            return -1;
        }
        catch (NullReferenceException e)
        {
            return -2;
        }
        finally
        {
            x = 1;
        }
    }

    [SvmTest(100)]
    public int FilterScope(int[] a, int i)
    {
        try
        {
            return a[i];
        }
        catch (IndexOutOfRangeException e) when (i < 0)
        {
            return -1;
        }
        catch
        {
            return 0;
        }
    }

    [SvmTest(90)]
    public int ExceptionInFilterScope(int[] a, int i)
    {
        try
        {
            return a[i];
        }
        catch (IndexOutOfRangeException) when (NotImplementedFunction())
        {
            return -1;
        }
        catch
        {
            return 0;
        }
    }

    private bool NotImplementedFunction() => throw new MyException();
    private class MyException: Exception { }

    [SvmTest(100)]
    public int ExceptionFromCallee(int[] a, int i)
    {
        try
        {
            return ReadIndex(a, i);
        }
        catch (IndexOutOfRangeException e)
        {
            return -1;
        }
    }
    
    private int ReadIndex(int[] a, int i) => a[i];

    [SvmTest(100)]
    public int ThrowExceptionInCatch(int[] a, int i)
    {
        try
        {
            return a[i];
        }
        catch (Exception)
        {
            var x = 0;
            throw;
        }
    }

    [SvmTest(100)]
    public int NestedBlocks(int[] a, int i)
    {
        try
        {
            var x = 0;
            try
            {
                return a[i];
            }
            catch (NullReferenceException)
            {
                return -1;
            }
        }
        catch (IndexOutOfRangeException)
        {
            return 100 ;
        }
    }

    [SvmTest(100)]
    public int SeveralBlocks(int[] a, int i, int j)
    {
        int fstReading;
        try
        {
            fstReading = a[i];
        }
        catch (Exception)
        {
            fstReading = -1;
        }
        
        int sndReading;
        try
        {
            sndReading = a[j];
        }
        catch (Exception)
        {
            sndReading = -100;
        }
        return fstReading + sndReading;
    }
    
    public bool SomeFun(int x) => true;
}