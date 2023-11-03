package betterquesting.api2.storage;

import java.util.List;
/*数据库的接口*/
public interface IDatabase<T>
{
    int nextID();   //取下一个物品

    DBEntry<T> add(int id, T value);    //添加
    boolean removeID(int key);          //根据ID移除
    boolean removeValue(T value);

    int getID(T value);
    T getValue(int id);

    int size();
    void reset();

    List<DBEntry<T>> getEntries();
    List<DBEntry<T>> bulkLookup(int... keys);
}
