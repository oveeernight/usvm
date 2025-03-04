namespace samples;

public class Classes
{
    public int ConcreteObjectDefaultValue()
    {
        var user = new User();
        var id = user.Id;
        if (id != 0)
        {
            return -1;
        }

        return 0;
    }
}

public class User
{
    public int Id;
} 