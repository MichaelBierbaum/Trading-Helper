package de.bierbaum.tradinghelper

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stock_logos")
data class StockLogo(
    @PrimaryKey val symbol: String,
    val logoUrl: String
)

@Dao
interface LogoDao {
    @Query("SELECT * FROM stock_logos WHERE symbol = :symbol")
    suspend fun getLogo(symbol: String): StockLogo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogo(logo: StockLogo)

    @Query("SELECT * FROM stock_logos")
    fun getAllLogos(): Flow<List<StockLogo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogos(logos: List<StockLogo>)
}

@Database(entities = [StockLogo::class], version = 1)
abstract class LogoDatabase : RoomDatabase() {
    abstract fun logoDao(): LogoDao
}
