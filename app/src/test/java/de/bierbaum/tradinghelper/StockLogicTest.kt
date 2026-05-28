package de.bierbaum.tradinghelper

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class StockLogicTest {

    @Test
    fun testGoldenCrossLogic() {
        // isGoldenCross: D10 >= 0, D50 >= 0, abs(D200) <= 10%
        
        val stock1 = Stock(name = "Test", symbol = "T1", price = 105.0, sma10 = 100.0, sma50 = 100.0, sma200 = 100.0)
        // D10=5%, D50=5%, D200=5% -> Golden Cross
        assertTrue(stock1.isGoldenCross)
        assertFalse(stock1.isDeathCross)

        val stock2 = Stock(name = "Test", symbol = "T2", price = 105.0, sma10 = 110.0, sma50 = 110.0, sma200 = 100.0)
        // D10 < 0 -> Not Golden
        assertFalse(stock2.isGoldenCross)
        assertTrue(stock2.isDeathCross) // D10 < 0, D50 < 0, D200 < 10%

        val stock3 = Stock(name = "Test", symbol = "T3", price = 120.0, sma10 = 110.0, sma50 = 110.0, sma200 = 100.0)
        // D10=9%, D50=9%, D200=20% -> Not Golden (D200 > 10%)
        assertFalse(stock3.isGoldenCross)
    }

    @Test
    fun testDeathCrossLogic() {
        // isDeathCross: D10 <= 0, D50 <= 0, abs(D200) <= 10%
        
        val stock1 = Stock(name = "Test", symbol = "T1", price = 95.0, sma10 = 100.0, sma50 = 100.0, sma200 = 100.0)
        // D10=-5%, D50=-5%, D200=-5% -> Death Cross
        assertTrue(stock1.isDeathCross)
        assertFalse(stock1.isGoldenCross)

        val stock2 = Stock(name = "Test", symbol = "T2", price = 95.0, sma10 = 90.0, sma50 = 90.0, sma200 = 100.0)
        // D10 > 0 -> Not Death
        assertFalse(stock2.isDeathCross)
        assertTrue(stock2.isGoldenCross)
    }
}
