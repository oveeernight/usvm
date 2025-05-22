namespace samples;

public class Arrays
{
    [SvmTest(100)]
    public int ArrayStore(int[] a, int i) {
        a[i] = 5;
        return a[i];
    }

    [SvmTest(100)]
    public int ArraySimpleBranch(int[] a, int i) {
        if (a[i] != 42) {
            return 1;
        }
        return 2;
    }

    [SvmTest(100)]
    public int StringIndex(string a, int i) {
        if (a[i] == 'h') {
            return 1;
        }
        return 2;
    }

    [SvmTest(100)]
    public int ClassesArray(MyClass[] array, int i)
    {
        if (array[i].x == 42)
        {
            return 1;
        }
        return 2;
    }

    [SvmTest(96)]
    public int ConcreteArraySymbolicIndex(int i)
    {
        var a = new int [10];
        a[0] = 0;
        a[1] = 1;
        a[2] = 2;
        a[3] = 3;
        a[4] = 4;
        a[5] = 5;
        a[6] = 6;
        a[7] = 7;
        a[8] = 8;
        a[9] = 9;
        var reading = a[i];
        if (reading == 9 && i != 9)
        {
            return -1;
        }

        return 0;
    }
    
    [SvmTest(100)]
    public static int CopyConcreteToConcreteArray()
    {
        int[] arr = new int[5]; // { 10, 2, 3, 4, 5 };
        arr[0] = 10;
        arr[1] = 2;
        arr[2] = 3;
        arr[3] = 4;
        arr[4] = 5;
        int[] a = new int[5];
        a[0] = 1;
        a[1] = 1;
        a[2] = 1;
        a[3] = 1;
        a[4] = 1;
        Array.Copy(arr, 1, a, 1, 3);
        return a[2];
    }

    [SvmTest(100)]
    public static int[] CopyConcreteToSymbolicArray(int[] a)
    {
        int[] arr = new int[5];
        arr[0] = 1;
        arr[1] = 2;
        arr[2] = 3;
        arr[3] = 4;
        arr[4] = 5;
        Array.Copy(arr, 2, a, 2, 2);
        return a;
    }

    [SvmTest(100)]
    public static int[] CopyAndThenWrite(int[] a)
    {
        int[] arr = new int[5];
        arr[0] = 1;
        arr[1] = 2;
        arr[2] = 3;
        arr[3] = 4;
        arr[4] = 5;
        Array.Copy(arr, 2, a, 2, 2);
        a[2] = 42;
        return a;
    }
    
    [SvmTest(100)]
    public static int CopyAndBranch(int[] a, int i)
    {
        int[] arr = new int[5];
        arr[0] = 1;
        arr[1] = 2;
        arr[2] = 3;
        arr[3] = 4;
        arr[4] = 5;
        Array.Copy(arr, 2, a, 2, 2);
        a[2] = 42;
        if (a[i] == 2)
            return 1;
        return 2;
    }
    
    // [SvmTest(95)]
    // public static int DoubleWriteAfterCopy(string[] a, int i, string[] b)
    // {
    //     if (a.Length == b.Length)
    //     {
    //         a[i] = "1";
    //         Array.Copy(b, a, a.Length - 1);
    //         a[i] = "3";
    //         a[i] = "3";
    //         if (a[0] != b[0] && i > 0)
    //             return -1;
    //         return 1;
    //     }
    //
    //     return 0;
    // }

    // [SvmTest(95)]
    // public static int DoubleWriteAfterCopy1(string[] a, int k, int i, int j, string[] b)
    // {
    //     if (a.Length == b.Length)
    //     {
    //         a[k] = "1";
    //         Array.Copy(b, a, a.Length - 1);
    //         a[i] = "3";
    //         a[j] = "3";
    //         if (a[0] != b[0] && i > 0 && j > 0)
    //             return -1;
    //         return 1;
    //     }
    //
    //     return 0;
    // }
    
