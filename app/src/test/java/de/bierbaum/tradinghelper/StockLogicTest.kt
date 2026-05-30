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
    fun testStatusAndAnimalLogic() {
        // Test Animals
        val bullAdult = Stock(name = "Bull", symbol = "B1", price = 110.0, sma10 = 105.0, sma50 = 100.0, sma200 = 90.0)
        assertEquals(Animals.BullAdult, bullAdult.getAnimal)

        val bearBaby = Stock(name = "Bear", symbol = "B2", price = 90.0, sma10 = 95.0, sma50 = 85.0, sma200 = 100.0)
        // d200 = -10%, d10 = -5%, d50 = +5.8% -> Not Adult Bear -> BearBaby
        assertEquals(Animals.BearBaby, bearBaby.getAnimal)

        // Test Status
        val star75 = Stock(name = "Star", symbol = "S1", price = 85.0, sma200 = 100.0)
        // D200 = -15% -> abs(D200) = 15%. Constants.TRESHOLD_CROSS = 10.0.
        // abs200 (15) <= 2 * 10 (20) -> Star75
        assertEquals(StockStatus.Star75, star75.getStatus)
        
        val golden = Stock(name = "Gold", symbol = "G1", price = 105.0, sma10 = 102.0, sma50 = 101.0, sma200 = 100.0)
        assertEquals(StockStatus.GoldenCross, golden.getStatus)
    }
}
