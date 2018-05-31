package cetc28.java.nlptools;
public class Pair<T, U>
{
    public T first;
    public U second;

    public Pair(T first, U second)
    {
        this.first = first;
        this.second = second;
    }

    public T getFirst()
    {
        return first;
    }

    public T getKey()
    {
        return first;
    }

    public U getSecond()
    {
        return second;
    }

    public U getValue()
    {
        return second;
    }

    @Override
    public String toString()
    {
        return first + "=" + second;
    }
}