    [SvmTest(100)]
    public static int[] CopySymbolicIndicesToConcreteArray(int srcI, int dstI, int len)
    {
        int[] arr = new int[5]; // { 10, 2, 3, 4, 5 };
        arr[0] = 10;
        arr[1] = 2;
        arr[2] = 3;
        arr[3] = 4;
        arr[4] = 5;
        int[] a = new int[5];
        a[0] = 1;
        a[1] = 1;
        a[2] = 1;
        a[3] = 1;
        a[4] = 1;
        Array.Copy(arr, srcI, a, dstI, len);
        if (a[2] == 3)
            return Array.Empty<int>();
        return a;
    }

    [SvmTest(100)]
    public static int[] CopySymbolicIndicesToConcreteArray1(int srcI1, int dstI1, int len1, int srcI2, int dstI2,
        int len2)
    {
        int[] arr = new int[5]; // { 10, 2, 3, 4, 5 };
        arr[0] = 10;
        arr[1] = 2;
        arr[2] = 3;
        arr[3] = 4;
        arr[4] = 5;
        int[] a = new int[5];
        a[0] = 1;
        a[1] = 1;
        a[2] = 1;
        a[3] = 1;
        a[4] = 1;
        Array.Copy(arr, srcI1, a, dstI1, len1);
        if (a[2] == 3)
            return a;
        int[] b = new int[5];
        b[0] = 1;
        b[1] = 1;
        b[2] = 1;
        b[3] = 1;
        b[4] = 1;
        Array.Copy(a, srcI2, b, dstI2, len2);
        if (b[2] == 3)
            return Array.Empty<int>();
        return a;
    }

    [SvmTest(100)]
    public static int[] CopySymbolicIndicesToConcreteArray2(int srcI, int dstI, int len)
    {
        int[] arr = new int[5]; // { 10, 2, 3, 4, 5 };
        arr[0] = 10;
        arr[1] = 2;
        arr[2] = 3;
        arr[3] = 4;
        arr[4] = 5;
        int[] a = new int[5];
        a[0] = 1;
        a[1] = 1;
        a[2] = 1;
        a[3] = 1;
        a[4] = 1;
        arr[dstI + len] = len;
        Array.Copy(arr, srcI, a, dstI, len);
        int[] b = new int[5];
        b[0] = 3;
        b[1] = 3;
        b[2] = 3;
        b[3] = 3;
        b[4] = 3;
        Array.Copy(a, dstI, b, dstI, len + 1);
        if (b[dstI + len] == len)
            // Should be unreachable
            return Array.Empty<int>();
        return a;
    }
    
        // [SvmTest(94)]
        // public static int TestSolvingCopy(int[] a, int[] b, int i)
        // {
        //     if (a.Length > b.Length && 0 <= i && i < b.Length)
        //     {
        //         Array.Fill(a, 1);
        //         Array.Copy(a, b, b.Length);
        //
        //         if (b[i] == b[i + 1])
        //             return 42;
        //         return 10;
        //     }
        //
        //     return 3;
        // }

        [SvmTest(100)]
        public static int TestSolvingCopy1(int[] a, int i, int[] b)
        {
            if (a != null && b != null && a.Length > b.Length)
            {
                a[0] = 42;
                b[i] = 4;
                Array.Copy(a, 0, b, 0, b.Length);
                b[0] = 31;
                var x = b.Length == 3;
                var y = a[0] == 42;
                var z = b[0] == 31;
                var k = a[1] == b[1];
                var j = a[2] == b[2];

                if (x && y && z && k && j)
                    return 42;

                a[0] = 12;
                a[3] = 31;
                Array.Copy(a, 0, b, 0, b.Length);

                if (b.Length == 4 && a[0] == b[0] && a[1] == b[1] && a[2] == b[2] && a[3] == b[3])
                    return 12;

                return 10;
            }

            return 3;
        }

        [SvmTest(100)]
        public static int TestSolvingCopy2(int[] a, int[] b, int[] c)
        {
            if (a != null && b != null && c != null && a.Length > b.Length && b.Length > c.Length)
            {
                a[0] = 42;
                Array.Copy(a, 0, b, 0, b.Length);
                b[0] = 31;
                var x = b.Length == 3;
                var y = a[0] == 42;
                var z = b[0] == 31;
                var k = a[1] == b[1];
                var j = a[2] == b[2];

                if (x && y && z && k && j)
                    return 42;

                b[0] = 12;
                b[3] = 31;
                Array.Copy(b, 0, c, 0, c.Length);

                if (c.Length == 4 && c[0] == b[0] && c[1] == b[1] && c[2] == b[2] && c[3] == b[3])
                    return 12;

                return 10;
            }

            return 3;
        }

