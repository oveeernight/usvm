namespace samples;

public class SvmTestAttribute : Attribute
{
    public int ExpectedCoverage { get; set; }

    public SvmTestAttribute() { }

    public SvmTestAttribute(int expectedCoverage = 100)
    {
        ExpectedCoverage = expectedCoverage;
    }
}