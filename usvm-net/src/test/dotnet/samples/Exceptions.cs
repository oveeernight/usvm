namespace samples;

public class Exceptions
{

    private int globalVar;
    
    [SvmTest(92)]
    public static int SymbolicDivision(int x, int y)
    {
        int addition = 1;
        try
        {
            return x / y;
        }
        catch (OverflowException)
        {
            return addition + 100500;
        }
        catch (DivideByZeroException) when (x == 100)
        {
            return addition + 90;
        }
        finally
        {
            addition++;
        }

        return checked(x + y);
    }
    
    [SvmTest(58)]
    public static int ThrowNpe()
    {
        var res = 1;
        try
        {
            throw null;
        }
        catch (DivideByZeroException)
        {
            res += 11;
        }
        catch (ArgumentException)
        {
            res += 12;
        }
        catch (NullReferenceException)
        {
            res += 10;
        }
        finally
        {
            res++;
        }

        return res;
    }
    
    [SvmTest(100)]
    public static int CatchRuntimeException(int x, int y)
    {
        try
        {
            return x / y;
        }
        catch (DivideByZeroException)
        {
            return -42;
        }
    }
    
    [SvmTest(100)]
    public static int TryWith2Leaves(bool f)
    {
        int res = 0;
        try
        {
            if (f)
                return 100;
        }
        finally
        {
            res = 42;
        }

        res++;
        return res;
    }

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
    public int SimpleFilterScope(int[] a, int i)
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
    private static int Always42() => 42;
    private static int Always84() => Always42() * 2;


    [SvmTest(94)]
    public static int FilterInsideFinally(bool f)
    {
        int globalMemory = 0;
        try
        {
            globalMemory++;
        }
        finally
        {
            try
            {
                globalMemory += 10;
                throw new Exception();
            }
            catch (Exception) when ((globalMemory += 100) > 50 && f && Always42() == 42)
            {
                globalMemory += 1000;
            }

            globalMemory += 10000;
        }

        globalMemory += 100000;
        return globalMemory;
    }

    private bool NotImplementedFunction() => throw new MyException();

    private class MyException : Exception
    {
    }

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
            return 100;
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

    [SvmTest(87)]
    public int FinallyChain()
    {
        var x = 0;
        try
        {
            try
            {
                try
                {
                    try
                    {
                        throw new Exception();
                    }
                    finally
                    {
                        if (x == 0)
                            x++;
                    }
                }
                finally
                {
                    if (x == 1)
                        x++;
                }
            }
            finally
            {
                if (x == 2)
                    x++;
            }
        }
        catch (Exception e)
        {
            if (x != 3)
            {
                return -1;
            }
        }
        return 0;
    }

    [SvmTest(61)]
    public int FinallyInCalleeExecutedWhenCaughtInCaller()
    {
        try
        {
            ThrowingFunctionWithFinally();
        }
        catch (Exception e)
        {
            
        }

        if (globalVar != 1)
        {
            return -1;
        }
        return 0;
    }

    private void ThrowingFunctionWithFinally()
    {
        try
        {
            throw new NullReferenceException();
        }
        finally
        {
            globalVar = 1;
        }
    }

    [SvmTest(63)]
    public int FilterInCallerExecutedBeforeFinallyInCallee()
    {
        try
        {
            ThrowingFunctionWithFinally();
        }
        catch (Exception) when (TrueIfGlobalIsZero())
        {
            if (globalVar == 1)
            {
                return 1;
            }
            return -1;
        }
        return -1;
    }

    [SvmTest(45)]
    public int FilterThrowingException()
    {
        try
        {
            throw new NullReferenceException();
        }
        catch (NullReferenceException) when (BooleanFunThrowingException())
        {
            return -1;
        }
        catch (Exception)
        {
            return 0;
        }
    }

    private bool BooleanFunThrowingException()
    {
        throw new MyException();
    }
    
    private bool TrueIfGlobalIsZero() => globalVar == 0;
    
    [SvmTest(32)]
    public static int ManyNestedTryBlocks()
    {
        var res = 1;
        try
        {
            try
            {
                try
                {
                    try
                    {
                        try
                        {
                            try
                            {
                                try
                                {
                                    using (var a = new Disposable())
                                    {
                                        throw null;
                                    }
                                }
                                catch (DivideByZeroException)
                                {
                                    res += 1;
                                }
                            }
                            catch (DivideByZeroException)
                            {
                                res += 2;
                            }
                        }
                        catch (NullReferenceException) when (ThrowNullReference())
                        {
                            res += 2;
                        }
                    }
                    catch (DivideByZeroException)
                    {
                        res += 3;
                    }
                }
                catch (DivideByZeroException)
                {
                    res += 3;
                }
            }
            catch (DivideByZeroException)
            {
                res += 4;
            }
        }
        catch (NullReferenceException)
        {
            res *= 100;
        }

        if (res != 100)
        {
            return -1;
        }
        return res;
    }
    
    [SvmTest(100)]
    public static int CallInsideFinally(bool f)
    {
        int res = 0;
        try
        {
            res += Always42();
        }
        finally
        {
            if (f)
            {
                try
                {
                    res += Always42();
                }
                finally
                {
                    res += Always84();
                }
            }
        }

        return res;
    }
    
    private static bool ThrowNullReference()
    {
        throw new NullReferenceException();
    }
    
    public class Disposable : IDisposable
    {
        public void Dispose()
        {
            // TODO release managed resources here
        }
    }
    
    private bool AlwaysTrue(int x) => true;
}