        [SvmTest(97)]
        public static int TestSolvingCopy3(int[] a, int[] b, int[] c)
        {
            if (a != null && b != null && c != null && a.Length > b.Length && b.Length > c.Length && c.Length > 3)
            {
                a[0] = 42;
                Array.Copy(a, 0, b, 0, 3);
                Array.Copy(b, 0, c, 0, 4);

                // Should be always true
                if (c[0] == b[0] && c[1] == b[1] && c[2] == b[2] && c[3] == b[3])
                    return 12;

                // Unreachable
                return 10;
            }

            return 3;
        }

        [SvmTest(100)]
        public static int TestSolvingCopy4(int[] a, int[] b)
        {
            if (a != null && b != null && a.Length > b.Length && b.Length > 3)
            {
                a[0] = 42;
                Array.Copy(a, 0, b, 0, 3);

                if (b[1] > 0)
                    return b[0];

                return b[1];
            }

            return 3;
        }

        // [SvmTest(100)]
        // public static int TestSolvingCopy5(int[] a, int[] b, int i)
        // {
        //     if (a.Length > b.Length && 0 <= i && i < b.Length)
        //     {
        //         a[i] = 1;
        //         a[0] = 2;
        //         Array.Copy(a, b, b.Length);
        //
        //         if (b[i] == b[i + 1])
        //             return 42;
        //         return 10;
        //     }
        //
        //     return 3;
        // }
        //
        // [SvmTest(100)]
        // public static int TestSolvingCopy6(int[] a, int[] b, int i)
        // {
        //     if (a.Length > b.Length && 0 <= i && i < b.Length)
        //     {
        //         a[i] = 1;
        //         a[0] = 2;
        //         Array.Copy(a, b, b.Length);
        //         b[i] = 4;
        //         b[0] = 3;
        //
        //         if (b[i] == b[i + 1])
        //             return 42;
        //         return 10;
        //     }
        //
        //     return 3;
        // }

        [SvmTest(98)]
        public static int TestSolvingCopy7(int[] a, int i, int[] b)
        {
            if (a != null && b != null && a.Length > b.Length)
            {
                a[0] = 42;
                b[i] = 4;
                Array.Copy(a, 0, b, 0, b.Length - 1);
                b[0] = 31;

                a[0] = 12;
                a[3] = 31;
                Array.Copy(a, 0, b, 0, b.Length - 1);

                if (i == b.Length - 1 && b[i] != 4 && i > 0)
                    return -1;

                if (b.Length == 4 && a[0] == b[0] && a[1] == b[1] && a[2] == b[2] && a[3] == b[3])
                    return 12;

                return 10;
            }

            return 3;
        }
        
        [SvmTest(94)]
        public static int TestSolvingCopy8(object[] a, object[] b, int i)
        {
            if (a.Length > b.Length && 0 <= i && i < b.Length)
            {
                Array.Fill(a, "abc");
                Array.Copy(a, b, b.Length);

                if (b[i] == b[i + 1])
                    return 42;
                return 10;
            }

            return 3;
        }
        
        [SvmTest(93)]
        public static int TestSolvingCopy9(object[] a, int i, object[] b)
        {
            if (a != null && b != null && a.Length > b.Length)
            {
                var x = (object)4;
                var y = (object)31;
                a[0] = 42;
                b[i] = x;
                Array.Copy(a, 0, b, 0, b.Length - 1);
                b[0] = y;

                a[0] = 12;
                a[3] = y;
                Array.Copy(a, 0, b, 0, b.Length - 1);

                if (i == b.Length - 1 && b[i] != x && i > 0)
                    return -1;

                return 10;
            }

            return 3;
        }

        
        // [SvmTest(100)]
        // public static int TestSolvingCopy10(string[] a, int i, string[] b)
        // {
        //     if (a.Length > b.Length && 0 <= i && i < b.Length)
        //     {
        //         Array.Copy(a, b, b.Length);
        //
        //         if (b[i][0] == b[i + 1][0])
        //             return 42;
        //         return 10;
        //     }
        //
        //     return 3;
        // }

