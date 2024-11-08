package com.example.cafeapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.cafeapp.ui.theme.CafeAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Initialize Firebase
        setContent {
            CafeApp()
        }
    }
}
@Composable
fun CafeApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var cartItems by remember { mutableStateOf(listOf<MenuItem>()) }
    var checkoutMessage by remember { mutableStateOf("") }
    var currentScreen by remember { mutableStateOf("menu") }
    var lastOrderId by remember { mutableStateOf<String?>(null) } // Store the last order ID
    val showOrderDialog = remember { mutableStateOf(false) } // Dialog for order confirmation

    CafeAppTheme {
        Scaffold { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                if (isLoggedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Welcome to CafeApp",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = {
                                logoutUser {
                                    isLoggedIn = false
                                    cartItems = emptyList()
                                    checkoutMessage = ""
                                    currentScreen = "menu"
                                }
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Logout")
                        }
                    }

                    // Render screens based on currentScreen state
                    when (currentScreen) {
                        "menu" -> MenuScreen(
                            onAddToCart = { menuItem -> cartItems = cartItems + menuItem },
                            onCheckout = { currentScreen = "checkout" },
                            cartItems = cartItems
                        )
                        "checkout" -> CheckoutScreen(
                            cartItems = cartItems,
                            onPlaceOrder = {
                                checkoutOrder(cartItems) { message, orderId ->
                                    checkoutMessage = message
                                    if (message == "Order placed successfully!" && orderId != null) {
                                        cartItems = emptyList()
                                        lastOrderId = orderId
                                        showOrderDialog.value = true // Show confirmation dialog
                                    }
                                }
                            },
                            onBackToMenu = { currentScreen = "menu" }
                        )
                        "feedback" -> lastOrderId?.let { orderId ->
                            FeedbackScreen(orderId = orderId) {
                                currentScreen = "menu"
                            }
                        }
                    }

                    // Order Confirmation Dialog
                    if (showOrderDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showOrderDialog.value = false },
                            title = { Text("Order Placed!") },
                            text = { Text("Do you want to provide feedback for your order?") },
                            confirmButton = {
                                Button(onClick = {
                                    showOrderDialog.value = false
                                    currentScreen = "feedback" // Navigate to Feedback screen
                                }) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                Button(onClick = {
                                    showOrderDialog.value = false
                                    currentScreen = "menu" // Navigate back to menu
                                }) {
                                    Text("No")
                                }
                            }
                        )
                    }

                    if (checkoutMessage.isNotEmpty()) {
                        Text(
                            text = checkoutMessage,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                }
            }
        }
    }
}


@Composable
fun FeedbackScreen(orderId: String, onFeedbackSubmitted: () -> Unit) {
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Submit Feedback",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Rating input
        Text(text = "Rating (1-5):", style = MaterialTheme.typography.bodyLarge)
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            for (star in 1..5) {
                TextButton(onClick = { rating = star }) {
                    Text(
                        text = if (star <= rating) "★" else "☆",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        // Comment input
        TextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Your Comment") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Submit Feedback Button
        Button(
            onClick = {
                submitFeedback(orderId, rating, comment) { message ->
                    statusMessage = message
                    if (message == "Feedback submitted successfully!") {
                        onFeedbackSubmitted()
                    }
                }
            },
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text("Submit Feedback")
        }

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun submitFeedback(orderId: String, rating: Int, comment: String, onResult: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId == null) {
        onResult("User not logged in.")
        return
    }

    if (rating < 1 || rating > 5) {
        onResult("Please provide a rating between 1 and 5.")
        return
    }

    val feedback = hashMapOf(
        "userId" to userId,
        "orderId" to orderId,
        "rating" to rating,
        "comment" to comment,
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("Feedback").add(feedback)
        .addOnSuccessListener {
            onResult("Feedback submitted successfully!")
        }
        .addOnFailureListener { e ->
            onResult("Failed to submit feedback: ${e.localizedMessage}")
        }
}


fun logoutUser(onLogout: () -> Unit) {
    FirebaseAuth.getInstance().signOut()
    onLogout()
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (isLoginMode) "Login" else "Register", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            if (isLoginMode) {
                loginUser(email, password, onLoginSuccess) { message ->
                    statusMessage = message
                }
            } else {
                registerUser(email, password) { success, message ->
                    statusMessage = message
                    if (success) onLoginSuccess()
                }
            }
        }) {
            Text(if (isLoginMode) "Login" else "Register")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = statusMessage, color = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login")
        }
    }
}

fun loginUser(email: String, password: String, onLoginSuccess: () -> Unit, onError: (String) -> Unit) {
    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onLoginSuccess()
            } else {
                onError(task.exception?.message ?: "Login failed")
            }
        }
}

fun registerUser(email: String, password: String, onResult: (Boolean, String) -> Unit) {
    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true, "Registration successful!")
            } else {
                onResult(false, task.exception?.message ?: "Registration failed")
            }
        }
}

@Composable
fun MenuScreen(onAddToCart: (MenuItem) -> Unit, onCheckout: () -> Unit, cartItems: List<MenuItem>) {
    val db = FirebaseFirestore.getInstance()
    var menuItems by remember { mutableStateOf(listOf<MenuItem>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("MenuItems").get()
            .addOnSuccessListener { result ->
                val items = result.map { document ->
                    document.toObject(MenuItem::class.java)
                }
                menuItems = items
                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Title for the Menu Screen
        Text(
            text = "Our Menu",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) { // Allow space for button at bottom
                items(menuItems) { menuItem ->
                    MenuItemCard(menuItem, onAddToCart)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display the "View Cart" button only if there are items in the cart
        if (cartItems.isNotEmpty()) {
            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("View Cart (${cartItems.size} items) - Checkout")
            }
        }
    }
}



@Composable
fun MenuItemCard(menuItem: MenuItem, onAddToCart: (MenuItem) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Item Name
            Text(
                text = menuItem.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Item Description
            Text(
                text = menuItem.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Row for Price and Add to Cart Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Price
                Text(
                    text = "$${menuItem.price}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Add to Cart Button
                Button(onClick = { onAddToCart(menuItem) }) {
                    Text("Add to Cart")
                }
            }
        }
    }
}


@Composable
fun CheckoutScreen(
    cartItems: List<MenuItem>,
    onPlaceOrder: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val totalPrice = cartItems.sumOf { it.price }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Checkout Summary",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(cartItems) { item ->
                Text(
                    text = "${item.name} - $${item.price}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Total: $${"%.2f".format(totalPrice)}",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPlaceOrder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Place Order")
        }

        TextButton(onClick = onBackToMenu) {
            Text("Back to Menu")
        }
    }
}


fun checkoutOrder(cartItems: List<MenuItem>, onResult: (String, String?) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId == null) {
        Log.e("CheckoutOrder", "User not logged in.")
        return onResult("User not logged in. Please log in to place an order.", null)
    }

    if (cartItems.isEmpty()) {
        Log.e("CheckoutOrder", "Cart is empty.")
        return onResult("Your cart is empty. Please add items to the cart before checking out.", null)
    }

    val order = hashMapOf(
        "userId" to userId,
        "items" to cartItems.map { it.name },
        "total" to cartItems.sumOf { it.price }
    )

    db.collection("Orders").add(order)
        .addOnSuccessListener { documentReference ->
            Log.d("CheckoutOrder", "Order placed successfully")
            onResult("Order placed successfully!", documentReference.id) // Return order ID
        }
        .addOnFailureListener { e ->
            Log.e("CheckoutOrder", "Error placing order", e)
            onResult("Failed to place order: ${e.localizedMessage ?: "Unknown error"}", null)
        }
}

