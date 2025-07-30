package db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.testsms.model.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(TransactionEntity transaction);

    @Update
    void update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<TransactionEntity> getAll();

    @Query("DELETE FROM transactions")
    void clearAll();
}
