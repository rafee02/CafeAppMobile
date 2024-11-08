package com.example.cafeapp

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

data class MenuItem(val name: String, val description: String, val price: Double)

class CafeAppTests {

    private lateinit var cartItems: MutableList<MenuItem>

    @Before
    fun setup() {
        cartItems = mutableListOf()
    }

    // Authentication Logic Tests (Basic Logic Validation)
    @Test
    fun `valid login sets user as logged in`() {
        val email = "test@example.com"
        val password = "password123"

        val loginResult = loginUser(email, password)
        assertTrue(loginResult)
    }

    @Test
    fun `invalid login fails`() {
        val email = "wrong@example.com"
        val password = "wrongpassword"

        val loginResult = loginUser(email, password)
        assertFalse(loginResult)
    }

    // Cart Management Tests
    @Test
    fun `adding item to cart increases cart size`() {
        val menuItem = MenuItem("Latte", "A rich coffee", 3.99)

        cartItems.add(menuItem)

        assertEquals(1, cartItems.size)
        assertEquals("Latte", cartItems[0].name)
    }

    @Test
    fun `multiple items can be added to the cart`() {
        val item1 = MenuItem("Espresso", "Strong coffee", 2.99)
        val item2 = MenuItem("Cappuccino", "Coffee with milk foam", 3.49)

        cartItems.add(item1)
        cartItems.add(item2)

        assertEquals(2, cartItems.size)
    }

    @Test
    fun `removing item from cart decreases cart size`() {
        val item1 = MenuItem("Espresso", "Strong coffee", 2.99)
        val item2 = MenuItem("Cappuccino", "Coffee with milk foam", 3.49)

        cartItems.add(item1)
        cartItems.add(item2)
        cartItems.remove(item1)

        assertEquals(1, cartItems.size)
        assertEquals("Cappuccino", cartItems[0].name)
    }

    // Order Placement Tests
    @Test
    fun `placing order with items returns success`() {
        val cartItems = listOf(
            MenuItem("Latte", "A rich coffee", 3.99),
            MenuItem("Espresso", "Strong coffee", 2.99)
        )

        val orderResult = placeOrder(cartItems)
        assertEquals("Order placed successfully!", orderResult)
    }

    @Test
    fun `placing order with empty cart returns failure`() {
        val cartItems = emptyList<MenuItem>()

        val orderResult = placeOrder(cartItems)
        assertEquals("Your cart is empty. Please add items to the cart before checking out.", orderResult)
    }

    // Supplementary Logic
    fun loginUser(email: String, password: String): Boolean {
        val validEmail = "test@example.com"
        val validPassword = "password123"
        return email == validEmail && password == validPassword
    }

    fun placeOrder(cartItems: List<MenuItem>): String {
        return if (cartItems.isEmpty()) {
            "Your cart is empty. Please add items to the cart before checking out."
        } else {
            "Order placed successfully!"
        }
    }
}