        // [SvmTest(66)]
        // public static int TestSolvingCopy11(string[] a, int i, string[] b)
        // {
        //     if (a.Length > b.Length && 0 <= i && i < b.Length)
        //     {
        //         Array.Copy(a, b, b.Length);
        //         if (b[i].Length == 0)
        //         {
        //             // unreachable
        //             if (b[i][0] == b[i + 1][0])
        //                 return 42;
        //         }
        //
        //         return 10;
        //     }
        //
        //     return 3;
        // }

        [SvmTest(93)]
        public static int TestOverlappingCopy(int[] a)
        {
            if (a != null && a.Length > 5)
            {
                a[0] = 42;
                a[2] = 41;
                Array.Copy(a, 0, a, 2, 3);

                // Should be always false
                if (a[2] != 42)
                    // Unreachable
                    return 42;

                return 10;
            }

            return 3;
        }

        [SvmTest(100)]
        public static int TestOverlappingCopy1(int[] a, int i)
        {
            if (a != null && a.Length > 5)
            {
                a[0] = 42;
                Array.Copy(a, 0, a, 2, 3);

                if (a[i] != 42)
                    return 42;

                a[i] = 41;
                Array.Copy(a, 0, a, i, 3);

                if (a[i] != 42)
                    return 42;

                return 10;
            }

            return 3;
        }

        [SvmTest(94)]
        public static int TestSolvingCopyOverwrittenValueUnreachable1(string[] a, string[] b)
        {
            if (a != null && b != null && a.Length > b.Length)
            {
                a[0] = "42";
                b[0] = "4";
                Array.Copy(a, 0, b, 0, b.Length);
                if (b[0] != "42") // unreachable
                {
                    return -1;
                }

                return 0;
            }

            return 3;
        }

        [SvmTest(95)]
        public static int TestSolvingCopyOverwrittenValueUnreachable2(string[] a, int i, string[] b)
        {
            if (a != null && b != null && a.Length > b.Length)
            {
                b[i] = "500";
                Array.Copy(a, 0, b, 0, b.Length);
                if (b.Length > 0 && a[i] != "500" && b[i] == "500") // unreachable
                {
                    return -1;
                }

                return 0;
            }

            return 3;
        }

        [SvmTest(100)]
        public static int ArrayAliasWrite(object[] o, string[] s, string str1, string str2)
        {
            if (o[42] == str1)
            {
                if (str1 != String.Empty)
                {
                    s[42] = str2;
                    if (o[42] == str2)
                        return 1;
                    if (o[42] != str1)
                        throw new ArgumentException("unreachable");
                }
            }

            return 0;
        }
        
        [SvmTest(100)]
        public static int SymbolicWriteAfterConcreteWrite(int k)
        {
            int[] arr = new int[5];
            arr[2] = 42;
            arr[k] = 12;
            return arr[2];
        }

        [SvmTest(100)]
        public static int SymbolicWriteAfterConcreteWrite2(int[] a, int k)
        {
            a[2] = 42;
            a[k] = 12;
            return a[2];
        }

        [SvmTest(100)]
        public static int SolverTestArrayKey(int[] a, int x)
        {
            a[1] = 12;
            a[x] = 12;
            if (x != 10)
            {
                a[10] = 42;
            }

            var res = 0;
            if (a[x] == 12)
            {
                res = 1;
            }

            return res;
        }
        
        [SvmTest(100)]
        public static int[] RetOneDArray2(int n)
        {
            int[] arr = new int[n];
            if (n == 5)
            {
                arr[4] = 99;
                arr[1] = 42;
            }

            if (n == 8)
            {
                arr[1] = 89;
                arr[7] = 66;
            }

            return arr;
        }

