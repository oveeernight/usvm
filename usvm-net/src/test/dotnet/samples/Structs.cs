namespace samples;

public class Structs
{
    public struct MyStruct
    {
        public int age;
        public string name;
    }

    [SvmTest(85)]
    public int SourceStructUnaffectedAfterWriteOnCopy()
    {
        var s = new MyStruct();
        var copy = s;
        copy.age = 42;
        if (s.age != 0)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(92)]
    public int WriteStructConcrete()
    {
        var s = new MyStruct() { age = 0, name = null };
        var age = s.age;
        s.age = age + 1;
        if (s.age == age)
        {
            return -1;
        }

        return 0;
    }

    [SvmTest(83)]
    public int StructImmutabilityCheck()
    {
        var s = new MyStruct();
        MutateStruct(s);
        if (s.age != 0)
        {
            return -1;
        }
        return 0;
    }
    
    [SvmTest(84)]
    public int StructMutabilityCheck()
    {
        var s = new MyStruct();
        MutateStructByRef(ref s);
        if (s.age != 1)
        {
            return -1;
        }
        return 0;
    }

    private void MutateStruct(MyStruct s)
    {
        s.age++;
    }

    private void MutateStructByRef(ref MyStruct s)
    {
        s.age++;
    }

    [SvmTest(95)]
    public int StructsArrayConcreteWrite()
    {
        var array = new MyStruct[2];
        array[0] = new MyStruct { age = 10, name = null };
        array[1] = new MyStruct { age = 20, name = "asd" };
        array[0].age = 20;
        if (array[0].age != 20)
        {
            return -1;
        }
        return 0;
    }
    
    [SvmTest(93)]
    public int StructsArraySymbolicReading(int i)
    {
        var array = new SomeStruct[2];
        array[0] =  new SomeStruct { x = 5 };
        array[1] = new SomeStruct { y = 1 };
        var reading = array[i];
        if (reading.y == 1 && i != 1)
        {
            return -1;
        }
        // a.s.location == a, a != b
        return 0;
    }

    public class ClassWithStruct
    {
        public MyStruct s;
        public int x;
    }

    [SvmTest(100)]
    public int SymbolicStructField(MyStruct s)
    {
        if (s.age == 500)
        {
            return 1;
        }
        return 2;
    }
    
    
    [SvmTest(85)]
    public int StructsAliasing(MyStruct a, MyStruct b)
    {
        a.age = 10;
        b.age = 20;
        if (a.age == 20)
        {
            return -1;
        }
        return 0;
    }

    [SvmTest(90)]
    public int StructsAsClassFieldsAliasing(ClassWithStruct a, ClassWithStruct b)
    {
        if (a != b)
        {
            a.s.age = 10;
            b.s.age = 20;
            if (a.s.age == 20)
            {
                return -1;
            }
            return 0;
        }
        return 1;
    }
}

