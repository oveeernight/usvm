namespace samples;

public class Exceptions
{
    [SvmTest(100)]
    public int IndexOutOfBounds(int[] a, int i)
    {
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
        catch
        {
            var b = "";
            throw;
        }
        finally
        {
            Console.WriteLine("qwe");
            Console.WriteLine("asd");
        }
    }
    
    public IEnumerable<int> TestFault()
    {
        using (new MyDisposable())
        {
            yield return 1;
            yield return 2;
        }
    }

    class MyDisposable : IDisposable
    {
        public void Dispose()
        {
            throw new NotImplementedException();
        }
    }
}