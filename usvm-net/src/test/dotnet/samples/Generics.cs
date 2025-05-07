namespace samples;

public class Generics
{
    [SvmTest(81)]
    public int GenericClassValue()
    {
        var c = new MyClass<int>();
        c.value = 10;
        if (c.value != 10)
        {
            return -1;
        }

        return 0;
    }
}

public class MyClass<T>
{
    public T value;
}