        // [SvmTest(92)]
        // public static int TestConnectionBetweenMultiIndicesAndValues(int[,] a, int i, int j, int f, int g)
        // {
        //     int x = a[i, j];
        //     int y = a[f, g];
        //     int res = 0;
        //     if (i == f && j == g && x != y)
        //         res = 1;
        //     return res;
        // }
        
        public class MyClass
        {
            public int x;
        }

        [SvmTest(88)]
        public static int LastRecordReachability(string[] a, string[] b, int i, string s)
        {
            a[i] = "1";
            b[1] = s;
            if (b[1] != s)
            {
                // unreachable
                return -1;
            }

            return 0;
        }

        [SvmTest(90)]
        public static int ArrayElementsAreReferences(MyClass[] a, int i, int j)
        {
            MyClass x = a[i];
            MyClass y = a[j];
            int res = 0;
            if (i == j && x != y)
                res = 1;
            return res;
        }

        [SvmTest(94)]
        public static bool ArraySymbolicUpdate(int i)
        {
            var array = new int[5];
            array[0] = 1;
            array[1] = 2;
            array[2] = 3;
            array[3] = 4;
            array[4] = 5;
            array[i] = 10;
            if (i == 0 && array[0] != 10)
                return false;
            else
                return true;
        }

        [SvmTest(95)]
        public static bool ArraySymbolicUpdate2(int i)
        {
            var array = new int[5];
            array[0] = 1;
            array[1] = 2;
            array[2] = 3;
            array[3] = 4;
            array[4] = 5;
            array[i] = 10;
            array[0] = 12;
            if (i == 0 && array[0] != 12)
                return false;
            else
                return true;
        }

        [SvmTest(97)]
        public static bool ArraySymbolicUpdate3(int i, int j)
        {
            var array = new int[5];
            array[0] = 1;
            array[1] = 2;
            array[2] = 3;
            array[3] = 4;
            array[4] = 5;
            array[i] = 10;
            array[0] = 12;
            array[j] = 42;
            if ((i == 0 && j == 0 && array[0] != 42) || (i == 0 && j == 2 && array[0] != 12) ||
                (i == 2 && j == 2 && array[2] != 42) || (i == 2 && j == 1 && array[i] != 10) ||
                (i == 2 && j == 1 && array[1] != 42))
                return false;
            else
                return true;
        }
        
        [SvmTest(100)]
        public static int TypeSolverCheck(int i, object[] l)
        {
            if (l[i] is int[] a)
                return a[0];

            return -12;
        }
        
        public class Person
        {
            public string FirstName { get; set; }
            public string LastName { get; init; }
        };
        
        [SvmTest(95)]
        public static int IteKeyWrite(int i)
        {
            var a = new Person[4];
            a[0] = new Person() {FirstName = "asd", LastName = "qwe"};
            a[3] = new Person() {FirstName = "zxc", LastName = "vbn"};
            var p = a[i];
            p.FirstName = "323";
            if (i == 0 && a[3].FirstName == "323")
            {
                return -1;
            }

            return 1;
        }
        
        class A
        {
            public int x;
            public int y;
        }
        class B
        {
        }
        public static int AccessLinearArray(object[] b, object x, int i)
        {
            b[i] = x;
            return i;
        }
        
        [SvmTest(100)]
        public static int ArrayExceptionsOrder(int f, object[] crr, object c, int i)
        {
            var arr = new A[10];
            object[] brr = arr;
            var a = new A();
            var b = new B();
            switch (f)
            {
                case 0:
                    AccessLinearArray(brr, a, 0); // ok
                    break;
                case 1:
                    AccessLinearArray(brr, b, 0); // pure: arr typ mis
                    break;
                case 2:
                    AccessLinearArray(brr, a, -1); // pure: index
                    break;
                case 3:
                    AccessLinearArray(null, a, 0); // pure: npe
                    break;
                case 4:
                    AccessLinearArray(null, a, -1); // npe < index
                    break;
                case 5:
                    AccessLinearArray(null, b, 0); // npe < arr typ mis
                    break;
                case 6:
                    AccessLinearArray(brr, b, -1); // index < arr typ mis
                    break;
                default:
                    AccessLinearArray(crr, c, i);
                    break;
            }
            return f;
        }

 }

public class Point
{
    public int x;
    public int y;
}
