package io.jans.chip.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.jans.chip.modal.Fido.config.FidoConfiguration;
@Dao
public interface FidoConfigurationDao {
    @Insert
    void insert(FidoConfiguration opConfiguration);

    @Update
    void update(FidoConfiguration opConfiguration);

    @Query("SELECT * FROM FIDO_CONFIGURATION")
    List<FidoConfiguration> getAll();

    @Query("DELETE FROM FIDO_CONFIGURATION")
    void deleteAll();
}
